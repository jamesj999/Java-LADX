package linksawakening.render;

import linksawakening.vfx.CutLeavesEffectRenderer;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.ScrollController;

public final class TransientVfxRenderLayer implements RenderLayer {
    private static final int ATTR_FLIP_X = 0x20;
    private static final int ATTR_FLIP_Y = 0x40;

    private final TransientVfxSystem transientVfxSystem;
    private final CutLeavesEffectRenderer cutLeavesEffectRenderer;
    private final ScrollController scrollController;
    private final int[] palette;

    public TransientVfxRenderLayer(TransientVfxSystem transientVfxSystem,
                                   CutLeavesEffectRenderer cutLeavesEffectRenderer,
                                   ScrollController scrollController,
                                   int[] palette) {
        this.transientVfxSystem = transientVfxSystem;
        this.cutLeavesEffectRenderer = cutLeavesEffectRenderer;
        this.scrollController = scrollController;
        this.palette = palette;
    }

    @Override
    public void render(RenderContext context) {
        if (scrollController.isActive()) {
            return;
        }
        for (TransientVfxSystem.Slot slot : transientVfxSystem.activeSlots()) {
            if (slot.type() != TransientVfxType.BUSH_LEAVES) {
                continue;
            }
            for (CutLeavesEffectRenderer.SpritePlacement sprite :
                cutLeavesEffectRenderer.renderBushLeaves(slot.worldX(), slot.worldY(), slot.countdown())) {
                IndexedRenderer.drawSpriteTile(context.buffer(), sprite.tile(), sprite.x(), sprite.y(),
                    (sprite.attributes() & ATTR_FLIP_X) != 0,
                    (sprite.attributes() & ATTR_FLIP_Y) != 0,
                    palette);
            }
        }
    }
}
