package linksawakening.world.room;

import java.util.Objects;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityGroundInteraction;
import linksawakening.world.entity.RoomEntityObjectSample;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.transition.RoomBoundaryInputs;

/** One loaded-room generation. Never represents absence; borrowed only until room replacement. */
final class RoomRuntime {
    final ActiveRoom room;
    final RoomEntityRuntime entities;
    final CollisionCache collisions = new CollisionCache();

    RoomRuntime(ActiveRoom room, RoomEntityRuntime entities) {
        this.room = Objects.requireNonNull(room, "room");
        this.entities = Objects.requireNonNull(entities, "entities");
    }

    /** Scratch results belong to this room, never to the session or an unloaded-room sentinel. */
    static final class CollisionCache {
        // Absent until a probe with this room's boundary metadata is requested.
        RoomBoundaryInputs.Room boundary;
        final RoomEntityObjectSample[] objectSamples = new RoomEntityObjectSample[RoomEntity.SLOT_COUNT];
        final RoomEntityGroundInteraction.Result[] groundResults =
                new RoomEntityGroundInteraction.Result[RoomEntity.SLOT_COUNT];
    }
}
