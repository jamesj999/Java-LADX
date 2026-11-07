package linksawakening.render;

import linksawakening.entity.Link;
import linksawakening.world.ScrollController;

/** Bank $02:$7590: Link's airborne shadow in OAM slot zero. */
@Deprecated
public final class LinkShadowRenderLayer implements HardwareRenderLayer {
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


    @Override public void submit(HardwareFrame frame, linksawakening.gpu.GPU gpu) {

        if (link.romEntityZ() == 0 || (this.frame & 1) != 0 || scroll.isActive()
            || link.romEntityY() + 0x0B >= 0x88) return;
        // bank2.asm $759B-$75AF writes wLinkOAMBuffer ($C000), OAM slot 0.
        frame.append8x16(gpu.getTile(0x26), gpu.getTile(0x27),
            link.pixelX() + 4 - scroll.screenShakeHorizontal(), link.pixelY() + 0x0B,
            false, false, palette, false);
    }
}
