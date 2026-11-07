package linksawakening.render;

import java.util.List;
import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.GPU;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class RenderPipelineParityTest {
    @Test void spriteListMatchesAcrossOverlapTransparencyHeightFlipAndClipping() {
        GPU gpu = new GPU();
        for (int tile = 0; tile < 4; tile++) for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++)
            gpu.getTile(tile).setPixel(x, y, (tile + x + 2 * y) & 3);
        int[][] palettes = {{0, 0x123456, 0x789ABC, 0xDEF012}};
        for (boolean fx : new boolean[]{false, true}) for (boolean fy : new boolean[]{false, true})
            for (int height : new int[]{8, 16}) for (int x : new int[]{-7, 0, 155}) {
                FrameScene scene = FrameScene.of(List.of(new SpriteListRenderLayer(List.of(
                    new SpriteListRenderLayer.Sprite(1, x, -3, 0, fx, fy, height),
                    new SpriteListRenderLayer.Sprite(2, x + 2, 0, 0, !fx, !fy, height)), palettes)));
                byte[] legacy = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
                byte[] hardware = legacy.clone();
                scene.drawTo(legacy, gpu, RenderPipeline.LEGACY_RGBA);
                scene.drawTo(hardware, gpu, RenderPipeline.HARDWARE);
                assertArrayEquals(legacy, hardware, "x=" + x + " height=" + height + " flip=" + fx + "," + fy);
            }
    }
    @Test void unmigratedLayerIsRejectedBeforeDrawing() {
        byte[] output = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        FrameScene scene = FrameScene.of(List.of(context -> fail("must not draw")));
        assertThrows(IllegalStateException.class, () -> scene.drawTo(output, new GPU(), RenderPipeline.HARDWARE));
        assertArrayEquals(new byte[output.length], output);
    }
}
