package linksawakening.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import linksawakening.gpu.GPU;
import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class InventoryHudPaletteTest {
    @Test
    void closedHudUsesRoomPalettesAndOpenInventoryUsesItsOwnPalettes() throws Exception {
        byte[] rom;
        try (var in = linksawakening.testing.TestRom.open()) {
            rom = in.readAllBytes();
        }
        GPU gpu = new GPU();
        gpu.loadBaseTiles(rom);
        var menu = new InventoryMenu(
                InventoryTilemapLoader.loadFromRom(new linksawakening.rom.Rom(rom)), new PlayerState());
        int[][] roomPalettes = new int[8][4];
        for (int[] palette : roomPalettes) Arrays.fill(palette, 0x123456);
        byte[] pixels = new byte[160 * 144 * 4];
        menu.render(pixels, gpu, roomPalettes);
        for (int pixel = 160 * 128; pixel < 160 * 144; pixel++) {
            assertEquals(0x123456, rgb(pixels, pixel));
        }
        menu.requestToggle();
        for (int frame = 0; frame < 60; frame++) menu.tick();
        assertTrue(menu.isFullyOpen());
        menu.render(pixels, gpu, roomPalettes);
        assertNotEquals(0x123456, rgb(pixels, 160 * 128));
    }

    private static int rgb(byte[] pixels, int pixel) {
        int i = pixel * 4;
        return (pixels[i] & 255) << 16 | (pixels[i + 1] & 255) << 8 | (pixels[i + 2] & 255);
    }
}
