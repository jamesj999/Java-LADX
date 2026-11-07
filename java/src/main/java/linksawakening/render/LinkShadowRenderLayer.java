package linksawakening.render;

import linksawakening.entity.Link;
import linksawakening.world.ScrollController;

/** Bank $02:$7590: Link's airborne shadow in OAM slot zero. */
@Deprecated
public final class LinkShadowRenderLayer implements RenderLayer {
    private final Link link;
    private final int[] palette;
    private final ScrollController scroll;
    private final int frame;

    public LinkShadowRenderLayer(Link link, int[] palette, ScrollController scroll, int frame) {
        this.link = link;
        this.palette = palette;
        this.scroll = scroll;
        this.frame = frame;
    }

    @Override
    public void render(RenderContext context) {
        if (link.romEntityZ() == 0 || (frame & 1) != 0 || scroll.isActive()
            || link.romEntityY() + 0x0B >= 0x88) return;
        new SpriteListRenderLayer(java.util.List.of(new SpriteListRenderLayer.Sprite(0x26,
            link.pixelX() + 4 - scroll.screenShakeHorizontal(), link.pixelY() + 0x0B,
            0, false, false, 16)), new int[][] { palette }).render(context);
    }
}
