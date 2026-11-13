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
    private linksawakening.world.transition.RoomBoundaryInputs.Room boundaryRoom;
    private final RoomEntityObjectSample[] objectSamples = new RoomEntityObjectSample[RoomEntity.SLOT_COUNT];
    private final RoomEntityGroundInteraction.Result[] groundResults =
            new RoomEntityGroundInteraction.Result[RoomEntity.SLOT_COUNT];

    RoomCollisionQueries(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    RoomBoundaryState boundaryState(int linkX, int linkY) {
        return dependencies.roomCollisionQueries().get().boundaryState(linkX, linkY, false);
    }

    RoomBoundaryState boundaryState(int linkX, int linkY, boolean linkAirborne) {
        return dependencies.roomCollisionQueries().get().boundaryState(linkX, linkY, linkAirborne, 0);
    }

    RoomBoundaryState boundaryState(int linkX, int linkY, boolean linkAirborne, int linkPhysicsModifier) {
        boolean sideViewEntityActive = dependencies.location().activeRoom.entityView() != null
                && !dependencies.location().activeRoom.entityView().slots().isEmpty()
                && dependencies
                                .location()
                                .activeRoom
                                .entityView()
                                .slots()
                                .get(0)
                                .status()
                        != EntityStatus.DISABLED;
        var active = dependencies.location().activeRoom;
        if (boundaryRoom == null
                || boundaryRoom.mapCategory() != active.mapCategory()
                || boundaryRoom.roomId() != active.roomId()
                || boundaryRoom.indoorHasSouthEntrance() != active.indoorHasSouthEntrance()
                || boundaryRoom.hasWarps() != active.hasWarps()
                || boundaryRoom.shutterDoorMask() != dependencies.puzzles().closedShutterMask
                || boundaryRoom.mapId() != active.mapId())
            boundaryRoom = new linksawakening.world.transition.RoomBoundaryInputs.Room(
                    dependencies.location().activeRoom.mapCategory(),
                    dependencies.location().activeRoom.roomId(),
                    dependencies.location().activeRoom.indoorHasSouthEntrance(),
                    dependencies.location().activeRoom.hasWarps(),
                    dependencies.puzzles().closedShutterMask,
                    dependencies.location().activeRoom.mapId());
        return new RoomBoundaryState(new linksawakening.world.transition.RoomBoundaryInputs(
                boundaryRoom,
                new linksawakening.world.transition.RoomBoundaryInputs.Link(
                        linkX, linkY, linkAirborne, linkPhysicsModifier, sideViewEntityActive)));
    }

    RoomEntityObjectSample entityObjectSample(RoomEntity entity) {
        OverworldCollision.GroundInteractionSample sample =
                dependencies.overworldCollision().groundInteractionSample(entity.x(), entity.y());
        var previous = objectSamples[entity.slot()];
        if (previous == null
                || previous.objectId() != sample.objectId()
                || previous.physicsFlag() != sample.physicsFlag()
                || previous.objectLeft() != sample.objectLeft()
                || previous.objectTop() != sample.objectTop()) {
            previous = new RoomEntityObjectSample(
                    sample.objectId(), sample.physicsFlag(), sample.objectLeft(), sample.objectTop());
            objectSamples[entity.slot()] = previous;
        }
        return previous;
    }

    RoomEntityObjectSample entityObjectIntersectionSample(RoomEntity entity) {
        OverworldCollision.GroundInteractionSample sample =
                dependencies.overworldCollision().entityInteractionSample(entity.x(), entity.y());
        var previous = objectSamples[entity.slot()];
        if (previous == null
                || previous.objectId() != sample.objectId()
                || previous.physicsFlag() != sample.physicsFlag()
                || previous.objectLeft() != sample.objectLeft()
                || previous.objectTop() != sample.objectTop()) {
            previous = new RoomEntityObjectSample(
                    sample.objectId(), sample.physicsFlag(), sample.objectLeft(), sample.objectTop());
            objectSamples[entity.slot()] = previous;
        }
        return previous;
    }

    boolean entityBackgroundCollision(RoomEntity entity, int direction, int nextX, int nextY) {
        return dependencies
                .roomCollisionQueries()
                .get()
                .entityBackgroundCollisionResult(entity, direction, nextX, nextY)
                .blocked();
    }

    EntityBackgroundCollisionResult entityBackgroundCollisionResult(
            RoomEntity entity, int direction, int nextX, int nextY) {
        int ignoreHitsCountdown = dependencies.entityRuntimeState().runtime == null
                ? 0
                : dependencies.entityRuntimeState().runtime.enemies().enemyIgnoreHitsCountdown(entity.slot());
        return dependencies
                .roomCollisionQueries()
                .get()
                .entityBackgroundCollisionResult(entity, direction, nextX, nextY, ignoreHitsCountdown, 0);
    }

    EntityBackgroundCollisionResult entityBackgroundCollisionResult(
            RoomEntity entity, int direction, int nextX, int nextY, int ignoreHitsCountdown) {
        return dependencies
                .roomCollisionQueries()
                .get()
                .entityBackgroundCollisionResult(entity, direction, nextX, nextY, ignoreHitsCountdown, 0);
    }

    EntityBackgroundCollisionResult entityBackgroundCollisionResult(
            RoomEntity entity, int direction, int nextX, int nextY, int ignoreHitsCountdown, int frameCounter) {
        EntityCollisionPointProbe.Sample sample =
                dependencies.entityCollisionPointProbe().sample(entity, direction, nextX, nextY);
        int pointX = sample.x();
        int pointY = sample.y();
        int objectId = dependencies.overworldCollision().objectIdAtPoint(pointX, pointY);
        int physicsFlag = dependencies.overworldCollision().objectPhysicsFlagAtPoint(pointX, pointY);
        int ledgeTimer = dependencies.entityRuntimeState().runtime == null
                ? 0
                : dependencies.entityRuntimeState().runtime.enemies().ledgeTransitionTimer(entity.slot());
        if ((entity.z() & 0x80) != 0) {
            ledgeTimer = 0;
        }
        int thrownDirection = dependencies.entityRuntimeState().runtime == null
                ? 0xFF
                : dependencies.entityRuntimeState().runtime.thrownDirection(entity.slot());
        EntityBackgroundCollisionState state = new EntityBackgroundCollisionState(
                frameCounter,
                dependencies.location().activeRoom != null
                        && dependencies.location().activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD,
                thrownDirection,
                ledgeTimer,
                dependencies.terrain().switchBlocksState,
                dependencies.overworldCollision().linkStandingOnSwitchBlock());
        EntityBackgroundCollisionResolution resolution = dependencies
                .entityBackgroundCollisionResolver()
                .resolveWithState(entity, direction, sample, objectId, physicsFlag, ignoreHitsCountdown, state);
        if (dependencies.entityRuntimeState().runtime != null) {
            dependencies
                    .entityRuntimeState()
                    .runtime
                    .configuration()
                    .setLedgeTransitionTimer(entity.slot(), resolution.nextLedgeTimer());
        }
        return resolution.result();
    }

    EntityBackgroundCollisionResult entityBackgroundCollisionResultForTest(
            RoomEntity entity, int direction, int nextX, int nextY) {
        return dependencies
                .roomCollisionQueries()
                .get()
                .entityBackgroundCollisionResult(entity, direction, nextX, nextY);
    }

    EntityBackgroundCollisionResult entityBackgroundCollisionResultForTest(
            RoomEntity entity, int direction, int nextX, int nextY, int frameCounter) {
        int ignoreHitsCountdown = dependencies.entityRuntimeState().runtime == null
                ? 0
                : dependencies.entityRuntimeState().runtime.enemies().enemyIgnoreHitsCountdown(entity.slot());
        return dependencies
                .roomCollisionQueries()
                .get()
                .entityBackgroundCollisionResult(entity, direction, nextX, nextY, ignoreHitsCountdown, frameCounter);
    }

    RoomEntityGroundInteraction.Result entityGroundInteraction(
            RoomEntity entity, int frameCounter, int previousGroundStatus, int speedZ, boolean sideScrolling) {
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
        var previous = groundResults[entity.slot()];
        if (previous == null
                || previous.entity() != updated
                || previous.groundStatus() != groundStatus
                || !java.util.Objects.equals(previous.pitTransition(), pit)
                || previous.unloaded()
                || previous.waterSplash() != splash) {
            previous = new RoomEntityGroundInteraction.Result(updated, groundStatus, pit, false, splash);
            groundResults[entity.slot()] = previous;
        }
        return previous;
    }

    boolean pairoddProjectileObjectCollision(RoomEntity entity) {
        int physicsFlag = dependencies.overworldCollision().objectPhysicsFlagAtEntityPosition(entity.x(), entity.y());
        return PairoddProjectileObjectCollision.collidesWithPhysicsFlag(physicsFlag);
    }

    record Dependencies(
            RoomEntityFrameState entityFrame,
            RoomLocationState location,
            RoomPuzzleState puzzles,
            RoomTerrainState terrain,
            EntityBackgroundCollisionResolver entityBackgroundCollisionResolver,
            RoomEntityFrameState entityRuntimeState,
            EntityCollisionPointProbe entityCollisionPointProbe,
            OverworldCollision overworldCollision,
            RomTables romTables,
            java.util.function.Supplier<RoomCollisionQueries> roomCollisionQueries) {
        Dependencies {
            java.util.Objects.requireNonNull(entityFrame, "entityFrame");
            java.util.Objects.requireNonNull(location, "location");
            java.util.Objects.requireNonNull(puzzles, "puzzles");
            java.util.Objects.requireNonNull(terrain, "terrain");
            java.util.Objects.requireNonNull(entityBackgroundCollisionResolver, "entityBackgroundCollisionResolver");
            java.util.Objects.requireNonNull(entityRuntimeState, "entityRuntimeState");
            java.util.Objects.requireNonNull(entityCollisionPointProbe, "entityCollisionPointProbe");
            java.util.Objects.requireNonNull(overworldCollision, "overworldCollision");
            java.util.Objects.requireNonNull(romTables, "romTables");
            java.util.Objects.requireNonNull(roomCollisionQueries, "roomCollisionQueries");
        }
    }
}
