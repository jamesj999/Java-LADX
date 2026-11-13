package linksawakening.render;

import java.util.ArrayList;
import java.util.List;

/** Session-local configurable layers and palette scopes for every screen. Lists and layers never escape ownership. */
final class WorldLayers {
    final List<RenderLayer> layers = new ArrayList<>(12);
    private final RoomRenderLayer room = new RoomRenderLayer();
    private EntityRenderLayer entities;
    private final LinkRenderLayer link = new LinkRenderLayer(null, null);
    private final InventoryRenderLayer inventory = new InventoryRenderLayer(null, null);
    private final DialogRenderLayer dialog = new DialogRenderLayer(null, null, null, 0);
    private final TransientVfxRenderLayer vfx = new TransientVfxRenderLayer(null, null, null, null);
    private final DroppableRupeeRenderLayer rupees = new DroppableRupeeRenderLayer(null, null);
    private final StoryLayer shadow = new StoryLayer(StoryLayer.Kind.SHADOW),
            grass = new StoryLayer(StoryLayer.Kind.GRASS),
            marin = new StoryLayer(StoryLayer.Kind.MARIN),
            tarin = new StoryLayer(StoryLayer.Kind.TARIN),
            gotItem = new StoryLayer(StoryLayer.Kind.GOT_ITEM),
            heart = new StoryLayer(StoryLayer.Kind.HEART);
    private final WarpLayers warp = new WarpLayers();

    private final BackgroundLayers background = new BackgroundLayers();
    private final ArrayList<PaletteLayer> paletteLayers = new ArrayList<>();

    void fill(GameFrameState state, DialogWindowPainter painter) {
        layers.clear();
        if (state.screen() == RenderScreen.OVERWORLD && state.roomInputs.tiles != null) fillWorld(state);
        else background.fill(layers, state);
        addDialog(state, painter);
        if (state.screen() == RenderScreen.OVERWORLD
                && ((state.link() != null && state.link().status().isPassingOut())
                        || (state.inventoryController() != null
                                && state.inventoryController().transition().blocksWorld()))) {
            for (int i = 0; i < layers.size(); i++) {
                if (i == paletteLayers.size()) paletteLayers.add(new PaletteLayer());
                var wrapper = paletteLayers.get(i);
                wrapper.configure(layers.get(i), state);
                layers.set(i, wrapper);
            }
        }
    }

    private void fillWorld(GameFrameState state) {
        var data = state.roomInputs;
        data.refreshSelection();
        boolean warping = data.warp != null;
        if (warping) warp.configure(state);
        int[][] bgPalettes = warping ? warp.backgroundPalettes : data.palettes;
        room.configure(data.tiles, data.attributes, bgPalettes, state.scrollController(), state.transitionController());
        List<RenderLayer> sprites = warping ? warp.sprites : layers;
        layers.add(room);
        boolean visible = state.inventoryController() == null
                || state.inventoryController().transition().worldSpritesVisible();
        if (visible && data.entities != null && data.entities.spriteSelection() != null) {
            if (entities == null)
                entities = new EntityRenderLayer(data.entities, data.objectPalettes, state.scrollController());
            entities.configure(
                    data.entities,
                    warping ? warp.objectPalettes : data.objectPalettes,
                    state.scrollController(),
                    state.frameCounter(),
                    state.dialogController() != null && state.dialogController().isActive()
                            ? state.dialogController().boxPosition()
                            : null);
            layers.add(entities);
        }
        if (visible) {
            if (state.link() != null) {
                if (state.link().animation().isMarinWakeUpBedVisible()) {
                    if (data.entities != null
                            && data.entities.spriteTiles() != null
                            && data.entities.spriteSelection() != null) {
                        marin.configure(state);
                        sprites.add(marin);
                    }
                } else {
                    link.configure(state.link(), state.scrollController());
                    sprites.add(link);
                    if (data.entities != null && data.entities.spriteSelection() != null) {
                        if (!data.entities.sideScrolling()) {
                            shadow.configure(state);
                            grass.configure(state);
                            sprites.add(shadow);
                            sprites.add(grass);
                        }
                        if (state.link().animation().presentedDialogGotItem() != 0) {
                            gotItem.configure(state);
                            sprites.add(gotItem);
                        }
                        if (state.link().animation().isTarinShieldPresentationVisible()) {
                            tarin.configure(state);
                            sprites.add(tarin);
                        }
                    }
                }
            }
            if (state.transientVfxSystem() != null && state.cutLeavesEffectRenderer() != null) {
                vfx.configure(
                        state.transientVfxSystem(),
                        state.cutLeavesEffectRenderer(),
                        state.scrollController(),
                        state.transientVfxPalette(),
                        state.frameCounter());
                sprites.add(vfx);
            }
            if (state.droppableRupeeSystem() != null) {
                rupees.configure(state.droppableRupeeSystem(), state.scrollController());
                sprites.add(rupees);
            }
        }
        if (warping && visible) warp.addSprites(layers);
        if (state.inventoryController() != null && (!warping || data.warp.commandBatches() == 0)) {
            inventory.configure(state.inventoryController(), bgPalettes);
            layers.add(inventory);
        }
        if (warping) warp.addFinalPlanes(layers);
    }

    private void addDialog(GameFrameState state, DialogWindowPainter painter) {
        var data = state.roomInputs;
        if (state.dialogController() != null) {
            dialog.windowPainter(painter);
            dialog.configure(
                    state.dialogController(),
                    data.palettes == null ? null : data.palettes[1],
                    data.objectPalettes,
                    state.frameCounter());
            layers.add(dialog);
        }
        if (data.heart != null
                && state.dialogController() != null
                && state.dialogController().isActive()
                && state.dialogController().characterIndex() < 0x21) {
            heart.configure(state);
            layers.add(heart);
        }
    }
}
