package linksawakening.save;

import linksawakening.state.PlayerState;

import java.util.Arrays;
import java.util.Objects;

/** Mutable raw image of the source-defined SRAM save area. */
public final class SaveRamImage {

    private final byte[] bytes;

    private SaveRamImage(byte[] bytes) {
        if (bytes.length != SaveRamLayout.IMAGE_SIZE) {
            throw new IllegalArgumentException("Save image must be exactly "
                + SaveRamLayout.IMAGE_SIZE + " bytes, got " + bytes.length);
        }
        this.bytes = bytes.clone();
    }

    public static SaveRamImage empty() {
        byte[] bytes = new byte[SaveRamLayout.IMAGE_SIZE];
        for (int slot = 0; slot < SaveRamLayout.SLOT_COUNT; slot++) {
            SaveRamLayout.writeValidPrefix(bytes, SaveRamLayout.slotOffset(slot));
        }
        return new SaveRamImage(bytes);
    }

    public static SaveRamImage fromBytes(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        return new SaveRamImage(bytes);
    }

    public byte[] bytes() {
        return bytes.clone();
    }

    /** Returns the file-menu mask derived from valid prefixes and nonzero names. */
    public int saveFilesMask() {
        int mask = 0;
        for (int slot = 0; slot < SaveRamLayout.SLOT_COUNT; slot++) {
            int slotOffset = SaveRamLayout.slotOffset(slot);
            if (!SaveRamLayout.hasValidPrefix(bytes, slotOffset)) {
                continue;
            }
            if (hasStoredName(slotOffset)) {
                mask |= 1 << slot;
            }
        }
        return mask;
    }

    public int[][] savedNames() {
        int[][] names = new int[SaveRamLayout.SLOT_COUNT][SaveRamLayout.NAME_LENGTH];
        for (int slot = 0; slot < SaveRamLayout.SLOT_COUNT; slot++) {
            int slotOffset = SaveRamLayout.slotOffset(slot);
            if (!SaveRamLayout.hasValidPrefix(bytes, slotOffset)) {
                continue;
            }
            int main = slotOffset + SaveRamLayout.mainOffset();
            for (int index = 0; index < SaveRamLayout.NAME_LENGTH; index++) {
                names[slot][index] = unsigned(bytes[main + SaveRamLayout.MAIN_NAME_OFFSET + index]);
            }
        }
        return names;
    }

    /** Writes the bytes emitted by the ROM's file-creation handler. */
    public void createNewGame(int slot, int[] nameBytes) {
        SaveRamLayout.checkSlot(slot);
        Objects.requireNonNull(nameBytes, "nameBytes");
        if (nameBytes.length != SaveRamLayout.NAME_LENGTH) {
            throw new IllegalArgumentException("nameBytes must contain "
                + SaveRamLayout.NAME_LENGTH + " bytes, got " + nameBytes.length);
        }
        for (int value : nameBytes) {
            if (value < 0 || value > 0xFF) {
                throw new IllegalArgumentException("Name byte outside unsigned byte range: " + value);
            }
        }

        int slotOffset = SaveRamLayout.slotOffset(slot);
        Arrays.fill(bytes, slotOffset, slotOffset + SaveRamLayout.SLOT_SIZE, (byte) 0);
        SaveRamLayout.writeValidPrefix(bytes, slotOffset);
        int main = slotOffset + SaveRamLayout.mainOffset();
        for (int index = 0; index < nameBytes.length; index++) {
            bytes[main + SaveRamLayout.MAIN_NAME_OFFSET + index] = (byte) nameBytes[index];
        }
        bytes[main + SaveRamLayout.MAIN_HEALTH_OFFSET] = 0x18;
        bytes[main + SaveRamLayout.MAIN_MAX_HEARTS_OFFSET] = 0x03;
        Arrays.fill(bytes, main + SaveRamLayout.MAIN_DEATH_COUNT_OFFSET,
            main + SaveRamLayout.MAIN_DEATH_COUNT_OFFSET + 3, (byte) 0);
    }

    /** Copies the complete source-defined 0x3AD-byte save slot. */
    public void copySlot(int sourceSlot, int targetSlot) {
        SaveRamLayout.checkSlot(sourceSlot);
        SaveRamLayout.checkSlot(targetSlot);
        System.arraycopy(bytes, SaveRamLayout.slotOffset(sourceSlot), bytes,
            SaveRamLayout.slotOffset(targetSlot), SaveRamLayout.SLOT_SIZE);
    }

