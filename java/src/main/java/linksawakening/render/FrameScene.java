package linksawakening.render;

import java.util.List;
import linksawakening.gpu.GPU;

public final class FrameScene {
    private static final FrameScene EMPTY = new FrameScene(List.of());

    private final List<RenderLayer> layers;

    @FunctionalInterface
    interface DrawObserver {
        void drawn(FrameScene scene, byte[] rgba, GPU gpu, RenderPipeline pipeline);
    }

    static final ThreadLocal<DrawObserver> DRAW_OBSERVER = new ThreadLocal<>();

    List<String> unmigratedLayers() {
        return layers.stream()
                .filter(layer -> !(layer instanceof HardwareRenderLayer hardware) || !hardware.supportsHardware())
                .map(layer -> layer.getClass().getName())
                .distinct()
                .toList();
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
        if (!unmigratedLayers().isEmpty()) {
            // Preserve the original functional RenderLayer callback API outside production scenes.
            RgbaCallbackCompatibility.drawCallbacks(layers, buffer, gpu);
            notifyDraw(buffer, gpu);
            return;
        }
        drawTo(buffer, gpu, RenderPipeline.HARDWARE);
    }

    public void drawTo(byte[] buffer, GPU gpu, RenderPipeline pipeline) {
        java.util.Objects.requireNonNull(pipeline, "pipeline");
        HardwareFrame frame = hardwareFrame(gpu);
        frame.compose(buffer);
        notifyDraw(buffer, gpu);
    }

    private void notifyDraw(byte[] buffer, GPU gpu) {
        DrawObserver observer = DRAW_OBSERVER.get();
        if (observer != null) observer.drawn(this, buffer, gpu, RenderPipeline.HARDWARE);
    }

    List<RenderLayer> layersForComparison() {
        return layers;
    }

    HardwareFrame hardwareFrame(GPU gpu) {
        for (RenderLayer layer : layers) {
            if (!(layer instanceof HardwareRenderLayer hardware) || !hardware.supportsHardware())
                throw new IllegalStateException(
                        "Layer not migrated: " + layer.getClass().getName());
        }
        HardwareFrame frame = new HardwareFrame(true);
        for (RenderLayer layer : layers) {
            frame.submittingLayer(layer.getClass().getSimpleName());
            ((HardwareRenderLayer) layer).submit(frame, gpu);
        }
        return frame;
    }

    public int layerCount() {
        return layers.size();
    }
}
