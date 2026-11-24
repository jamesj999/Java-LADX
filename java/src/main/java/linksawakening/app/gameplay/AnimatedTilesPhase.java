package linksawakening.app.gameplay;

import java.util.Objects;
import linksawakening.app.state.FrameClock;
import linksawakening.dialog.DialogController;
import linksawakening.entity.Link;
import linksawakening.ui.InventoryController;
import linksawakening.world.room.RoomSession;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;

/** Runs the gameplay VBlank tile-animation gate. Retained for one simulation. */
public final class AnimatedTilesPhase {
    private final FrameClock clock;
    private final Link link;
    private final DialogController dialog;
    private final InventoryController inventoryController;
    private final RoomSession rooms;
    private final ScrollController scrollController;
    private final TransitionController transitionController;

    public AnimatedTilesPhase(
            FrameClock clock,
            Link link,
            DialogController dialog,
            InventoryController inventoryController,
            RoomSession roomSession,
            ScrollController scrollController,
            TransitionController transitionController) {
        this.clock = Objects.requireNonNull(clock);
        this.link = Objects.requireNonNull(link);
        this.dialog = Objects.requireNonNull(dialog);
        this.inventoryController = Objects.requireNonNull(inventoryController);
        this.rooms = Objects.requireNonNull(roomSession);
        this.scrollController = Objects.requireNonNull(scrollController);
        this.transitionController = Objects.requireNonNull(transitionController);
    }

    public void tick() {
        if (scrollController.isActive()
                || transitionController.isInputBlocked()
                || inventoryController.shouldBlockOverworldInput()
                || dialog.tileTransferPending()) return;

        // $00:0FEB..100A queues a HUD slot every fourth ordinary world frame.
        // A pending draw command preempts both world transfers at $00:1B6E.
        if ((clock.value() & 3) == 0 && link.airborne().romMotionState() < 2) return;
        if (rooms.entityState().tickGameplayVBlank()) link.animation().uploadBodyTiles();
    }
}
