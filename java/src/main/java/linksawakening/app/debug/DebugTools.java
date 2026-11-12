package linksawakening.app.debug;

import java.util.Objects;
import linksawakening.app.state.DebugState;
import linksawakening.state.PlayerState;
import linksawakening.world.room.RoomSession;

/** Session-local owner of lazily created debug menus and their presentation. */
public final class DebugTools {
    private final DebugState state;
    private final PlayerState player;
    private final RoomSession rooms;
    private final DebugTeleporter teleporter;

    public DebugTools(DebugState state, PlayerState player, RoomSession rooms, DebugTeleporter teleporter) {
        this.state = Objects.requireNonNull(state);
        this.player = Objects.requireNonNull(player);
        this.rooms = Objects.requireNonNull(rooms);
        this.teleporter = Objects.requireNonNull(teleporter);
    }

    public DebugMenu menu() {
        if (state.debugMenu == null) state.debugMenu = new DebugMenu(player, rooms, teleporter);
        return state.debugMenu;
    }

    public void render(byte[] framebuffer) {
        if (state.debugMenu != null && state.debugMenu.isOpen()) {
            if (state.debugMenuOverlay == null) state.debugMenuOverlay = new DebugMenuOverlay();
            state.debugMenuOverlay.render(state.debugMenu, framebuffer);
        }
    }
}
