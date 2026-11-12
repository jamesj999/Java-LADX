package linksawakening.app.services;

import java.util.Objects;
import linksawakening.physics.OverworldCollision;
import linksawakening.world.object.OverworldBushInteraction;
import linksawakening.world.room.RoomSession;
import linksawakening.world.room.RoomTransitionCoordinator;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;

/**
 * Long-lived world services constructed before the first frame. The immutable bundle is retained for the
 * owning session.
 */
public record WorldServices(
        RoomSession roomSession,
        RoomTransitionCoordinator roomTransitionCoordinator,
        OverworldCollision overworldCollision,
        OverworldBushInteraction overworldBushInteraction,
        ScrollController scrollController,
        TransitionController transitionController) {
    public WorldServices {
        Objects.requireNonNull(scrollController);
        Objects.requireNonNull(transitionController);
        Objects.requireNonNull(roomSession);
        Objects.requireNonNull(roomTransitionCoordinator);
        Objects.requireNonNull(overworldCollision);
        Objects.requireNonNull(overworldBushInteraction);
    }
}
