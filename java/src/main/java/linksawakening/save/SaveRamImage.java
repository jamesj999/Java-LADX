package linksawakening.save;

import static linksawakening.save.SaveSlotCodec.unsigned;

import java.util.Arrays;
import java.util.Objects;
import linksawakening.state.PlayerState;

/** Mutable raw image of the source-defined SRAM save area. */
public final class SaveRamImage {

    private final byte[] bytes;

    private SaveRamImage(byte[] bytes) {
        if (bytes.length != SaveRamLayout.IMAGE_SIZE) {
            throw new IllegalArgumentException(
                    "Save image must be exactly " + SaveRamLayout.IMAGE_SIZE + " bytes, got " + bytes.length);
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
            throw new IllegalArgumentException(
                    "nameBytes must contain " + SaveRamLayout.NAME_LENGTH + " bytes, got " + nameBytes.length);
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
        Arrays.fill(
                bytes,
                main + SaveRamLayout.MAIN_DEATH_COUNT_OFFSET,
                main + SaveRamLayout.MAIN_DEATH_COUNT_OFFSET + 3,
                (byte) 0);
    }

    /** Copies the complete source-defined 0x3AD-byte save slot. */
    public void copySlot(int sourceSlot, int targetSlot) {
        SaveRamLayout.checkSlot(sourceSlot);
        SaveRamLayout.checkSlot(targetSlot);
        System.arraycopy(
                bytes,
                SaveRamLayout.slotOffset(sourceSlot),
                bytes,
                SaveRamLayout.slotOffset(targetSlot),
                SaveRamLayout.SLOT_SIZE);
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
        bytes[main + SaveRamLayout.MAIN_SELECTED_SONG_INDEX_OFFSET] =
                (byte) Math.max(0, Math.min(2, selectedSongIndex));
    }

    /**
     * Writes the room-status bytes copied by the source {@code SaveGameToFile} routine.
     *
     * <p>The overworld, indoor-A, and indoor-B tables are contiguous at the
     * beginning of the main save block. The Color Dungeon table is the DX2
     * extension and is intentionally limited to the source's 0x20 bytes.</p>
     */
    public void writeRoomStatuses(
            int slot,
            byte[] overworldRoomStatus,
            byte[] indoorARoomStatus,
            byte[] indoorBRoomStatus,
            byte[] colorDungeonRoomStatus) {
        SaveRamLayout.checkSlot(slot);
        requireLength(overworldRoomStatus, SaveRamLayout.ROOM_STATUS_TABLE_SIZE, "overworldRoomStatus");
        requireLength(indoorARoomStatus, SaveRamLayout.ROOM_STATUS_TABLE_SIZE, "indoorARoomStatus");
        requireLength(indoorBRoomStatus, SaveRamLayout.ROOM_STATUS_TABLE_SIZE, "indoorBRoomStatus");
        requireLength(
                colorDungeonRoomStatus, SaveRamLayout.DX2_COLOR_DUNGEON_ROOM_STATUS_SIZE, "colorDungeonRoomStatus");

        int slotOffset = SaveRamLayout.slotOffset(slot);
        int main = slotOffset + SaveRamLayout.mainOffset();
        System.arraycopy(
                overworldRoomStatus,
                0,
                bytes,
                main + SaveRamLayout.MAIN_OVERWORLD_ROOM_STATUS_OFFSET,
                SaveRamLayout.ROOM_STATUS_TABLE_SIZE);
        System.arraycopy(
                indoorARoomStatus,
                0,
                bytes,
                main + SaveRamLayout.MAIN_INDOOR_A_ROOM_STATUS_OFFSET,
                SaveRamLayout.ROOM_STATUS_TABLE_SIZE);
        System.arraycopy(
                indoorBRoomStatus,
                0,
                bytes,
                main + SaveRamLayout.MAIN_INDOOR_B_ROOM_STATUS_OFFSET,
                SaveRamLayout.ROOM_STATUS_TABLE_SIZE);
        System.arraycopy(
                colorDungeonRoomStatus,
                0,
                bytes,
                slotOffset + SaveRamLayout.dx2Offset(),
                SaveRamLayout.DX2_COLOR_DUNGEON_ROOM_STATUS_SIZE);
    }

    /** Writes the source's persistent dungeon item table and Color Dungeon extension. */
    public void writeDungeonItemFlags(int slot, byte[] dungeonItemFlags, byte[] colorDungeonItemFlags) {
        SaveRamLayout.checkSlot(slot);
        requireLength(dungeonItemFlags, SaveRamLayout.MAIN_DUNGEON_ITEM_FLAGS_SIZE, "dungeonItemFlags");
        requireLength(colorDungeonItemFlags, SaveRamLayout.DX1_COLOR_DUNGEON_ITEM_FLAGS_SIZE, "colorDungeonItemFlags");

        int slotOffset = SaveRamLayout.slotOffset(slot);
        int main = slotOffset + SaveRamLayout.mainOffset();
        System.arraycopy(
                dungeonItemFlags,
                0,
                bytes,
                main + SaveRamLayout.MAIN_DUNGEON_ITEM_FLAGS_OFFSET,
                SaveRamLayout.MAIN_DUNGEON_ITEM_FLAGS_SIZE);
        System.arraycopy(
                colorDungeonItemFlags,
                0,
                bytes,
                slotOffset + SaveRamLayout.dx1Offset(),
                SaveRamLayout.DX1_COLOR_DUNGEON_ITEM_FLAGS_SIZE);
    }

    /** Writes wHasInstrument1..wHasInstrument8 at DB65..DB6C. */
    public void writeDungeonProgressFlags(int slot, byte[] dungeonProgressFlags) {
        SaveRamLayout.checkSlot(slot);
        requireLength(dungeonProgressFlags, SaveRamLayout.MAIN_DUNGEON_PROGRESS_FLAGS_SIZE, "dungeonProgressFlags");
        int main = SaveRamLayout.slotOffset(slot) + SaveRamLayout.mainOffset();
        System.arraycopy(
                dungeonProgressFlags,
                0,
                bytes,
                main + SaveRamLayout.MAIN_DUNGEON_PROGRESS_FLAGS_OFFSET,
                SaveRamLayout.MAIN_DUNGEON_PROGRESS_FLAGS_SIZE);
    }

    /** Writes wIsBowWowFollowingLink at DB56. */
    public void writeBowWowState(int slot, int state) {
        SaveRamLayout.checkSlot(slot);
        requireByte(state, "BowWow state");
        int main = SaveRamLayout.slotOffset(slot) + SaveRamLayout.mainOffset();
        bytes[main + SaveRamLayout.MAIN_BOW_WOW_STATE_OFFSET] = (byte) state;
    }

    /** Writes wTarinFlag at DB48. */
    public void writeTarinFlag(int slot, int flag) {
        SaveRamLayout.checkSlot(slot);
        requireByte(flag, "Tarin flag");
        int main = SaveRamLayout.slotOffset(slot) + SaveRamLayout.mainOffset();
        bytes[main + SaveRamLayout.MAIN_TARIN_FLAG_OFFSET] = (byte) flag;
    }

    /** Writes wRichardSpokenFlag at DB55. */
    public void writeRichardSpokenFlag(int slot, int flag) {
        SaveRamLayout.checkSlot(slot);
        if ((flag & ~0xFF) != 0) {
            throw new IllegalArgumentException("Richard spoken flag must be an unsigned byte");
        }
        int main = SaveRamLayout.slotOffset(slot) + SaveRamLayout.mainOffset();
        bytes[main + SaveRamLayout.MAIN_RICHARD_SPOKEN_FLAG_OFFSET] = (byte) flag;
    }

    /** Writes the source {@code wSpawnLocationData} fields in the main block. */
    public void writeSpawnLocation(
            int slot, int isIndoor, int mapId, int mapRoom, int positionX, int positionY, int indoorRoom) {
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
        new SaveSlotCodec(bytes).writePlayerState(slot, playerState);
    }

    public SaveSlotState readSlot(int slot) {
        return new SaveSlotCodec(bytes).readSlot(slot);
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

    // SRAM stores these counters as packed decimal; PlayerState uses decimal ints.

    private static void requireLength(byte[] values, int expectedLength, String label) {
        Objects.requireNonNull(values, label);
        if (values.length != expectedLength) {
            throw new IllegalArgumentException(
                    label + " must contain " + expectedLength + " bytes, got " + values.length);
        }
    }

    private static void requireByte(int value, String label) {
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException(label + " must be an unsigned byte: " + value);
        }
    }
}
