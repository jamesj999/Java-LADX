package linksawakening.world.room;

import java.util.Objects;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityGroundInteraction;
import linksawakening.world.entity.RoomEntityObjectSample;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.transition.RoomBoundaryView;

/** One loaded-room generation. Never represents absence; borrowed only until room replacement. */
final class RoomRuntime {
    final ActiveRoom room;
    final RoomContents contents;
    final RoomEntityRuntime entities;
    final RoomRenderSnapshot.View renderView;
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
            linksawakening.vfx.TransientVfxSystem vfx) {
        Services {
            Objects.requireNonNull(queries);
            Objects.requireNonNull(objects);
            Objects.requireNonNull(frame);
            Objects.requireNonNull(vfx);
        }
    }

    RoomRuntime(RoomContents contents, RoomEntityRuntime entities, Services services) {
        this.contents = Objects.requireNonNull(contents, "contents");
        this.room = contents.room;
        this.entities = Objects.requireNonNull(entities, "entities");
        Objects.requireNonNull(room.entityView(), "loaded room entity view");
        renderView = RoomRenderLiveView.live(room, entities);
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
        final BoundaryProbe boundary = new BoundaryProbe();
        final RoomEntityObjectSample[] objectSamples = new RoomEntityObjectSample[RoomEntity.SLOT_COUNT];
        final RoomEntityGroundInteraction.Result[] groundResults =
                new RoomEntityGroundInteraction.Result[RoomEntity.SLOT_COUNT];
    }

    /** Per-room synchronous boundary registers; no consumer keeps them after decide(). */
    static final class BoundaryProbe implements RoomBoundaryView {
        int mapCategory;
        int roomId;
        boolean indoorHasSouthEntrance;
        boolean hasWarps;
        int shutterDoorMask;
        int linkX;
        int linkY;
        int mapId;
        boolean linkAirborne;
        int linkPhysicsModifier;
        boolean sideViewEntityActive;

        @Override
        public int mapCategory() {
            return mapCategory;
        }

        @Override
        public int roomId() {
            return roomId;
        }

        @Override
        public boolean indoorHasSouthEntrance() {
            return indoorHasSouthEntrance;
        }

        @Override
        public boolean hasWarps() {
            return hasWarps;
        }

        @Override
        public int shutterDoorMask() {
            return shutterDoorMask;
        }

        @Override
        public int linkX() {
            return linkX;
        }

        @Override
        public int linkY() {
            return linkY;
        }

        @Override
        public int mapId() {
            return mapId;
        }

        @Override
        public boolean linkAirborne() {
            return linkAirborne;
        }

        @Override
        public int linkPhysicsModifier() {
            return linkPhysicsModifier;
        }

        @Override
        public boolean sideViewEntityActive() {
            return sideViewEntityActive;
        }
    }

    RoomRenderSnapshot renderSnapshot() {

        RoomRenderSnapshot snapshot = room.renderSnapshot();
        return new RoomRenderSnapshot(
                snapshot.tileIds(),
                snapshot.tileAttrs(),
                snapshot.palettes(),
                snapshot.entities(),
                entities.inspection().instrumentWarpVisual(),
                entities.inspection().heartPieceVisual(),
                snapshot.linkGrassPalette());
    }
}
