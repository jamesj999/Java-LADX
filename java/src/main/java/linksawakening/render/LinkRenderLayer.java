package linksawakening.render;

import linksawakening.entity.Link;
import linksawakening.world.ScrollController;

public final class LinkRenderLayer implements RenderLayer {
    private final Link link;
    private final ScrollController scrollController;

    public LinkRenderLayer(Link link, ScrollController scrollController) {
        this.link = link;
        this.scrollController = scrollController;
    }

    @Override
    public void render(RenderContext context) {
        if (!scrollController.isActive()) {
            link.render(context.buffer(), 0, 0);
            return;
        }

        int startScreenX = scrollController.linkScreenX();
        int startScreenY = scrollController.linkScreenY();
        int endScreenX = link.pixelX();
        int endScreenY = link.pixelY();
        int screenX = startScreenX + (endScreenX - startScreenX) * scrollController.offset() / scrollController.target();
        int screenY = startScreenY + (endScreenY - startScreenY) * scrollController.offset() / scrollController.target();
        link.render(context.buffer(), screenX - link.pixelX(), screenY - link.pixelY());
    }
}
