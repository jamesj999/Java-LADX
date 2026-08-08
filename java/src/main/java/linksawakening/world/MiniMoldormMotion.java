package linksawakening.world;

import java.util.Arrays;
import java.util.function.IntSupplier;

/**
 * Bank-$04 movement and position history for Mini Moldorm ($29).
 *
 * <p>The ROM gives each loaded Mini Moldorm a 32-byte X/Y history keyed by
 * source load order.  Its head moves from the fixed-point speed tables while
 * the two tail segments render samples nine and sixteen frames behind it.</p>
 */
final class MiniMoldormMotion {
    private static final int HISTORY_LENGTH = 0x20;
    private static final int HISTORY_MASK = HISTORY_LENGTH - 1;
    private static final int[] HEAD_SPRITE_VARIANTS = {
        0x03, 0x03, 0x05, 0x05, 0x00, 0x00, 0x04, 0x04,
        0x02, 0x02, 0x06, 0x06, 0x01, 0x01, 0x07, 0x07
    };
    // The ROM's four-byte MoldormYSpeeds label is immediately followed by
    // MoldormXSpeeds. Indexing it with the full 0..$0F angle therefore yields
    // this contiguous sixteen-byte sine table.
    private static final int[] Y_SPEEDS = {
        0x00, 0x06, 0x0C, 0x0E, 0x10, 0x0E, 0x0C, 0x06,
        0x00, 0xFA, 0xF4, 0xF2, 0xF0, 0xF2, 0xF4, 0xFA
    };
    private static final int[] X_SPEEDS = {
        0x10, 0x0E, 0x0C, 0x06, 0x00, 0xFA, 0xF4, 0xF2,
        0xF0, 0xF2, 0xF4, 0xFA, 0x00, 0x06, 0x0C, 0x0E
    };