    /** Clears the ROM slot extent, then restores this model's valid empty-slot prefix. */
    public void eraseSlot(int slot) {
        SaveRamLayout.checkSlot(slot);
        int slotOffset = SaveRamLayout.slotOffset(slot);
        Arrays.fill(bytes, slotOffset, slotOffset + SaveRamLayout.SLOT_SIZE, (byte) 0);
        SaveRamLayout.writeValidPrefix(bytes, slotOffset);
    }

    /**
     * Writes the live Ocarina fields copied by the source save path.
     *
     * <p>The game only assigns bits 0..2 of {@code wOcarinaSongFlags}; the
     * selected-song byte is constrained to the three song entries exposed by
     * the Ocarina popup.</p>
     */
    public void writeOcarinaState(int slot, int songFlags, int selectedSongIndex) {
        SaveRamLayout.checkSlot(slot);
        int main = SaveRamLayout.slotOffset(slot) + SaveRamLayout.mainOffset();
        bytes[main + SaveRamLayout.MAIN_OCARINA_SONG_FLAGS_OFFSET] = (byte) (songFlags & 0x07);
        bytes[main + SaveRamLayout.MAIN_SELECTED_SONG_INDEX_OFFSET]
            = (byte) Math.max(0, Math.min(2, selectedSongIndex));
    }

    /**
     * Writes the room-status bytes copied by the source {@code SaveGameToFile} routine.
     *
     * <p>The overworld, indoor-A, and indoor-B tables are contiguous at the
     * beginning of the main save block. The Color Dungeon table is the DX2
     * extension and is intentionally limited to the source's 0x20 bytes.</p>
     */
    public void writeRoomStatuses(int slot, byte[] overworldRoomStatus,
                                  byte[] indoorARoomStatus, byte[] indoorBRoomStatus,
                                  byte[] colorDungeonRoomStatus) {
        SaveRamLayout.checkSlot(slot);
        requireLength(overworldRoomStatus, SaveRamLayout.ROOM_STATUS_TABLE_SIZE,
            "overworldRoomStatus");
        requireLength(indoorARoomStatus, SaveRamLayout.ROOM_STATUS_TABLE_SIZE,
            "indoorARoomStatus");
        requireLength(indoorBRoomStatus, SaveRamLayout.ROOM_STATUS_TABLE_SIZE,
            "indoorBRoomStatus");
        requireLength(colorDungeonRoomStatus, SaveRamLayout.DX2_COLOR_DUNGEON_ROOM_STATUS_SIZE,
            "colorDungeonRoomStatus");

        int slotOffset = SaveRamLayout.slotOffset(slot);
        int main = slotOffset + SaveRamLayout.mainOffset();
        System.arraycopy(overworldRoomStatus, 0,
            bytes, main + SaveRamLayout.MAIN_OVERWORLD_ROOM_STATUS_OFFSET,
            SaveRamLayout.ROOM_STATUS_TABLE_SIZE);
        System.arraycopy(indoorARoomStatus, 0,
            bytes, main + SaveRamLayout.MAIN_INDOOR_A_ROOM_STATUS_OFFSET,
            SaveRamLayout.ROOM_STATUS_TABLE_SIZE);
        System.arraycopy(indoorBRoomStatus, 0,
            bytes, main + SaveRamLayout.MAIN_INDOOR_B_ROOM_STATUS_OFFSET,
            SaveRamLayout.ROOM_STATUS_TABLE_SIZE);
        System.arraycopy(colorDungeonRoomStatus, 0,
            bytes, slotOffset + SaveRamLayout.dx2Offset(),
            SaveRamLayout.DX2_COLOR_DUNGEON_ROOM_STATUS_SIZE);
    }

