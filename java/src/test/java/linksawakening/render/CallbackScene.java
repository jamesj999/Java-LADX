package linksawakening.render;

import java.util.List;
import linksawakening.gpu.GPU;

/** Source-compatible adapters for external RGBA callbacks; never used by production indexed scenes. */
final class CallbackScene {
    private CallbackScene() {}

    static void drawCallbacks(List<RenderLayer> layers, byte[] buffer, GPU gpu) {
        IndexedRenderer.clear(buffer);
        RenderContext context = new RenderContext(buffer, gpu);
        for (RenderLayer layer : layers) layer.render(context);
    }

    static void renderPaletteCallbacks(List<RenderLayer> layers, int increment, RenderContext context) {
        byte[] plane = new byte[context.buffer().length];
        RenderContext target = new RenderContext(plane, context.gpu());
        for (RenderLayer layer : layers) layer.render(target);
        for (int pixel = 0; pixel < plane.length; pixel += 4) {
            if (plane[pixel + 3] == 0) continue;
            for (int channel = 0; channel < 3; channel++) {
                int value = plane[pixel + channel] & 255;
                context.buffer()[pixel + channel] =
                        (byte) (increment == 0 ? value : Math.min(31, (value >> 3) + increment) << 3);
            }
            context.buffer()[pixel + 3] = plane[pixel + 3];
        }
    }
}
