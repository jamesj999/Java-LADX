package linksawakening.render;

import java.util.Arrays;
import java.util.List;
import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.GPU;
import linksawakening.world.InstrumentWarpVisual;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class InstrumentWarpRenderLayerTest {
    @Test void paletteFadeSaturatesChannelsAndPreservesTransparentPixels() {
        byte[] buffer = new byte[8];
        Arrays.fill(buffer, (byte) 64);
        RenderLayer sprite = context -> {
            context.buffer()[0] = (byte) 240;
            context.buffer()[1] = 16;
            context.buffer()[2] = 0;
            context.buffer()[3] = (byte) 255;
        };
        new InstrumentWarpRenderLayer(List.of(sprite), 4).render(new RenderContext(buffer, new GPU()));
        assertEquals(248, buffer[0] & 255);
        assertEquals(48, buffer[1] & 255);
        assertEquals(32, buffer[2] & 255);
        for (int i = 4; i < 8; i++) assertEquals(64, buffer[i] & 255);
    }

    @Test void fullscreenCommandsReplaceOnlyTwoRowsPerBatchIncludingHudRows() {
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        Arrays.fill(buffer, (byte) 32);
        int[] tiles = new int[360];
        int[] attrs = new int[360];
        InstrumentWarpVisual visual = new InstrumentWarpVisual(31, 31, 1, tiles, attrs);
        int[][] palettes = new int[8][4];
        InstrumentWarpRenderLayer.fullscreen(visual, palettes).render(new RenderContext(buffer, new GPU()));
        assertEquals(248, buffer[0] & 255);
        assertEquals(248, buffer[(160 * 16 - 1) * 4] & 255);
        assertEquals(32, buffer[160 * 16 * 4] & 255);
        visual = new InstrumentWarpVisual(31, 31, 9, tiles, attrs);
        InstrumentWarpRenderLayer.fullscreen(visual, palettes).render(new RenderContext(buffer, new GPU()));
        assertEquals(248, buffer[(160 * 144 - 1) * 4] & 255);
    }
}
