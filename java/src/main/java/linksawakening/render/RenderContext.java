package linksawakening.render;

import linksawakening.gpu.GPU;

/** Carries the target RGBA buffer, tile source and optional indexed OAM sink. */
record RenderContext(byte[] buffer, GPU gpu, OamSink oam) {
    private static final byte[] INDEXED_BUFFER = new byte[0];

    static RenderContext indexed(GPU gpu, OamSink sink) {
        return sink instanceof HardwareFrame frame
                ? frame.indexedContext(gpu)
                : new RenderContext(INDEXED_BUFFER, gpu, sink);
    }

    public RenderContext(byte[] buffer, GPU gpu) {
        this(buffer, gpu, null);
    }
}
