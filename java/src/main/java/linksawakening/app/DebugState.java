package linksawakening.app;

import static linksawakening.world.room.RoomConstants.*;

/** Mutable debug state owned by one session. */
public final class DebugState {
    public linksawakening.app.debug.DebugMenu debugMenu;
    public linksawakening.app.debug.DebugMenuOverlay debugMenuOverlay;
    public boolean debugNoClip;
}
