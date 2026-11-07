package linksawakening.world;

import linksawakening.world.entity.PairoddProjectileObjectCollision;

import linksawakening.world.entity.RoomEntityGroundInteraction;
import linksawakening.world.entity.EntityBackgroundCollisionState;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.EntityBackgroundCollisionResult;
import linksawakening.world.entity.EntityCollisionPointProbe;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.RoomEntityObjectSample;
import linksawakening.world.entity.EntityBackgroundCollisionResolution;

import linksawakening.world.transition.RoomBoundaryState;
import linksawakening.world.transition.Warp;

import linksawakening.physics.OverworldCollision;
import linksawakening.physics.PhysicsFlags;
import static linksawakening.world.RoomConstants.ROOM_PIXEL_HEIGHT;
import static linksawakening.world.RoomConstants.ROOM_PIXEL_WIDTH;
import static linksawakening.world.RoomSession.*;

/** RoomCollisionQueries operations for the live room session. */
final class RoomCollisionQueries {
    private final RoomSession session;
    RoomCollisionQueries(RoomSession session) { this.session = session; }

    RoomBoundaryState boundaryState(int linkX, int linkY) {
        return session.boundaryState(linkX, linkY, false);
    }

    RoomBoundaryState boundaryState(int linkX, int linkY, boolean linkAirborne) {
        return session.boundaryState(linkX, linkY, linkAirborne, 0);
    }

    RoomBoundaryState boundaryState(int linkX, int linkY, boolean linkAirborne, int linkPhysicsModifier) {
        boolean sideViewEntityActive = session.context.activeRoom.entities() != null
            && !session.context.activeRoom.entities().slots().isEmpty()
            && session.context.activeRoom.entities().slots().get(0).status() != EntityStatus.DISABLED;
        return new RoomBoundaryState(
            session.context.activeRoom.mapCategory(),
            session.context.activeRoom.roomId(),
            session.context.activeRoom.indoorHasSouthEntrance(),
            session.context.activeRoom.hasWarps(),
            session.context.closedShutterMask,
            linkX,
            linkY,
            session.context.activeRoom.mapId(),
            linkAirborne,
            linkPhysicsModifier,
            sideViewEntityActive
        );
    }

    RoomEntityObjectSample entityObjectSample(RoomEntity entity) {
        OverworldCollision.GroundInteractionSample sample =
            session.context.overworldCollision.groundInteractionSample(entity.x(), entity.y());
        return new RoomEntityObjectSample(sample.objectId(), sample.physicsFlag(),
            sample.objectLeft(), sample.objectTop());
    }

    RoomEntityObjectSample entityObjectIntersectionSample(RoomEntity entity) {
        OverworldCollision.GroundInteractionSample sample =
            session.context.overworldCollision.entityInteractionSample(entity.x(), entity.y());
        return new RoomEntityObjectSample(sample.objectId(), sample.physicsFlag(),
            sample.objectLeft(), sample.objectTop());
    }

    boolean entityBackgroundCollision(RoomEntity entity, int direction, int nextX, int nextY) {
        return session.entityBackgroundCollisionResult(entity, direction, nextX, nextY).blocked();
    }

    EntityBackgroundCollisionResult entityBackgroundCollisionResult( RoomEntity entity, int direction, int nextX, int nextY) {
        int ignoreHitsCountdown = session.context.entityBridge.entityRuntime == null
            ? 0 : session.context.entityBridge.entityRuntime.enemyIgnoreHitsCountdown(entity.slot());
        return session.entityBackgroundCollisionResult(
            entity, direction, nextX, nextY, ignoreHitsCountdown, 0);
    }

    EntityBackgroundCollisionResult entityBackgroundCollisionResult( RoomEntity entity, int direction, int nextX, int nextY, int ignoreHitsCountdown) {
        return session.entityBackgroundCollisionResult(entity, direction, nextX, nextY,
            ignoreHitsCountdown, 0);
    }

