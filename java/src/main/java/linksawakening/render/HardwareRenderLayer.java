package linksawakening.render;

import linksawakening.gpu.GPU;

/** Submits indexed data without writing the output buffer. */
public interface HardwareRenderLayer extends RenderLayer {
    void submit(HardwareFrame frame, GPU gpu);
}
