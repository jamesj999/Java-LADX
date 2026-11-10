package linksawakening.render;

import linksawakening.gpu.GPU;
import linksawakening.ui.InventoryTransition;

/** DX $20:6C4F/$20:6C7A fade BG and OBJ palettes on alternating handler calls. */
final class InventoryFadeRenderLayer implements HardwareRenderLayer {
    private final HardwareRenderLayer layer;
    private final InventoryTransition transition;

    InventoryFadeRenderLayer(RenderLayer layer, InventoryTransition transition) {
        this.layer = (HardwareRenderLayer) layer;
        this.transition = transition;
    }
    @Override public void submit(HardwareFrame frame, GPU gpu) {
        frame.withPaletteTransforms(rgb -> transition.color(rgb, false), rgb -> transition.color(rgb, true),
            () -> frame.withLayerSource(layer.getClass().getSimpleName(), () -> layer.submit(frame, gpu)));
    }
    @Override public void render(RenderContext context) {
        // Keep the legacy layer boundary: dialog/inventory filters may only see
        // their own OAM here, whereas production sees the complete frame's OAM.
        HardwareFrame isolated = new HardwareFrame(true);
        submit(isolated, context.gpu());
        isolated.composeOverlay(context.buffer());
    }
}