    /** Writes the source's persistent dungeon item table and Color Dungeon extension. */
    public void writeDungeonItemFlags(int slot, byte[] dungeonItemFlags,
                                      byte[] colorDungeonItemFlags) {
        SaveRamLayout.checkSlot(slot);
        requireLength(dungeonItemFlags, SaveRamLayout.MAIN_DUNGEON_ITEM_FLAGS_SIZE,
            "dungeonItemFlags");
        requireLength(colorDungeonItemFlags, SaveRamLayout.DX1_COLOR_DUNGEON_ITEM_FLAGS_SIZE,
            "colorDungeonItemFlags");

        int slotOffset = SaveRamLayout.slotOffset(slot);
        int main = slotOffset + SaveRamLayout.mainOffset();
        System.arraycopy(dungeonItemFlags, 0, bytes,
            main + SaveRamLayout.MAIN_DUNGEON_ITEM_FLAGS_OFFSET,
            SaveRamLayout.MAIN_DUNGEON_ITEM_FLAGS_SIZE);
        System.arraycopy(colorDungeonItemFlags, 0, bytes,
            slotOffset + SaveRamLayout.dx1Offset(),
            SaveRamLayout.DX1_COLOR_DUNGEON_ITEM_FLAGS_SIZE);
    }

    /** Writes the source {@code wSpawnLocationData} fields in the main block. */
    public void writeSpawnLocation(int slot, int isIndoor, int mapId, int mapRoom,
                                   int positionX, int positionY, int indoorRoom) {
        SaveRamLayout.checkSlot(slot);
        requireByte(isIndoor, "isIndoor");
        requireByte(mapId, "mapId");
        requireByte(mapRoom, "mapRoom");
        requireByte(positionX, "positionX");
        requireByte(positionY, "positionY");
        requireByte(indoorRoom, "indoorRoom");

        int main = SaveRamLayout.slotOffset(slot) + SaveRamLayout.mainOffset();
        bytes[main + SaveRamLayout.MAIN_SPAWN_INDOOR_OFFSET] = (byte) isIndoor;
        bytes[main + SaveRamLayout.MAIN_SPAWN_MAP_ID_OFFSET] = (byte) mapId;
        bytes[main + SaveRamLayout.MAIN_SPAWN_MAP_ROOM_OFFSET] = (byte) mapRoom;
        bytes[main + SaveRamLayout.MAIN_SPAWN_X_OFFSET] = (byte) positionX;
        bytes[main + SaveRamLayout.MAIN_SPAWN_Y_OFFSET] = (byte) positionY;
        bytes[main + SaveRamLayout.MAIN_SPAWN_INDOOR_ROOM_OFFSET] = (byte) indoorRoom;
    }

