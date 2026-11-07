package linksawakening.render;

import java.util.List;
import linksawakening.gpu.Framebuffer;
import linksawakening.world.InstrumentWarpVisual;

/** Applies the ROM's saturating RGB555 palette increments to a rendered plane. */
final class InstrumentWarpRenderLayer implements HardwareRenderLayer {
    private final List<RenderLayer> layers;
    private final int increment;

    InstrumentWarpRenderLayer(List<RenderLayer> layers, int increment) {
        this.layers = List.copyOf(layers);
        this.increment = increment;
    }



    @Override public boolean supportsHardware() {
        return new PaletteOverlayRenderLayer(layers, increment).supportsHardware();
    }
    @Override public void submit(HardwareFrame frame, linksawakening.gpu.GPU gpu) {
        new PaletteOverlayRenderLayer(layers, increment).submit(frame, gpu);
    }

    static RenderLayer swirl(InstrumentWarpVisual visual,
                             linksawakening.world.RoomEntitySnapshot entities, int frame) {
        int[] commands = visual.swirl();
        int[][] palettes = visual.objectPalettes(entities.spriteSelection().objectPalettes());
        return new HardwareRenderLayer() {

            @Override public void submit(HardwareFrame model, linksawakening.gpu.GPU gpu) {
            if (commands.length != 256 || entities.spriteTiles() == null) return;
            int start = ((frame >> 3) & 3) * 64;
            var sprites = new java.util.ArrayList<SpriteListRenderLayer.Sprite>();
            for (int i = 0; i < 16; i++) {
                int at = start + i * 4;
                int tile = commands[at + 2];
                if (tile == 255) continue;
                int attr = commands[at + 3];
                sprites.add(new SpriteListRenderLayer.Sprite(tile,
                    visual.centerX() + (byte) commands[at + 1] - 8,
                    visual.centerY() + (byte) commands[at] - 16,
                    attr & 7, (attr & 32) != 0, (attr & 64) != 0, 16));
            }
            new SpriteListRenderLayer(sprites, palettes, entities.spriteTiles()).submit(model, gpu);
            }
        };
    }

    static RenderLayer fullscreen(InstrumentWarpVisual visual, int[][] palettes) {
        int[][] faded = visual.backgroundPalettes(palettes);
        return new HardwareRenderLayer() {

            @Override public void submit(HardwareFrame model, linksawakening.gpu.GPU gpu) {
                HardwareFrame background = new HardwareFrame(true);
                HardwareBackgroundRenderer.renderBackground(background, gpu, visual.tiles(), visual.attributes(),
                    faded, 20, 18);
                model.copyBackgroundRows(background, Math.min(Framebuffer.HEIGHT, visual.commandBatches() * 16));
            }
        };
    }
    @Override public void render(RenderContext context) {
        new PaletteOverlayRenderLayer(layers, increment).render(context);
    }
}
