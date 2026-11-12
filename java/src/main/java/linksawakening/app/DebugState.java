package linksawakening.app;

import static linksawakening.world.room.RoomConstants.*;

import linksawakening.app.debug.DebugMenu;
import linksawakening.app.debug.DebugMenuOverlay;

/** Mutable debug state owned by one session. */
public final class DebugState {
    public DebugMenu debugMenu;
    public DebugMenuOverlay debugMenuOverlay;
    public boolean debugNoClip;
}
