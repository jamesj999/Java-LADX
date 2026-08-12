package linksawakening.render;

import linksawakening.entity.Link;
import linksawakening.equipment.ItemRegistry;
import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.GPU;
import linksawakening.gpu.Tile;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.state.PlayerState;
import linksawakening.world.ScrollController;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TarinShieldRenderLayerTest {
    @Test
    void rendersRomShieldTileTwelvePixelsAboveLink() {
        GPU gpu = new GPU();
        fill(gpu.getTile(0x86), 2);
        fill(gpu.getTile(0x87), 2);
        int[][] palettes = new int[8][4];
        palettes[7][2] = 0x00CC7722;
        Link link = new Link(new InputState(), new InputConfig(1, 2, 3, 4, 5, 6, 7),
            null, null, null, new PlayerState(), new ItemRegistry());
        link.setPixelPosition(0x30, 0x50);
        link.showTarinShieldPresentation(palettes[7]);
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        IndexedRenderer.clear(buffer);

        new TarinShieldRenderLayer(link, new ScrollController())
            .render(new RenderContext(buffer, gpu));

        assertEquals(0x00CC7722, pixel(buffer, 0x30, 0x44));
        assertEquals(0, pixel(buffer, 0x30, 0x54));
    }

    private static void fill(Tile tile, int color) {
        for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) tile.setPixel(x, y, color);
    }

    private static int pixel(byte[] buffer, int x, int y) {
        int offset = (y * Framebuffer.WIDTH + x) * 4;
        return (buffer[offset] & 0xFF) << 16 | (buffer[offset + 1] & 0xFF) << 8
            | buffer[offset + 2] & 0xFF;
    }
}
