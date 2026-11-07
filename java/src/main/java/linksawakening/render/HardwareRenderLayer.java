package linksawakening.render;

import linksawakening.gpu.GPU;

/** Submits indexed data without writing the output buffer. */
public interface HardwareRenderLayer extends RenderLayer {
    @Override default void render(RenderContext context) {
        RgbaCallbackCompatibility.renderIndexed(this, context);
    }
    default boolean supportsHardware() { return true; }
    void submit(HardwareFrame frame, GPU gpu);
}
