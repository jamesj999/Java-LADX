package linksawakening.world;

/** Room warp and Manbo transitions, including the blank WorldLoad interval. */
public final class TransitionController {

    public enum State { IDLE, FADING_OUT, ROOM_LOAD, FADING_IN, MANBO_IN, MANBO_ROOM_LOAD, MANBO_OUT }

    /** Frames per "step" — matches the BGP cycle cadence at bank20.asm:2099. */
    private static final int FRAMES_PER_STEP = 4;

    /** Four discrete darkness steps from clear (0) to fully faded (3). */
    public static final int MAX_FADE_STEP = 3;

    private static final int STEPS = MAX_FADE_STEP + 1;
    private static final int FADE_FRAMES = FRAMES_PER_STEP * STEPS; // 16
    // Measured on the A2 -> A3 house doorway, from the first map fade frame.
    private static final int ROOM_CHANGE_TICK = 3;
    private static final int ROOM_LOAD_FRAMES = 28;
    public static final int MANBO_TRANSITION_FRAMES = 0xC0;
    // LinkMotionMapFadeInHandler seeds $30 before the render loop advances it.
    public static final int MANBO_OUT_INITIAL_FRAME = 0x31;
    // WorldLoad0 changes rooms after the 16-frame map fade. WorldLoad6 selects music.
    private static final int MANBO_ROOM_CHANGE_TICK = 16;
    private static final int MANBO_WORLD_READY_TICK = 43;
    private boolean doorMusicFade;
    private boolean keepDoorMusic;
    private int roomChangedAt;
    private int fadeOutTimer;
    private int fadeInTimer;
    private int volume = 7;
    private boolean manboDestinationIndoor;
    private Runnable pendingRoomReady;

    private State state = State.IDLE;
    private int frameInPhase;
    private Runnable pendingLoad;
    private boolean manboFrameHeld;
    private ManboTransitionWave manboTransitionWave;

    public TransitionController() {
    }

    public TransitionController(byte[] romData) {
        setRomData(romData);
    }

    /** Supplies the ROM-backed wave table used by Manbo's non-interactive effect. */
    public void setRomData(byte[] romData) {
        manboTransitionWave = romData == null ? null : new ManboTransitionWave(romData);
    }

    /**
     * Start a fade-out. When the fade-out completes, {@code loadNewRoom} is
     * invoked (on the main thread) to swap rooms, then the fade-in runs.
     */
    public void startFadeOut(Runnable loadNewRoom) {
        if (state != State.IDLE) {
            return;
        }
        doorMusicFade = false;
        keepDoorMusic = false;
        roomChangedAt = -1;
        state = State.FADING_OUT;
        frameInPhase = 0;
        pendingLoad = loadNewRoom;
    }

    void startDoorFade(boolean fadeMusic, Runnable loadNewRoom) {
        if (state != State.IDLE) return;
        startFadeOut(loadNewRoom);
        doorMusicFade = fadeMusic;
        keepDoorMusic = !fadeMusic;
        // The starting frame reaches the audio dispatcher after the warp request.
        if (fadeMusic) fadeOutTimer = 0x30 - 1;
    }

    /** The sound dispatcher advances both timers after gameplay, even during a second warp. */
    private void advanceDoorVolume() {
        if (state == State.ROOM_LOAD && roomChangedAt < 0 && doorMusicFade && volume != 0) {
            // FadeOutMusic holds the final visual step and accelerates the sound fade.
            fadeOutTimer = (fadeOutTimer & 0xFC) | 1;
        }
        if (fadeOutTimer > 0 && (--fadeOutTimer & 3) == 0 && volume > 0) volume--;
        if (fadeInTimer > 0 && (--fadeInTimer & 1) == 0 && volume < 7) volume++;
    }

    public int doorVolume() {
        return doorMusicFade && (state == State.FADING_OUT || state == State.ROOM_LOAD
            || state == State.FADING_IN) ? volume : -1;
    }

    public boolean continuesMusicOnArrival() {
        return keepDoorMusic && state == State.FADING_IN;
    }

    public boolean stopDoorMusic() {
        return doorMusicFade && state == State.ROOM_LOAD && roomChangedAt >= 0 && frameInPhase == roomChangedAt + 1;
    }

    /**
     * Starts Manbo's non-interactive transition. The room changes while the LCD is blank.
     */
    public boolean startManboIn(boolean destinationIndoor, Runnable onRoomChange) {
        if (state != State.IDLE) {
            return false;
        }
        state = State.MANBO_IN;
        frameInPhase = 1;
        manboFrameHeld = false;
        manboDestinationIndoor = destinationIndoor;
        pendingLoad = onRoomChange;
        return true;
    }

    public void tick() {
        tickTransition();
        advanceDoorVolume();
    }

