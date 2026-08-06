package linksawakening.world;

/**
 * The position/carry presentation portion of bank-$03's EntityLiftedHandler.
 *
 * <p>The original indexes the four-byte direction tables with the current
 * lift phase. Phase four is deliberately allowed to read the next byte in
 * the contiguous ROM data, so the arrays below retain the same seventeen-byte
 * layout rather than treating each source label as an isolated Java array.</p>
 */
final class LiftedEntityMotion {
    static final int ROM_DIRECTION_RIGHT = 0;
    static final int ROM_DIRECTION_LEFT = 1;
    static final int ROM_DIRECTION_UP = 2;
    static final int ROM_DIRECTION_DOWN = 3;

    private static final int ENTITY_BOMB = 0x02;

    // Data_003_56EA and the following byte in Data_003_56F1.
    private static final int[] NORMAL_TRANSITION_COUNTDOWN = {0x01, 0x08, 0x08, 0x10};
    private static final int[] FAST_TRANSITION_COUNTDOWN = {0x01, 0x04, 0x04, 0x0A};

    // Data_003_56F1 through the first byte after each following table.
    private static final int[] CARRY_STATE = {
        0x0A, 0x37, 0x37, 0x37,
        0x01, 0x39, 0x39, 0x39,
        0x01, 0x3B, 0x3B, 0x3B,
        0x01, 0x3D, 0x3D, 0x3D,
        0x01
    };
    private static final int[] X_OFFSET = {
        0x01, 0x10, 0x10, 0x08,
        0x00, 0xF0, 0xF0, 0xF8,
        0x00, 0x00, 0x00, 0x00,
        0x00, 0xFF, 0xFF, 0xFF,
        0xFF
    };
    private static final int[] Y_OFFSET = {
        0xFF, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x08,
        0x00
    };
    private static final int[] Z_OFFSET = {
        0x00, 0x00, 0x00, 0x08,
        0x0E, 0x00, 0x00, 0x08,
        0x0E, 0x00, 0x00, 0x08,
        0x0E, 0x00, 0x00, 0x00,
        0x0E
    };

    record Update(int phase, int transitionCountdown, int carryState,
                  int effectiveDirection, int x, int y, int z) {
    }

    private LiftedEntityMotion() {
    }

    /** Advances one frame of the ROM's lifted-object presentation. */
    static Update advance(int phase, int transitionCountdown, int sourceRomDirection,
                          int currentRomDirection, int linkX, int linkY, int linkZ,
                          int linkC13B, int entityType, boolean sideScrolling,
                          boolean fastTransition, int existingEntityZ) {
        validatePhase(phase);
        validateByte(transitionCountdown, "Transition countdown");
        validateDirection(sourceRomDirection);
        validateDirection(currentRomDirection);
        validateByte(linkX, "Link X");
        validateByte(linkY, "Link Y");
        validateByte(linkZ, "Link Z");
        validateByte(linkC13B, "Link C13B");
        validateByte(entityType, "Entity type");
        validateByte(existingEntityZ, "Entity Z");

        int nextPhase = phase;
        int nextCountdown = transitionCountdown;
        int effectiveDirection = phase == 4 ? currentRomDirection : sourceRomDirection;

        if (phase != 4) {
            if (transitionCountdown == 0) {
                nextPhase = phase + 1;
                int[] countdownTable = fastTransition || entityType == ENTITY_BOMB
                    ? FAST_TRANSITION_COUNTDOWN : NORMAL_TRANSITION_COUNTDOWN;
                // The ROM's fast table is followed immediately by the first
                // carry byte, which is the phase-three read at offset three.
                nextCountdown = countdownTable[phase];
            }
            // EntityGetLiftedUp jumps directly into this handler with phase 0.
            // The handler increments the local phase for presentation even
            // while its countdown is still non-zero.
            if (nextPhase == 0) {
                nextPhase = 1;
            }
        }

        int tableIndex = effectiveDirection * 4 + nextPhase;
        if (tableIndex < 0 || tableIndex >= CARRY_STATE.length) {
            throw new IllegalArgumentException("Lift table index out of range: " + tableIndex);
        }

        int x = (linkX + X_OFFSET[tableIndex]) & 0xFF;
        int y = (linkY + Y_OFFSET[tableIndex] + linkC13B) & 0xFF;
        int z = existingEntityZ;
        if (sideScrolling) {
            y = (y - Z_OFFSET[tableIndex]) & 0xFF;
        } else {
            z = (linkZ + Z_OFFSET[tableIndex]) & 0xFF;
        }

        return new Update(nextPhase, nextCountdown, CARRY_STATE[tableIndex],
            effectiveDirection, x, y, z);
    }

    private static void validatePhase(int phase) {
        if (phase < 0 || phase > 4) {
            throw new IllegalArgumentException("Lift phase must be between 0 and 4");
        }
    }

    private static void validateDirection(int direction) {
        if (direction < ROM_DIRECTION_RIGHT || direction > ROM_DIRECTION_DOWN) {
            throw new IllegalArgumentException("ROM direction must be between 0 and 3");
        }
    }

    private static void validateByte(int value, String label) {
        if ((value & ~0xFF) != 0) {
            throw new IllegalArgumentException(label + " must be an unsigned byte");
        }
    }
}
