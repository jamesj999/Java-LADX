package linksawakening.render;



import linksawakening.entity.Link;
import linksawakening.world.transition.ScrollController;

public final class LinkRenderLayer implements HardwareRenderLayer {
    private final Link link;
    private final ScrollController scrollController;

    public LinkRenderLayer(Link link, ScrollController scrollController) {
        this.link = link;
        this.scrollController = scrollController;
    }


    @Override public void submit(HardwareFrame frame, linksawakening.gpu.GPU gpu) {
        collectOam(frame);
    }

    private void collectOam(OamSink sink) {
        int shakeX = -scrollController.screenShakeHorizontal();
        if (!scrollController.isActive()) {
            link.submitOam(sink, shakeX, 0);
            return;
        }

        int startScreenX = scrollController.linkScreenX();
        int startScreenY = scrollController.linkScreenY();
        int endScreenX = link.pixelX();
        int endScreenY = link.pixelY();
        int screenX = startScreenX + (endScreenX - startScreenX) * scrollController.offset() / scrollController.target();
        int screenY = startScreenY + (endScreenY - startScreenY) * scrollController.offset() / scrollController.target();
        link.submitOam(sink, screenX - link.pixelX() + shakeX,
            screenY - link.pixelY());
    }
}
