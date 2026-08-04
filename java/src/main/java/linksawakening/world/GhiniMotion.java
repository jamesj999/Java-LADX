package linksawakening.world;

import java.util.function.IntSupplier;

/** The ordinary visible branch of bank-$04's GhiniEntityHandler. */
final class GhiniMotion {
    private static final int[] TARGET_X_SPEEDS = {0x0C, 0xF4};
    private static final int[] TARGET_Y_SPEEDS = {0x08, 0xF8};

    private final int[] transitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateCountdown2 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] targetXDirection = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] targetYDirection = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    void initialize(int slot) {
        transitionCountdown[slot] = 0;
        privateCountdown1[slot] = 0;
        privateCountdown2[slot] = 0;
        targetXDirection[slot] = 0;
        targetYDirection[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        initialized[slot] = true;
    }

    RoomEntity advance(RoomEntity entity, int frameCounter, IntSupplier randomByteSupplier) {
        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot);
        }
        decrementCountdowns(slot);

        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
        int z = correctZPosition(entity.z(), frameCounter);

        // The ordinary Ghini never enters the hiding state. Preserve the ROM
        // early return so the same state can be enabled for hiding Ghinis
        // without changing this visible branch.
        if (privateCountdown2[slot] != 0) {
            return withPosition(entity, x, y, z);
        }

        if (transitionCountdown[slot] == 0) {
            int random = randomByteSupplier.getAsInt() & 0xFF;
            transitionCountdown[slot] = 0x20 | (random & 0x1F);
            targetXDirection[slot] = random & 0x01;
        }
        if (privateCountdown1[slot] == 0) {
            int random = randomByteSupplier.getAsInt() & 0xFF;
            privateCountdown1[slot] = 0x18 | (random & 0x0F);
            targetYDirection[slot] = random & 0x01;
        }

        if ((((frameCounter & 0xFF) ^ slot) & 0x03) == 0) {
            if (x < 0x28) {
                targetXDirection[slot] = 0;
                transitionCountdown[slot] = 0x20;
            } else if (x >= 0x78) {
                targetXDirection[slot] = 1;
                transitionCountdown[slot] = 0x20;
            }

            int visualY = (y - z) & 0xFF;
            if (visualY < 0x28) {
                targetYDirection[slot] = 0;
                privateCountdown1[slot] = 0x20;
            } else if (visualY >= 0x60) {
                targetYDirection[slot] = 1;
                privateCountdown1[slot] = 0x20;
            }

            speedX[slot] = approachTarget(speedX[slot],
                TARGET_X_SPEEDS[targetXDirection[slot]]);
            speedY[slot] = approachTarget(speedY[slot],
                TARGET_Y_SPEEDS[targetYDirection[slot]]);
        }

        int flipAttribute = entity.entityFlipAttribute()
            ^ (isNegative(speedX[slot]) ? 0 : 0x20);
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(), x, y,
            entity.status(), entity.spriteDefinition(), entity.spriteVariant(), flipAttribute,
            entity.spriteTileOffset(), z);
    }

    void clear(int slot) {
        initialize(slot);
        initialized[slot] = false;
    }

    int transitionCountdown(int slot) {
        return transitionCountdown[slot];
    }

    int privateCountdown1(int slot) {
        return privateCountdown1[slot];
    }

    int targetXDirection(int slot) {
        return targetXDirection[slot];
    }

    int targetYDirection(int slot) {
        return targetYDirection[slot];
    }

    int speedX(int slot) {
        return speedX[slot];
    }

    int speedY(int slot) {
        return speedY[slot];
    }

    private void decrementCountdowns(int slot) {
        if (transitionCountdown[slot] > 0) {
            transitionCountdown[slot]--;
        }
        if (privateCountdown1[slot] > 0) {
            privateCountdown1[slot]--;
        }
        if (privateCountdown2[slot] > 0) {
            privateCountdown2[slot]--;
        }
    }

    private static int approachTarget(int current, int target) {
        int difference = (target - current) & 0xFF;
        return (difference & 0x80) != 0 ? (current - 1) & 0xFF : (current + 1) & 0xFF;
    }

    private static int correctZPosition(int z, int frameCounter) {
        if ((frameCounter & 0x03) != 0 || z == 0x10) {
            return z & 0xFF;
        }
        if ((z & 0x80) != 0 || z < 0x10) {
            return (z + 1) & 0xFF;
        }
        return (z - 1) & 0xFF;
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

    private static boolean isNegative(int speed) {
        return (speed & 0x80) != 0;
    }

    private static RoomEntity withPosition(RoomEntity entity, int x, int y, int z) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(), x, y,
            entity.status(), entity.spriteDefinition(), entity.spriteVariant(),
            entity.entityFlipAttribute(), entity.spriteTileOffset(), z);
    }
}
