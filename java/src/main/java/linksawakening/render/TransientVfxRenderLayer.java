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
    private final int frameCounter;

    public TransientVfxRenderLayer(TransientVfxSystem transientVfxSystem,
                                   CutLeavesEffectRenderer cutLeavesEffectRenderer,
                                   ScrollController scrollController,
                                   int[] palette) {
        this(transientVfxSystem, cutLeavesEffectRenderer, scrollController, palette, 0);
    }

    public TransientVfxRenderLayer(TransientVfxSystem transientVfxSystem,
                                   CutLeavesEffectRenderer cutLeavesEffectRenderer,
                                   ScrollController scrollController,
                                   int[] palette, int frameCounter) {
        this.transientVfxSystem = transientVfxSystem;
        this.cutLeavesEffectRenderer = cutLeavesEffectRenderer;
        this.scrollController = scrollController;
        this.palette = palette;
        this.frameCounter = frameCounter & 0xFF;
    }

    @Override
    public void render(RenderContext context) {
        if (scrollController.isActive()) {
            return;
        }
        int shakeX = -scrollController.screenShakeHorizontal();
        for (TransientVfxSystem.Slot slot : transientVfxSystem.activeSlots()) {
            var sprites = switch (slot.type()) {
                case BUSH_LEAVES -> cutLeavesEffectRenderer.renderBushLeaves(
                    slot.worldX(), slot.worldY(), slot.countdown());
                case WATER_SPLASH -> cutLeavesEffectRenderer.renderWaterSplash(
                    slot.worldX(), slot.worldY(), slot.countdown());
                case POOF, PEGASUS_DUST, CHEST_APPEARS, STAIRS_APPEARS -> cutLeavesEffectRenderer.renderPoof(
                    slot.worldX(), slot.worldY(), slot.countdown());
                case SWORD_POKE -> cutLeavesEffectRenderer.renderSwordPoke(
                    slot.worldX(), slot.worldY(), slot.countdown());
                case SMOKE -> cutLeavesEffectRenderer.renderSmoke(
                    slot.worldX(), slot.worldY(), slot.countdown());
                case LASER_BEAM -> cutLeavesEffectRenderer.renderLaserBeam(
                    slot.worldX(), slot.worldY(), slot.countdown(), frameCounter,
                    slot.slotIndex());
                case MOVING_SPARKLE -> cutLeavesEffectRenderer.renderMovingSparkle(
                    slot.worldX(), slot.worldY(), slot.countdown(), slot.variant());
                case SWORD_BEAM -> cutLeavesEffectRenderer.renderSwordBeam(
                    slot.worldX(), slot.worldY(), slot.countdown(), frameCounter,
                    slot.slotIndex(), slot.variant());
            };
            for (CutLeavesEffectRenderer.SpritePlacement sprite : sprites) {
                IndexedRenderer.drawSpriteTile(context.buffer(), sprite.tile(), sprite.x() + shakeX,
                    sprite.y(),
                    (sprite.attributes() & ATTR_FLIP_X) != 0,
                    (sprite.attributes() & ATTR_FLIP_Y) != 0,
                    palette);
            }
        }
    }
}
