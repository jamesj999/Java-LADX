package linksawakening.save;

import static org.junit.jupiter.api.Assertions.*;

import linksawakening.save.SaveSlotComponents.*;
import org.junit.jupiter.api.Test;

final class SaveSlotComponentsTest {
    @Test
    void builderPreservesFlattenedAccessorsAndDefensiveArrays() {
        int[] name = {1, 2, 3, 4, 5};
        int[] pack = new int[SaveRamLayout.SUBSCREEN_SLOT_COUNT];
        pack[0] = 7;
        byte[] raw = new byte[SaveRamLayout.SLOT_SIZE];
        var resources = new Resources(24, 3, 2, 321, 12);
        var inventory = new Inventory(1, 2, pack, 3, 2, 1);
        var ammunition = new Ammunition(4, 30, 5, 60, 6, 40);
        var progression = new Progression(new Quest(1, 1, 4, 2, 1, 2), new Keys(1, 2, 3, 4, 5), new Songs(7, 2));
        var spawn = new Spawn(1, 0x10, 0xa3, 40, 64, 0x42);
        var state = SaveSlotState.builder(1, name, raw)
                .resources(resources)
                .inventory(inventory)
                .ammunition(ammunition)
                .progression(progression)
                .spawn(spawn)
                .build();
        name[0] = 99;
        pack[0] = 99;
        raw[0] = 99;
        assertEquals(1, state.nameBytes()[0]);
        assertEquals(7, state.subscreen()[0]);
        assertEquals(0, state.rawSlot()[0]);
        state.nameBytes()[1] = 99;
        state.subscreen()[1] = 99;
        state.rawSlot()[1] = 99;
        assertEquals(2, state.nameBytes()[1]);
        assertEquals(0, state.subscreen()[1]);
        assertEquals(0, state.rawSlot()[1]);
        assertEquals(resources, state.resources());
        assertEquals(inventory, state.inventory());
        assertEquals(ammunition, state.ammunition());
        assertEquals(progression, state.progression());
        assertEquals(spawn, state.spawn());
        assertAll(
                () -> assertEquals(24, state.health()),
                () -> assertEquals(3, state.maxHearts()),
                () -> assertEquals(2, state.heartPieces()),
                () -> assertEquals(321, state.rupees()),
                () -> assertEquals(12, state.seashells()),
                () -> assertEquals(1, state.itemA()),
                () -> assertEquals(2, state.itemB()),
                () -> assertEquals(3, state.powerBraceletLevel()),
                () -> assertEquals(2, state.shieldLevel()),
                () -> assertEquals(1, state.swordLevel()),
                () -> assertEquals(4, state.arrowCount()),
                () -> assertEquals(30, state.maxArrows()),
                () -> assertEquals(5, state.bombCount()),
                () -> assertEquals(60, state.maxBombs()),
                () -> assertEquals(6, state.magicPowderCount()),
                () -> assertEquals(40, state.maxMagicPowder()),
                () -> assertEquals(1, state.hasFlippers()),
                () -> assertEquals(1, state.hasMedicine()),
                () -> assertEquals(4, state.tradeSequenceItem()),
                () -> assertEquals(2, state.medicineCount()),
                () -> assertEquals(1, state.hasToadstool()),
                () -> assertEquals(2, state.tunicType()),
                () -> assertEquals(1, state.tailKeyCount()),
                () -> assertEquals(2, state.anglerKeyCount()),
                () -> assertEquals(3, state.faceKeyCount()),
                () -> assertEquals(4, state.birdKeyCount()),
                () -> assertEquals(5, state.goldenLeavesCount()),
                () -> assertEquals(7, state.ocarinaSongFlags()),
                () -> assertEquals(2, state.selectedSongIndex()),
                () -> assertEquals(1, state.spawnIsIndoor()),
                () -> assertEquals(0x10, state.spawnMapId()),
                () -> assertEquals(0xa3, state.spawnMapRoom()),
                () -> assertEquals(40, state.spawnPositionX()),
                () -> assertEquals(64, state.spawnPositionY()),
                () -> assertEquals(0x42, state.spawnIndoorRoom()));
    }

    @Test
    void inventoryRejectsWrongLengthAndBuilderRequiresAllGroups() {
        assertThrows(IllegalArgumentException.class, () -> new Inventory(0, 0, new int[1], 0, 0, 0));
        assertThrows(NullPointerException.class, () -> SaveSlotState.builder(
                        0, new int[5], new byte[SaveRamLayout.SLOT_SIZE])
                .build());
    }
}
