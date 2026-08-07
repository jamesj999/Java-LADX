package linksawakening.save;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class SaveRamLayoutTest {

    @Test
    void matchesTheDisassemblySaveImageAndSlotRegions() {
        assertEquals(0x100, SaveRamLayout.SRAM_IMAGE_OFFSET);
        assertEquals(0x3AD, SaveRamLayout.SLOT_SIZE);
        assertEquals(0xC07, SaveRamLayout.IMAGE_SIZE);
        assertEquals(0x05, SaveRamLayout.PREFIX_SIZE);
        assertEquals(0x380, SaveRamLayout.MAIN_SIZE);
        assertEquals(0x05, SaveRamLayout.DX1_SIZE);
        assertEquals(0x20, SaveRamLayout.DX2_SIZE);
        assertEquals(0x03, SaveRamLayout.DX3_SIZE);

        assertEquals(0x100, SaveRamLayout.slotOffset(0));
        assertEquals(0x4AD, SaveRamLayout.slotOffset(1));
        assertEquals(0x85A, SaveRamLayout.slotOffset(2));
        assertEquals(0x005, SaveRamLayout.mainOffset());
        assertEquals(0x385, SaveRamLayout.dx1Offset());
        assertEquals(0x38A, SaveRamLayout.dx2Offset());
        assertEquals(0x3AA, SaveRamLayout.dx3Offset());
    }

    @Test
    void exposesTheSourceDerivedMainFieldOffsets() {
        assertEquals(0x300, SaveRamLayout.MAIN_ITEM_B_OFFSET);
        assertEquals(0x301, SaveRamLayout.MAIN_ITEM_A_OFFSET);
        assertEquals(0x302, SaveRamLayout.MAIN_SUBSCREEN_OFFSET);
        assertEquals(0x30F, SaveRamLayout.MAIN_SEASHELLS_OFFSET);
        assertEquals(0x344, SaveRamLayout.MAIN_SHIELD_OFFSET);
        assertEquals(0x345, SaveRamLayout.MAIN_ARROWS_OFFSET);
        assertEquals(0x34C, SaveRamLayout.MAIN_MAGIC_POWDER_OFFSET);
        assertEquals(0x34D, SaveRamLayout.MAIN_BOMBS_OFFSET);
        assertEquals(0x34E, SaveRamLayout.MAIN_SWORD_OFFSET);
        assertEquals(0x34F, SaveRamLayout.MAIN_NAME_OFFSET);
        assertEquals(0x35A, SaveRamLayout.MAIN_HEALTH_OFFSET);
        assertEquals(0x35B, SaveRamLayout.MAIN_MAX_HEARTS_OFFSET);
        assertEquals(0x35C, SaveRamLayout.MAIN_HEART_PIECES_OFFSET);
        assertEquals(0x35D, SaveRamLayout.MAIN_RUPEE_HIGH_OFFSET);
        assertEquals(0x35E, SaveRamLayout.MAIN_RUPEE_LOW_OFFSET);
        assertEquals(0x35F, SaveRamLayout.MAIN_SPAWN_INDOOR_OFFSET);
        assertEquals(0x360, SaveRamLayout.MAIN_SPAWN_MAP_ID_OFFSET);
        assertEquals(0x361, SaveRamLayout.MAIN_SPAWN_MAP_ROOM_OFFSET);
        assertEquals(0x362, SaveRamLayout.MAIN_SPAWN_X_OFFSET);
        assertEquals(0x363, SaveRamLayout.MAIN_SPAWN_Y_OFFSET);
        assertEquals(0x364, SaveRamLayout.MAIN_SPAWN_INDOOR_ROOM_OFFSET);
        assertEquals(0x376, SaveRamLayout.MAIN_MAX_MAGIC_POWDER_OFFSET);
        assertEquals(0x377, SaveRamLayout.MAIN_MAX_BOMBS_OFFSET);
        assertEquals(0x378, SaveRamLayout.MAIN_MAX_ARROWS_OFFSET);
    }

    @Test
    void exposesTheContiguousRoomStatusSaveRegions() {
        assertEquals(0x000, SaveRamLayout.MAIN_OVERWORLD_ROOM_STATUS_OFFSET);
        assertEquals(0x100, SaveRamLayout.MAIN_INDOOR_A_ROOM_STATUS_OFFSET);
        assertEquals(0x200, SaveRamLayout.MAIN_INDOOR_B_ROOM_STATUS_OFFSET);
        assertEquals(0x300, SaveRamLayout.MAIN_ROOM_STATUS_SIZE);
        assertEquals(0x20, SaveRamLayout.DX2_COLOR_DUNGEON_ROOM_STATUS_SIZE);
    }

    @Test
    void rejectsInvalidSlotIndexes() {
        assertThrows(IllegalArgumentException.class, () -> SaveRamLayout.slotOffset(-1));
        assertThrows(IllegalArgumentException.class, () -> SaveRamLayout.slotOffset(3));
    }
}
