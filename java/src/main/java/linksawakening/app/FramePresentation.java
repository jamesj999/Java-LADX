package linksawakening.app;

import java.util.Objects;
import linksawakening.app.debug.DebugMenuOverlay;

/** Composes the game followed by the optional debug overlay. */
public final class FramePresentation {
    private final RenderStateAssembler renderer;
    private final DebugState debug;
    private final byte[] framebuffer;

    public FramePresentation(RenderStateAssembler renderer, DebugState debug, byte[] framebuffer) {
        this.renderer = Objects.requireNonNull(renderer);
        this.debug = Objects.requireNonNull(debug);
        this.framebuffer = Objects.requireNonNull(framebuffer);
    }

    void composeFrameBuffer() {
        renderer.composeFrameBuffer();
        if (debug.debugMenu != null && debug.debugMenu.isOpen()) {
            if (debug.debugMenuOverlay == null) debug.debugMenuOverlay = new DebugMenuOverlay();
            debug.debugMenuOverlay.render(debug.debugMenu, framebuffer);
        }
    }
}
