package linksawakening.ui;

import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.GPU;
import linksawakening.render.IndexedRenderer;
import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class OcarinaPopupRendererTest {

    @Test
    void loaderKeepsTheRomOamFramesAndInventoryObjectPalettes() throws Exception {
        byte[] rom = loadRom();
        InventoryTilemapLoader loader = InventoryTilemapLoader.loadFromRom(rom);

        int popupOffset = linksawakening.rom.RomBank.romOffset(0x20, 0x604B);
        int[] popup = loader.ocarinaPopupOam();
        assertEquals(0xC0, popup.length);
        assertEquals(Byte.toUnsignedInt(rom[popupOffset]), popup[0]);
        assertEquals(Byte.toUnsignedInt(rom[popupOffset + 0xBF]), popup[0xBF]);
        assertEquals(8, loader.objectPalettes().length);
    }

    @Test
    void frameZeroUsesTheRomOamCoordinatesAndObjectPalette() throws Exception {
        byte[] rom = loadRom();
        InventoryTilemapLoader loader = InventoryTilemapLoader.loadFromRom(rom);
        GPU gpu = new GPU();
        gpu.loadOcarinaSymbolsTiles(rom);
        writeSolidTile(gpu, 0x22, 1);

        PlayerState player = new PlayerState();
        player.setOcarinaSongFlags(0x07);
        OcarinaSongMenu menu = new OcarinaSongMenu(player, linksawakening.gameplay.GameplaySoundSink.none());
        menu.requestOpen();
        for (int frame = 0; frame < 16; frame++) {
            menu.tick();
        }

        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        IndexedRenderer.clear(buffer);
        new OcarinaPopupRenderer(loader.ocarinaPopupOam()).render(
            buffer, gpu, loader.objectPalettes(), menu);

        // Data_020_604B's first entry is [Y=$F8, X=$F0, tile=$22,
        // attr=$01], followed by the ROM's $30/$60 OAM origin adjustment.
        assertPixel(buffer, 0x50 - 0x08, 0x28 - 0x10,
            loader.objectPalettes()[1][1]);
    }

    private static void writeSolidTile(GPU gpu, int tileIndex, int colorIndex) {
        int base = tileIndex * GPU.TILE_DATA_SIZE;
        int low = (colorIndex & 1) == 0 ? 0x00 : 0xFF;
        int high = (colorIndex & 2) == 0 ? 0x00 : 0xFF;
        for (int row = 0; row < 8; row++) {
            gpu.writeVRAM(base + row * 2, (byte) low);
            gpu.writeVRAM(base + row * 2 + 1, (byte) high);
        }
    }

    private static void assertPixel(byte[] buffer, int x, int y, int color) {
        int offset = (y * Framebuffer.WIDTH + x) * 4;
        assertEquals((color >> 16) & 0xFF, Byte.toUnsignedInt(buffer[offset]));
        assertEquals((color >> 8) & 0xFF, Byte.toUnsignedInt(buffer[offset + 1]));
        assertEquals(color & 0xFF, Byte.toUnsignedInt(buffer[offset + 2]));
        assertEquals(0xFF, Byte.toUnsignedInt(buffer[offset + 3]));
    }

    private static byte[] loadRom() throws Exception {
        try (InputStream stream = OcarinaPopupRendererTest.class.getClassLoader()
                .getResourceAsStream("rom/azle.gbc")) {
            return stream.readAllBytes();
        }
    }
}
