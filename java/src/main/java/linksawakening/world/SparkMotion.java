package linksawakening.world;

/** Bank-$06 SparkClockwise/CounterClockwiseEntityHandler's orbit loop. */
final class SparkMotion {
    // Data_006_661D and Data_006_6625. The tables are indexed by
    // (privateState1 + privateState2), as in the ROM.
    private static final int[] SPEED_Y_BY_INDEX = {
        0x00, 0x10, 0x00, 0xF0, 0x00, 0xF0, 0x00, 0x10
    };
    private static final int[] SPEED_X_BY_INDEX = {
        0x10, 0x00, 0xF0, 0x00, 0x10, 0x00, 0xF0, 0x00
    };
    private static final int[] COLLISION_MASK_BY_INDEX = {
        0x01, 0x08, 0x02, 0x04, 0x01, 0x04, 0x02, 0x08
    };

    private final int[] privateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateState2 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] transitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    /** Mirrors EntityInitSparkCounterClockwise/Clockwise in bank $03. */
    void initialize(int slot, int entityType) {
        privateState1[slot] = 0;
        privateState2[slot] = entityType == 0x17 ? 4 : 0;
        transitionCountdown[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        initialized[slot] = true;
    }

    RoomEntity applyInitializationOffset(RoomEntity entity) {
        int offset = privateState2[entity.slot()] == 4 ? 3 : -3;
        return withPosition(entity, entity.x(), entity.y() + offset);
    }

    RoomEntity advance(RoomEntity entity, int frameCounter,
                       RoomEntityBackgroundCollision backgroundCollision) {
        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot, entity.type());
            entity = applyInitializationOffset(entity);
        }
        if (transitionCountdown[slot] > 0) {
            transitionCountdown[slot]--;
        }

        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
        RoomEntity moved = withPosition(entity, x, y);
        int collisionMask = collisionMask(moved, backgroundCollision);
        int tableIndex = (privateState1[slot] + privateState2[slot]) & 0x07;

        if ((collisionMask & COLLISION_MASK_BY_INDEX[tableIndex]) != 0) {
            privateState1[slot] = (privateState1[slot] + 1) & 0x03;
        } else if (transitionCountdown[slot] != 0) {
            if (transitionCountdown[slot] == 6) {
                privateState1[slot] = (privateState1[slot] + 3) & 0x03;
            }
        } else if ((collisionMask & 0x0F) == 0) {
            transitionCountdown[slot] = 9;
        }

        tableIndex = (privateState1[slot] + privateState2[slot]) & 0x07;
        speedY[slot] = SPEED_Y_BY_INDEX[tableIndex];
        speedX[slot] = SPEED_X_BY_INDEX[tableIndex];
        int variant = (frameCounter >>> 1) & 0x01;
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(), x, y,
            entity.status(), entity.spriteDefinition(), variant, entity.entityFlipAttribute(),
            entity.spriteTileOffset(), entity.z());
    }

    void clear(int slot) {
        privateState1[slot] = 0;
        privateState2[slot] = 0;
        transitionCountdown[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        initialized[slot] = false;
    }

    int privateState1(int slot) {
        return privateState1[slot];
    }

    int privateState2(int slot) {
        return privateState2[slot];
    }

    int transitionCountdown(int slot) {
        return transitionCountdown[slot];
    }

    int speedX(int slot) {
        return speedX[slot];
    }

    int speedY(int slot) {
        return speedY[slot];
    }

    private static int collisionMask(RoomEntity entity,
                                     RoomEntityBackgroundCollision backgroundCollision) {
        if (backgroundCollision == null) {
            return 0;
        }
        int mask = 0;
        if (backgroundCollision.blocks(entity, 0, entity.x(), entity.y())) {
            mask |= 0x01;
        }
        if (backgroundCollision.blocks(entity, 1, entity.x(), entity.y())) {
            mask |= 0x02;
        }
        if (backgroundCollision.blocks(entity, 2, entity.x(), entity.y())) {
            mask |= 0x04;
        }
        if (backgroundCollision.blocks(entity, 3, entity.x(), entity.y())) {
            mask |= 0x08;
        }
        return mask;
    }

    private static RoomEntity withPosition(RoomEntity entity, int x, int y) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x & 0xFF, y & 0xFF, entity.status(), entity.spriteDefinition(),
            entity.spriteVariant(), entity.entityFlipAttribute(), entity.spriteTileOffset(),
            entity.z());
    }

    private static int addSpeedToPosition(int position, int speed, int[] accumulator,
                                          int slot) {
        speed &= 0xFF;
        if (speed == 0) {
            return position & 0xFF;
        }

        int fractionalSum = accumulator[slot] + ((speed << 4) & 0xF0);
        accumulator[slot] = fractionalSum & 0xFF;
        int signedSpeed = speed < 0x80 ? speed : speed - 0x100;
        int delta = signedSpeed >> 4;
        if (fractionalSum > 0xFF) {
            delta++;
        }
        return (position + delta) & 0xFF;
    }
}
