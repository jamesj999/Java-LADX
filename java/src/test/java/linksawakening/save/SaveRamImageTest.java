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
        player.setOcarinaSongFlags(0x07);
        player.setSelectedSongIndex(2);
        player.setTunicType(PlayerState.TUNIC_BLUE);

        image.writePlayerState(1, player);

        SaveSlotState state = image.readSlot(1);
        assertEquals(PlayerState.INVENTORY_BOW, state.itemA());
        assertEquals(PlayerState.INVENTORY_HOOKSHOT, state.itemB());
        assertArrayEquals(new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, state.subscreen());
        assertEquals(37, state.seashells());
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

    private static int unsigned(byte value) {
        return value & 0xFF;
    }
}
