package linksawakening.save;

import java.util.Arrays;
import java.util.Objects;
import linksawakening.save.SaveSlotComponents.*;

/**
 * Immutable view of the currently modeled fields in one raw SRAM slot.
 * Counts and capacities are decimal integers; rawSlot retains their packed BCD bytes.
 */
public record SaveSlotState(
        int slot,
        int[] nameBytes,
        Resources resources,
        Inventory inventory,
        Ammunition ammunition,
        Progression progression,
        Spawn spawn,
        byte[] rawSlot) {

    public SaveSlotState {
        SaveRamLayout.checkSlot(slot);
        nameBytes = copyExact(nameBytes, SaveRamLayout.NAME_LENGTH, "nameBytes");
        Objects.requireNonNull(resources, "resources");
        Objects.requireNonNull(inventory, "inventory");
        Objects.requireNonNull(ammunition, "ammunition");
        Objects.requireNonNull(progression, "progression");
        Objects.requireNonNull(spawn, "spawn");
        rawSlot = copyExact(rawSlot, SaveRamLayout.SLOT_SIZE, "rawSlot");
    }

    @Override
    public int[] nameBytes() {
        return nameBytes.clone();
    }

    public int[] subscreen() {
        return inventory.subscreen();
    }

    @Override
    public byte[] rawSlot() {
        return rawSlot.clone();
    }

    /** Begins construction from the slot identity and complete original SRAM bytes. */
    public static SaveSlotStateBuilder builder(int slot, int[] nameBytes, byte[] rawSlot) {
        return new SaveSlotStateBuilder(slot, nameBytes, rawSlot);
    }

    public int health() {
        return resources.health();
    }

    public int maxHearts() {
        return resources.maxHearts();
    }

    public int heartPieces() {
        return resources.heartPieces();
    }

    public int rupees() {
        return resources.rupees();
    }

    public int seashells() {
        return resources.seashells();
    }

    public int itemA() {
        return inventory.itemA();
    }

    public int itemB() {
        return inventory.itemB();
    }

    public int powerBraceletLevel() {
        return inventory.powerBraceletLevel();
    }

    public int shieldLevel() {
        return inventory.shieldLevel();
    }

    public int swordLevel() {
        return inventory.swordLevel();
    }

    public int arrowCount() {
        return ammunition.arrowCount();
    }

    public int maxArrows() {
        return ammunition.maxArrows();
    }

    public int bombCount() {
        return ammunition.bombCount();
    }

    public int maxBombs() {
        return ammunition.maxBombs();
    }

    public int magicPowderCount() {
        return ammunition.magicPowderCount();
    }

    public int maxMagicPowder() {
        return ammunition.maxMagicPowder();
    }

    public int hasFlippers() {
        return progression.quest().hasFlippers();
    }

    public int hasMedicine() {
        return progression.quest().hasMedicine();
    }

    public int tradeSequenceItem() {
        return progression.quest().tradeSequenceItem();
    }

    public int medicineCount() {
        return progression.quest().medicineCount();
    }

    public int hasToadstool() {
        return progression.quest().hasToadstool();
    }

    public int tunicType() {
        return progression.quest().tunicType();
    }

    public int tailKeyCount() {
        return progression.keys().tailKeyCount();
    }

    public int anglerKeyCount() {
        return progression.keys().anglerKeyCount();
    }

    public int faceKeyCount() {
        return progression.keys().faceKeyCount();
    }

    public int birdKeyCount() {
        return progression.keys().birdKeyCount();
    }

    public int goldenLeavesCount() {
        return progression.keys().goldenLeavesCount();
    }

    public int ocarinaSongFlags() {
        return progression.songs().ocarinaSongFlags();
    }

    public int selectedSongIndex() {
        return progression.songs().selectedSongIndex();
    }

    public int spawnIsIndoor() {
        return spawn.spawnIsIndoor();
    }

    public int spawnMapId() {
        return spawn.spawnMapId();
    }

    public int spawnMapRoom() {
        return spawn.spawnMapRoom();
    }

    public int spawnPositionX() {
        return spawn.spawnPositionX();
    }

    public int spawnPositionY() {
        return spawn.spawnPositionY();
    }

    public int spawnIndoorRoom() {
        return spawn.spawnIndoorRoom();
    }

    /** Packed BCD, low byte first in the original SRAM. */
    public int deathCountBcd() {
        int offset = SaveRamLayout.mainOffset() + SaveRamLayout.MAIN_DEATH_COUNT_OFFSET;
        return Byte.toUnsignedInt(rawSlot[offset]) | (Byte.toUnsignedInt(rawSlot[offset + 1]) << 8);
    }

    public byte[] overworldRoomStatus() {
        return roomStatus(
                SaveRamLayout.mainOffset() + SaveRamLayout.MAIN_OVERWORLD_ROOM_STATUS_OFFSET,
                SaveRamLayout.ROOM_STATUS_TABLE_SIZE);
    }

    public byte[] indoorARoomStatus() {
        return roomStatus(
                SaveRamLayout.mainOffset() + SaveRamLayout.MAIN_INDOOR_A_ROOM_STATUS_OFFSET,
                SaveRamLayout.ROOM_STATUS_TABLE_SIZE);
    }

    public byte[] indoorBRoomStatus() {
        return roomStatus(
                SaveRamLayout.mainOffset() + SaveRamLayout.MAIN_INDOOR_B_ROOM_STATUS_OFFSET,
                SaveRamLayout.ROOM_STATUS_TABLE_SIZE);
    }

    public byte[] colorDungeonRoomStatus() {
        return roomStatus(SaveRamLayout.dx2Offset(), SaveRamLayout.DX2_COLOR_DUNGEON_ROOM_STATUS_SIZE);
    }

    public int richardSpokenFlag() {
        return Byte.toUnsignedInt(rawSlot[SaveRamLayout.mainOffset() + SaveRamLayout.MAIN_RICHARD_SPOKEN_FLAG_OFFSET]);
    }

    public byte[] dungeonItemFlags() {
        return roomStatus(
                SaveRamLayout.mainOffset() + SaveRamLayout.MAIN_DUNGEON_ITEM_FLAGS_OFFSET,
                SaveRamLayout.MAIN_DUNGEON_ITEM_FLAGS_SIZE);
    }

    public byte[] colorDungeonItemFlags() {
        return roomStatus(SaveRamLayout.dx1Offset(), SaveRamLayout.DX1_COLOR_DUNGEON_ITEM_FLAGS_SIZE);
    }

    public byte[] dungeonProgressFlags() {
        return roomStatus(
                SaveRamLayout.mainOffset() + SaveRamLayout.MAIN_DUNGEON_PROGRESS_FLAGS_OFFSET,
                SaveRamLayout.MAIN_DUNGEON_PROGRESS_FLAGS_SIZE);
    }

    public int bowWowState() {
        return Byte.toUnsignedInt(rawSlot[SaveRamLayout.mainOffset() + SaveRamLayout.MAIN_BOW_WOW_STATE_OFFSET]);
    }

    public int tarinFlag() {
        return Byte.toUnsignedInt(rawSlot[SaveRamLayout.mainOffset() + SaveRamLayout.MAIN_TARIN_FLAG_OFFSET]);
    }

    private byte[] roomStatus(int offset, int length) {
        return Arrays.copyOfRange(rawSlot, offset, offset + length);
    }

    static int[] copyExact(int[] values, int expectedLength, String label) {
        Objects.requireNonNull(values, label);
        if (values.length != expectedLength) {
            throw new IllegalArgumentException(
                    label + " must contain " + expectedLength + " bytes, got " + values.length);
        }
        return values.clone();
    }

    private static byte[] copyExact(byte[] values, int expectedLength, String label) {
        Objects.requireNonNull(values, label);
        if (values.length != expectedLength) {
            throw new IllegalArgumentException(
                    label + " must contain " + expectedLength + " bytes, got " + values.length);
        }
        return values.clone();
    }
}
