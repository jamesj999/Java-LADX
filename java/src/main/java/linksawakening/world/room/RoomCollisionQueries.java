package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.ENTITY_BOW_WOW;
import static linksawakening.world.room.RoomSessionRules.ENTITY_CONVEYOR_MOVEMENT_X;
import static linksawakening.world.room.RoomSessionRules.ENTITY_CONVEYOR_MOVEMENT_Y;
import static linksawakening.world.room.RoomSessionRules.ENTITY_FISH;
import static linksawakening.world.room.RoomSessionRules.ENTITY_MARIN_AT_THE_SHORE;
import static linksawakening.world.room.RoomSessionRules.ENTITY_OPT1_NO_GROUND_INTERACTION;
import static linksawakening.world.room.RoomSessionRules.ENTITY_PEAHAT;
import static linksawakening.world.room.RoomSessionRules.ENTITY_ROOSTER;

import linksawakening.physics.OverworldCollision;
import linksawakening.physics.PhysicsFlags;
import linksawakening.rom.RomTables;
import linksawakening.world.entity.EntityBackgroundCollisionResolution;
import linksawakening.world.entity.EntityBackgroundCollisionResolver;
import linksawakening.world.entity.EntityBackgroundCollisionResult;
import linksawakening.world.entity.EntityBackgroundCollisionState;
import linksawakening.world.entity.EntityCollisionPointProbe;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.PairoddProjectileObjectCollision;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityGroundInteraction;
import linksawakening.world.entity.RoomEntityObjectSample;
import linksawakening.world.transition.RoomBoundaryState;
import linksawakening.world.transition.Warp;

/** ApplyLinkMotionState ground probes and entity collision queries over current room state. */
final class RoomCollisionQueries {
    private final Dependencies dependencies;

