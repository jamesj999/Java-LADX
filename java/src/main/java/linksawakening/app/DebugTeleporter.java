package linksawakening.app;

import java.util.Objects;
import linksawakening.app.debug.TeleportTarget;
import linksawakening.app.screen.GameplayScreen;
import linksawakening.entity.Link;
import linksawakening.input.InputState;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.world.room.*;
import linksawakening.world.transition.Warp;

/** Resets only the source-defined debug-entry state after loading a destination. */
public final class DebugTeleporter implements TeleportTarget {
    private final RoomSession rooms;
    private final Link link;
    private final OpeningStoryState opening;
    private final DebugState debug;
    private final TransientVfxSystem effects;
    private final DroppableRupeeSystem rupees;
    private final InputState input;
    private final GameplayScreen screen;

    public DebugTeleporter(
            RoomSession rooms,
            Link link,
            OpeningStoryState opening,
            DebugState debug,
            TransientVfxSystem effects,
            DroppableRupeeSystem rupees,
            InputState input,
            GameplayScreen screen) {
        this.rooms = Objects.requireNonNull(rooms);
        this.link = Objects.requireNonNull(link);
        this.opening = Objects.requireNonNull(opening);
        this.debug = Objects.requireNonNull(debug);
        this.effects = Objects.requireNonNull(effects);
        this.rupees = Objects.requireNonNull(rupees);
        this.input = Objects.requireNonNull(input);
        this.screen = Objects.requireNonNull(screen);
    }

    public void teleport(Warp destination) {
        rooms.transitions().loadWarpDestination(destination);
        opening.clear();
        link.resetTransientStateForDebug();
        link.setDebugNoClip(debug.debugNoClip);
        link.setSideScrolling(destination.category() == Warp.CATEGORY_SIDESCROLL);
        link.setRoomEntryPixelPosition(destination.javaPixelX(), destination.javaPixelY());
        effects.clear();
        rupees.clear();
        input.clear();
        screen.clearPendingInput();
    }
}
