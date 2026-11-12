package linksawakening.render;

import java.util.function.IntUnaryOperator;
import linksawakening.gpu.GPU;
import linksawakening.ui.InventoryTransition;

/** Owns synchronous submission callbacks and nested pass-out/inventory palette scopes. */
final class ReusablePaletteLayer implements HardwareRenderLayer {
    private HardwareRenderLayer child;
    private InventoryTransition inventory;
    private int passOutFrames;
    private boolean passOut;
    private String source;
    private HardwareFrame frame;
    private GPU gpu;
    private final IntUnaryOperator passTransform = this::passColor;
    private final IntUnaryOperator background = rgb -> inventory.color(rgb, false),
            object = rgb -> inventory.color(rgb, true);
    private final Runnable draw = this::drawChild, inner = this::drawInner, sourced = this::drawSourced;

    void configure(RenderLayer layer, GameFrameInputs state) {
        child = (HardwareRenderLayer) layer;
        passOut = state.screen() == RenderScreen.OVERWORLD
                && state.link() != null
                && state.link().status().isPassingOut()
                && !(layer instanceof LinkRenderLayer);
        passOutFrames = passOut ? state.link().status().passOutPaletteFrames() : 0;
        inventory = state.screen() == RenderScreen.OVERWORLD
                        && state.inventoryController() != null
                        && state.inventoryController().transition().blocksWorld()
                ? state.inventoryController().transition()
                : null;
        source = passOut ? "PassOutPaletteRenderLayer" : layer.getClass().getSimpleName();
    }

    @Override
    public void submit(HardwareFrame frame, GPU gpu) {
        this.frame = frame;
        this.gpu = gpu;
        if (inventory != null) frame.withPaletteTransforms(background, object, sourced);
        else drawSourced();
    }

    private void drawSourced() {
        frame.withLayerSource(source, inner);
    }

    private void drawInner() {
        if (passOut) frame.withPaletteTransform(passTransform, draw);
        else drawChild();
    }

    private void drawChild() {
        child.submit(frame, gpu);
    }

    private int passColor(int rgb) {
        int result = 0;
        for (int shift = 0; shift <= 16; shift += 8)
            result |= (Math.min(31, ((rgb >>> shift) & 255) / 8 + passOutFrames) << 3) << shift;
        return result;
    }
}
