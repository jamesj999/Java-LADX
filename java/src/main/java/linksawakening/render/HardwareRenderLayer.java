package linksawakening.render;

import linksawakening.gpu.GPU;

/** Submits indexed data without writing the output buffer. */
public interface HardwareRenderLayer extends RenderLayer {
    @Override
    default void render(RenderContext context) {
        if (context == null || context.buffer() == null || context.gpu() == null)
            throw new IllegalArgumentException("Render context is incomplete");
        HardwareFrame frame = new HardwareFrame(true);
        submit(frame, context.gpu());
        frame.composeOverlay(context.buffer());
    }

    default boolean supportsHardware() {
        return true;
    }

    void submit(HardwareFrame frame, GPU gpu);
}
