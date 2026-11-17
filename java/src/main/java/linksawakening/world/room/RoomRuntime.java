package linksawakening.world.room;

import java.util.Objects;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityGroundInteraction;
import linksawakening.world.entity.RoomEntityObjectSample;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.transition.RoomBoundaryView;

/** One loaded-room generation. Never represents absence; borrowed only until room replacement. */
final class RoomRuntime {
    private final ActiveRoom room;
    private final RoomContents contents;
    private final RoomEntityRuntime entities;
    private final RoomRenderSnapshot.View renderView;
    private final RoomEntityInteractions interactions;
    private final CollisionCache collisions = new CollisionCache();
    private final linksawakening.world.entity.RoomEntityBackgroundCollision backgroundCollision;
    private final linksawakening.world.entity.RoomEntityObjectCollision objectCollision;
    private final linksawakening.world.entity.RoomEntityGroundInteraction groundInteraction;
    private final linksawakening.world.entity.RoomEntityObjectQuery objectQuery;
    private final linksawakening.world.entity.RoomEntityObjectQuery objectIntersectionQuery;
    private final linksawakening.world.entity.RoomEntityBackgroundInteraction backgroundInteraction;
    private final linksawakening.world.entity.ColorShellWorld colorShellWorld;

    record Services(
            RoomCollisionQueries queries,
            RoomObjectMutations objects,
            RoomEntitySessionState frame,
            RoomFollowerState followers,
            RoomWorldState world,
            linksawakening.vfx.TransientVfxSystem vfx) {
        Services {
            Objects.requireNonNull(queries);
            Objects.requireNonNull(objects);
            Objects.requireNonNull(frame);
            Objects.requireNonNull(followers);
            Objects.requireNonNull(world);
            Objects.requireNonNull(vfx);
        }
    }

    RoomRuntime(RoomContents contents, RoomEntityRuntime entities, Services services) {
        this.contents = Objects.requireNonNull(contents, "contents");
        this.room = contents.room();
        this.entities = Objects.requireNonNull(entities, "entities");
        Objects.requireNonNull(room.entityView(), "loaded room entity view");
        renderView = RoomRenderLiveView.live(room, entities);
        interactions = new RoomEntityInteractions(
                room,
                entities.combat(),
                entities.pickups(),
                entities.lifecycle(),
                entities.inspection(),
                entities.outbox(),
                services.frame(),
                services.followers(),
                services.world());
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
        private final BoundaryProbe boundary = new BoundaryProbe();
        private final RoomEntityObjectSample[] objectSamples = new RoomEntityObjectSample[RoomEntity.SLOT_COUNT];
        private final RoomEntityGroundInteraction.Result[] groundResults =
                new RoomEntityGroundInteraction.Result[RoomEntity.SLOT_COUNT];

        BoundaryProbe boundary() {
            return this.boundary;
        }

        RoomEntityObjectSample[] objectSamples() {
            return this.objectSamples;
        }

        RoomEntityGroundInteraction.Result[] groundResults() {
            return this.groundResults;
        }
    }

    /** Per-room synchronous boundary registers; no consumer keeps them after decide(). */
    static final class BoundaryProbe implements RoomBoundaryView {
        private int mapCategory;
        private int roomId;
        private boolean indoorHasSouthEntrance;
        private boolean hasWarps;
        private int shutterDoorMask;
        private int linkX;
        private int linkY;
        private int mapId;
        private boolean linkAirborne;
        private int linkPhysicsModifier;
        private boolean sideViewEntityActive;

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

        int mapCategory(int value) {
            this.mapCategory = value;
            return this.mapCategory;
        }

        int roomId(int value) {
            this.roomId = value;
            return this.roomId;
        }

        boolean indoorHasSouthEntrance(boolean value) {
            this.indoorHasSouthEntrance = value;
            return this.indoorHasSouthEntrance;
        }

        boolean hasWarps(boolean value) {
            this.hasWarps = value;
            return this.hasWarps;
        }

        int shutterDoorMask(int value) {
            this.shutterDoorMask = value;
            return this.shutterDoorMask;
        }

        int mapId(int value) {
            this.mapId = value;
            return this.mapId;
        }

        int linkX(int value) {
            this.linkX = value;
            return this.linkX;
        }

        int linkY(int value) {
            this.linkY = value;
            return this.linkY;
        }

        boolean linkAirborne(boolean value) {
            this.linkAirborne = value;
            return this.linkAirborne;
        }

        int linkPhysicsModifier(int value) {
            this.linkPhysicsModifier = value;
            return this.linkPhysicsModifier;
        }

        boolean sideViewEntityActive(boolean value) {
            this.sideViewEntityActive = value;
            return this.sideViewEntityActive;
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

    ActiveRoom room() {
        return this.room;
    }

    CollisionCache collisions() {
        return this.collisions;
    }

    RoomContents contents() {
        return this.contents;
    }

    RoomEntityRuntime entities() {
        return this.entities;
    }

    RoomRenderSnapshot.View renderView() {
        return this.renderView;
    }

    linksawakening.world.entity.ColorShellWorld colorShellWorld() {
        return this.colorShellWorld;
    }

    linksawakening.world.entity.RoomEntityGroundInteraction groundInteraction() {
        return this.groundInteraction;
    }

    linksawakening.world.entity.RoomEntityBackgroundInteraction backgroundInteraction() {
        return this.backgroundInteraction;
    }

    linksawakening.world.entity.RoomEntityObjectQuery objectQuery() {
        return this.objectQuery;
    }

    linksawakening.world.entity.RoomEntityObjectQuery objectIntersectionQuery() {
        return this.objectIntersectionQuery;
    }

    RoomEntityInteractions interactions() {
        return this.interactions;
    }

    linksawakening.world.entity.RoomEntityBackgroundCollision backgroundCollision() {
        return this.backgroundCollision;
    }

    linksawakening.world.entity.RoomEntityObjectCollision objectCollision() {
        return this.objectCollision;
    }
}
