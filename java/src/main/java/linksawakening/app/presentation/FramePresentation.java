package linksawakening.app.presentation;

import java.util.Objects;
import linksawakening.app.debug.DebugTools;

/** Composes the game followed by the optional debug overlay. */
public final class FramePresentation {
    private final RenderStateAssembler renderer;
    private final DebugTools debug;
    private final byte[] framebuffer;

    public FramePresentation(RenderStateAssembler renderer, DebugTools debug, byte[] framebuffer) {
        this.renderer = Objects.requireNonNull(renderer);
        this.debug = Objects.requireNonNull(debug);
        this.framebuffer = Objects.requireNonNull(framebuffer);
    }

    public void composeFrameBuffer() {
        renderer.composeFrameBuffer();
        debug.render(framebuffer);
    }
}
