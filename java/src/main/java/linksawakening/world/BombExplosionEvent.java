package linksawakening.world;

/** A source-shaped bomb-explosion interaction request that has not been applied to room state. */
public record BombExplosionEvent(int bombSlot, int targetSlot, int bombX, int bombVisualY,
                                 int countdown, int damageType) {
    public static final int OBJECT_TARGET = -1;
    public static final int DAMAGE_TYPE_BOMB = 0x07;

    public BombExplosionEvent {
        validateSlot(bombSlot, "Bomb");
        if (targetSlot != OBJECT_TARGET) {
            validateSlot(targetSlot, "Target");
        }
        validateUnsignedByte(bombX, "Bomb X");
        validateUnsignedByte(bombVisualY, "Bomb visual Y");
        validateUnsignedByte(countdown, "Countdown");
        validateUnsignedByte(damageType, "Damage type");
    }

    public boolean targetsRoomObjects() {
        return targetSlot == OBJECT_TARGET;
    }

    private static void validateSlot(int slot, String name) {
        if (slot < 0 || slot >= EntityRoomLoader.MAX_ENTITIES) {
            throw new IllegalArgumentException(name + " slot out of range: " + slot);
        }
    }

    private static void validateUnsignedByte(int value, String name) {
        if ((value & ~0xFF) != 0) {
            throw new IllegalArgumentException(name + " must be an unsigned byte: " + value);
        }
    }
}
