package linksawakening.world;

import java.util.Objects;

/** Fixed-point motion for the side-view pot's dedicated throw state. */
final class SideViewPotMotion {
    private static final int[] SPEED_X = {0x30, 0xD0, 0x00, 0x00};
    private static final int[] SPEED_Y = {0xF4, 0xF4, 0xD0, 0x00};

    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];

    record Update(RoomEntity entity, int speedY) {
    }

    /** Initializes the D6 side-scroll throw window from func_014_5409. */
    void start(int slot, int romDirection) {
        validateSlot(slot);
        validateDirection(romDirection);
        speedX[slot] = SPEED_X[romDirection];
        speedY[slot] = SPEED_Y[romDirection];
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        state[slot] = 2;
    }

    /** Advances X and Y before applying the source's unsigned gravity check. */
    Update advance(RoomEntity entity) {
        Objects.requireNonNull(entity, "Side-view pot entity cannot be null");
        validateSlot(entity.slot());
        int slot = entity.slot();
        if (state[slot] != 2) {
            return new Update(entity, speedY[slot]);
        }
        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);

        int currentSpeedY = speedY[slot] & 0xFF;
        if ((currentSpeedY & 0x80) != 0 || currentSpeedY < 0x40) {
            speedY[slot] = (currentSpeedY + 2) & 0xFF;
        }
        return new Update(withPosition(entity, x, y), speedY[slot]);
    }

    boolean active(int slot) {
        validateSlot(slot);
        return state[slot] == 2;
    }

    int speedX(int slot) {
        validateSlot(slot);
        return speedX[slot];
    }

    int speedY(int slot) {
        validateSlot(slot);
        return speedY[slot];
    }

    void setSpeedX(int slot, int value) {
        validateSlot(slot);
        validateByte(value, "Side-view pot X speed");
        speedX[slot] = value;
        state[slot] = 2;
    }

    void setSpeedY(int slot, int value) {
        validateSlot(slot);
        validateByte(value, "Side-view pot Y speed");
        speedY[slot] = value;
        state[slot] = 2;
    }

    void clear(int slot) {
        validateSlot(slot);
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        state[slot] = 0;
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

    private static RoomEntity withPosition(RoomEntity entity, int x, int y) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x & 0xFF, y & 0xFF, entity.status(), entity.spriteDefinition(),
            entity.spriteVariant(), entity.entityFlipAttribute(), entity.spriteTileOffset(),
            entity.z(), entity.deathSpriteVariant(), entity.powerRecoilDeath());
    }

    private static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    int state(int slot) {
        validateSlot(slot);
        return state[slot];
    }

    private static void validateDirection(int direction) {
        if (direction < ThrownEntityMotion.ROM_DIRECTION_RIGHT
            || direction > ThrownEntityMotion.ROM_DIRECTION_DOWN) {
            throw new IllegalArgumentException("ROM direction out of range: " + direction);
        }
    }

    private static void validateSlot(int slot) {
        if (slot < 0 || slot >= EntityRoomLoader.MAX_ENTITIES) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
    }

    private static void validateByte(int value, String label) {
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException(label + " must be an unsigned byte: " + value);
        }
    }
}
