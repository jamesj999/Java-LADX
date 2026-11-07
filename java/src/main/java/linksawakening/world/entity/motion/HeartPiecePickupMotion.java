package linksawakening.world.entity.motion;

import linksawakening.world.entity.event.HeartPiecePickupUpdate;


import linksawakening.world.transition.*;

/** HeartPieceState1..8Handler ($03:5A6D), including the locked meter animation. */
public final class HeartPiecePickupMotion {
    private int state = 1;
    private int inertia;



    public HeartPiecePickupUpdate advance(int countdown, boolean dialogActive, int pieces) {
        boolean locked = state == 4 || state == 5;
        int dialog = -1, tiles = 0;
        boolean increment = false, container = false, complete = false;
        switch (state) {
            case 1 -> { if (countdown == 0) state = 2; }
            case 2 -> { tiles = 3; state = 3; }
            case 3 -> { tiles = 4; state = 4; }
            case 4 -> { dialog = 0x4F; state = 5; }
            case 5 -> {
                increment = ++inertia == 0x38;
                if (inertia == 0xA8) state = 6;
            }
            case 6 -> {
                if (!dialogActive) {
                    if (pieces == 4) { container = true; dialog = 0x50; }
                    state = 7;
                }
            }
            case 7 -> { if (!dialogActive) { tiles = 5; state = 8; } }
            case 8 -> { tiles = 6; complete = true; state = 9; }
            default -> { }
        }
        return new HeartPiecePickupUpdate(dialog, tiles, increment, container, locked, complete);
    }

    public boolean showMeter() { return state >= 5 && state <= 7; }

    public boolean active() { return state < 9; }
}
