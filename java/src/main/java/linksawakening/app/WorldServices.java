package linksawakening.app;

import static linksawakening.world.room.RoomConstants.*;

import java.util.Objects;
import linksawakening.physics.OverworldCollision;
import linksawakening.world.object.OverworldBushInteraction;
import linksawakening.world.room.RoomSession;
import linksawakening.world.room.RoomTransitionCoordinator;

/** Long-lived world services constructed before the first frame. */
public record WorldServices(
        RoomSession roomSession,
        RoomTransitionCoordinator roomTransitionCoordinator,
        OverworldCollision overworldCollision,
        OverworldBushInteraction overworldBushInteraction) {
    public WorldServices {
        Objects.requireNonNull(roomSession);
        Objects.requireNonNull(roomTransitionCoordinator);
        Objects.requireNonNull(overworldCollision);
        Objects.requireNonNull(overworldBushInteraction);
    }
}
