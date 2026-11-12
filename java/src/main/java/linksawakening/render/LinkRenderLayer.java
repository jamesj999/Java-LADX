package linksawakening.render;

import linksawakening.entity.Link;
import linksawakening.world.transition.ScrollController;

/** Submits Link OAM using interpolated scrolling coordinates and screen shake. */
public final class LinkRenderLayer implements HardwareRenderLayer {
    private Link link;
    private ScrollController scrollController;

    public LinkRenderLayer(Link link, ScrollController scrollController) {
        this.link = link;
        this.scrollController = scrollController;
    }

    void configure(Link link, ScrollController scrollController) {
        this.link = link;
        this.scrollController = scrollController;
    }

    @Override
    public void submit(HardwareFrame frame, linksawakening.gpu.GPU gpu) {
        collectOam(frame);
    }

    private void collectOam(OamSink sink) {
        int shakeX = -scrollController.screenShakeHorizontal();
        if (!scrollController.isActive()) {
            link.animation().submitOam(sink, shakeX, 0);
            return;
        }

        int startScreenX = scrollController.linkScreenX();
        int startScreenY = scrollController.linkScreenY();
        int endScreenX = link.position().pixelX();
        int endScreenY = link.position().pixelY();
        int screenX =
                startScreenX + (endScreenX - startScreenX) * scrollController.offset() / scrollController.target();
        int screenY =
                startScreenY + (endScreenY - startScreenY) * scrollController.offset() / scrollController.target();
        link.animation()
                .submitOam(
                        sink,
                        screenX - link.position().pixelX() + shakeX,
                        screenY - link.position().pixelY());
    }
}
