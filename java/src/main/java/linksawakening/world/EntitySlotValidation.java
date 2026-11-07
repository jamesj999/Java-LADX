package linksawakening.world;

/** Byte, direction and slot validation at ROM-facing boundaries. */
final class EntitySlotValidation {
    private EntitySlotValidation() {}

    static void validateCountdownTestValue(int slot, int value) {
        if (slot < 0 || slot >= EntityRoomLoader.MAX_ENTITIES) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException("Countdown must be an unsigned byte: " + value);
        }
    }

    static void validateEntitySlot(int slot) {
        if (slot < 0 || slot >= EntityRoomLoader.MAX_ENTITIES) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
    }

    static void validateEnemyDropByte(int value, String label) {
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException(label + " must be an unsigned byte: " + value);
        }
    }

    static void validateByte(int value, String label) {
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException(label + " must be an unsigned byte: " + value);
        }
    }

    static void validateRomDirection(int direction) {
        if (direction < LiftedEntityMotion.ROM_DIRECTION_RIGHT
            || direction > LiftedEntityMotion.ROM_DIRECTION_DOWN) {
            throw new IllegalArgumentException("ROM direction must be between 0 and 3");
        }
    }
}
