package linksawakening.render;

import linksawakening.gpu.GPU;

import java.util.List;

public final class FrameScene {
    private static final FrameScene EMPTY = new FrameScene(List.of());

    private final List<RenderLayer> layers;

    private FrameScene(List<RenderLayer> layers) {
        this.layers = List.copyOf(layers);
    }

    public static FrameScene empty() {
        return EMPTY;
    }

    public static FrameScene of(List<RenderLayer> layers) {
        if (layers.isEmpty()) {
            return EMPTY;
        }
        return new FrameScene(layers);
    }

    public void render(byte[] buffer, GPU gpu) {
        IndexedRenderer.clear(buffer);
        RenderContext context = new RenderContext(buffer, gpu);
        for (RenderLayer layer : layers) {
            layer.render(context);
        }
    }
}