    /**
     * Writes every persistent field currently represented by {@link PlayerState}.
     *
     * <p>The source copies the complete WRAM save block. Unknown fields in
     * this host model are deliberately left untouched until their runtime
     * owners are implemented, so this method cannot erase unrelated progress.</p>
     */
    public void writePlayerState(int slot, PlayerState playerState) {
        SaveRamLayout.checkSlot(slot);
        Objects.requireNonNull(playerState, "playerState");
        int slotOffset = SaveRamLayout.slotOffset(slot);
        int main = slotOffset + SaveRamLayout.mainOffset();

        bytes[main + SaveRamLayout.MAIN_ITEM_B_OFFSET] = (byte) playerState.itemB();
        bytes[main + SaveRamLayout.MAIN_ITEM_A_OFFSET] = (byte) playerState.itemA();
        for (int index = 0; index < SaveRamLayout.SUBSCREEN_SLOT_COUNT; index++) {
            bytes[main + SaveRamLayout.MAIN_SUBSCREEN_OFFSET + index]
                = (byte) playerState.subscreenItem(index);
        }
        bytes[main + SaveRamLayout.MAIN_FLIPPERS_OFFSET]
            = (byte) (playerState.hasFlippers() ? 1 : 0);
        bytes[main + SaveRamLayout.MAIN_MEDICINE_OFFSET]
            = (byte) (playerState.hasMedicine() ? 1 : 0);
        bytes[main + SaveRamLayout.MAIN_TRADE_SEQUENCE_ITEM_OFFSET]
            = (byte) playerState.tradeSequenceItem();
        bytes[main + SaveRamLayout.MAIN_SEASHELLS_OFFSET] = (byte) playerState.seashells();
        bytes[main + SaveRamLayout.MAIN_MEDICINE_COUNT_OFFSET]
            = (byte) playerState.medicineCount();
        bytes[main + SaveRamLayout.MAIN_TAIL_KEY_OFFSET] = (byte) playerState.tailKeyCount();
        bytes[main + SaveRamLayout.MAIN_ANGLER_KEY_OFFSET]
            = (byte) playerState.anglerKeyCount();
        bytes[main + SaveRamLayout.MAIN_FACE_KEY_OFFSET] = (byte) playerState.faceKeyCount();
        bytes[main + SaveRamLayout.MAIN_BIRD_KEY_OFFSET] = (byte) playerState.birdKeyCount();
        bytes[main + SaveRamLayout.MAIN_GOLDEN_LEAVES_OFFSET]
            = (byte) playerState.goldenLeavesCount();
        bytes[main + SaveRamLayout.MAIN_POWER_BRACELET_OFFSET]
            = (byte) playerState.powerBraceletLevel();
        bytes[main + SaveRamLayout.MAIN_SHIELD_OFFSET] = (byte) playerState.shieldLevel();
        bytes[main + SaveRamLayout.MAIN_ARROWS_OFFSET] = (byte) playerState.arrowCount();
        bytes[main + SaveRamLayout.MAIN_MAGIC_POWDER_OFFSET]
            = (byte) playerState.magicPowderCount();
        bytes[main + SaveRamLayout.MAIN_BOMBS_OFFSET] = (byte) playerState.bombCount();
        bytes[main + SaveRamLayout.MAIN_SWORD_OFFSET] = (byte) playerState.swordLevel();
        bytes[main + SaveRamLayout.MAIN_HEALTH_OFFSET] = (byte) savedHealth(playerState);
        bytes[main + SaveRamLayout.MAIN_MAX_HEARTS_OFFSET] = (byte) playerState.maxHearts();
        bytes[main + SaveRamLayout.MAIN_HEART_PIECES_OFFSET] = (byte) playerState.heartPieces();
        writeRupees(main, playerState.rupees());
        bytes[main + SaveRamLayout.MAIN_MAX_MAGIC_POWDER_OFFSET]
            = (byte) playerState.maxMagicPowder();
        bytes[main + SaveRamLayout.MAIN_MAX_BOMBS_OFFSET] = (byte) playerState.maxBombs();
        bytes[main + SaveRamLayout.MAIN_MAX_ARROWS_OFFSET] = (byte) playerState.maxArrows();
        writeOcarinaState(slot, playerState.ocarinaSongFlags(), playerState.selectedSongIndex());
        bytes[slotOffset + SaveRamLayout.dx3Offset() + SaveRamLayout.DX3_TUNIC_OFFSET]
            = (byte) playerState.tunicType();
    }

    public SaveSlotState readSlot(int slot) {
        SaveRamLayout.checkSlot(slot);
        int slotOffset = SaveRamLayout.slotOffset(slot);
        int main = slotOffset + SaveRamLayout.mainOffset();
        int[] nameBytes = new int[SaveRamLayout.NAME_LENGTH];
        for (int index = 0; index < nameBytes.length; index++) {
            nameBytes[index] = unsigned(bytes[main + SaveRamLayout.MAIN_NAME_OFFSET + index]);
        }
        int[] subscreen = new int[SaveRamLayout.SUBSCREEN_SLOT_COUNT];
        for (int index = 0; index < subscreen.length; index++) {
            subscreen[index] = unsigned(bytes[main + SaveRamLayout.MAIN_SUBSCREEN_OFFSET + index]);
        }
        byte[] rawSlot = Arrays.copyOfRange(bytes, slotOffset, slotOffset + SaveRamLayout.SLOT_SIZE);
        return new SaveSlotState(
            slot,
            nameBytes,
            unsigned(bytes[main + SaveRamLayout.MAIN_HEALTH_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_MAX_HEARTS_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_HEART_PIECES_OFFSET]),
            decodeRupees(bytes[main + SaveRamLayout.MAIN_RUPEE_HIGH_OFFSET],
                bytes[main + SaveRamLayout.MAIN_RUPEE_LOW_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_ITEM_A_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_ITEM_B_OFFSET]),
            subscreen,
            unsigned(bytes[main + SaveRamLayout.MAIN_SEASHELLS_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_FLIPPERS_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_MEDICINE_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_TRADE_SEQUENCE_ITEM_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_MEDICINE_COUNT_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_TAIL_KEY_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_ANGLER_KEY_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_FACE_KEY_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_BIRD_KEY_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_GOLDEN_LEAVES_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_POWER_BRACELET_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_SHIELD_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_SWORD_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_ARROWS_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_MAX_ARROWS_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_BOMBS_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_MAX_BOMBS_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_MAGIC_POWDER_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_MAX_MAGIC_POWDER_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_OCARINA_SONG_FLAGS_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_SELECTED_SONG_INDEX_OFFSET]),
            unsigned(bytes[slotOffset + SaveRamLayout.dx3Offset() + SaveRamLayout.DX3_TUNIC_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_SPAWN_INDOOR_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_SPAWN_MAP_ID_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_SPAWN_MAP_ROOM_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_SPAWN_X_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_SPAWN_Y_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_SPAWN_INDOOR_ROOM_OFFSET]),
            rawSlot);
    }

