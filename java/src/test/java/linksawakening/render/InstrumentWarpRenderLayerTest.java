package linksawakening.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.GPU;
import linksawakening.world.entity.event.InstrumentWarpVisual;
import org.junit.jupiter.api.Test;

final class InstrumentWarpRenderLayerTest {
    @Test
    void paletteFadeSaturatesChannelsAndPreservesTransparentPixels() {
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

    @Test
    void shippedRomLatePassAffectsUpperBackgroundAndLowerObjectPalettesOnly() {
        int[][] original = new int[8][4];
        for (int[] palette : original) Arrays.fill(palette, 0x101010);
        InstrumentWarpVisual visual = new InstrumentWarpVisual(3, 5, 0, new int[360], new int[360]);
        int[][] background = visual.backgroundPalettes(original);
        int[][] objects = visual.objectPalettes(original);
        assertEquals(0x282828, background[3][1]);
        assertEquals(0x505050, background[4][1]);
        assertEquals(0x383838, objects[3][1]);
        assertEquals(0x101010, objects[4][1]);
        assertEquals(0x101010, original[4][1]);
    }

    @Test
    void fullscreenCommandsReplaceOnlyTwoRowsPerBatchIncludingHudRows() {
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

    @Test
    void indexedPaletteTransformsAndFullscreenRowsMatchLegacy() {
        GPU gpu = new GPU();
        for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) gpu.getTile(0).setPixel(x, y, (x + y) % 4);
        int[][] palettes = {{0, 0xF01000, 0x123456, 0x00A0D0}};
        RenderLayer sprites = new SpriteListRenderLayer(
                List.of(new SpriteListRenderLayer.Sprite(0, 3, 5, 0, true, false, 8)), palettes);
        RenderLayer nested =
                new PaletteOverlayRenderLayer(List.of(new PaletteOverlayRenderLayer(List.of(sprites), 2)), 4);
        var visual = new InstrumentWarpVisual(31, 31, 3, new int[360], new int[360]);
        for (List<RenderLayer> layers : List.<List<RenderLayer>>of(
                List.of(nested),
                List.of(InstrumentWarpRenderLayer.fullscreen(visual, new int[8][4])),
                List.of(
                        new InstrumentWarpRenderLayer(List.of(sprites), 31),
                        InstrumentWarpRenderLayer.fullscreen(visual, new int[8][4])))) {
            FrameScene scene = FrameScene.of(layers);
            byte[] legacy = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
            byte[] indexed = legacy.clone();
            LegacySceneReference.render(scene, legacy, gpu);
            scene.hardwareFrame(gpu).compose(indexed);
            org.junit.jupiter.api.Assertions.assertArrayEquals(legacy, indexed);
        }
    }
}
