package linksawakening.app.debug;

import java.util.Objects;
import linksawakening.app.screen.GameplayScreen;
import linksawakening.app.state.DebugState;
import linksawakening.app.state.OpeningStoryState;
import linksawakening.entity.Link;
import linksawakening.input.InputState;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.world.room.DroppableRupeeSystem;
import linksawakening.world.room.RoomSession;
import linksawakening.world.transition.Warp;

/**
 * Resets only the source-defined debug-entry state after loading a destination. One instance is retained for
 * the owning session.
 */
public final class DebugTeleporter {
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
        link.motion().resetTransientStateForDebug();
        link.motion().setDebugNoClip(debug.debugNoClip);
        link.sideView().setSideScrolling(destination.category() == Warp.CATEGORY_SIDESCROLL);
        link.position().setRoomEntryPixelPosition(destination.javaPixelX(), destination.javaPixelY());
        effects.clear();
        rupees.clear();
        input.clear();
        screen.clearPendingInput();
    }
}
