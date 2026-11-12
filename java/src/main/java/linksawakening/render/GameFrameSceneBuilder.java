package linksawakening.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Builds ordered scene snapshots while retaining session-local composition scratch. */
public final class GameFrameSceneBuilder {
    private final List<RenderLayer> layers = new ArrayList<>();
    private final HardwareFrame frame = new HardwareFrame(true);
    private final WorldSceneLayers world = new WorldSceneLayers();

    private DialogWindowPainter dialogWindowPainter;
    /** Uses alternate owned glyphs/window submission; null restores ROM-backed dialog rendering. */
    void setDialogWindowPainter(DialogWindowPainter painter) {
        dialogWindowPainter = painter;
    }

    private FrameDrawObserver observer;
    private final ReusableGameFrameState reusableInputs = new ReusableGameFrameState();
    private final ReusableWorldLayers reusable = new ReusableWorldLayers();
    private final FrameScene reusableScene = FrameScene.borrowed(reusable.layers, frame);

    /** Attaches optional inspection to scenes built by this session-local builder. */
    void setDrawObserver(FrameDrawObserver observer) {
        this.observer = observer;
    }

    /** Compatibility bridge for app callers already holding an immutable frame; new room snapshots retain defensive-copy cost. */
    public FrameScene buildReusable(GameFrameState state) {
        return build(reusableInputs.copyFrom(state));
    }

    /** Refills a borrowed scene, valid until this builder's next build. Capture callers use immutable build(GameFrameState). */
    public FrameScene build(ReusableGameFrameState state) {
        reusable.fill(state, dialogWindowPainter);
        return reusableScene.observedBy(observer);
    }

    public FrameScene build(GameFrameState state) {
        layers.clear();
        if (state.screen() == RenderScreen.OVERWORLD && state.room() != null) {
            world.add(layers, state);
        } else if (state.fileMenuFrame() != null
                || (state.introFrameSnapshot() != null || state.tilemap() != null)
                        && (state.introFrameSnapshot() != null || state.attrmap() != null)
                        && (state.introFrameSnapshot() != null || state.bgPalettes() != null)) {
            BackgroundSceneLayers.add(layers, state);
        }

        addDialogLayers(state);
        if (state.link() != null && state.link().status().isPassingOut() && state.screen() == RenderScreen.OVERWORLD) {
            for (int i = 0; i < layers.size(); i++) {
                RenderLayer layer = layers.get(i);
                if (!(layer instanceof LinkRenderLayer)) {
                    layers.set(
                            i,
                            new PassOutPaletteRenderLayer(
                                    layer, state.link().status().passOutPaletteFrames()));
                }
            }
        }
        if (state.screen() == RenderScreen.OVERWORLD
                && state.inventoryController() != null
                && state.inventoryController().transition().blocksWorld()) {
            for (int i = 0; i < layers.size(); i++)
                layers.set(
                        i,
                        new InventoryFadeRenderLayer(
                                layers.get(i), state.inventoryController().transition()));
        }
        return FrameScene.of(layers, frame).observedBy(observer);
    }

    private void addDialogLayers(GameFrameState state) {
        if (state.dialogController() != null) {
            int[] dialogPalette = state.room() == null ? null : state.room().palettes()[1];
            int[][] dialogObjectPalettes = state.room() == null
                            || state.room().entities() == null
                            || state.room().entities().spriteSelection() == null
                    ? null
                    : state.room().entities().spriteSelection().objectPalettes();
            var dialog = new DialogRenderLayer(
                    state.dialogController(), dialogPalette, dialogObjectPalettes, state.frameCounter());
            dialog.windowPainter = dialogWindowPainter;
            layers.add(dialog);
        }
        if (state.room() != null
                && state.room().heartPiece() != null
                && state.dialogController() != null
                && state.dialogController().isActive()
                && state.dialogController().characterIndex() < 0x21) {
            var visual = state.room().heartPiece();
            int[] pairs = visual.spritePairs();
            int offset = Math.min(4, visual.pieces()) * 4;
            int y = state.dialogController().boxPosition() == linksawakening.dialog.DialogController.BoxPosition.BOTTOM
                    ? 0x6B - 16
                    : 0x23 - 16;
            int[][] palettes = state.room().entities().spriteSelection().objectPalettes();
            var heartSprites = new ArrayList<SpriteListRenderLayer.Sprite>();
            for (int half = 0; half < 2; half++) {
                int attr = pairs[offset + half * 2 + 1];
                heartSprites.add(new SpriteListRenderLayer.Sprite(
                        pairs[offset + half * 2],
                        0x8E - 8 + half * 8,
                        y,
                        attr & 7,
                        (attr & 32) != 0,
                        (attr & 64) != 0,
                        16));
            }
            Collections.reverse(heartSprites);
            layers.add(new SpriteListRenderLayer(heartSprites, palettes));
        }
    }
}
