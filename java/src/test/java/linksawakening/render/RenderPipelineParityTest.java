package linksawakening.render;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.GPU;
import org.junit.jupiter.api.Test;

final class RenderPipelineParityTest {
    @Test
    void spriteListMatchesAcrossOverlapTransparencyHeightFlipAndClipping() {
        GPU gpu = new GPU();
        for (int tile = 0; tile < 4; tile++)
            for (int y = 0; y < 8; y++)
                for (int x = 0; x < 8; x++) gpu.getTile(tile).setPixel(x, y, (tile + x + 2 * y) & 3);
        int[][] palettes = {{0, 0x123456, 0x789ABC, 0xDEF012}};
        for (boolean fx : new boolean[] {false, true})
            for (boolean fy : new boolean[] {false, true})
                for (int height : new int[] {8, 16})
                    for (int x : new int[] {-7, 0, 155}) {
                        FrameScene scene = FrameScene.of(List.of(new SpriteListRenderLayer(
                                List.of(
                                        new SpriteListRenderLayer.Sprite(1, x, -3, 0, fx, fy, height),
                                        new SpriteListRenderLayer.Sprite(2, x + 2, 0, 0, !fx, !fy, height)),
                                palettes)));
                        byte[] legacy = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
                        byte[] hardware = legacy.clone();
                        LegacySceneReference.render(scene, legacy, gpu);
                        scene.hardwareFrame(gpu).compose(hardware);
                        assertArrayEquals(legacy, hardware, "x=" + x + " height=" + height + " flip=" + fx + "," + fy);
                    }
    }

    @Test
    void backgroundMatchesScrollFlipPaletteAndClipping() {
        GPU gpu = new GPU();
        int[] map = new int[32 * 32], attrs = new int[map.length];
        int[][] palettes = new int[8][4];
        for (int p = 0; p < 8; p++) for (int c = 0; c < 4; c++) palettes[p][c] = p * 1000 + c * 150;
        for (int i = 0; i < map.length; i++) {
            map[i] = i & 3;
            attrs[i] = (i & 7) | ((i & 3) << 5);
        }
        for (int tile = 0; tile < 4; tile++)
            for (int y = 0; y < 8; y++)
                for (int x = 0; x < 8; x++) gpu.getTile(256 + tile).setPixel(x, y, (tile + x + y) & 3);
        for (int x : new int[] {-9, 0, 3, 255})
            for (int y : new int[] {-7, 0, 5, 255}) {
                FrameScene scene = FrameScene.of(
                        List.of(BackgroundRenderLayer.scrolling(map, attrs, palettes, 32, 32, 20, 18, x, y)));
                byte[] legacy = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
                byte[] hardware = legacy.clone();
                LegacySceneReference.render(scene, legacy, gpu);
                scene.hardwareFrame(gpu).compose(hardware);
                assertArrayEquals(legacy, hardware, "scroll=" + x + "," + y);
            }
    }

    @Test
    void unmigratedLayerIsRejectedBeforeDrawing() {
        byte[] output = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        FrameScene scene = FrameScene.of(List.of(context -> fail("must not draw")));
        assertThrows(IllegalStateException.class, () -> scene.hardwareFrame(new GPU())
                .compose(output));
        assertArrayEquals(new byte[output.length], output);
    }
}
