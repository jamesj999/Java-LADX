package linksawakening.render;

import java.util.List;

/** Composites a transparent plane with a saturating RGB555 channel increment. */
public final class PaletteOverlayRenderLayer implements RenderLayer {
    private final List<RenderLayer> layers;
    private final int increment;
    public PaletteOverlayRenderLayer(List<RenderLayer> layers, int increment) {
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
}
