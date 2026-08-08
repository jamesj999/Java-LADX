package linksawakening.world;

/** Bank-$19 RoosterEntityHandler's active lifted/carry state. */
final class RoosterMotion {
    static final int ENTITY_TYPE = 0xD5;
    static final int INITIAL_PHYSICS_FLAGS =
        0x02 | 0x80 | 0x40 | 0x10;
    static final int LIFT_WAVE_SFX = 0x02;
    static final int BOOMERANG_SFX = 0x2D;

    private static final int[] DIRECTION_BY_DPAD = {
        0x0F, 0x00, 0x01, 0x0F, 0x02, 0x0F, 0x0F, 0x0F,
        0x03, 0x0F, 0x0F
    };
    private static final int[] POSITION_Z_BY_FRAME = {
        0x14, 0x14, 0x15, 0x16, 0x17, 0x17, 0x16, 0x15
    };

    record Update(int spriteVariant, int positionZ, int velocityZ,
                  int speedX, int speedY, int romDirection) {
    }

    private RoosterMotion() {
    }

    /**
     * Advances label_019_5B3C. The input speed and collision bytes are the
     * values Link wrote before AnimateEntities; the returned values are the
     * handler's next-frame HRAM writes.
     */
    static Update advanceLifted(int frameCounter, int positionZ,
                                int pressedButtonsMask, int collisionType,
                                int speedX, int speedY, int currentRomDirection) {
        validateByte(frameCounter, "Frame counter");
        validateByte(positionZ, "Link Z");
        validateByte(pressedButtonsMask, "Pressed buttons mask");
        validateByte(collisionType, "Collision type");
        validateByte(speedX, "Link X speed");
        validateByte(speedY, "Link Y speed");
        validateDirection(currentRomDirection);

        int dpadDirection = DIRECTION_BY_DPAD[pressedButtonsMask & 0x0F];
        int romDirection = dpadDirection == 0x0F
            ? currentRomDirection : dpadDirection;
        int spriteVariant = ((romDirection << 1) & 0x06)
            | ((frameCounter >>> 2) & 0x01);
        int nextPositionZ = positionZ;
        if ((frameCounter & 0x03) == 0) {
            int targetZ = POSITION_Z_BY_FRAME[(frameCounter >>> 2) & 0x07];
            int difference = (positionZ - targetZ) & 0xFF;
            if (difference != 0) {
                // The source uses SUB followed by the sign-bit test and then
                // INC/INC/DEC, which is a one-pixel move toward the target.
                nextPositionZ = (positionZ + ((difference & 0x80) != 0 ? 1 : -1))
                    & 0xFF;
            }
        }
        int nextSpeedX = (collisionType & 0x0C) == 0 ? speedX : 0;
        int nextSpeedY = (collisionType & 0x03) == 0 ? speedY : 0;
        return new Update(spriteVariant, nextPositionZ, 0,
            nextSpeedX, nextSpeedY, romDirection);
    }

    private static void validateByte(int value, String label) {
        if ((value & ~0xFF) != 0) {
            throw new IllegalArgumentException(label + " must be an unsigned byte");
        }
    }

    private static void validateDirection(int direction) {
        if (direction < LiftedEntityMotion.ROM_DIRECTION_RIGHT
            || direction > LiftedEntityMotion.ROM_DIRECTION_DOWN) {
            throw new IllegalArgumentException("ROM direction must be between 0 and 3");
        }
    }
}
