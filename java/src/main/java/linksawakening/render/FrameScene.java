package linksawakening.render;

import java.util.List;
import linksawakening.gpu.GPU;

/** Retains ordered layers and reusable scratch storage for indexed frame composition. */
public final class FrameScene {
    private static final FrameScene EMPTY = new FrameScene(List.of());

    private final List<RenderLayer> layers;
    private final HardwareFrame reusableFrame;

    private FrameDrawObserver observer = FrameDrawObserver.NONE;

    private FrameScene(List<RenderLayer> layers) {
        this(layers, false, new HardwareFrame(true));
    }

    private FrameScene(List<RenderLayer> layers, boolean borrowed, HardwareFrame frame) {
        reusableFrame = frame;
        this.layers = borrowed ? layers : List.copyOf(layers);
    }

    static FrameScene empty() {
        return EMPTY;
    }

    static FrameScene of(List<RenderLayer> layers) {
        if (layers.isEmpty()) {
            return EMPTY;
        }
        return new FrameScene(layers);
    }

    static FrameScene borrowed(List<RenderLayer> layers, HardwareFrame frame) {
        return new FrameScene(layers, true, frame);
    }

    static FrameScene of(List<RenderLayer> layers, HardwareFrame frame) {
        return new FrameScene(layers, false, frame);
    }

    public void drawTo(byte[] buffer, GPU gpu) {
        drawTo(buffer, gpu, observer);
    }

    public void drawWithFileLoadFade(byte[] buffer, GPU gpu, linksawakening.ui.FileLoadTransition load) {
        reusableFrame.reset();
        reusableFrame.withPaletteTransforms(
                load::backgroundColor, load::objectColor, () -> hardwareFrame(gpu, reusableFrame));
        reusableFrame.compose(buffer);
        observer.drawn(this, buffer, gpu);
    }

    public int layerCount() {
        return layers.size();
    }

    void drawTo(byte[] buffer, GPU gpu, FrameDrawObserver observer) {
        hardwareFrame(gpu).compose(buffer);
        observer.drawn(this, buffer, gpu);
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
            frame = reusableFrame;
            frame.reset();
        }
        for (int layerIndex = 0; layerIndex < layers.size(); layerIndex++) {
            RenderLayer layer = layers.get(layerIndex);
            if (frame.observer() != HardwareFrameObserver.NONE)
                frame.submittingLayer(layer.getClass().getSimpleName());
            else frame.objectVisibilitySource(layer);
            ((HardwareRenderLayer) layer).submit(frame, gpu);
        }
        return frame;
    }

    FrameScene observedBy(FrameDrawObserver observer) {
        this.observer = observer == null ? FrameDrawObserver.NONE : observer;
        return this;
    }
}