    /** Mirrors InitSaveFiles for a host image with an invalid slot prefix. */
    void initializeInvalidSlots() {
        for (int slot = 0; slot < SaveRamLayout.SLOT_COUNT; slot++) {
            int slotOffset = SaveRamLayout.slotOffset(slot);
            if (SaveRamLayout.hasValidPrefix(bytes, slotOffset)) {
                continue;
            }
            Arrays.fill(bytes, slotOffset, slotOffset + SaveRamLayout.SLOT_SIZE, (byte) 0);
            SaveRamLayout.writeValidPrefix(bytes, slotOffset);
        }
    }

    private boolean hasStoredName(int slotOffset) {
        int main = slotOffset + SaveRamLayout.mainOffset();
        for (int index = 0; index < SaveRamLayout.NAME_LENGTH; index++) {
            if (bytes[main + SaveRamLayout.MAIN_NAME_OFFSET + index] != 0) {
                return true;
            }
        }
        return false;
    }

    private static int decodeRupees(byte high, byte low) {
        int hundreds = unsigned(high) & 0x0F;
        int tens = (unsigned(low) >>> 4) & 0x0F;
        int ones = unsigned(low) & 0x0F;
        return Math.min(999, hundreds * 100 + tens * 10 + ones);
    }

    private void writeRupees(int main, int rupees) {
        int value = Math.max(0, Math.min(PlayerState.MAX_RUPEES, rupees));
        bytes[main + SaveRamLayout.MAIN_RUPEE_HIGH_OFFSET] = (byte) (value / 100);
        bytes[main + SaveRamLayout.MAIN_RUPEE_LOW_OFFSET]
            = (byte) ((((value / 10) % 10) << 4) | (value % 10));
    }

    private static int savedHealth(PlayerState playerState) {
        if (playerState.health() != 0) {
            return playerState.health();
        }
        int[] startingHealthByMaxHearts = {
            3 * PlayerState.HP_PER_HEART,
            3 * PlayerState.HP_PER_HEART,
            3 * PlayerState.HP_PER_HEART,
            3 * PlayerState.HP_PER_HEART,
            3 * PlayerState.HP_PER_HEART,
            3 * PlayerState.HP_PER_HEART,
            5 * PlayerState.HP_PER_HEART,
            5 * PlayerState.HP_PER_HEART,
            5 * PlayerState.HP_PER_HEART,
            5 * PlayerState.HP_PER_HEART,
            7 * PlayerState.HP_PER_HEART,
            7 * PlayerState.HP_PER_HEART,
            7 * PlayerState.HP_PER_HEART,
            7 * PlayerState.HP_PER_HEART,
            10 * PlayerState.HP_PER_HEART
        };
        return startingHealthByMaxHearts[Math.min(
            Math.max(playerState.maxHearts(), 0), startingHealthByMaxHearts.length - 1)];
    }

    private static int unsigned(byte value) {
        return value & 0xFF;
    }

    private static void requireLength(byte[] values, int expectedLength, String label) {
        Objects.requireNonNull(values, label);
        if (values.length != expectedLength) {
            throw new IllegalArgumentException(label + " must contain " + expectedLength
                + " bytes, got " + values.length);
        }
    }

    private static void requireByte(int value, String label) {
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException(label + " must be an unsigned byte: " + value);
        }
    }
}
