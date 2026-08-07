package linksawakening.save;

import java.util.Arrays;
import java.util.Objects;

/** Immutable view of the currently modeled fields in one raw SRAM slot. */
public record SaveSlotState(
    int slot,
    int[] nameBytes,
    int health,
    int maxHearts,
    int heartPieces,
    int rupees,
    int itemA,
    int itemB,
    int[] subscreen,
    int seashells,
    int shieldLevel,
    int swordLevel,
    int arrowCount,
    int maxArrows,
    int bombCount,
    int maxBombs,
    int magicPowderCount,
    int maxMagicPowder,
    int ocarinaSongFlags,
    int selectedSongIndex,
    int tunicType,
    int spawnIsIndoor,
    int spawnMapId,
    int spawnMapRoom,
    int spawnPositionX,
    int spawnPositionY,
    int spawnIndoorRoom,
    byte[] rawSlot
) {

    public SaveSlotState {
        SaveRamLayout.checkSlot(slot);
        nameBytes = copyExact(nameBytes, SaveRamLayout.NAME_LENGTH, "nameBytes");
        subscreen = copyExact(subscreen, SaveRamLayout.SUBSCREEN_SLOT_COUNT, "subscreen");
        rawSlot = copyExact(rawSlot, SaveRamLayout.SLOT_SIZE, "rawSlot");
    }

    @Override
    public int[] nameBytes() {
        return nameBytes.clone();
    }

    @Override
    public int[] subscreen() {
        return subscreen.clone();
    }

    @Override
    public byte[] rawSlot() {
        return rawSlot.clone();
    }

    public byte[] overworldRoomStatus() {
        return roomStatus(SaveRamLayout.mainOffset()
            + SaveRamLayout.MAIN_OVERWORLD_ROOM_STATUS_OFFSET,
            SaveRamLayout.ROOM_STATUS_TABLE_SIZE);
    }

    public byte[] indoorARoomStatus() {
        return roomStatus(SaveRamLayout.mainOffset()
            + SaveRamLayout.MAIN_INDOOR_A_ROOM_STATUS_OFFSET,
            SaveRamLayout.ROOM_STATUS_TABLE_SIZE);
    }

    public byte[] indoorBRoomStatus() {
        return roomStatus(SaveRamLayout.mainOffset()
            + SaveRamLayout.MAIN_INDOOR_B_ROOM_STATUS_OFFSET,
            SaveRamLayout.ROOM_STATUS_TABLE_SIZE);
    }

    public byte[] colorDungeonRoomStatus() {
        return roomStatus(SaveRamLayout.dx2Offset(),
            SaveRamLayout.DX2_COLOR_DUNGEON_ROOM_STATUS_SIZE);
    }

    public byte[] dungeonItemFlags() {
        return roomStatus(SaveRamLayout.mainOffset()
            + SaveRamLayout.MAIN_DUNGEON_ITEM_FLAGS_OFFSET,
            SaveRamLayout.MAIN_DUNGEON_ITEM_FLAGS_SIZE);
    }

    public byte[] colorDungeonItemFlags() {
        return roomStatus(SaveRamLayout.dx1Offset(),
            SaveRamLayout.DX1_COLOR_DUNGEON_ITEM_FLAGS_SIZE);
    }

    private byte[] roomStatus(int offset, int length) {
        return Arrays.copyOfRange(rawSlot, offset, offset + length);
    }

    private static int[] copyExact(int[] values, int expectedLength, String label) {
        Objects.requireNonNull(values, label);
        if (values.length != expectedLength) {
            throw new IllegalArgumentException(label + " must contain " + expectedLength
                + " bytes, got " + values.length);
        }
        return values.clone();
    }

    private static byte[] copyExact(byte[] values, int expectedLength, String label) {
        Objects.requireNonNull(values, label);
        if (values.length != expectedLength) {
            throw new IllegalArgumentException(label + " must contain " + expectedLength
                + " bytes, got " + values.length);
        }
        return values.clone();
    }
}
