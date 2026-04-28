package linksawakening.world;

import linksawakening.rom.RomBank;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class RoomPaletteLoaderTest {

    @Test
    void overworldPaletteComesFromRoomPaletteMapAndPalettePointerTable() {
        byte[] rom = new byte[RomBank.romOffset(0x21, 0x5000) + 0x40];
        rom[RomBank.romOffset(0x21, 0x42EF) + 0x92] = 0x02;
        int pointerOffset = RomBank.romOffset(0x21, 0x42B1) + 0x04;
        rom[pointerOffset] = 0x00;
        rom[pointerOffset + 1] = 0x50;
        rom[RomBank.romOffset(0x21, 0x5000)] = 0x1F;
        rom[RomBank.romOffset(0x21, 0x5000) + 1] = 0x00;

        int[][] palettes = new RoomPaletteLoader(rom).loadOverworld(0x92);

        assertEquals(0xFF0000, palettes[0][0]);
    }
}
