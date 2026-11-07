package linksawakening.render;

import java.util.List;
import linksawakening.gpu.Framebuffer;
import linksawakening.world.InstrumentWarpVisual;

/** Applies the ROM's saturating RGB555 palette increments to a rendered plane. */
final class InstrumentWarpRenderLayer implements RenderLayer {
    private final List<RenderLayer> layers;
    private final int increment;

    InstrumentWarpRenderLayer(List<RenderLayer> layers, int increment) {
        this.layers = List.copyOf(layers);
        this.increment = increment;
    }

    @Override public void render(RenderContext context) {
        byte[] plane = new byte[context.buffer().length];
        RenderContext target = new RenderContext(plane, context.gpu());
        for (RenderLayer layer : layers) layer.render(target);
        for (int pixel = 0; pixel < plane.length; pixel += 4) {
            if (plane[pixel + 3] == 0) continue;
            for (int channel = 0; channel < 3; channel++) {
                int value = plane[pixel + channel] & 255;
                context.buffer()[pixel + channel] = (byte) (increment == 0 ? value
                    : Math.min(31, (value >> 3) + increment) << 3);
            }
            context.buffer()[pixel + 3] = plane[pixel + 3];
        }
    }

    static RenderLayer swirl(InstrumentWarpVisual visual,
                             linksawakening.world.RoomEntitySnapshot entities, int frame) {
        int[] commands = visual.swirl();
        int[][] palettes = visual.objectPalettes(entities.spriteSelection().objectPalettes());
        return context -> {
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
            new SpriteListRenderLayer(sprites, palettes, entities.spriteTiles()).render(context);
        };
    }

    static RenderLayer fullscreen(InstrumentWarpVisual visual, int[][] palettes) {
        int[][] faded = visual.backgroundPalettes(palettes);
        return context -> {
            byte[] plane = new byte[context.buffer().length];
            IndexedRenderer.renderBackground(plane, context.gpu(), visual.tiles(), visual.attributes(),
                faded, 20, 18);
            int bytes = Math.min(Framebuffer.HEIGHT, visual.commandBatches() * 16)
                * Framebuffer.WIDTH * 4;
            System.arraycopy(plane, 0, context.buffer(), 0, bytes);
        };
    }
}
