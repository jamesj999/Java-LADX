package linksawakening.world;

/** Bank-$19 ordinary Masked Mimic/Goriya movement for entity $8F. */
final class MaskedMimicMotion {
    static final int NORMAL_OPTIONS1 = 0x48;
    static final int SPLASH_ONLY_OPTIONS1 = 0x08;

    // Data_019_47B6 is indexed by the low two joypad bits. The fourth entry
    // is the first opcode of Data_019_47B9, reached only by a simultaneous
    // left/right mask, exactly as the ROM's unchecked table indexing does.
    private static final int[] SPEED_X_BY_INPUT = {0x00, 0xF4, 0x0C, 0x00};
    // Data_019_47B9 is followed immediately by the handler's $F0 opcode.
    private static final int[] SPEED_Y_BY_INPUT = {0x00, 0x0C, 0xF4, 0xF0};

    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] inertia = new int[EntityRoomLoader.MAX_ENTITIES];

    record Update(RoomEntity entity, int options1, int directionToLink, boolean blocked) {
    }

    void initialize(int slot) {
        validateSlot(slot);
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        direction[slot] = 0;
        inertia[slot] = 0;
    }

    /**
     * Mirrors the ordinary path of MaskedMimicGoriyaEntityHandler. The
     * Cave-Water dispatch to GoriyaEntityHandler is selected by the caller;
     * this helper intentionally contains only the input-driven path.
     */
    Update advance(RoomEntity entity, int linkEntityX, int linkEntityY,
                   int romLinkDirection, int pressedButtonsMask, int collisionType,
                   RoomEntityBackgroundInteraction backgroundInteraction,
                   int ignoreHitsCountdown, int currentFrame) {
        validateRomDirection(romLinkDirection);
        int slot = entity.slot();
        int directionToLink = directionToLink(entity.x(), entity.y(), linkEntityX, linkEntityY);
        int options = ((direction[slot] ^ 0x01) & 0x03) == directionToLink
            ? SPLASH_ONLY_OPTIONS1 : NORMAL_OPTIONS1;

        // DefaultEnemyDamageCollisionHandler runs before the joypad path.
        // A nonzero collision byte returns without selecting new movement.
        if ((collisionType & 0xFF) != 0) {
            return new Update(entity, options, directionToLink, false);
        }

        int buttons = pressedButtonsMask & 0x0F;
        if (buttons == 0) {
            // The ROM returns before UpdateEntityPosWithSpeed and leaves the
            // previously selected speed, direction, and inertia intact.
            return new Update(entity, options, directionToLink, false);
        }

        speedX[slot] = SPEED_X_BY_INPUT[buttons & 0x03];
        speedY[slot] = SPEED_Y_BY_INPUT[(buttons >>> 2) & 0x03];

        int x = entity.x() & 0xFF;
        int y = entity.y() & 0xFF;
        boolean blocked = false;
        int nextX = addSpeedToPosition(x, speedX[slot], speedXAccumulator, slot);
        if (nextX != x) {
            EntityBackgroundCollisionResult result = backgroundInteraction == null
                ? null : backgroundInteraction.probe(entity, horizontalDirection(speedX[slot]),
                    nextX, y, ignoreHitsCountdown, currentFrame);
            if (result != null && result.blocked()) {
                blocked = true;
            } else {
                x = nextX;
            }
        }

        int nextY = addSpeedToPosition(y, speedY[slot], speedYAccumulator, slot);
        if (nextY != y) {
            EntityBackgroundCollisionResult result = backgroundInteraction == null
                ? null : backgroundInteraction.probe(entity, verticalDirection(speedY[slot]),
                    x, nextY, ignoreHitsCountdown, currentFrame);
            if (result != null && result.blocked()) {
                blocked = true;
            } else {
                y = nextY;
            }
        }

        // ApplyEntityInteractionWithBackground has returned at this point;
        // unlike the earlier collision byte, a terrain contact does not
        // bypass the handler's direction and inertia writes.
        direction[slot] = (romLinkDirection ^ 0x01) & 0x03;
        inertia[slot] = (inertia[slot] + 1) & 0xFF;
        int variantBase = (direction[slot] << 1) & 0x06;
        int variant = variantBase | ((inertia[slot] >>> 4) & 0x01);
        RoomEntity updated = withPositionAndVariant(entity, x, y, variant);
        return new Update(updated, options, directionToLink, blocked);
    }

    int speedX(int slot) {
        validateSlot(slot);
        return speedX[slot];
    }

    int speedY(int slot) {
        validateSlot(slot);
        return speedY[slot];
    }

    int direction(int slot) {
        validateSlot(slot);
        return direction[slot];
    }

    int inertia(int slot) {
        validateSlot(slot);
        return inertia[slot];
    }

    void clear(int slot) {
        initialize(slot);
    }

    private static int directionToLink(int entityX, int entityY,
                                       int linkEntityX, int linkEntityY) {
        int distanceX = signedByte((linkEntityX - entityX) & 0xFF);
        int distanceY = signedByte((linkEntityY - entityY) & 0xFF);
        if (Math.abs(distanceY) >= Math.abs(distanceX)) {
            return distanceY < 0 ? 2 : 3;
        }
        return distanceX < 0 ? 1 : 0;
    }

    /** Port of AddEntitySpeedToPos_19 for one Java pixel axis. */
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

    private static void validateRomDirection(int value) {
        if (value < 0 || value > 3) {
            throw new IllegalArgumentException("ROM Link direction must be 0..3: " + value);
        }
    }
}
