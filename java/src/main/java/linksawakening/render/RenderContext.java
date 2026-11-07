package linksawakening.render;

import linksawakening.gpu.GPU;

public record RenderContext(byte[] buffer, GPU gpu, OamSink oam) {
    public RenderContext(byte[] buffer, GPU gpu) { this(buffer, gpu, null); }
}
