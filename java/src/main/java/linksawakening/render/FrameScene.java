package linksawakening.render;

import java.util.List;
import java.util.ServiceLoader;
import linksawakening.gpu.GPU;

/** Retains ordered layers and reusable scratch storage for indexed frame composition. */
public final class FrameScene {
    private static final FrameScene EMPTY = new FrameScene(List.of());

    private final List<RenderLayer> layers;
    private HardwareFrame reusableFrame;
    private final boolean hardwareSupported;

    private static final FrameDrawObserver.Provider OBSERVERS =
            ServiceLoader.load(FrameDrawObserver.Provider.class).findFirst().orElse(null);

    private FrameScene(List<RenderLayer> layers) {
        this.layers = List.copyOf(layers);
        boolean supported = true;
        for (RenderLayer layer : layers)
            if (!(layer instanceof HardwareRenderLayer hardware) || !hardware.supportsHardware()) supported = false;
        hardwareSupported = supported;
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
        drawTo(buffer, gpu, OBSERVERS == null ? null : OBSERVERS.current());
    }

    public void drawTo(byte[] buffer, GPU gpu, FrameDrawObserver observer) {
        if (!hardwareSupported) {
            // Preserve the original functional RenderLayer callback API outside production scenes.
            RgbaCallbackCompatibility.drawCallbacks(layers, buffer, gpu);
            if (observer != null) observer.drawn(this, buffer, gpu);
            return;
        }
        hardwareFrame(gpu).compose(buffer);
        if (observer != null) observer.drawn(this, buffer, gpu);
    }

    List<RenderLayer> layersForComparison() {
        return layers;
    }

    HardwareFrame hardwareFrame(GPU gpu) {
        return hardwareFrame(gpu, null);
    }

    HardwareFrame hardwareFrame(GPU gpu, HardwareFrameDiagnostics diagnostics) {
        for (RenderLayer layer : layers) {
            if (!(layer instanceof HardwareRenderLayer hardware) || !hardware.supportsHardware())
                throw new IllegalStateException(
                        "Layer not migrated: " + layer.getClass().getName());
        }
        HardwareFrame frame;
        if (diagnostics != null) frame = new HardwareFrame(true, diagnostics);
        else {
            if (reusableFrame == null) reusableFrame = new HardwareFrame(true);
            frame = reusableFrame;
            frame.reset();
        }
        for (RenderLayer layer : layers) {
            frame.submittingLayer(layer.getClass().getSimpleName());
            ((HardwareRenderLayer) layer).submit(frame, gpu);
        }
        return frame;
    }

    static FrameScene of(List<RenderLayer> layers, HardwareFrame frame) {
        FrameScene scene = new FrameScene(layers);
        scene.reusableFrame = frame;
        return scene;
    }

    public int layerCount() {
        return layers.size();
    }
}
