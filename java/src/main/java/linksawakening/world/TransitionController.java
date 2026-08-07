package linksawakening.world;

/**
 * Room-to-room fade transition state machine, ported from the LADX disassembly's
 * {@code ApplyMapFadeOutTransition} path (bank0.asm:866) and its per-frame
 * handler {@code LinkMotionMapFadeOutHandler} (bank0.asm:2723).
 *
 * <p>Timing model matches the original: every 4 frames the BG palette darkens
 * one "step", where a step on DMG decrements each non-zero 2-bit color in the
 * {@code BGP} register by 1 (see {@code FadeOutMusic}, bank20.asm:2092-2140).
 * Four steps = 16 frames = fully faded. The original DMG progression
 * {@code $E4 → $94 → $54 → $00} lands on $00 (all colors map to white), so on
 * DMG this is a fade-to-white. On CGB the equivalent effect is brightening
 * each palette color toward white over four discrete steps.
 *
 * <p>State machine:
 * <pre>
 *   IDLE --startFadeOut()--> FADING_OUT (16 frames) --[midpoint]--> (callback loads room)
 *     --> FADING_IN (16 frames) --> IDLE
 * </pre>
 *
 * <p>During FADING_OUT and FADING_IN, input is blocked and the caller should
 * apply {@link #fadeLevel()} to the live BG palettes before rendering.
 */
public final class TransitionController {

    public enum State { IDLE, FADING_OUT, FADING_IN, MANBO_IN, MANBO_OUT }

    /** Frames per "step" — matches the BGP cycle cadence at bank20.asm:2099. */
    private static final int FRAMES_PER_STEP = 4;

    /** Four discrete darkness steps from clear (0) to fully faded (3). */
    public static final int MAX_FADE_STEP = 3;

    private static final int STEPS = MAX_FADE_STEP + 1;
    private static final int FADE_FRAMES = FRAMES_PER_STEP * STEPS; // 16
    private static final int MANBO_FADE_START = 0x60;
    public static final int MANBO_TRANSITION_FRAMES = 0xC0;
    public static final int MANBO_OUT_INITIAL_FRAME = 0x30;

    private State state = State.IDLE;
    private int frameInPhase;
    private Runnable pendingLoad;
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
        state = State.FADING_OUT;
        frameInPhase = 0;
        pendingLoad = loadNewRoom;
    }

    /** Starts the source's non-interactive 0xC0-frame Manbo entry effect. */
    public boolean startManboIn(Runnable onComplete) {
        if (state != State.IDLE) {
            return false;
        }
        state = State.MANBO_IN;
        frameInPhase = 0;
        pendingLoad = onComplete;
        return true;
    }

    public void tick() {
        if (state == State.IDLE) {
            return;
        }
        frameInPhase++;
        if (state == State.MANBO_IN && frameInPhase >= MANBO_TRANSITION_FRAMES) {
            if (pendingLoad != null) {
                pendingLoad.run();
                pendingLoad = null;
            }
            state = State.MANBO_OUT;
            frameInPhase = MANBO_OUT_INITIAL_FRAME;
        } else if (state == State.MANBO_OUT && frameInPhase >= MANBO_TRANSITION_FRAMES) {
            state = State.IDLE;
            frameInPhase = 0;
        } else if (state == State.FADING_OUT && frameInPhase >= FADE_FRAMES) {
            if (pendingLoad != null) {
                pendingLoad.run();
                pendingLoad = null;
            }
            state = State.FADING_IN;
            frameInPhase = 0;
        } else if (state == State.FADING_IN && frameInPhase >= FADE_FRAMES) {
            state = State.IDLE;
            frameInPhase = 0;
        }
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

    /**
     * Current fade step. 0 = no fade (original palette), MAX_FADE_STEP = fully
     * faded. Symmetric: FADING_OUT ramps 0→MAX; FADING_IN ramps MAX→0.
     */
    public int fadeLevel() {
        switch (state) {
            case FADING_OUT:
                return Math.min(MAX_FADE_STEP, frameInPhase / FRAMES_PER_STEP);
            case FADING_IN:
                return Math.max(0, MAX_FADE_STEP - frameInPhase / FRAMES_PER_STEP);
            case MANBO_IN:
                if (frameInPhase < MANBO_FADE_START) {
                    return 0;
                }
                return Math.min(MAX_FADE_STEP,
                    (frameInPhase - MANBO_FADE_START) * STEPS
                        / (MANBO_TRANSITION_FRAMES - MANBO_FADE_START));
            case MANBO_OUT:
                if (frameInPhase < MANBO_FADE_START) {
                    return MAX_FADE_STEP;
                }
                return Math.max(0, MAX_FADE_STEP
                    - (frameInPhase - MANBO_FADE_START) * STEPS
                        / (MANBO_TRANSITION_FRAMES - MANBO_FADE_START));
            case IDLE:
            default:
                return 0;
        }
    }

    /**
     * Apply the current fade level to a palette array. Produces a new array so
     * the caller's source palettes remain untouched. On DMG the equivalent op
     * decrements each non-zero 2-bit BGP slot toward 0 (white); here we
     * brighten each CGB RGB color toward full-white in {@code STEPS} steps.
     */
    public int[][] applyFade(int[][] sourcePalettes) {
        int level = fadeLevel();
        if (level == 0) {
            return sourcePalettes;
        }
        // Step 3 = fully faded; each step nudges 1/STEPS of the way to white.
        int num = level;
        int den = STEPS;

        int[][] out = new int[sourcePalettes.length][];
        for (int p = 0; p < sourcePalettes.length; p++) {
            int[] src = sourcePalettes[p];
            int[] dst = new int[src.length];
            for (int c = 0; c < src.length; c++) {
                dst[c] = fadeTowardWhite(src[c], num, den);
            }
            out[p] = dst;
        }
        return out;
    }

    private static int fadeTowardWhite(int rgb, int num, int den) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        r = r + (0xFF - r) * num / den;
        g = g + (0xFF - g) * num / den;
        b = b + (0xFF - b) * num / den;
        return (r << 16) | (g << 8) | b;
    }
}
