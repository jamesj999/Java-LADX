package linksawakening.world;

import java.util.Arrays;
import java.util.Objects;

/**
 * ROM-shaped dungeon item flags shared by the world handler and chest logic.
 *
 * <p>The disassembly keeps one five-byte current-dungeon buffer, a nine-entry
 * persistent table at {@code wDungeonItemFlags}, and a separate five-byte
 * Color Dungeon table. This class deliberately keeps those regions distinct
 * instead of treating Color Dungeon as an ordinary tenth table entry.</p>
 */
public final class DungeonItemState {

    public static final int MAP_WINDFISHS_EGG = 0x08;
    public static final int MAP_CAVE_B = 0x0A;
    public static final int MAP_COLOR_DUNGEON = 0xFF;

    public static final int ITEM_FLAG_SIZE = 0x05;
    public static final int DUNGEON_ITEM_FLAGS_SIZE = 0x2D;
    public static final int COLOR_DUNGEON_ITEM_FLAGS_SIZE = ITEM_FLAG_SIZE;

    public static final int MAP_INDEX = 0;
    public static final int COMPASS_INDEX = 1;
    public static final int STONE_BEAK_INDEX = 2;
    public static final int NIGHTMARE_KEY_INDEX = 3;
    public static final int SMALL_KEYS_INDEX = 4;

    // CHEST_MAP..CHEST_SMALL_KEY in constants/gameplay.asm.
    public static final int CHEST_MAP = 0x16;
    public static final int CHEST_COMPASS = 0x17;
    public static final int CHEST_STONE_BEAK = 0x18;
    public static final int CHEST_NIGHTMARE_KEY = 0x19;
    public static final int CHEST_SMALL_KEY = 0x1A;

    private final byte[] dungeonItemFlags = new byte[DUNGEON_ITEM_FLAGS_SIZE];
    private final byte[] colorDungeonItemFlags = new byte[COLOR_DUNGEON_ITEM_FLAGS_SIZE];
    private final byte[] currentFlags = new byte[ITEM_FLAG_SIZE];
    private int currentMapId = -1;

    /** Restores the persistent WRAM/SRAM regions without changing the current map buffer. */
    public void restore(byte[] persistentFlags, byte[] colorFlags) {
        requireLength(persistentFlags, DUNGEON_ITEM_FLAGS_SIZE, "persistentFlags");
        requireLength(colorFlags, COLOR_DUNGEON_ITEM_FLAGS_SIZE, "colorFlags");
        System.arraycopy(persistentFlags, 0, dungeonItemFlags, 0, DUNGEON_ITEM_FLAGS_SIZE);
        System.arraycopy(colorFlags, 0, colorDungeonItemFlags,
            0, COLOR_DUNGEON_ITEM_FLAGS_SIZE);
    }

    /**
     * Mirrors GameplayWorldLoad0Handler's copy into wCurrentDungeonItemFlags.
     * Ordinary dungeon IDs are 0..7; Wind Fish's Egg and cave/non-dungeon maps
     * intentionally expose zeroes to the dungeon inventory.
     */
    public void loadForMap(int mapId, boolean indoor) {
        if ((mapId & ~0xFF) != 0) {
            throw new IllegalArgumentException("Map id must be an unsigned byte: " + mapId);
        }
        currentMapId = indoor ? mapId : -1;
        Arrays.fill(currentFlags, (byte) 0);
        if (!indoor) {
            return;
        }
        if (mapId == MAP_COLOR_DUNGEON) {
            System.arraycopy(colorDungeonItemFlags, 0, currentFlags, 0, ITEM_FLAG_SIZE);
            return;
        }
        if (mapId < MAP_WINDFISHS_EGG) {
            int offset = mapId * ITEM_FLAG_SIZE;
            System.arraycopy(dungeonItemFlags, offset, currentFlags, 0, ITEM_FLAG_SIZE);
        }
    }

    public byte[] dungeonItemFlagsSnapshot() {
        return dungeonItemFlags.clone();
    }

    public byte[] colorDungeonItemFlagsSnapshot() {
        return colorDungeonItemFlags.clone();
    }

    public byte[] currentFlagsSnapshot() {
        return currentFlags.clone();
    }

    public int currentFlag(int index) {
        checkFlagIndex(index);
        return Byte.toUnsignedInt(currentFlags[index]);
    }

    /** Mirrors the chest handler's {@code inc [wHasDungeon...]} operation. */
    public void incrementCurrentFlag(int chestType) {
        int index = chestType - CHEST_MAP;
        if (index < MAP_INDEX || index > SMALL_KEYS_INDEX) {
            throw new IllegalArgumentException("Not a dungeon-item chest type: 0x"
                + Integer.toHexString(chestType));
        }
        currentFlags[index] = (byte) ((Byte.toUnsignedInt(currentFlags[index]) + 1) & 0xFF);
        synchronize();
    }

    /** Mirrors SynchronizeDungeonsItemFlags for the selected indoor map. */
    public void synchronize() {
        if (currentMapId == MAP_COLOR_DUNGEON) {
            System.arraycopy(currentFlags, 0, colorDungeonItemFlags, 0, ITEM_FLAG_SIZE);
            return;
        }
        if (currentMapId >= 0 && currentMapId < MAP_WINDFISHS_EGG) {
            int offset = currentMapId * ITEM_FLAG_SIZE;
            System.arraycopy(currentFlags, 0, dungeonItemFlags, offset, ITEM_FLAG_SIZE);
        }
    }

    private static void checkFlagIndex(int index) {
        if (index < 0 || index >= ITEM_FLAG_SIZE) {
            throw new IllegalArgumentException("Dungeon item flag index out of range: " + index);
        }
    }

    private static void requireLength(byte[] values, int expectedLength, String label) {
        Objects.requireNonNull(values, label);
        if (values.length != expectedLength) {
            throw new IllegalArgumentException(label + " must contain " + expectedLength
                + " bytes, got " + values.length);
        }
    }
}
