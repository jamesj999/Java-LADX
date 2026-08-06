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
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(sample, "sample");

        int unsignedObjectId = objectId & UNSIGNED_BYTE_MASK;
        int unsignedPhysicsFlag = physicsFlag & UNSIGNED_BYTE_MASK;
        boolean noWall = hasNoWallCollision(entity);
        boolean blocked = isBlocked(entity, sample, unsignedPhysicsFlag, noWall,
            ignoreHitsCountdown);
        return blocked
            ? EntityBackgroundCollisionResult.blocked(direction, unsignedObjectId,
                unsignedPhysicsFlag, sample.x(), sample.y())
            : EntityBackgroundCollisionResult.passableWithObject(direction, unsignedObjectId,
                unsignedPhysicsFlag, sample.x(), sample.y());
    }

    private boolean isBlocked(RoomEntity entity,
                              EntityCollisionPointProbe.Sample sample,
                              int physicsFlag,
                              boolean noWall,
                              int ignoreHitsCountdown) {
        if (isWaterEntity(entity)) {
            if (physicsFlag == PHYSICS_SHALLOW_WATER || physicsFlag == PHYSICS_DEEP_WATER) {
                return false;
            }
            return !noWall;
        }

        if (physicsFlag == PHYSICS_NONE) {
            return false;
        }

        if (physicsFlag == PHYSICS_LAVA
            || physicsFlag == PHYSICS_NORMAL_PIT
            || physicsFlag == PHYSICS_PIT_WARP) {
            if (entity.z() != 0) {
                return false;
            }
            if ((ignoreHitsCountdown & UNSIGNED_BYTE_MASK) != 0
                && entity.type() != ENTITY_MOLDORM) {
                return false;
            }
            return !noWall;
        }

        if (isFineOrOpenDoorPhysics(physicsFlag)) {
            if (isOpenDoorPhysics(physicsFlag) && isSparkOrBoss(entity)) {
                return true;
            }
            if (physicsFlag >= PHYSICS_FINE_START && isBombOrWreckingBall(entity)) {
                return false;
            }
            int quadrant = fineCollisionQuadrant(sample);
            return romTables.entityFineCollisionShape(physicsFlag, quadrant) != 0 && !noWall;
        }

        if (isConservativeLedgePhysics(physicsFlag)) {
            // Unconditional blocking is intentional until thrown-direction and
            // WRAM ledge-timer state are exposed.
            return true;
        }
        if (physicsFlag == PHYSICS_SWITCH_BLOCK) {
            if (isBombOrWreckingBall(entity)) {
                return false;
            }
            // Object/state-dependent switch-block exceptions remain deferred
            // until the required WRAM state is exposed.
            return true;
        }
        if (physicsFlag == PHYSICS_TRACTOR) {
            return true;
        }
        if (isBroadPassablePhysics(physicsFlag)) {
            return false;
        }
        if (isGenericCollisionPhysics(physicsFlag)
            || physicsFlag == PHYSICS_SOLID
            || physicsFlag == PHYSICS_DOOR) {
            return !noWall;
        }
        return false;
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
}
