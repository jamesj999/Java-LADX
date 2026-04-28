package linksawakening.world;

import linksawakening.rom.RomBank;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OverworldTilesetTableTest {

    @Test
    void readsTilesetByTwoByTwoOverworldBlockFromRomTable() {
        byte[] rom = new byte[RomBank.romOffset(0x20, 0x6E73) + 64];
        int table = RomBank.romOffset(0x20, 0x6E73);
        rom[table + 0x20] = 0x12;

        OverworldTilesetTable tilesets = new OverworldTilesetTable(rom);

        assertEquals(0x12, tilesets.tilesetIdForRoom(0x90));
        assertEquals(0x12, tilesets.tilesetIdForRoom(0x91));
    }

    @Test
    void mabeVillageSquareUsesMabeVillageTilesetInsteadOfKeepEntry() {
        byte[] rom = new byte[RomBank.romOffset(0x20, 0x6E73) + 64];
        int table = RomBank.romOffset(0x20, 0x6E73);
        rom[table + 0x21] = 0x26;
        rom[table + 0x24] = 0x0F;

        OverworldTilesetTable tilesets = new OverworldTilesetTable(rom);

        assertEquals(0x26, tilesets.tilesetIdForRoom(0x92));
        assertEquals(0x26, tilesets.tilesetIdForRoom(0x93));
    }

    @Test
    void skipsKeepNoUpdateAndNonCameraShopCameraTileset() {
        assertFalse(OverworldTilesetTable.shouldLoadRoomSpecificTileset(0x00, 0x0F, 0xFF));
        assertFalse(OverworldTilesetTable.shouldLoadRoomSpecificTileset(0x00, 0xFF, 0x00));
        assertFalse(OverworldTilesetTable.shouldLoadRoomSpecificTileset(0x00, 0x1A, 0x00));
        assertTrue(OverworldTilesetTable.shouldLoadRoomSpecificTileset(0x37, 0x1A, 0x00));
        assertFalse(OverworldTilesetTable.shouldLoadRoomSpecificTileset(0x37, 0x1A, 0x1A));
    }
}