    private void tickTransition() {
        if (state == State.IDLE) {
            return;
        }
        if (state == State.MANBO_IN || state == State.MANBO_OUT) {
            tickManboEffect();
            return;
        }
        frameInPhase++;
        if (state == State.MANBO_ROOM_LOAD) {
            if (frameInPhase == MANBO_ROOM_CHANGE_TICK) {
                Runnable load = pendingLoad;
                pendingLoad = null;
                if (load != null) {
                    load.run();
                }
            } else if (frameInPhase == MANBO_WORLD_READY_TICK && pendingRoomReady != null) {
                Runnable ready = pendingRoomReady;
                pendingRoomReady = null;
                ready.run();
            } else if (frameInPhase > MANBO_WORLD_READY_TICK) {
                state = State.MANBO_OUT;
                frameInPhase = MANBO_OUT_INITIAL_FRAME;
            }
        } else if (state == State.FADING_OUT && frameInPhase >= FADE_FRAMES) {
            state = State.ROOM_LOAD;
            frameInPhase = 0;
        } else if (state == State.ROOM_LOAD) {
            boolean readyToChange = doorMusicFade ? volume == 0 : frameInPhase >= ROOM_CHANGE_TICK;
            if (roomChangedAt < 0 && readyToChange) {
                roomChangedAt = frameInPhase;
                if (pendingLoad != null) pendingLoad.run();
                pendingLoad = null;
            } else if (roomChangedAt >= 0 && frameInPhase == roomChangedAt + ROOM_LOAD_FRAMES) {
                if (doorMusicFade) {
                    fadeOutTimer = 0;
                    fadeInTimer = 0x38;
                }
                state = State.FADING_IN;
                frameInPhase = 0;
                if (pendingRoomReady != null) {
                    pendingRoomReady.run();
                    pendingRoomReady = null;
                }
            }
        } else if (state == State.FADING_IN && frameInPhase >= FADE_FRAMES) {
            state = State.IDLE;
            frameInPhase = 0;
        }
    }

    private void tickManboEffect() {
        // The CGB trace repeats logical frames when the effect spans a VBlank.
        // The ROM's render loop still terminates both effects at logical $C0.
        boolean repeatFrame = state == State.MANBO_IN
            ? frameInPhase >= 0x60 && frameInPhase < 0x80
            : (!manboDestinationIndoor && frameInPhase >= 0x48 && frameInPhase <= 0x58 && frameInPhase % 8 == 0)
                || (frameInPhase >= 0x60 && frameInPhase < 0x80 && frameInPhase % 4 == 0)
                || (!manboDestinationIndoor && frameInPhase == 0x90) || frameInPhase == 0xA8;
        if (repeatFrame && !manboFrameHeld) {
            manboFrameHeld = true;
            return;
        }
        manboFrameHeld = false;
        if (++frameInPhase == MANBO_TRANSITION_FRAMES) {
            state = state == State.MANBO_IN ? State.MANBO_ROOM_LOAD : State.IDLE;
            frameInPhase = 0;
        }
    }

    /** WorldLoad6 starts the destination score after the map and tiles have loaded. */
    public void whenRoomReady(Runnable action) {
        if (state == State.ROOM_LOAD || (state == State.MANBO_ROOM_LOAD && frameInPhase < MANBO_WORLD_READY_TICK)) pendingRoomReady = action;
        else action.run();
    }

    public boolean isManboTransition() {
        return state == State.MANBO_IN || state == State.MANBO_OUT || state == State.MANBO_ROOM_LOAD;
    }

    public boolean isHoldingManboFrame() {
        return manboFrameHeld && (state == State.MANBO_IN || state == State.MANBO_OUT);
    }

    public boolean isActive() {
        return state != State.IDLE;
    }

    public boolean isInputBlocked() {
        return state != State.IDLE;
    }

    public State state() {
        return state;
    }

    /** Current frame within the active transition effect or fade phase. */
    public int transitionFrame() {
        return frameInPhase;
    }

    /** Returns the source scanline offsets while the Manbo effect is active. */
    public int[] manboWaveOffsets(int scanlineCount) {
        if ((state != State.MANBO_IN && state != State.MANBO_OUT)
            || manboTransitionWave == null) {
            return null;
        }
        return manboTransitionWave.offsetsForFrame(frameInPhase, scanlineCount,
            state == State.MANBO_OUT);
    }

    public int fadeLevel() {
        int amount = Math.abs(RoomPaletteFade.uploadedStep(this, 0));
        return (amount * MAX_FADE_STEP + 30) / 31;
    }

    public int[][] applyFade(int[][] sourcePalettes) {
        return isActive() ? RoomPaletteFade.palettes(this, sourcePalettes, false) : sourcePalettes;
    }

    public int[][] applyObjectFade(int[][] sourcePalettes) {
        return isActive() ? RoomPaletteFade.palettes(this, sourcePalettes, true) : sourcePalettes;
    }
}