    EntityBackgroundCollisionResult entityBackgroundCollisionResult( RoomEntity entity, int direction, int nextX, int nextY, int ignoreHitsCountdown, int frameCounter) {
        EntityCollisionPointProbe.Sample sample = session.context.entityCollisionPointProbe.sample(
            entity, direction, nextX, nextY);
        int pointX = sample.x();
        int pointY = sample.y();
        int objectId = session.context.overworldCollision.objectIdAtPoint(pointX, pointY);
        int physicsFlag = session.context.overworldCollision.objectPhysicsFlagAtPoint(pointX, pointY);
        int ledgeTimer = session.context.entityBridge.entityRuntime == null
            ? 0 : session.context.entityBridge.entityRuntime.ledgeTransitionTimer(entity.slot());
        if ((entity.z() & 0x80) != 0) {
            ledgeTimer = 0;
        }
        int thrownDirection = session.context.entityBridge.entityRuntime == null
            ? 0xFF : session.context.entityBridge.entityRuntime.thrownDirection(entity.slot());
        EntityBackgroundCollisionState state = new EntityBackgroundCollisionState(
            frameCounter, session.context.activeRoom != null
                && session.context.activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD,
            thrownDirection, ledgeTimer, session.context.switchBlocksState,
            session.context.overworldCollision.linkStandingOnSwitchBlock());
        EntityBackgroundCollisionResolution resolution =
            session.context.entityBackgroundCollisionResolver.resolveWithState(
                entity, direction, sample, objectId, physicsFlag,
                ignoreHitsCountdown, state);
        if (session.context.entityBridge.entityRuntime != null) {
            session.context.entityBridge.entityRuntime.setLedgeTransitionTimer(entity.slot(), resolution.nextLedgeTimer());
        }
        return resolution.result();
    }

    EntityBackgroundCollisionResult entityBackgroundCollisionResultForTest( RoomEntity entity, int direction, int nextX, int nextY) {
        return session.entityBackgroundCollisionResult(entity, direction, nextX, nextY);
    }

    EntityBackgroundCollisionResult entityBackgroundCollisionResultForTest( RoomEntity entity, int direction, int nextX, int nextY, int frameCounter) {
        int ignoreHitsCountdown = session.context.entityBridge.entityRuntime == null
            ? 0 : session.context.entityBridge.entityRuntime.enemyIgnoreHitsCountdown(entity.slot());
        return session.entityBackgroundCollisionResult(entity, direction, nextX, nextY,
            ignoreHitsCountdown, frameCounter);
    }

    RoomEntityGroundInteraction.Result entityGroundInteraction( RoomEntity entity, int frameCounter, int previousGroundStatus, int speedZ, boolean sideScrolling) {
        int options = session.context.romTables.entityOptions1(entity.type());
        if ((options & session.ENTITY_OPT1_NO_GROUND_INTERACTION) != 0) {
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
            session.context.overworldCollision.groundInteractionSample(entity.x(), entity.y());
        int physicsFlag = sample.physicsFlag();
        int groundStatus = session.groundStatusFor(sample);
        boolean deepWaterOrLava = physicsFlag == PhysicsFlags.DEEP_WATER
            || physicsFlag == PhysicsFlags.LAVA;
        boolean retainsDeepWaterStatus = entity.type() == session.ENTITY_FISH
            || entity.type() == session.ENTITY_PEAHAT
            || entity.type() == session.ENTITY_ROOSTER
            || entity.type() == session.ENTITY_BOW_WOW
            || entity.type() == session.ENTITY_MARIN_AT_THE_SHORE;
        if (deepWaterOrLava && !retainsDeepWaterStatus) {
            // The source unloads ordinary entities before jumping directly to
            // .createWaterSplash, so this splash is not gated by options or
            // the status-transition speed test.
            return RoomEntityGroundInteraction.Result.unloaded(entity, 0x00, true);
        }

        boolean splash = session.splashAllowed(options, previousGroundStatus, groundStatus,
            speedZ, sideScrolling);
        int movementIndex = physicsFlag - PhysicsFlags.CAT_CONVEYOR;
        RoomEntity updated = entity;
        if (movementIndex >= 0 && movementIndex < session.ENTITY_CONVEYOR_MOVEMENT_X.length
            && (frameCounter & 0x03) == 0) {
            updated = session.withEntityPosition(entity,
                entity.x() + session.ENTITY_CONVEYOR_MOVEMENT_X[movementIndex],
                entity.y() + session.ENTITY_CONVEYOR_MOVEMENT_Y[movementIndex]);
        }

        RoomEntityGroundInteraction.PitTransition pit = session.pitTransitionFor(
            entity, sample, session.context.currentLinkMotionState);
        return new RoomEntityGroundInteraction.Result(updated, groundStatus, pit, false, splash);
    }

    boolean pairoddProjectileObjectCollision(RoomEntity entity) {
        int physicsFlag = session.context.overworldCollision.objectPhysicsFlagAtEntityPosition(
            entity.x(), entity.y());
        return PairoddProjectileObjectCollision.collidesWithPhysicsFlag(physicsFlag);
    }
}
