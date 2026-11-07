package linksawakening.render;

import linksawakening.gpu.GPU;

import java.util.List;

public final class FrameScene {
    private static final FrameScene EMPTY = new FrameScene(List.of());

    private final List<RenderLayer> layers;

    @FunctionalInterface
    interface DrawObserver { void drawn(FrameScene scene, byte[] rgba, GPU gpu); }
    static final ThreadLocal<DrawObserver> DRAW_OBSERVER = new ThreadLocal<>();

    List<String> unmigratedLayers() {
        return layers.stream().filter(layer -> !(layer instanceof HardwareRenderLayer hardware) || !hardware.supportsHardware())
            .map(layer -> layer.getClass().getName()).distinct().toList();
    }


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

    public void drawTo(byte[] buffer, GPU gpu) {
        drawTo(buffer, gpu, RenderPipeline.LEGACY_RGBA);
    }

    public void drawTo(byte[] buffer, GPU gpu, RenderPipeline pipeline) {
        if (pipeline == RenderPipeline.HARDWARE) {
            HardwareFrame frame = hardwareFrame(gpu);
            frame.compose(buffer);
            return;
        }
        IndexedRenderer.clear(buffer);
        RenderContext context = new RenderContext(buffer, gpu);
        for (RenderLayer layer : layers) {
            layer.render(context);
        }
        DrawObserver observer = DRAW_OBSERVER.get();
        if (observer != null) observer.drawn(this, buffer, gpu);
    }

    HardwareFrame hardwareFrame(GPU gpu) {
        for (RenderLayer layer : layers) {
            if (!(layer instanceof HardwareRenderLayer hardware) || !hardware.supportsHardware())
                throw new IllegalStateException("Layer not migrated: " + layer.getClass().getName());
        }
        HardwareFrame frame = new HardwareFrame(true);
        for (RenderLayer layer : layers) ((HardwareRenderLayer) layer).submit(frame, gpu);
        return frame;
    }

    public int layerCount() {
        return layers.size();
    }
}