    private final int[][] historyX = new int[EntityRoomLoader.MAX_ENTITIES][HISTORY_LENGTH];
    private final int[][] historyY = new int[EntityRoomLoader.MAX_ENTITIES][HISTORY_LENGTH];
    private final int[] historyKey = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] inertia = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateState2 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] headSpriteVariant = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    record Update(RoomEntity entity, int transitionCountdown, int headSpriteVariant,
                  int collisionFlags, int segment1X, int segment1Y,
                  int segment2X, int segment2Y) {
    }

    void initialize(RoomEntity entity) {
        int slot = entity.slot();
        int key = historyKeyFor(entity);
        historyKey[slot] = key;
        Arrays.fill(historyX[key], 0);
        Arrays.fill(historyY[key], 0);
        inertia[slot] = 0;
        privateState1[slot] = 0;
        privateState2[slot] = 0;
        privateCountdown1[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        headSpriteVariant[slot] = entity.spriteVariant() >= 0
            && entity.spriteVariant() < 8 ? entity.spriteVariant() : 0;
        initialized[slot] = true;
    }

    void clear(int slot) {
        if (slot < 0 || slot >= initialized.length) {
            return;
        }
        if (initialized[slot]) {
            Arrays.fill(historyX[historyKey[slot]], 0);
            Arrays.fill(historyY[historyKey[slot]], 0);
        }
        initialized[slot] = false;
        inertia[slot] = 0;
        privateState1[slot] = 0;
        privateState2[slot] = 0;
        privateCountdown1[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        headSpriteVariant[slot] = 0;
    }

    /** Records the current visual position after the ROM's reset check. */
    void beginFrame(RoomEntity entity, boolean resetHistory) {
        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(entity);
        }
        int key = historyKey[slot];
        if (resetHistory) {
            Arrays.fill(historyX[key], 0);
            Arrays.fill(historyY[key], 0);
        }
        inertia[slot] = (inertia[slot] + 1) & HISTORY_MASK;
        int index = inertia[slot];
        historyX[key][index] = entity.x() & 0xFF;
        historyY[key][index] = (entity.y() - entity.z()) & 0xFF;
    }

    Update advanceAfterHistory(RoomEntity entity, int transitionCountdown,
                               int ignoreHitsCountdown, int flashCountdown,
                               RoomEntityBackgroundInteraction backgroundInteraction,
                               IntSupplier randomByteSupplier, int frameCounter) {
        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(entity);
        }
        if (randomByteSupplier == null) {
            throw new IllegalArgumentException("Mini Moldorm random source cannot be null");
        }
        if (privateCountdown1[slot] > 0) {
            privateCountdown1[slot]--;
        }

        int nextTransitionCountdown = transitionCountdown & 0xFF;
        if (nextTransitionCountdown > 0) {
            nextTransitionCountdown--;
        }
        int x = entity.x() & 0xFF;
        int y = entity.y() & 0xFF;
        int collisionFlags = 0;
        if ((flashCountdown & 0xFF) == 0) {
            int nextX = addSpeedToPosition(x, speedX[slot], speedXAccumulator, slot);
            if (nextX != x && backgroundInteraction != null) {
                int direction = signedByte(speedX[slot]) < 0
                    ? EntityBackgroundCollisionResult.LEFT
                    : EntityBackgroundCollisionResult.RIGHT;
                EntityBackgroundCollisionResult result = backgroundInteraction.probe(
                    entity, direction, nextX, y, ignoreHitsCountdown & 0xFF, frameCounter);
                if (result.blocked()) {
                    collisionFlags |= result.collisionFlag();
                } else {
                    x = nextX;
                }
            } else {
                x = nextX;
            }

            int nextY = addSpeedToPosition(y, speedY[slot], speedYAccumulator, slot);
            if (nextY != y && backgroundInteraction != null) {
                int direction = signedByte(speedY[slot]) < 0
                    ? EntityBackgroundCollisionResult.UP
                    : EntityBackgroundCollisionResult.DOWN;
                EntityBackgroundCollisionResult result = backgroundInteraction.probe(
                    entity, direction, x, nextY, ignoreHitsCountdown & 0xFF, frameCounter);
                if (result.blocked()) {
                    collisionFlags |= result.collisionFlag();
                } else {
                    y = nextY;
                }
            } else {
                y = nextY;
            }
        }

        if (collisionFlags != 0) {
            privateState1[slot] = (collisionFlags & 0x01) != 0 ? 0x08
                : (collisionFlags & 0x02) != 0 ? 0x00
                : (collisionFlags & 0x04) != 0 ? 0x04 : 0x0C;
            if ((randomByteSupplier.getAsInt() & 0x01) == 0) {
                privateState2[slot] = -privateState2[slot];
            }
            nextTransitionCountdown = 0x10;
        }

        if (privateCountdown1[slot] == 0) {
            privateCountdown1[slot] = 0x04;
            privateState1[slot] = (privateState1[slot] + privateState2[slot]) & 0x0F;
            headSpriteVariant[slot] = HEAD_SPRITE_VARIANTS[privateState1[slot]];
            speedY[slot] = Y_SPEEDS[privateState1[slot]];
            speedX[slot] = X_SPEEDS[privateState1[slot]];
        }

        if (nextTransitionCountdown == 0) {
            nextTransitionCountdown = 0x10
                + (randomByteSupplier.getAsInt() & 0x1F);
            privateState2[slot] = (randomByteSupplier.getAsInt() & 0x02) - 1;
        }

        int key = historyKey[slot];
        int segment1Index = (inertia[slot] - 0x09) & HISTORY_MASK;
        int segment2Index = (inertia[slot] - 0x10) & HISTORY_MASK;
        RoomEntity moved = withPosition(entity, x, y);
        return new Update(moved, nextTransitionCountdown, headSpriteVariant[slot],
            collisionFlags, historyX[key][segment1Index], historyY[key][segment1Index],
            historyX[key][segment2Index], historyY[key][segment2Index]);
    }

    int inertia(int slot) {
        return inertia[slot];
    }

    int historyX(int sourceLoadOrder, int index) {
        return historyX[historyKeyFor(sourceLoadOrder)][index & HISTORY_MASK];
    }

    int historyY(int sourceLoadOrder, int index) {
        return historyY[historyKeyFor(sourceLoadOrder)][index & HISTORY_MASK];
    }

    int privateState1(int slot) {
        return privateState1[slot];
    }

    int privateState2(int slot) {
        return privateState2[slot];
    }

    int speedX(int slot) {
        return speedX[slot];
    }

    int speedY(int slot) {
        return speedY[slot];
    }

    private static int historyKeyFor(RoomEntity entity) {
        return historyKeyFor(entity.sourceLoadOrder() < 0
            ? entity.slot() : entity.sourceLoadOrder());
    }

    private static int historyKeyFor(int sourceLoadOrder) {
        if (sourceLoadOrder < 0 || sourceLoadOrder >= EntityRoomLoader.MAX_ENTITIES) {
            throw new IllegalArgumentException("Mini Moldorm history key out of range: "
                + sourceLoadOrder);
        }
        return sourceLoadOrder;
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
        int delta = signedByte(speed) >> 4;
        if (fractionalSum > 0xFF) {
            delta++;
        }
        return (position + delta) & 0xFF;
    }

    private static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }
}
