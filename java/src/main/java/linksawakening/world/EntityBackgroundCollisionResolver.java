package linksawakening.world;

import java.util.Objects;

import linksawakening.rom.RomTables;

/** Resolves the ROM's entity/background collision result for one sampled object. */
final class EntityBackgroundCollisionResolver {
    private static final int ENTITY_FISH = 0xCC;
    private static final int ENTITY_WATER_TEKTITE = 0x99;
    private static final int ENTITY_BOMB = 0x02;
    private static final int ENTITY_WRECKING_BALL = 0xA8;
    private static final int ENTITY_MOLDORM = 0x59;
    private static final int ENTITY_SPARK_COUNTER_CLOCKWISE = 0x16;
    private static final int ENTITY_SPARK_CLOCKWISE = 0x17;

    private static final int ENTITY_OPTION_NO_WALL_COLLISION = 0x01;
    private static final int ENTITY_OPTION_BOSS = 0x80;

    private static final int PHYSICS_NONE = 0x00;
    private static final int PHYSICS_SOLID = 0x01;
    private static final int PHYSICS_DOOR = 0x03;
    private static final int PHYSICS_SHALLOW_WATER = 0x05;
    private static final int PHYSICS_DEEP_WATER = 0x07;
    private static final int PHYSICS_LAVA = 0x0B;
    private static final int PHYSICS_GENERIC_START = 0x10;
    private static final int PHYSICS_GENERIC_END = 0x9F;
    private static final int PHYSICS_FINE_START = 0x80;
    private static final int PHYSICS_FINE_END = 0x8F;
    private static final int PHYSICS_OPEN_DOOR_START = 0x7C;
    private static final int PHYSICS_OPEN_DOOR_END = 0x7F;
    private static final int PHYSICS_BROAD_PASSABLE_START = 0xA0;
    private static final int PHYSICS_BROAD_PASSABLE_END = 0xFE;
    private static final int PHYSICS_LEDGE_START = 0xD0;
    private static final int PHYSICS_LEDGE_END = 0xD3;
    private static final int PHYSICS_NORMAL_PIT = 0x50;
    private static final int PHYSICS_PIT_WARP = 0x51;
    private static final int PHYSICS_SWITCH_BLOCK = 0x04;
    private static final int PHYSICS_TRACTOR = 0xFF;
    private static final int OBJECT_LOWERED_BLOCK = 0xDB;
    private static final int OBJECT_RAISED_BLOCK = 0xDC;
    /** Bank-$03 SwitchBlockLoweredStatePerObject, indexed by $DB/$DC. */
    private static final int[] SWITCH_BLOCK_STATE_BY_OBJECT = {0x00, 0x02};
    private static final int UNSIGNED_BYTE_MASK = 0xFF;
    private static final int FINE_QUADRANT_SHIFT = 3;
    private static final int FINE_QUADRANT_MASK = 0x01;

    private final RomTables romTables;

    EntityBackgroundCollisionResolver(RomTables romTables) {
        this.romTables = Objects.requireNonNull(romTables, "romTables");
    }

    EntityBackgroundCollisionResult resolve(
            RoomEntity entity,
            int direction,
            EntityCollisionPointProbe.Sample sample,
            int objectId,
            int physicsFlag) {
        return resolve(entity, direction, sample, objectId, physicsFlag, 0);
    }

    EntityBackgroundCollisionResult resolve(
            RoomEntity entity,
            int direction,
            EntityCollisionPointProbe.Sample sample,
            int objectId,
            int physicsFlag,
            int ignoreHitsCountdown) {
        return resolveWithState(entity, direction, sample, objectId, physicsFlag,
            ignoreHitsCountdown,
            new EntityBackgroundCollisionState(0, false, 0xFF, 0)).result();
    }

    EntityBackgroundCollisionResolution resolveWithState(
            RoomEntity entity,
            int direction,
            EntityCollisionPointProbe.Sample sample,
            int objectId,
            int physicsFlag,
            EntityBackgroundCollisionState state) {
        return resolveWithState(entity, direction, sample, objectId, physicsFlag, 0, state);
    }

