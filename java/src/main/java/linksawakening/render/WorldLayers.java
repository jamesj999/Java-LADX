package linksawakening.render;

import java.util.ArrayList;
import java.util.List;

/** Session-local configurable layers and palette scopes for every screen. Lists and layers never escape ownership.  Hardware analogue: ordered BG/window and OBJ submission. */
final class WorldLayers {
    final List<RenderLayer> layers = new ArrayList<>(12);
    private final RoomRenderLayer room = new RoomRenderLayer();
    private final EntityRenderLayer entities = new EntityRenderLayer();
    private final LinkRenderLayer link = new LinkRenderLayer(null, null);
    private final InventoryRenderLayer inventory = new InventoryRenderLayer(null, null);
    private final DialogRenderLayer dialog = new DialogRenderLayer(null, null, null, 0);
    private final TransientVfxRenderLayer vfx = new TransientVfxRenderLayer();
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

    void fill(GameFrameState state, DialogWindowPainter painter, boolean showMenuObjects) {
        layers.clear();
        switch (state.screen()) {
            case OVERWORLD -> {
                if (state.roomInputs.present) fillWorld(state);
                else background.fill(layers, state, showMenuObjects);
            }
            case WORLD_MAP -> {
                if (state.worldMap() != null) layers.add(state.worldMap());
                return;
            }
            case TITLE, CUTSCENE, FILE_MENU, FILE_SAVE -> background.fill(layers, state, showMenuObjects);
        }
        addDialog(state, painter);
        if (state.screen() == RenderScreen.OVERWORLD
                && ((state.overworld.hasTransitionController()
                                && state.transitionController().isActive())
                        || (state.overworld.hasLink() && state.link().status().isPassingOut())
                        || (state.overworld.hasInventoryController()
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
        boolean warping = data.warpPresent;
        if (warping) warp.configure(state);
        int[][] bgPalettes = warping ? warp.backgroundPalettes : data.palettes;
        room.configure(data.tiles, data.attributes, bgPalettes, state.scrollController(), state.transitionController());
        List<RenderLayer> sprites = warping ? warp.sprites : layers;
        layers.add(room);
        boolean visible = !state.overworld.hasInventoryController()
                || state.inventoryController().transition().worldSpritesVisible();
        if (visible && data.entitiesPresent) {
            entities.configure(
                    data.entities,
                    state.transitionController()
                            .applyManboFade(warping ? warp.objectPalettes : data.objectPalettes, true),
                    state.scrollController(),
                    state.frameCounter(),
                    state.overworld.hasDialogController()
                                    && state.dialogController().isActive()
                            ? state.dialogController().boxPosition()
                            : null);
            layers.add(entities);
        }
        if (visible) {
            if (state.overworld.hasLink()) {
                if (state.link().animation().isMarinWakeUpBedVisible()) {
                    if (data.entityTilesPresent) {
                        marin.configure(state);
                        sprites.add(marin);
                    }
                } else {
                    link.configure(state.link(), state.scrollController());
                    sprites.add(link);
                    if (data.entitiesPresent) {
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
            if (state.overworld.hasTransientVfxSystem() && state.overworld.hasCutLeavesEffectRenderer()) {
                vfx.configure(
                        state.transientVfxSystem(),
                        state.cutLeavesEffectRenderer(),
                        state.scrollController(),
                        state.transientVfxPalette(),
                        state.frameCounter());
                sprites.add(vfx);
            }
            if (state.overworld.hasDroppableRupeeSystem()) {
                rupees.configure(state.droppableRupeeSystem(), state.scrollController());
                sprites.add(rupees);
            }
        }
        if (warping && visible) warp.addSprites(layers);
        if (state.overworld.hasInventoryController() && (!warping || data.warp.commandBatches() == 0)) {
            inventory.configure(state.inventoryController(), bgPalettes);
            layers.add(inventory);
        }
        if (warping) warp.addFinalPlanes(layers);
    }

    private void addDialog(GameFrameState state, DialogWindowPainter painter) {
        var data = state.roomInputs;
        if (state.overworld.hasDialogController()) {
            dialog.windowPainter(painter);
            dialog.configure(
                    state.dialogController(),
                    data.palettes == null ? null : data.palettes[1],
                    data.objectPalettes,
                    state.frameCounter());
            layers.add(dialog);
        }
        if (data.heartPresent
                && state.overworld.hasDialogController()
                && state.dialogController().isActive()
                && state.dialogController().characterIndex() < 0x21) {
            heart.configure(state);
            layers.add(heart);
        }
    }
}
