package linksawakening.ui;

import static org.junit.jupiter.api.Assertions.*;

import linksawakening.gpu.GPU;
import linksawakening.rom.RomUtil;
import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class InventoryInstrumentPanelTest {
    @Test
    void romPositionsNumbersAndCollectionBitProduceTheTwoByTwoInstrumentLayout() throws Exception {
        byte[] rom = rom();
        var panel = new InventoryInstrumentPanel(linksawakening.rom.Rom.of(rom));
        int[] offsets = {0x0F, 0x51, 0xB1, 0xEF, 0xEC, 0xAA, 0x4A, 0x0C};
        for (int i = 0; i < 8; i++) {
            assertEquals(0x100 + offsets[i], panel.position(i));
            assertArrayEquals(new int[] {0x7C, 0x7C, 0x7C, 0xB1 + i}, panel.tilemapForInstrument(i, 1));
            int tile = 0xD0 + 2 * i;
            assertArrayEquals(new int[] {tile, tile + 1, tile + 16, tile + 17}, panel.tilemapForInstrument(i, 2));
        }
        // Confirm layout values are read from the ROM, not duplicated literals.
        rom[RomUtil.romOffset(1, 0x6BCF)] = 0x11;
        rom[RomUtil.romOffset(1, 0x6BD7)] = (byte) 0xB9;
        var changed = new InventoryInstrumentPanel(linksawakening.rom.Rom.of(rom));
        assertEquals(0x111, changed.position(0));
        assertEquals(0xB9, changed.tilemapForInstrument(0, 0)[3]);
    }

    @Test
    void paletteCyclesEveryEightTicksAndLeavesTheOtherPalettesUnchanged() throws Exception {
        byte[] rom = rom();
        var panel = new InventoryInstrumentPanel(linksawakening.rom.Rom.of(rom));
        var base = InventoryTilemapLoader.loadFromRom(rom).palettes();
        assertEquals(RomUtil.decodeRgb555(0x2680), panel.palettes(base, 0)[7][1]);
        assertArrayEquals(panel.palettes(base, 0)[7], panel.palettes(base, 7)[7]);
        assertFalse(java.util.Arrays.equals(panel.palettes(base, 7)[7], panel.palettes(base, 8)[7]));
        assertArrayEquals(panel.palettes(base, 0)[7], panel.palettes(base, 128)[7]);
        assertArrayEquals(base[0], panel.palettes(base, 8)[0]);
        assertEquals(
                RomUtil.decodeRgb555(0x001F),
                InventoryTilemapLoader.loadFromRom(rom).objectPalettes()[6][2]);
    }

    @Test
    void liveMenuUsesProgressFlagsAndLeavesDungeonMinimapAreaAlone() throws Exception {
        byte[] rom = rom();
        GPU gpu = new GPU();
        gpu.loadBaseTiles(rom);
        var menu = new InventoryMenu(
                InventoryTilemapLoader.loadFromRom(rom),
                new PlayerState(),
                linksawakening.gameplay.GameplaySoundSink.none(),
                rom);
        menu.requestToggle();
        for (int i = 0; i < 60; i++) menu.tick();
        byte[] flags = new byte[8];
        byte[] empty = new byte[160 * 144 * 4];
        menu.setWorldContext(true, 0x0E, flags);
        menu.render(empty, gpu);
        flags[0] = 2;
        byte[] owned = new byte[empty.length];
        menu.setWorldContext(true, 0x0E, flags);
        menu.render(owned, gpu);
        assertFalse(java.util.Arrays.equals(empty, owned));
        for (int map : new int[] {0, 7, 0xFF}) {
            menu.setWorldContext(true, map, new byte[8]);
            menu.render(empty, gpu);
            menu.setWorldContext(true, map, flags);
            menu.render(owned, gpu);
            assertArrayEquals(empty, owned, "Instrument flags must not overwrite dungeon minimap " + map);
        }
    }

    private static byte[] rom() throws Exception {
        try (var in = linksawakening.testing.TestRom.open()) {
            return in.readAllBytes();
        }
    }
}
