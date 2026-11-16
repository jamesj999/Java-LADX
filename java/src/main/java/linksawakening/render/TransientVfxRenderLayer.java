package linksawakening.render;

import linksawakening.vfx.CutLeavesEffectRenderer;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.world.transition.ScrollController;

/** TransientVfxRenderLayer supplies frame rendering inputs or composition.  Hardware analogue: transient OBJ effects. */
final class TransientVfxRenderLayer implements HardwareRenderLayer {
    private static final int ATTR_FLIP_X = 0x20;
    private static final int ATTR_FLIP_Y = 0x40;

    private TransientVfxSystem transientVfxSystem;
    private CutLeavesEffectRenderer cutLeavesEffectRenderer;
    private ScrollController scrollController;
    private int[] palette;
    private int frameCounter;

    private VfxPoseCache poses;

    /** Retained frame adapter; configure supplies its borrowed inputs before submission. */
    TransientVfxRenderLayer() {}

    @Override
    public void submit(HardwareFrame frame, linksawakening.gpu.GPU gpu) {
        submitEffects(RenderContext.indexed(gpu, frame));
    }

    void configure(
            TransientVfxSystem system,
            CutLeavesEffectRenderer renderer,
            ScrollController scroll,
            int[] palette,
            int counter) {
        if (cutLeavesEffectRenderer != renderer) poses = new VfxPoseCache(renderer);
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
        var slots = transientVfxSystem.liveSlots();
        for (int slotIndex = 0; slotIndex < slots.size(); slotIndex++) {
            var slot = slots.get(slotIndex);

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
        }
    }
}
