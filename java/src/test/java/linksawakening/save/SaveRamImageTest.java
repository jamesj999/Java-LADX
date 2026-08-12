package linksawakening.save;

import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class SaveRamImageTest {

    @Test
    void emptyImageHasValidPrefixesButNoInitializedNames() {
        SaveRamImage image = SaveRamImage.empty();

        assertEquals(SaveRamLayout.IMAGE_SIZE, image.bytes().length);
        for (int slot = 0; slot < SaveRamLayout.SLOT_COUNT; slot++) {
            assertArrayEquals(new byte[] {1, 3, 5, 7, 9},
                Arrays.copyOfRange(image.bytes(), SaveRamLayout.slotOffset(slot),
                    SaveRamLayout.slotOffset(slot) + SaveRamLayout.PREFIX_SIZE));
            assertArrayEquals(new int[SaveRamLayout.NAME_LENGTH], image.savedNames()[slot]);
        }
        assertEquals(0, image.saveFilesMask());
    }

    @Test
    void newFileWritesTheRomCreationBytesAndUpdatesMenuDiscovery() {
        SaveRamImage image = SaveRamImage.empty();
        int[] name = {1, 2, 3, 4, 5};

        image.createNewGame(1, name);

        int main = SaveRamLayout.slotOffset(1) + SaveRamLayout.mainOffset();
        assertArrayEquals(new byte[] {1, 2, 3, 4, 5}, Arrays.copyOfRange(image.bytes(),
            main + SaveRamLayout.MAIN_NAME_OFFSET,
            main + SaveRamLayout.MAIN_NAME_OFFSET + SaveRamLayout.NAME_LENGTH));
        assertEquals(0x18, unsigned(image.bytes()[main + SaveRamLayout.MAIN_HEALTH_OFFSET]));
        assertEquals(0x03, unsigned(image.bytes()[main + SaveRamLayout.MAIN_MAX_HEARTS_OFFSET]));
        assertArrayEquals(new byte[] {0, 0, 0}, Arrays.copyOfRange(image.bytes(),
            main + SaveRamLayout.MAIN_DEATH_COUNT_OFFSET,
            main + SaveRamLayout.MAIN_DEATH_COUNT_OFFSET + 3));
        assertEquals(1 << 1, image.saveFilesMask());
        assertArrayEquals(name, image.savedNames()[1]);
    }

    @Test
    void copySlotCopiesTheCompleteRomSlotExtent() {
        SaveRamImage image = SaveRamImage.empty();
        image.createNewGame(0, new int[] {1, 2, 3, 4, 5});
        byte[] seeded = image.bytes();
        int source = SaveRamLayout.slotOffset(0);
        int target = SaveRamLayout.slotOffset(1);
        for (int index = SaveRamLayout.PREFIX_SIZE; index < SaveRamLayout.SLOT_SIZE; index++) {
            seeded[source + index] = (byte) index;
        }
        seeded[target - 1] = (byte) 0x5A;
        seeded[target + SaveRamLayout.SLOT_SIZE] = (byte) 0xA5;
        image = SaveRamImage.fromBytes(seeded);

        image.copySlot(0, 1);

        assertArrayEquals(Arrays.copyOfRange(image.bytes(), source,
                source + SaveRamLayout.SLOT_SIZE),
            Arrays.copyOfRange(image.bytes(), target, target + SaveRamLayout.SLOT_SIZE));
        assertEquals((byte) 0x5A, image.bytes()[target - 1]);
        assertEquals((byte) 0xA5, image.bytes()[target + SaveRamLayout.SLOT_SIZE]);
    }

    @Test
    void eraseSlotClearsTheRomExtentAndRestoresTheJavaValidityPrefix() {
        SaveRamImage image = SaveRamImage.empty();
        image.createNewGame(1, new int[] {1, 2, 3, 4, 5});
        byte[] seeded = image.bytes();
        int target = SaveRamLayout.slotOffset(1);
        seeded[target - 1] = (byte) 0x5A;
        seeded[target + SaveRamLayout.SLOT_SIZE] = (byte) 0xA5;
        image = SaveRamImage.fromBytes(seeded);

        image.eraseSlot(1);

        assertEquals(0, image.saveFilesMask());
        byte[] slot = Arrays.copyOfRange(image.bytes(), SaveRamLayout.slotOffset(1),
            SaveRamLayout.slotOffset(1) + SaveRamLayout.SLOT_SIZE);
        assertArrayEquals(new byte[] {1, 3, 5, 7, 9},
            Arrays.copyOf(slot, SaveRamLayout.PREFIX_SIZE));
        assertArrayEquals(new byte[SaveRamLayout.SLOT_SIZE - SaveRamLayout.PREFIX_SIZE],
            Arrays.copyOfRange(slot, SaveRamLayout.PREFIX_SIZE, slot.length));
        assertEquals((byte) 0x5A, image.bytes()[target - 1]);
        assertEquals((byte) 0xA5, image.bytes()[target + SaveRamLayout.SLOT_SIZE]);
    }

    @Test
    void copyAndEraseValidateSlotIndexes() {
        SaveRamImage image = SaveRamImage.empty();
        assertThrows(IllegalArgumentException.class, () -> image.copySlot(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> image.copySlot(0, 3));
        assertThrows(IllegalArgumentException.class, () -> image.eraseSlot(3));
    }

    @Test
    void writesLiveOcarinaStateAtTheRomOffsetsWithoutTouchingAdjacentBytes() {
        SaveRamImage image = SaveRamImage.empty();
        image.createNewGame(1, new int[] {1, 2, 3, 4, 5});

        int main = SaveRamLayout.slotOffset(1) + SaveRamLayout.mainOffset();
        byte[] before = image.bytes();
        before[main + SaveRamLayout.MAIN_OCARINA_SONG_FLAGS_OFFSET - 1] = (byte) 0xA5;
        before[main + SaveRamLayout.MAIN_SELECTED_SONG_INDEX_OFFSET + 1] = (byte) 0x5A;
        image = SaveRamImage.fromBytes(before);

        image.writeOcarinaState(1, 0xFF, 99);

        byte[] after = image.bytes();
        assertEquals(0x07, unsigned(after[main + SaveRamLayout.MAIN_OCARINA_SONG_FLAGS_OFFSET]));
        assertEquals(0x02, unsigned(after[main + SaveRamLayout.MAIN_SELECTED_SONG_INDEX_OFFSET]));
        assertEquals((byte) 0xA5,
            after[main + SaveRamLayout.MAIN_OCARINA_SONG_FLAGS_OFFSET - 1]);
        assertEquals((byte) 0x5A,
            after[main + SaveRamLayout.MAIN_SELECTED_SONG_INDEX_OFFSET + 1]);
        assertEquals(0x07, image.readSlot(1).ocarinaSongFlags());
        assertEquals(0x02, image.readSlot(1).selectedSongIndex());
    }

    @Test
    void writesTheRomSpawnLocationFieldsWithoutTouchingAdjacentProgress() {
        SaveRamImage image = SaveRamImage.empty();
        image.createNewGame(1, new int[] {1, 2, 3, 4, 5});
        int main = SaveRamLayout.slotOffset(1) + SaveRamLayout.mainOffset();
        byte[] before = image.bytes();
        before[main + SaveRamLayout.MAIN_SPAWN_INDOOR_OFFSET - 1] = (byte) 0xA1;
        before[main + SaveRamLayout.MAIN_SPAWN_INDOOR_ROOM_OFFSET + 1] = (byte) 0xB2;
        image = SaveRamImage.fromBytes(before);

        image.writeSpawnLocation(1, 1, 0x06, 0x18, 0x52, 0x63, 0x27);

        SaveSlotState state = image.readSlot(1);
        assertEquals(1, state.spawnIsIndoor());
        assertEquals(0x06, state.spawnMapId());
        assertEquals(0x18, state.spawnMapRoom());
        assertEquals(0x52, state.spawnPositionX());
        assertEquals(0x63, state.spawnPositionY());
        assertEquals(0x27, state.spawnIndoorRoom());
        assertEquals((byte) 0xA1,
            image.bytes()[main + SaveRamLayout.MAIN_SPAWN_INDOOR_OFFSET - 1]);
        assertEquals((byte) 0xB2,
            image.bytes()[main + SaveRamLayout.MAIN_SPAWN_INDOOR_ROOM_OFFSET + 1]);
    }

    @Test
    void rejectsSpawnLocationValuesThatCannotBeStoredInTheSourceByteFields() {
        SaveRamImage image = SaveRamImage.empty();

        assertThrows(IllegalArgumentException.class,
            () -> image.writeSpawnLocation(0, 0, 0, 0, 0x100, 0, 0));
        assertThrows(IllegalArgumentException.class,
            () -> image.writeSpawnLocation(0, 0, 0, 0, 0, -1, 0));
    }

    @Test
    void writesEveryCurrentlyModeledPlayerFieldAndPreservesUnknownSaveBytes() {
        SaveRamImage image = SaveRamImage.empty();
        image.createNewGame(1, new int[] {1, 2, 3, 4, 5});
        int slotStart = SaveRamLayout.slotOffset(1);
        int main = slotStart + SaveRamLayout.mainOffset();
        byte[] before = image.bytes();
        before[main + SaveRamLayout.MAIN_SPAWN_X_OFFSET] = (byte) 0xA5;
        before[main + SaveRamLayout.MAIN_DEATH_COUNT_OFFSET] = (byte) 0xB6;
        before[slotStart + SaveRamLayout.dx3Offset() + 1] = (byte) 0xC7;
        image = SaveRamImage.fromBytes(before);

        PlayerState player = new PlayerState();
        player.initializeNewGame(0x30, 0x30, 0x20);
        player.setItemA(PlayerState.INVENTORY_BOW);
        player.setItemB(PlayerState.INVENTORY_HOOKSHOT);
        player.setSubscreenItems(new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
        player.setSeashells(37);
        player.setHasFlippers(true);
        player.setHasMedicine(true);
        player.setTradeSequenceItem(0x0E);
        player.setMedicineCount(2);
        player.setTailKeyCount(1);
        player.setAnglerKeyCount(2);
        player.setFaceKeyCount(3);
        player.setBirdKeyCount(4);
        player.setGoldenLeavesCount(5);
        player.setShieldLevel(2);
        player.setSwordLevel(1);
        player.setMaxArrows(40);
        player.setArrowCount(17);
        player.setMaxBombs(60);
        player.setBombCount(23);
        player.setMaxMagicPowder(32);
        player.setMagicPowderCount(19);
        player.setMaxHearts(10);
        player.setHealth(0);
        player.setHeartPieces(2);
        player.setRupees(509);
        player.setPowerBraceletLevel(2);
        player.setHasMedicine(true);
        player.setOcarinaSongFlags(0x07);
        player.setSelectedSongIndex(2);
        player.setTunicType(PlayerState.TUNIC_BLUE);

        image.writePlayerState(1, player);

        SaveSlotState state = image.readSlot(1);
        assertEquals(PlayerState.INVENTORY_BOW, state.itemA());
        assertEquals(PlayerState.INVENTORY_HOOKSHOT, state.itemB());
        assertArrayEquals(new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, state.subscreen());
        assertEquals(37, state.seashells());
        assertEquals(1, state.hasFlippers());
        assertEquals(1, state.hasMedicine());
        assertEquals(0x0E, state.tradeSequenceItem());
        assertEquals(2, state.medicineCount());
        assertEquals(1, state.tailKeyCount());
        assertEquals(2, state.anglerKeyCount());
        assertEquals(3, state.faceKeyCount());
        assertEquals(4, state.birdKeyCount());
        assertEquals(5, state.goldenLeavesCount());
        assertEquals(2, state.powerBraceletLevel());
        assertEquals(2, state.shieldLevel());
        assertEquals(1, state.swordLevel());
        assertEquals(17, state.arrowCount());
        assertEquals(40, state.maxArrows());
        assertEquals(23, state.bombCount());
        assertEquals(60, state.maxBombs());
        assertEquals(19, state.magicPowderCount());
        assertEquals(32, state.maxMagicPowder());
        assertEquals(56, state.health());
        assertEquals(10, state.maxHearts());
        assertEquals(2, state.heartPieces());
        assertEquals(509, state.rupees());
        assertEquals(0x07, state.ocarinaSongFlags());
        assertEquals(2, state.selectedSongIndex());
        assertEquals(PlayerState.TUNIC_BLUE, state.tunicType());
        assertEquals((byte) 0xA5,
            image.bytes()[main + SaveRamLayout.MAIN_SPAWN_X_OFFSET]);
        assertEquals((byte) 0xB6,
            image.bytes()[main + SaveRamLayout.MAIN_DEATH_COUNT_OFFSET]);
        assertEquals((byte) 0xC7,
            image.bytes()[slotStart + SaveRamLayout.dx3Offset() + 1]);
        assertEquals(0x05, unsigned(image.bytes()[main + SaveRamLayout.MAIN_RUPEE_HIGH_OFFSET]));
        assertEquals(0x09, unsigned(image.bytes()[main + SaveRamLayout.MAIN_RUPEE_LOW_OFFSET]));
    }

    @Test
    void writesRoomStatusTablesToTheSourceMainAndDx2Regions() {
        SaveRamImage image = SaveRamImage.empty();
        image.createNewGame(1, new int[] {1, 2, 3, 4, 5});
        int slotStart = SaveRamLayout.slotOffset(1);
        byte[] before = image.bytes();
        before[slotStart + SaveRamLayout.dx1Offset()] = (byte) 0xD1;
        before[slotStart + SaveRamLayout.mainOffset() + SaveRamLayout.MAIN_ITEM_B_OFFSET]
            = (byte) 0xE2;
        image = SaveRamImage.fromBytes(before);

        byte[] overworld = pattern(0x100, 0x10);
        byte[] indoorA = pattern(0x100, 0x40);
        byte[] indoorB = pattern(0x100, 0x70);
        byte[] colorDungeon = pattern(SaveRamLayout.DX2_SIZE, 0xA0);

        image.writeRoomStatuses(1, overworld, indoorA, indoorB, colorDungeon);

        SaveSlotState state = image.readSlot(1);
        assertArrayEquals(overworld, state.overworldRoomStatus());
        assertArrayEquals(indoorA, state.indoorARoomStatus());
        assertArrayEquals(indoorB, state.indoorBRoomStatus());
        assertArrayEquals(colorDungeon, state.colorDungeonRoomStatus());
        assertEquals((byte) 0xD1, image.bytes()[slotStart + SaveRamLayout.dx1Offset()]);
        assertEquals((byte) 0xE2,
            image.bytes()[slotStart + SaveRamLayout.mainOffset()
                + SaveRamLayout.MAIN_ITEM_B_OFFSET]);
    }

    @Test
    void writesDungeonItemFlagsToTheMainAndDx1RegionsWithoutTouchingNeighbors() {
        SaveRamImage image = SaveRamImage.empty();
        image.createNewGame(1, new int[] {1, 2, 3, 4, 5});
        int slotStart = SaveRamLayout.slotOffset(1);
        int main = slotStart + SaveRamLayout.mainOffset();
        byte[] before = image.bytes();
        before[main + SaveRamLayout.MAIN_DUNGEON_ITEM_FLAGS_OFFSET - 1] = (byte) 0xA1;
        before[main + SaveRamLayout.MAIN_DUNGEON_ITEM_FLAGS_OFFSET
            + SaveRamLayout.MAIN_DUNGEON_ITEM_FLAGS_SIZE] = (byte) 0xB2;
        before[slotStart + SaveRamLayout.dx1Offset() - 1] = (byte) 0xC3;
        image = SaveRamImage.fromBytes(before);

        byte[] dungeonFlags = pattern(SaveRamLayout.MAIN_DUNGEON_ITEM_FLAGS_SIZE, 0x10);
        byte[] colorFlags = pattern(SaveRamLayout.DX1_COLOR_DUNGEON_ITEM_FLAGS_SIZE, 0xE0);
        image.writeDungeonItemFlags(1, dungeonFlags, colorFlags);

        SaveSlotState state = image.readSlot(1);
        assertArrayEquals(dungeonFlags, state.dungeonItemFlags());
        assertArrayEquals(colorFlags, state.colorDungeonItemFlags());
        assertEquals((byte) 0xA1,
            image.bytes()[main + SaveRamLayout.MAIN_DUNGEON_ITEM_FLAGS_OFFSET - 1]);
        assertEquals((byte) 0xB2,
            image.bytes()[main + SaveRamLayout.MAIN_DUNGEON_ITEM_FLAGS_OFFSET
                + SaveRamLayout.MAIN_DUNGEON_ITEM_FLAGS_SIZE]);
        assertEquals((byte) 0xC3,
            image.bytes()[slotStart + SaveRamLayout.dx1Offset() - 1]);
    }

    @Test
    void decodesModeledFieldsAndKeepsUnknownRawBytes() {
        byte[] bytes = SaveRamImage.empty().bytes();
        int slotStart = SaveRamLayout.slotOffset(0);
        int main = slotStart + SaveRamLayout.mainOffset();
        put(bytes, main, SaveRamLayout.MAIN_ITEM_B_OFFSET, PlayerState.INVENTORY_BOW);
        put(bytes, main, SaveRamLayout.MAIN_ITEM_A_OFFSET, PlayerState.INVENTORY_HOOKSHOT);
        for (int slot = 0; slot < PlayerState.SUBSCREEN_SLOT_COUNT; slot++) {
            put(bytes, main, SaveRamLayout.MAIN_SUBSCREEN_OFFSET + slot, 0x20 + slot);
        }
        put(bytes, main, SaveRamLayout.MAIN_SEASHELLS_OFFSET, 17);
        put(bytes, main, SaveRamLayout.MAIN_FLIPPERS_OFFSET, 1);
        put(bytes, main, SaveRamLayout.MAIN_MEDICINE_OFFSET, 1);
        put(bytes, main, SaveRamLayout.MAIN_TRADE_SEQUENCE_ITEM_OFFSET, 0x0E);
        put(bytes, main, SaveRamLayout.MAIN_MEDICINE_COUNT_OFFSET, 2);
        put(bytes, main, SaveRamLayout.MAIN_TAIL_KEY_OFFSET, 1);
        put(bytes, main, SaveRamLayout.MAIN_ANGLER_KEY_OFFSET, 2);
        put(bytes, main, SaveRamLayout.MAIN_FACE_KEY_OFFSET, 3);
        put(bytes, main, SaveRamLayout.MAIN_BIRD_KEY_OFFSET, 4);
        put(bytes, main, SaveRamLayout.MAIN_GOLDEN_LEAVES_OFFSET, 5);
        put(bytes, main, SaveRamLayout.MAIN_SHIELD_OFFSET, 2);
        put(bytes, main, SaveRamLayout.MAIN_ARROWS_OFFSET, 7);
        put(bytes, main, SaveRamLayout.MAIN_MAGIC_POWDER_OFFSET, 9);
        put(bytes, main, SaveRamLayout.MAIN_BOMBS_OFFSET, 11);
        put(bytes, main, SaveRamLayout.MAIN_SWORD_OFFSET, 2);
        put(bytes, main, SaveRamLayout.MAIN_HEALTH_OFFSET, 31);
        put(bytes, main, SaveRamLayout.MAIN_MAX_HEARTS_OFFSET, 5);
        put(bytes, main, SaveRamLayout.MAIN_HEART_PIECES_OFFSET, 2);
        put(bytes, main, SaveRamLayout.MAIN_RUPEE_HIGH_OFFSET, 0x05);
        put(bytes, main, SaveRamLayout.MAIN_RUPEE_LOW_OFFSET, 0x09);
        put(bytes, main, SaveRamLayout.MAIN_POWER_BRACELET_OFFSET, 2);
        put(bytes, main, SaveRamLayout.MAIN_MEDICINE_OFFSET, 1);
        put(bytes, main, SaveRamLayout.MAIN_SPAWN_INDOOR_OFFSET, 1);
        put(bytes, main, SaveRamLayout.MAIN_SPAWN_MAP_ID_OFFSET, 0x10);
        put(bytes, main, SaveRamLayout.MAIN_SPAWN_MAP_ROOM_OFFSET, 0xA3);
        put(bytes, main, SaveRamLayout.MAIN_SPAWN_X_OFFSET, 0x52);
        put(bytes, main, SaveRamLayout.MAIN_SPAWN_Y_OFFSET, 0x63);
        put(bytes, main, SaveRamLayout.MAIN_SPAWN_INDOOR_ROOM_OFFSET, 0xA3);
        put(bytes, main, SaveRamLayout.MAIN_MAX_MAGIC_POWDER_OFFSET, 0x20);
        put(bytes, main, SaveRamLayout.MAIN_MAX_BOMBS_OFFSET, 0x30);
        put(bytes, main, SaveRamLayout.MAIN_MAX_ARROWS_OFFSET, 0x30);
        put(bytes, main, SaveRamLayout.MAIN_OCARINA_SONG_FLAGS_OFFSET, 0x05);
        put(bytes, main, SaveRamLayout.MAIN_SELECTED_SONG_INDEX_OFFSET, 0x02);
        put(bytes, slotStart, SaveRamLayout.dx3Offset(), 0x02);
        bytes[slotStart + 0x12] = (byte) 0xA5;
        bytes[slotStart + SaveRamLayout.mainOffset() + SaveRamLayout.MAIN_NAME_OFFSET] = 1;

        SaveSlotState state = SaveRamImage.fromBytes(bytes).readSlot(0);

        assertEquals(0, state.slot());
        assertEquals(PlayerState.INVENTORY_BOW, state.itemB());
        assertEquals(PlayerState.INVENTORY_HOOKSHOT, state.itemA());
        assertEquals(0x20, state.subscreen()[0]);
        assertEquals(17, state.seashells());
        assertEquals(1, state.hasFlippers());
        assertEquals(1, state.hasMedicine());
        assertEquals(0x0E, state.tradeSequenceItem());
        assertEquals(2, state.medicineCount());
        assertEquals(1, state.tailKeyCount());
        assertEquals(2, state.anglerKeyCount());
        assertEquals(3, state.faceKeyCount());
        assertEquals(4, state.birdKeyCount());
        assertEquals(5, state.goldenLeavesCount());
        assertEquals(2, state.powerBraceletLevel());
        assertEquals(2, state.shieldLevel());
        assertEquals(7, state.arrowCount());
        assertEquals(9, state.magicPowderCount());
        assertEquals(11, state.bombCount());
        assertEquals(2, state.swordLevel());
        assertEquals(31, state.health());
        assertEquals(5, state.maxHearts());
        assertEquals(2, state.heartPieces());
        assertEquals(509, state.rupees());
        assertEquals(1, state.spawnIsIndoor());
        assertEquals(0x10, state.spawnMapId());
        assertEquals(0xA3, state.spawnMapRoom());
        assertEquals(0x52, state.spawnPositionX());
        assertEquals(0x63, state.spawnPositionY());
        assertEquals(0xA3, state.spawnIndoorRoom());
        assertEquals(0x20, state.maxMagicPowder());
        assertEquals(0x30, state.maxBombs());
        assertEquals(0x30, state.maxArrows());
        assertEquals(0x05, state.ocarinaSongFlags());
        assertEquals(0x02, state.selectedSongIndex());
        assertEquals(2, state.tunicType());
        assertEquals((byte) 0xA5, state.rawSlot()[0x12]);
    }

    @Test
    void imageAndDecodedArraysAreDefensiveCopies() {
        SaveRamImage image = SaveRamImage.empty();
        byte[] bytes = image.bytes();
        bytes[0] = (byte) 0xFF;
        assertEquals(1, unsigned(image.bytes()[SaveRamLayout.slotOffset(0)]));

        int[][] names = image.savedNames();
        names[0][0] = 0x40;
        assertEquals(0, image.savedNames()[0][0]);

        SaveSlotState state = image.readSlot(0);
        int[] stateName = state.nameBytes();
        stateName[0] = 0x40;
        assertEquals(0, state.nameBytes()[0]);
        byte[] raw = state.rawSlot();
        raw[0] = (byte) 0xFF;
        assertEquals(1, unsigned(state.rawSlot()[0]));
    }

    @Test
    void rejectsMalformedImageLengthsAndNames() {
        assertThrows(IllegalArgumentException.class,
            () -> SaveRamImage.fromBytes(new byte[SaveRamLayout.IMAGE_SIZE - 1]));
        assertThrows(IllegalArgumentException.class,
            () -> SaveRamImage.empty().createNewGame(0, new int[4]));
        assertThrows(IllegalArgumentException.class,
            () -> SaveRamImage.empty().createNewGame(0, new int[] {0, 1, 2, 3, 0x100}));
    }

    private static void put(byte[] bytes, int base, int offset, int value) {
        bytes[base + offset] = (byte) value;
    }

    private static byte[] pattern(int length, int start) {
        byte[] values = new byte[length];
        for (int index = 0; index < values.length; index++) {
            values[index] = (byte) (start + index);
        }
        return values;
    }

    private static int unsigned(byte value) {
        return value & 0xFF;
    }
}
