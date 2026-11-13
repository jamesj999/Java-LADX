package linksawakening.app.debug;

import java.util.Objects;
import linksawakening.state.PlayerState;
import linksawakening.world.room.RoomSession;

/** Session-local owner of lazily created debug menus and their presentation. */
public final class DebugTools {
    private final DebugMenu menu;
    private final DebugMenuOverlay overlay = new DebugMenuOverlay();

    public DebugTools(PlayerState player, RoomSession rooms, DebugTeleporter teleporter) {
        menu = new DebugMenu(
                Objects.requireNonNull(player), Objects.requireNonNull(rooms), Objects.requireNonNull(teleporter));
    }

    public DebugMenu menu() {
        return menu;
    }

    public void render(byte[] framebuffer) {
        if (menu.isOpen()) overlay.render(menu, framebuffer);
    }
}
