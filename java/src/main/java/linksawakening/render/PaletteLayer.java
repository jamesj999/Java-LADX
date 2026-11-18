package linksawakening.render;

import java.util.function.IntUnaryOperator;
import linksawakening.gpu.GPU;
import linksawakening.ui.InventoryTransition;

/** Owns synchronous submission callbacks and nested pass-out/inventory palette scopes.  Hardware analogue: BG/window and OBJ palette scopes. */
final class PaletteLayer implements HardwareRenderLayer {
    private HardwareRenderLayer child;
    private final InventoryTransition worldPalette = new InventoryTransition();
    private InventoryTransition inventory = worldPalette;
    private boolean inventoryPalette;
    private int passOutFrames;
    private boolean passOut;
    private String source;
    private HardwareFrame frame;
    private GPU gpu;
    private final IntUnaryOperator passTransform = this::passColor;
    // Retain one callable instance: its identity or allocation-free reuse is part of this retained path.
    @SuppressWarnings("UnnecessaryLambda")
    private final IntUnaryOperator background = rgb -> inventory.color(rgb, false);
    // Retain one callable instance: its identity or allocation-free reuse is part of this retained path.
    @SuppressWarnings("UnnecessaryLambda")
    private final IntUnaryOperator object = rgb -> inventory.color(rgb, true);

    private linksawakening.world.transition.TransitionController mapFade;
    private final IntUnaryOperator mapBackground = this::mapBackgroundColor;
    private final IntUnaryOperator mapObject = this::mapObjectColor;
    private final Runnable drawInventory = this::drawInventory;
    private final Runnable draw = this::drawChild, inner = this::drawInner, sourced = this::drawSourced;

    @Override
    public void submit(HardwareFrame frame, GPU gpu) {
        this.frame = frame;
        this.gpu = gpu;
        if (mapFade != null) frame.withPaletteTransforms(mapBackground, mapObject, drawInventory);
        else drawInventory();
    }

    private int mapBackgroundColor(int rgb) {
        return mapFade.mapFadeColor(rgb, false);
    }

    private int mapObjectColor(int rgb) {
        return mapFade.mapFadeColor(rgb, true);
    }

    private void drawInventory() {
        if (inventoryPalette) frame.withPaletteTransforms(background, object, sourced);
        else drawSourced();
    }

    void configure(RenderLayer layer, GameFrameState state) {
        child = (HardwareRenderLayer) layer;
        mapFade = state.overworld.hasTransitionController()
                        && state.transitionController().isMapFade()
                ? state.transitionController()
                : null;
        passOut = state.screen() == RenderScreen.OVERWORLD
                && state.overworld.hasLink()
                && state.link().status().isPassingOut()
                && !(layer instanceof LinkRenderLayer);
        passOutFrames = passOut ? state.link().status().passOutPaletteFrames() : 0;
        inventoryPalette = state.screen() == RenderScreen.OVERWORLD
                && state.overworld.hasInventoryController()
                && state.inventoryController().transition().blocksWorld();
        inventory = inventoryPalette ? state.inventoryController().transition() : worldPalette;
        source = passOut ? "PassOutPaletteRenderLayer" : layer.getClass().getSimpleName();
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
