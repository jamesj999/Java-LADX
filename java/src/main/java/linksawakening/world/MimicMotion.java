package linksawakening.world;

/** Bank-$19 movement and input state for entity $28 (Mimic). */
final class MimicMotion {
    // Data_019_6AAC. Index $00 is the first opcode of the handler, but the
    // handler only reaches the meaningful $01/$02 entries for one direction.
    private static final int[] SPEED_BY_BUTTON_INDEX = {0x21, 0xF0, 0x10, 0x21};
    private static final int[] VERTICAL_VARIANT_BASE_BY_INDEX = {0, 2, 0, 6};

    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];

    record Update(RoomEntity entity) {
    }

    void initialize(int slot) {
        validateSlot(slot);
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
    }

    /**
     * Mirrors MimicEntityHandler's order: recoil has already run when this
     * method is called, so the current speed advances first; only then does
     * the handler inspect the background collision byte and joypad mask.
     */
    Update advance(RoomEntity entity, int frameCounter, int pressedButtonsMask,
                   int collisionType, RoomEntityBackgroundInteraction backgroundInteraction) {
        return advance(entity, frameCounter, pressedButtonsMask, collisionType,
            backgroundInteraction, 0, frameCounter);
    }

    Update advance(RoomEntity entity, int frameCounter, int pressedButtonsMask,
                   int collisionType, RoomEntityBackgroundInteraction backgroundInteraction,
                   int ignoreHitsCountdown, int currentFrame) {
        int slot = entity.slot();
        int x = entity.x() & 0xFF;
        int y = entity.y() & 0xFF;
        boolean blocked = false;

        int nextX = addSpeedToPosition(x, speedX[slot], speedXAccumulator, slot);
        if (nextX != x) {
            int direction = horizontalDirection(speedX[slot]);
            if (backgroundInteraction != null
                && backgroundInteraction.probe(entity, direction, nextX, y,
                    ignoreHitsCountdown, currentFrame).blocked()) {
                blocked = true;
            } else {
                x = nextX;
            }
        }

        int nextY = addSpeedToPosition(y, speedY[slot], speedYAccumulator, slot);
        if (nextY != y) {
            int direction = verticalDirection(speedY[slot]);
            if (backgroundInteraction != null
                && backgroundInteraction.probe(entity, direction, x, nextY,
                    ignoreHitsCountdown, currentFrame).blocked()) {
                blocked = true;
            } else {
                y = nextY;
            }
        }

        if (blocked || (collisionType & 0xFF) != 0) {
            clear(slot);
            return new Update(withPositionAndVariant(entity, x, y, entity.spriteVariant()));
        }

        int frameBit = ((frameCounter & 0xFF) >>> 3) & 0x01;
        int buttons = pressedButtonsMask & 0xFF;
        int horizontalButtons = buttons & 0x03;
        if (horizontalButtons != 0) {
            speedX[slot] = speedForIndex(horizontalButtons);
            speedY[slot] = 0;
            int variant = 0x04 + (horizontalButtons & 0x02) + frameBit;
            return new Update(withPositionAndVariant(entity, x, y, variant));
        }

        int verticalButtons = buttons & 0x0F;
        if (verticalButtons == 0) {
            clear(slot);
            return new Update(withPositionAndVariant(entity, x, y, entity.spriteVariant()));
        }

        int index = ((~(verticalButtons >>> 2)) & 0x03);
        speedY[slot] = speedForIndex(index);
        speedX[slot] = 0;
        int variant = VERTICAL_VARIANT_BASE_BY_INDEX[index] + frameBit;
        return new Update(withPositionAndVariant(entity, x, y, variant));
    }

    int speedX(int slot) {
        validateSlot(slot);
        return speedX[slot];
    }

    int speedY(int slot) {
        validateSlot(slot);
        return speedY[slot];
    }

    void setSpeedForTest(int slot, int newSpeedX, int newSpeedY) {
        validateSlot(slot);
        validateByte(newSpeedX, "Mimic X speed");
        validateByte(newSpeedY, "Mimic Y speed");
        speedX[slot] = newSpeedX;
        speedY[slot] = newSpeedY;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
    }

    void clear(int slot) {
        initialize(slot);
    }

    private static int speedForIndex(int index) {
        return index < SPEED_BY_BUTTON_INDEX.length ? SPEED_BY_BUTTON_INDEX[index] : 0;
    }

    /** Port of AddEntitySpeedToPos_19 for one axis. */
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

    private static int horizontalDirection(int speed) {
        return (speed & 0x80) != 0
            ? EntityBackgroundCollisionResult.LEFT : EntityBackgroundCollisionResult.RIGHT;
    }

    private static int verticalDirection(int speed) {
        return (speed & 0x80) != 0
            ? EntityBackgroundCollisionResult.UP : EntityBackgroundCollisionResult.DOWN;
    }

    private static RoomEntity withPositionAndVariant(RoomEntity entity, int x, int y,
                                                      int variant) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x & 0xFF, y & 0xFF, entity.status(), entity.spriteDefinition(), variant,
            entity.entityFlipAttribute(), entity.spriteTileOffset(), entity.z(),
            entity.deathSpriteVariant(), entity.powerRecoilDeath());
    }

    private static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    private static void validateSlot(int slot) {
        if (slot < 0 || slot >= EntityRoomLoader.MAX_ENTITIES) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
    }

    private static void validateByte(int value, String name) {
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException(name + " must be an unsigned byte: " + value);
        }
    }
}
