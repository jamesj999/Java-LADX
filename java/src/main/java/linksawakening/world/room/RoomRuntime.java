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
    final RoomContents contents;
    final RoomEntityRuntime entities;
    final CollisionCache collisions = new CollisionCache();
    final linksawakening.world.entity.RoomEntityBackgroundCollision backgroundCollision;
    final linksawakening.world.entity.RoomEntityObjectCollision objectCollision;
    final linksawakening.world.entity.RoomEntityGroundInteraction groundInteraction;
    final linksawakening.world.entity.RoomEntityObjectQuery objectQuery;
    final linksawakening.world.entity.RoomEntityObjectQuery objectIntersectionQuery;
    final linksawakening.world.entity.RoomEntityBackgroundInteraction backgroundInteraction;
    final linksawakening.world.entity.ColorShellWorld colorShellWorld;

    record Services(
            RoomCollisionQueries queries,
            RoomObjectMutations objects,
            RoomEntitySessionState frame,
            linksawakening.vfx.TransientVfxSystem vfx,
            RoomPuzzleState puzzles,
            RoomTerrainState terrain) {
        Services {
            Objects.requireNonNull(queries);
            Objects.requireNonNull(objects);
            Objects.requireNonNull(frame);
            Objects.requireNonNull(puzzles);
            Objects.requireNonNull(terrain);
        }
    }

    RoomRuntime(ActiveRoom room, RoomEntityRuntime entities, Services services) {
        this.room = Objects.requireNonNull(room, "room");
        this.entities = Objects.requireNonNull(entities, "entities");
        contents = new RoomContents(room, services.puzzles().current, services.terrain().overrides);
        backgroundCollision = (entity, direction, x, y) ->
                services.queries().entityBackgroundCollision(this, entity, direction, x, y);
        objectCollision = entity -> services.queries().pairoddProjectileObjectCollision(this, entity);
        groundInteraction = (entity, frame, status, speed, side) ->
                services.queries().entityGroundInteraction(this, entity, frame, status, speed, side);
        objectQuery = entity -> services.queries().entityObjectSample(this, entity);
        objectIntersectionQuery = entity -> services.queries().entityObjectIntersectionSample(this, entity);
        backgroundInteraction = RoomEntityWorldAdapters.background(services.queries(), this);
        colorShellWorld =
                RoomEntityWorldAdapters.colorShell(services.objects(), services.frame(), services.vfx(), this);
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
