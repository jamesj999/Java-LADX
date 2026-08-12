package linksawakening.render;

import linksawakening.entity.Link;
import linksawakening.world.ScrollController;

/** Renders data_005_4cc6 while Tarin holds the shield above Link. */
public final class TarinShieldRenderLayer implements RenderLayer {
    private static final int SHIELD_TILE = 0x86;
    private final Link link;
    private final ScrollController scrollController;

    public TarinShieldRenderLayer(Link link, ScrollController scrollController) {
        if (link == null || link.tarinShieldPalette() == null || scrollController == null) {
            throw new IllegalArgumentException("Tarin shield render inputs are incomplete");
        }
        this.link = link;
        this.scrollController = scrollController;
    }

    @Override
    public void render(RenderContext context) {
        int screenX = link.pixelX() - scrollController.screenShakeHorizontal();
        int screenY = link.pixelY() - 0x0C;
        IndexedRenderer.drawSpriteTile8x16(context.buffer(), context.gpu(), SHIELD_TILE,
            screenX, screenY, false, false, link.tarinShieldPalette());
    }
}
