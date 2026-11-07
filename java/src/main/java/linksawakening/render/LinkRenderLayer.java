package linksawakening.render;

import linksawakening.entity.Link;
import linksawakening.world.ScrollController;

public final class LinkRenderLayer implements HardwareRenderLayer {
    private final Link link;
    private final ScrollController scrollController;

    public LinkRenderLayer(Link link, ScrollController scrollController) {
        this.link = link;
        this.scrollController = scrollController;
    }

    @Override
    public void render(RenderContext context) {
        int shakeX = -scrollController.screenShakeHorizontal();
        if (!scrollController.isActive()) {
            link.render(context.buffer(), shakeX, 0);
            return;
        }

        int startScreenX = scrollController.linkScreenX();
        int startScreenY = scrollController.linkScreenY();
        int endScreenX = link.pixelX();
        int endScreenY = link.pixelY();
        int screenX = startScreenX + (endScreenX - startScreenX) * scrollController.offset() / scrollController.target();
        int screenY = startScreenY + (endScreenY - startScreenY) * scrollController.offset() / scrollController.target();
        link.render(context.buffer(), screenX - link.pixelX() + shakeX,
            screenY - link.pixelY());
    }
    @Override public void submit(HardwareFrame frame, linksawakening.gpu.GPU gpu) {
        java.util.List<java.util.function.Consumer<OamSink>> commands = new java.util.ArrayList<>();
        OamSink sink = (tile, x, y, fx, fy, palette, behind) ->
            commands.add(target -> target.append(tile, x, y, fx, fy, palette, behind));
        collectOam(sink);
        // Legacy body/equipment drawing is painter ordered; submit its visible
        // priority order explicitly, without converting the tiles to RGBA.
        for (int i = commands.size() - 1; i >= 0; i--) commands.get(i).accept(frame);
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
