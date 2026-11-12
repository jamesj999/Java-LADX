package linksawakening.ui;

/**
 * DX InventoryEntryPoint ($20:5904), not the unused DMG $02:61C6 slide.
 * Durations include the offscreen LCD/VBlank load stalls: PyBoy house route
 * frames 2959 (request), 2984 (load1), 2995 (WY=0), 3011 (interactive),
 * 3087 (close), 3103 (world/WY=$80), 3110 (rWY=$80).
 * This is a bounded presentation schedule: CPU/LCD-phase-dependent load stalls
 * and the corresponding global hFrameCounter pauses still belong to the session
 * clock, not this UI state. In particular, other ROM returns can take 8 rather
 * than the house trace's 7 frames in WORLD_TILESET.
 */
public final class InventoryTransition {
    public enum State {
        WORLD,
        INITIAL,
        MAP_FADE_OUT,
        LOAD1,
        LOAD2,
        LOAD3,
        LOAD4,
        LOAD5,
        FADE_IN,
        INTERACTIVE,
        FADE_OUT,
        WORLD_TILESET,
        WORLD_TILEMAP,
        WORLD_HUD,
        WORLD_BUTTONS,
        WORLD_FINISH,
        WORLD_FADE_IN
    }

    private State state = State.WORLD;
    private int frames, fadeCalls;
    private boolean firstTick;
    private boolean minimap;
    private int windowY = 128, displayedWindowY = 128;

    public State state() {
        return state;
    }

    public int windowY() {
        return windowY;
    }

    public int displayedWindowY() {
        return displayedWindowY;
    }

    public boolean blocksWorld() {
        return state != State.WORLD;
    }

    public boolean worldHandlerActive() {
        return state == State.WORLD || state == State.WORLD_FADE_IN;
    }

    public boolean interactive() {
        return state == State.INTERACTIVE;
    }

    public boolean appearing() {
        return state == State.INITIAL || state == State.MAP_FADE_OUT;
    }

    public boolean worldSpritesVisible() {
        return state == State.WORLD || appearing() || state == State.WORLD_FADE_IN;
    }

    public void open(boolean minimap) {
        if (state != State.WORLD) return;
        this.minimap = minimap;
        enter(State.INITIAL);
        firstTick = true; // $02:6171: dispatch changes after this frame's world handler.
    }

    public void close() {
        if (!interactive()) return;
        enter(State.FADE_OUT);
        fadeCalls = 0; // CloseInventory $20:6436-$20:6443.
        firstTick = true;
    }

    private void enter(State next) {
        state = next;
        frames = 0;
    }

    public void tick() {
        if (firstTick) {
            firstTick = false;
            return;
        }
        frames++;
        switch (state) {
            case INITIAL -> {
                // LoadSirenInstruments ($01:6BB5), or 12 minimap VBlanks ($00:0826).
                if (frames == (minimap ? 12 : 10)) {
                    enter(State.MAP_FADE_OUT);
                    fadeCalls = 1; // InventoryInitialHandler falls through ($20:5940).
                }
            }
            case MAP_FADE_OUT -> {
                if (++fadeCalls == 16) enter(State.LOAD1); // $20:594D clears appearing.
            }
            case LOAD1 -> enter(State.LOAD2); // $20:5AEB, LCDOff/draw commands.
            case LOAD2 -> {
                if (frames == 3) enter(State.LOAD3);
            }
            case LOAD3 -> enter(State.LOAD4); // $20:5D25-$20:5D51.
            case LOAD4 -> {
                if (frames == 5) enter(State.LOAD5);
            } // ReloadColorDungeonNpcTiles.
            case LOAD5 -> {
                windowY = displayedWindowY = 0; // $20:5E1A-$20:5E1B.
                enter(State.FADE_IN);
                fadeCalls = 0;
            }
            case FADE_IN -> {
                if (++fadeCalls == 16) enter(State.INTERACTIVE);
            }
            case FADE_OUT -> {
                if (++fadeCalls == 16) {
                    windowY = 128; // $20:6638-$20:6643; register copied on return to loop.
                    enter(State.WORLD_TILESET);
                }
            }
                // InventoryFadeOutHandler resumes WorldHandlerEntryPoint at subtype 2,
                // $01:44B4-$01:451E. House-route LCD load stalls are 7,4,5,1,1 frames.
            case WORLD_TILESET -> {
                if (frames == 7) {
                    displayedWindowY = 128;
                    enter(State.WORLD_TILEMAP);
                }
            }
            case WORLD_TILEMAP -> {
                if (frames == 4) enter(State.WORLD_HUD);
            }
            case WORLD_HUD -> {
                if (frames == 5) enter(State.WORLD_BUTTONS);
            }
            case WORLD_BUTTONS -> enter(State.WORLD_FINISH);
            case WORLD_FINISH -> {
                enter(State.WORLD_FADE_IN);
                fadeCalls = 0;
            }
            case WORLD_FADE_IN -> {
                if (++fadeCalls == 16) enter(State.WORLD);
            }
            default -> {}
        }
    }
    /** RGB555 palette arithmetic at $20:6C4F/$20:6C7A: alternate BG/OBJ each call. */
    public int color(int rgb, boolean object) {
        if (state == State.WORLD || state == State.INTERACTIVE || state == State.INITIAL) return rgb;
        boolean out = state == State.MAP_FADE_OUT || state == State.FADE_OUT;
        boolean in = state == State.FADE_IN || state == State.WORLD_FADE_IN;
        if (!out && !in) return 0xFFFFFF;
        int calls = (fadeCalls + (object ? 0 : 1)) / 2;
        int result = 0;
        for (int shift = 16; shift >= 0; shift -= 8) {
            int target = (rgb >> shift & 255) >> 3;
            int value = out ? Math.min(31, target + calls * 4) : Math.max(target, 31 - calls * 4);
            result |= ((value << 3) | (value >> 2)) << shift;
        }
        return result;
    }
}
