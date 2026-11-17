package linksawakening.app.gameplay;

import java.util.Objects;
import linksawakening.ui.InventoryController;
import linksawakening.world.room.RoomSession;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;

/** Runs the gameplay VBlank tile-animation gate. Retained for one simulation. */
public final class AnimatedTilesPhase {
    private final InventoryController inventoryController;
    private final RoomSession rooms;
    private final ScrollController scrollController;
    private final TransitionController transitionController;

    public AnimatedTilesPhase(
            InventoryController inventoryController,
            RoomSession roomSession,
            ScrollController scrollController,
            TransitionController transitionController) {
        this.inventoryController = Objects.requireNonNull(inventoryController);
        this.rooms = Objects.requireNonNull(roomSession);
        this.scrollController = Objects.requireNonNull(scrollController);
        this.transitionController = Objects.requireNonNull(transitionController);
    }

    public void tick(boolean dialogBlocksGameplay) {
        // Advance the animated BG tiles (waterfalls, weather vanes, etc.).
        // Original gates this behind wRoomTransitionState == 0 and the
        // inventory window not overlapping; mirror both here.
        if (!this.scrollController.isActive()
                && !this.transitionController.isInputBlocked()
                && !this.inventoryController.shouldBlockOverworldInput()
                && !dialogBlocksGameplay) {

            this.rooms.entityState().tickGameplayVBlank();
        }
    }
}