    EntityBackgroundCollisionResolution resolveWithState(
            RoomEntity entity,
            int direction,
            EntityCollisionPointProbe.Sample sample,
            int objectId,
            int physicsFlag,
            int ignoreHitsCountdown,
            EntityBackgroundCollisionState state) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(sample, "sample");
        Objects.requireNonNull(state, "state");

        int unsignedObjectId = objectId & UNSIGNED_BYTE_MASK;
        int unsignedPhysicsFlag = physicsFlag & UNSIGNED_BYTE_MASK;
        boolean noWall = hasNoWallCollision(entity);
        CollisionDecision decision = isBlocked(entity, sample, unsignedObjectId,
            unsignedPhysicsFlag, noWall, ignoreHitsCountdown, state);
        EntityBackgroundCollisionResult result = decision.blocked()
            ? EntityBackgroundCollisionResult.blocked(direction, unsignedObjectId,
                unsignedPhysicsFlag, sample.x(), sample.y())
            : EntityBackgroundCollisionResult.passableWithObject(direction, unsignedObjectId,
                unsignedPhysicsFlag, sample.x(), sample.y());
        return new EntityBackgroundCollisionResolution(result, decision.nextLedgeTimer());
    }

    private CollisionDecision isBlocked(RoomEntity entity,
                                         EntityCollisionPointProbe.Sample sample,
                                         int objectId,
                                         int physicsFlag,
                                         boolean noWall,
                                         int ignoreHitsCountdown,
                                         EntityBackgroundCollisionState state) {
        if (isWaterEntity(entity)) {
            if (physicsFlag == PHYSICS_SHALLOW_WATER || physicsFlag == PHYSICS_DEEP_WATER) {
                return passable(state);
            }
            return decision(!noWall, state);
        }

        if (physicsFlag == PHYSICS_NONE) {
            return passable(state);
        }

        if (physicsFlag == PHYSICS_LAVA
            || physicsFlag == PHYSICS_NORMAL_PIT
            || physicsFlag == PHYSICS_PIT_WARP) {
            if (entity.z() != 0) {
                return passable(state);
            }
            if ((ignoreHitsCountdown & UNSIGNED_BYTE_MASK) != 0
                && entity.type() != ENTITY_MOLDORM) {
                return passable(state);
            }
            return decision(!noWall, state);
        }

        if (isFineOrOpenDoorPhysics(physicsFlag)) {
            if (isOpenDoorPhysics(physicsFlag) && isSparkOrBoss(entity)) {
                return blocked(state);
            }
            if (physicsFlag >= PHYSICS_FINE_START && isBombOrWreckingBall(entity)) {
                return passable(state);
            }
            int quadrant = fineCollisionQuadrant(sample);
            return decision(romTables.entityFineCollisionShape(physicsFlag, quadrant) != 0
                && !noWall, state);
        }

        if (isConservativeLedgePhysics(physicsFlag)) {
            return resolveLedgeCollision(entity, physicsFlag, state);
        }
        if (physicsFlag == PHYSICS_SWITCH_BLOCK) {
            return resolveSwitchBlockCollision(entity, objectId, noWall, state);
        }
        if (physicsFlag == PHYSICS_TRACTOR) {
            return blocked(state);
        }
        if (isBroadPassablePhysics(physicsFlag)) {
            return passable(state);
        }
        if (isGenericCollisionPhysics(physicsFlag)
            || physicsFlag == PHYSICS_SOLID
            || physicsFlag == PHYSICS_DOOR) {
            return decision(!noWall, state);
        }
        return passable(state);
    }

    private static CollisionDecision resolveSwitchBlockCollision(
            RoomEntity entity, int objectId, boolean noWall,
            EntityBackgroundCollisionState state) {
        if (isBombOrWreckingBall(entity)) {
            return passable(state);
        }
        if (objectId < OBJECT_LOWERED_BLOCK || objectId > OBJECT_RAISED_BLOCK) {
            // The ROM treats other $04 objects as ocean and reaches the
            // collision-flag path without consulting the entity no-wall bit.
            return blocked(state);
        }
        int expectedState = SWITCH_BLOCK_STATE_BY_OBJECT[objectId - OBJECT_LOWERED_BLOCK];
        // A valid switch-block mismatch follows doesCollide -> hookshotEnd,
        // where hActiveEntityNoBGCollision can make the contact passable.
        return decision(expectedState != state.switchBlocksState() && !noWall, state);
    }

    private static CollisionDecision resolveLedgeCollision(
            RoomEntity entity, int physicsFlag, EntityBackgroundCollisionState state) {
        int nextTimer = state.ledgeTimer();
        int ledgeDirection = physicsFlag - PHYSICS_LEDGE_START;
        if (ledgeDirection == state.thrownDirection()) {
            if (entity.z() == 0) {
                return new CollisionDecision(true, nextTimer);
            }
            return new CollisionDecision(false, (nextTimer + 1) & UNSIGNED_BYTE_MASK);
        }

        if (entity.type() == ENTITY_WRECKING_BALL || nextTimer == 0) {
            return new CollisionDecision(true, nextTimer);
        }

        if ((state.frameCounter() & 0x03) != 0
            && (state.indoorRoom() || (state.frameCounter() & 0x01) != 0)) {
            nextTimer = (nextTimer - 1) & UNSIGNED_BYTE_MASK;
        }
        return new CollisionDecision(false, nextTimer);
    }

    private static CollisionDecision blocked(EntityBackgroundCollisionState state) {
        return new CollisionDecision(true, state.ledgeTimer());
    }

    private static CollisionDecision passable(EntityBackgroundCollisionState state) {
        return new CollisionDecision(false, state.ledgeTimer());
    }

    private static CollisionDecision decision(boolean blocked,
                                              EntityBackgroundCollisionState state) {
        return new CollisionDecision(blocked, state.ledgeTimer());
    }

    private boolean hasNoWallCollision(RoomEntity entity) {
        return (romTables.entityOptions1(entity.type()) & ENTITY_OPTION_NO_WALL_COLLISION) != 0;
    }

    private static boolean isWaterEntity(RoomEntity entity) {
        return entity.type() == ENTITY_FISH || entity.type() == ENTITY_WATER_TEKTITE;
    }

    private boolean isSparkOrBoss(RoomEntity entity) {
        return entity.type() == ENTITY_SPARK_COUNTER_CLOCKWISE
            || entity.type() == ENTITY_SPARK_CLOCKWISE
            || (romTables.entityOptions1(entity.type()) & ENTITY_OPTION_BOSS) != 0;
    }

    private static boolean isBombOrWreckingBall(RoomEntity entity) {
        return entity.type() == ENTITY_BOMB || entity.type() == ENTITY_WRECKING_BALL;
    }

    private static boolean isFineOrOpenDoorPhysics(int physicsFlag) {
        return physicsFlag >= PHYSICS_OPEN_DOOR_START && physicsFlag <= PHYSICS_FINE_END;
    }

    private static boolean isOpenDoorPhysics(int physicsFlag) {
        return physicsFlag >= PHYSICS_OPEN_DOOR_START && physicsFlag <= PHYSICS_OPEN_DOOR_END;
    }

    private static boolean isConservativeLedgePhysics(int physicsFlag) {
        return physicsFlag >= PHYSICS_LEDGE_START && physicsFlag <= PHYSICS_LEDGE_END;
    }

    private static boolean isBroadPassablePhysics(int physicsFlag) {
        return physicsFlag >= PHYSICS_BROAD_PASSABLE_START
            && physicsFlag <= PHYSICS_BROAD_PASSABLE_END;
    }

    private static boolean isGenericCollisionPhysics(int physicsFlag) {
        return physicsFlag >= PHYSICS_GENERIC_START && physicsFlag <= PHYSICS_GENERIC_END;
    }

    private static int fineCollisionQuadrant(EntityCollisionPointProbe.Sample sample) {
        int xQuadrant = (sample.x() & UNSIGNED_BYTE_MASK) >>> FINE_QUADRANT_SHIFT
            & FINE_QUADRANT_MASK;
        int yQuadrant = (sample.y() & UNSIGNED_BYTE_MASK) >>> FINE_QUADRANT_SHIFT
            & FINE_QUADRANT_MASK;
        return xQuadrant | (yQuadrant << 1);
    }

    private record CollisionDecision(boolean blocked, int nextLedgeTimer) {
    }
}
