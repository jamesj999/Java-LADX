package linksawakening.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ChestContentsTableTest {

    @Test
    void decodesTheSharedRoomChestTableForOverworldAndIndoorRoomIds() {
        ChestContentsTable table = new ChestContentsTable(loadRom());

        assertEquals(ChestContentsTable.CHEST_RUPEES_50, table.itemForRoom(0x00, 0x04));
        assertEquals(ChestContentsTable.CHEST_TAIL_KEY, table.itemForRoom(0x10, 0x41));
        assertEquals(ChestContentsTable.CHEST_SEASHELL, table.itemForRoom(0x02, 0x1D));
    }

    @Test
    void selectsTheSeparateColorDungeonTable() {
        ChestContentsTable table = new ChestContentsTable(loadRom());

        assertEquals(ChestContentsTable.CHEST_NIGHTMARE_KEY,
            table.itemForRoom(ChestContentsTable.MAP_COLOR_DUNGEON, 0x02));
        assertEquals(ChestContentsTable.CHEST_MAP,
            table.itemForRoom(ChestContentsTable.MAP_COLOR_DUNGEON, 0x06));
        assertEquals(ChestContentsTable.CHEST_SMALL_KEY,
            table.itemForRoom(ChestContentsTable.MAP_COLOR_DUNGEON, 0x14));
    }

    @Test
    void resolvesTheSourceSeashellFallbackWhenTheSwordIsAlreadyUpgraded() {
        ChestContentsTable table = new ChestContentsTable(loadRom());

        assertEquals(ChestContentsTable.CHEST_SEASHELL,
            table.itemForSpawn(0x00, 0x1D, 1));
        assertEquals(ChestContentsTable.CHEST_RUPEES_20,
            table.itemForSpawn(0x00, 0x1D, 2));
    }

    @Test
    void rejectsRoomAndMapValuesOutsideTheSourceByteRanges() {
        ChestContentsTable table = new ChestContentsTable(loadRom());

        assertThrows(IllegalArgumentException.class, () -> table.itemForRoom(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> table.itemForRoom(0x100, 0));
        assertThrows(IllegalArgumentException.class, () -> table.itemForRoom(0xFF, 0x20));
        assertThrows(IllegalArgumentException.class, () -> table.itemForRoom(0, 0x100));
    }

    private static byte[] loadRom() {
        try (var stream = ChestContentsTableTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return stream.readAllBytes();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load ROM", exception);
        }
    }
}
