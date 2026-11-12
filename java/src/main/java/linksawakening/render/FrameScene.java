package linksawakening.render;

import java.util.List;
import linksawakening.gpu.GPU;

/** Retains ordered layers and reusable scratch storage for indexed frame composition. */
public final class FrameScene {
    private static final FrameScene EMPTY = new FrameScene(List.of());

    private final List<RenderLayer> layers;
    private HardwareFrame reusableFrame;

    private FrameDrawObserver observer;

    private FrameScene(List<RenderLayer> layers) {
        this(layers, false);
    }

    private FrameScene(List<RenderLayer> layers, boolean borrowed) {
        this.layers = borrowed ? layers : List.copyOf(layers);
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
        drawTo(buffer, gpu, observer);
    }

    public void drawTo(byte[] buffer, GPU gpu, FrameDrawObserver observer) {
        hardwareFrame(gpu).compose(buffer);
        if (observer != null) observer.drawn(this, buffer, gpu);
    }

    List<RenderLayer> layersForComparison() {
        return layers;
    }

    HardwareFrame hardwareFrame(GPU gpu) {
        return hardwareFrame(gpu, (HardwareFrame) null);
    }

    HardwareFrame hardwareFrame(GPU gpu, HardwareFrame suppliedFrame) {
        for (int layerIndex = 0; layerIndex < layers.size(); layerIndex++) {
            RenderLayer layer = layers.get(layerIndex);
            if (!(layer instanceof HardwareRenderLayer hardware) || !hardware.supportsHardware())
                throw new IllegalStateException(
                        "Layer not migrated: " + layer.getClass().getName());
        }
        HardwareFrame frame;
        if (suppliedFrame != null) frame = suppliedFrame;
        else {
            if (reusableFrame == null) reusableFrame = new HardwareFrame(true);
            frame = reusableFrame;
            frame.reset();
        }
        for (int layerIndex = 0; layerIndex < layers.size(); layerIndex++) {
            RenderLayer layer = layers.get(layerIndex);
            if (frame.observer() != null) frame.submittingLayer(layer.getClass().getSimpleName());
            else frame.objectVisibilitySource(layer);
            ((HardwareRenderLayer) layer).submit(frame, gpu);
        }
        return frame;
    }

    static FrameScene borrowed(List<RenderLayer> layers, HardwareFrame frame) {
        FrameScene scene = new FrameScene(layers, true);
        scene.reusableFrame = frame;
        return scene;
    }

    static FrameScene of(List<RenderLayer> layers, HardwareFrame frame) {
        FrameScene scene = new FrameScene(layers);
        scene.reusableFrame = frame;
        return scene;
    }

    FrameScene observedBy(FrameDrawObserver observer) {
        this.observer = observer;
        return this;
    }

    public int layerCount() {
        return layers.size();
    }
}
