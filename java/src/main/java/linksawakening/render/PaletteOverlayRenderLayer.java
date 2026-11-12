package linksawakening.render;

import java.util.List;

/** Composites a transparent plane with a saturating RGB555 channel increment. */
final class PaletteOverlayRenderLayer implements HardwareRenderLayer {
    private final List<RenderLayer> layers;
    private final int increment;

    public PaletteOverlayRenderLayer(List<RenderLayer> layers, int increment) {
        this.layers = List.copyOf(layers);
        this.increment = increment;
    }

    @Override
    public boolean supportsHardware() {
        for (RenderLayer layer : layers)
            if (!(layer instanceof HardwareRenderLayer indexed) || !indexed.supportsHardware()) return false;
        return true;
    }

    @Override
    public void submit(HardwareFrame frame, linksawakening.gpu.GPU gpu) {
        if (!supportsHardware()) throw new IllegalStateException("Unmigrated palette child");
        frame.withPaletteTransform(this::transform, () -> {
            for (RenderLayer layer : layers)
                frame.withLayerSource(
                        layer.getClass().getSimpleName(), () -> ((HardwareRenderLayer) layer).submit(frame, gpu));
        });
    }

    private int transform(int rgb) {
        if (increment == 0) return rgb;
        int result = 0;
        for (int shift = 16; shift >= 0; shift -= 8) {
            int value = (rgb >> shift) & 255;
            value = Math.min(31, (value >> 3) + increment) << 3;
            result |= (value & 255) << shift;
        }
        return result;
    }
}