    RoomCollisionQueries(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    RoomBoundaryState boundaryState(RoomRuntime loaded, int linkX, int linkY) {
        return boundaryState(loaded, linkX, linkY, false);
    }

    RoomBoundaryState boundaryState(RoomRuntime loaded, int linkX, int linkY, boolean linkAirborne) {
        return boundaryState(loaded, linkX, linkY, linkAirborne, 0);
    }

    RoomBoundaryState boundaryState(
            RoomRuntime loaded, int linkX, int linkY, boolean linkAirborne, int linkPhysicsModifier) {
        boolean sideViewEntityActive = !loaded.room.entityView().slots().isEmpty()
                && loaded.room.entityView().slots().get(0).status() != EntityStatus.DISABLED;
        var active = loaded.room;
        if (loaded.collisions.boundary == null
                || loaded.collisions.boundary.mapCategory() != active.mapCategory()
                || loaded.collisions.boundary.roomId() != active.roomId()
                || loaded.collisions.boundary.indoorHasSouthEntrance() != active.indoorHasSouthEntrance()
                || loaded.collisions.boundary.hasWarps() != active.hasWarps()
                || loaded.collisions.boundary.shutterDoorMask() != loaded.contents.puzzles.closedShutterMask
                || loaded.collisions.boundary.mapId() != active.mapId())
            loaded.collisions.boundary = new linksawakening.world.transition.RoomBoundaryInputs.Room(
                    loaded.room.mapCategory(),
                    loaded.room.roomId(),
                    loaded.room.indoorHasSouthEntrance(),
                    loaded.room.hasWarps(),
                    loaded.contents.puzzles.closedShutterMask,
                    loaded.room.mapId());
        return new RoomBoundaryState(new linksawakening.world.transition.RoomBoundaryInputs(
                loaded.collisions.boundary,
                new linksawakening.world.transition.RoomBoundaryInputs.Link(
                        linkX, linkY, linkAirborne, linkPhysicsModifier, sideViewEntityActive)));
    }

    RoomEntityObjectSample entityObjectSample(RoomRuntime loaded, RoomEntity entity) {
        OverworldCollision.GroundInteractionSample sample =
                dependencies.overworldCollision().groundInteractionSample(entity.x(), entity.y());
        var previous = loaded.collisions.objectSamples[entity.slot()];
        if (previous == null
                || previous.objectId() != sample.objectId()
                || previous.physicsFlag() != sample.physicsFlag()
                || previous.objectLeft() != sample.objectLeft()
                || previous.objectTop() != sample.objectTop()) {
            previous = new RoomEntityObjectSample(
                    sample.objectId(), sample.physicsFlag(), sample.objectLeft(), sample.objectTop());
            loaded.collisions.objectSamples[entity.slot()] = previous;
        }
        return previous;
    }

    RoomEntityObjectSample entityObjectIntersectionSample(RoomRuntime loaded, RoomEntity entity) {
        OverworldCollision.GroundInteractionSample sample =
                dependencies.overworldCollision().entityInteractionSample(entity.x(), entity.y());
        var previous = loaded.collisions.objectSamples[entity.slot()];
        if (previous == null
                || previous.objectId() != sample.objectId()
                || previous.physicsFlag() != sample.physicsFlag()
                || previous.objectLeft() != sample.objectLeft()
                || previous.objectTop() != sample.objectTop()) {
            previous = new RoomEntityObjectSample(
                    sample.objectId(), sample.physicsFlag(), sample.objectLeft(), sample.objectTop());
            loaded.collisions.objectSamples[entity.slot()] = previous;
        }
        return previous;
    }

    boolean entityBackgroundCollision(RoomRuntime loaded, RoomEntity entity, int direction, int nextX, int nextY) {
        return entityBackgroundCollisionResult(loaded, entity, direction, nextX, nextY)
                .blocked();
    }

    EntityBackgroundCollisionResult entityBackgroundCollisionResult(
            RoomRuntime loaded, RoomEntity entity, int direction, int nextX, int nextY) {
        int ignoreHitsCountdown = loaded.entities.enemies().enemyIgnoreHitsCountdown(entity.slot());
        return entityBackgroundCollisionResult(loaded, entity, direction, nextX, nextY, ignoreHitsCountdown, 0);
    }

    EntityBackgroundCollisionResult entityBackgroundCollisionResult(
            RoomRuntime loaded, RoomEntity entity, int direction, int nextX, int nextY, int ignoreHitsCountdown) {
        return entityBackgroundCollisionResult(loaded, entity, direction, nextX, nextY, ignoreHitsCountdown, 0);
    }

    EntityBackgroundCollisionResult entityBackgroundCollisionResult(
            RoomRuntime loaded,
            RoomEntity entity,
            int direction,
            int nextX,
            int nextY,
            int ignoreHitsCountdown,
            int frameCounter) {
        EntityCollisionPointProbe.Sample sample =
                dependencies.entityCollisionPointProbe().sample(entity, direction, nextX, nextY);
        int pointX = sample.x();
        int pointY = sample.y();
        int objectId = dependencies.overworldCollision().objectIdAtPoint(pointX, pointY);
        int physicsFlag = dependencies.overworldCollision().objectPhysicsFlagAtPoint(pointX, pointY);
        int ledgeTimer = loaded.entities.enemies().ledgeTransitionTimer(entity.slot());
        if ((entity.z() & 0x80) != 0) {
            ledgeTimer = 0;
        }
        int thrownDirection = loaded.entities.thrownDirection(entity.slot());
        EntityBackgroundCollisionState state = new EntityBackgroundCollisionState(
                frameCounter,
                loaded.room.mapCategory() != Warp.CATEGORY_OVERWORLD,
                thrownDirection,
                ledgeTimer,
                dependencies.terrain().switchBlocksState,
                dependencies.overworldCollision().linkStandingOnSwitchBlock());
        EntityBackgroundCollisionResolution resolution = dependencies
                .entityBackgroundCollisionResolver()
                .resolveWithState(entity, direction, sample, objectId, physicsFlag, ignoreHitsCountdown, state);

        loaded.entities.configuration().setLedgeTransitionTimer(entity.slot(), resolution.nextLedgeTimer());
        return resolution.result();
    }

    EntityBackgroundCollisionResult entityBackgroundCollisionResultForTest(
            RoomRuntime loaded, RoomEntity entity, int direction, int nextX, int nextY) {
        return entityBackgroundCollisionResult(loaded, entity, direction, nextX, nextY);
    }

    EntityBackgroundCollisionResult entityBackgroundCollisionResultForTest(
            RoomRuntime loaded, RoomEntity entity, int direction, int nextX, int nextY, int frameCounter) {
        int ignoreHitsCountdown = loaded.entities.enemies().enemyIgnoreHitsCountdown(entity.slot());
        return entityBackgroundCollisionResult(
                loaded, entity, direction, nextX, nextY, ignoreHitsCountdown, frameCounter);
    }

    // Identity distinguishes cache generations or explicit sentinels; equal payloads must not coalesce.
    @SuppressWarnings("ReferenceEquality")
    RoomEntityGroundInteraction.Result entityGroundInteraction(
            RoomRuntime loaded,
            RoomEntity entity,
            int frameCounter,
            int previousGroundStatus,
            int speedZ,
            boolean sideScrolling) {
        int options = dependencies.romTables().entityPropertyTables().entityOptions1(entity.type());
        if ((options & ENTITY_OPT1_NO_GROUND_INTERACTION) != 0) {
            return RoomEntityGroundInteraction.Result.unchanged(entity, 0);
        }

        // The ROM skips ground interaction while an entity is moving upward;
        // a negative Z byte denotes a falling entity and continues through
        // the shared helper.
        int z = entity.z() & 0xFF;
        if (z != 0 && (z & 0x80) == 0) {
            return RoomEntityGroundInteraction.Result.unchanged(entity, 0);
        }

        OverworldCollision.GroundInteractionSample sample =
                dependencies.overworldCollision().groundInteractionSample(entity.x(), entity.y());
        int physicsFlag = sample.physicsFlag();
        int groundStatus = RoomInteractionGeometry.groundStatusFor(sample);
        boolean deepWaterOrLava = physicsFlag == PhysicsFlags.DEEP_WATER || physicsFlag == PhysicsFlags.LAVA;
        boolean retainsDeepWaterStatus = entity.type() == ENTITY_FISH
                || entity.type() == ENTITY_PEAHAT
                || entity.type() == ENTITY_ROOSTER
                || entity.type() == ENTITY_BOW_WOW
                || entity.type() == ENTITY_MARIN_AT_THE_SHORE;
        if (deepWaterOrLava && !retainsDeepWaterStatus) {
            // The source unloads ordinary entities before jumping directly to
            // .createWaterSplash, so this splash is not gated by options or
            // the status-transition speed test.
            return RoomEntityGroundInteraction.Result.unloaded(entity, 0x00, true);
        }

        boolean splash = RoomInteractionGeometry.splashAllowed(
                options, previousGroundStatus, groundStatus, speedZ, sideScrolling);
        int movementIndex = physicsFlag - PhysicsFlags.CAT_CONVEYOR;
        RoomEntity updated = entity;
        if (movementIndex >= 0 && movementIndex < ENTITY_CONVEYOR_MOVEMENT_X.length && (frameCounter & 0x03) == 0) {
            updated = RoomInteractionGeometry.withEntityPosition(
                    entity,
                    entity.x() + ENTITY_CONVEYOR_MOVEMENT_X[movementIndex],
                    entity.y() + ENTITY_CONVEYOR_MOVEMENT_Y[movementIndex]);
        }

        RoomEntityGroundInteraction.PitTransition pit = RoomInteractionGeometry.pitTransitionFor(
                entity, sample, dependencies.entityFrame().currentLinkMotionState);
        var previous = loaded.collisions.groundResults[entity.slot()];
        if (previous == null
                || previous.entity() != updated
                || previous.groundStatus() != groundStatus
                || !java.util.Objects.equals(previous.pitTransition(), pit)
                || previous.unloaded()
                || previous.waterSplash() != splash) {
            previous = new RoomEntityGroundInteraction.Result(updated, groundStatus, pit, false, splash);
            loaded.collisions.groundResults[entity.slot()] = previous;
        }
        return previous;
    }

    boolean pairoddProjectileObjectCollision(RoomRuntime loaded, RoomEntity entity) {
        int physicsFlag = dependencies.overworldCollision().objectPhysicsFlagAtEntityPosition(entity.x(), entity.y());
        return PairoddProjectileObjectCollision.collidesWithPhysicsFlag(physicsFlag);
    }

    record Dependencies(
            RoomEntitySessionState entityFrame,
            RoomTerrainState terrain,
            EntityBackgroundCollisionResolver entityBackgroundCollisionResolver,
            EntityCollisionPointProbe entityCollisionPointProbe,
            OverworldCollision overworldCollision,
            RomTables romTables) {
        Dependencies {
            java.util.Objects.requireNonNull(entityFrame, "entityFrame");
            java.util.Objects.requireNonNull(terrain, "terrain");
            java.util.Objects.requireNonNull(entityBackgroundCollisionResolver, "entityBackgroundCollisionResolver");
            java.util.Objects.requireNonNull(entityCollisionPointProbe, "entityCollisionPointProbe");
            java.util.Objects.requireNonNull(overworldCollision, "overworldCollision");
            java.util.Objects.requireNonNull(romTables, "romTables");
        }
    }

    /* Transitional boundary adapter; caller has already checked the facade's loaded state. */
}
