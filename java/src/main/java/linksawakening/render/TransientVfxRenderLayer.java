package linksawakening.render;

import linksawakening.vfx.CutLeavesEffectRenderer;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.world.transition.ScrollController;

/** TransientVfxRenderLayer supplies frame rendering inputs or composition. */
final class TransientVfxRenderLayer implements HardwareRenderLayer {
    private static final int ATTR_FLIP_X = 0x20;
    private static final int ATTR_FLIP_Y = 0x40;

    private TransientVfxSystem transientVfxSystem;
    private CutLeavesEffectRenderer cutLeavesEffectRenderer;
    private ScrollController scrollController;
    private int[] palette;
    private int frameCounter;

    public TransientVfxRenderLayer(
            TransientVfxSystem transientVfxSystem,
            CutLeavesEffectRenderer cutLeavesEffectRenderer,
            ScrollController scrollController,
            int[] palette) {
        this(transientVfxSystem, cutLeavesEffectRenderer, scrollController, palette, 0);
    }

    public TransientVfxRenderLayer(
            TransientVfxSystem transientVfxSystem,
            CutLeavesEffectRenderer cutLeavesEffectRenderer,
            ScrollController scrollController,
            int[] palette,
            int frameCounter) {
        this.transientVfxSystem = transientVfxSystem;
        this.cutLeavesEffectRenderer = cutLeavesEffectRenderer;
        this.scrollController = scrollController;
        this.palette = palette;
        this.frameCounter = frameCounter & 0xFF;
    }

    private VfxPoseCache poses;

    void configure(
            TransientVfxSystem system,
            CutLeavesEffectRenderer renderer,
            ScrollController scroll,
            int[] palette,
            int counter) {
        if (poses == null || cutLeavesEffectRenderer != renderer) poses = new VfxPoseCache(renderer);
        transientVfxSystem = system;
        cutLeavesEffectRenderer = renderer;
        scrollController = scroll;
        this.palette = palette;
        frameCounter = counter & 255;
    }

    private void submitEffects(RenderContext context) {
        if (scrollController.isActive()) {
            return;
        }
        int shakeX = -scrollController.screenShakeHorizontal();
        if (transientVfxSystem.activeCount() == 0) return;
        var slots = transientVfxSystem.activeSlots();
        for (int slotIndex = 0; slotIndex < slots.size(); slotIndex++) {
            var slot = slots.get(slotIndex);
            if (poses != null) {
                var pose = poses.pose(slot, frameCounter);
                for (int i = 0; i < pose.size(); i++) {
                    var sprite = pose.get(i);
                    OamDrawing.draw8(
                            context,
                            sprite.tile(),
                            slot.worldX() + sprite.x() + shakeX,
                            slot.worldY() + sprite.y(),
                            (sprite.attributes() & ATTR_FLIP_X) != 0,
                            (sprite.attributes() & ATTR_FLIP_Y) != 0,
                            palette);
                }
                continue;
            }
            var sprites =
                    switch (slot.type()) {
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
                                slot.worldX(), slot.worldY(), slot.countdown(), frameCounter, slot.slotIndex());
                        case MOVING_SPARKLE -> cutLeavesEffectRenderer.renderMovingSparkle(
                                slot.worldX(), slot.worldY(), slot.countdown(), slot.variant());
                        case SWORD_BEAM -> cutLeavesEffectRenderer.renderSwordBeam(
                                slot.worldX(),
                                slot.worldY(),
                                slot.countdown(),
                                frameCounter,
                                slot.slotIndex(),
                                slot.variant());
                    };
            for (CutLeavesEffectRenderer.SpritePlacement sprite : sprites) {
                OamDrawing.draw8(
                        context,
                        sprite.tile(),
                        sprite.x() + shakeX,
                        sprite.y(),
                        (sprite.attributes() & ATTR_FLIP_X) != 0,
                        (sprite.attributes() & ATTR_FLIP_Y) != 0,
                        palette);
            }
        }
    }

    @Override
    public void submit(HardwareFrame frame, linksawakening.gpu.GPU gpu) {
        submitEffects(RenderContext.indexed(gpu, frame));
    }
}
