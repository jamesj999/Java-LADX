package linksawakening.render;

import linksawakening.entity.Link;
import linksawakening.equipment.ItemRegistry;
import linksawakening.gpu.GPU;
import linksawakening.gpu.Framebuffer;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.state.PlayerState;
import linksawakening.world.transition.ScrollController;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class DialogGotItemRenderTest {
    @Test void toadstoolUsesSingleRomOamColumnAndPaletteFour() {
        var gpu = new GPU();
        for (int id : new int[] {0x8E, 0x8F})
            for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) gpu.getTile(id).setPixel(x, y, 2);
        int[][] palettes = new int[8][4];
        palettes[4][2] = 0x00CC7722;
        var link = new Link(new InputState(), new InputConfig(1, 2, 3, 4, 5, 6, 7),
            null, null, null, new PlayerState(), new ItemRegistry());
        link.setPixelPosition(0x30, 0x50);
        link.presentToadstool();
        link.tickDialogGotItem(false);
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        StorySprites.dialogGotItem(link, palettes, new ScrollController()).render(new RenderContext(buffer, gpu));
        assertEquals(0xCC7722, pixel(buffer, 0x30, 0x42));
        assertEquals(0xCC7722, pixel(buffer, 0x37, 0x51));
        assertEquals(0, pixel(buffer, 0x38, 0x42));
        assertEquals(0, pixel(buffer, 0x30, 0x52));
    }
    private static int pixel(byte[] buffer, int x, int y) {
        int offset = (y * Framebuffer.WIDTH + x) * 4;
        return (buffer[offset] & 255) << 16 | (buffer[offset+1] & 255) << 8 | buffer[offset+2] & 255;
    }
}
