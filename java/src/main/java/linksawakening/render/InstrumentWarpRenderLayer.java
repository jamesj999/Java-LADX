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

    static RenderLayer fullscreen(InstrumentWarpVisual visual, int[][] palettes) {
        int[][] faded = new int[palettes.length][4];
        for (int p = 0; p < palettes.length; p++) {
            for (int c = 0; c < 4; c++) {
                int rgb = palettes[p][c];
                for (int shift : new int[]{16, 8, 0}) {
                    int value = Math.min(31, ((rgb >> shift) & 255) / 8 + visual.backgroundFade());
                    faded[p][c] |= (value << 3) << shift;
                }
            }
        }
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
