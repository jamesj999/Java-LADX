package linksawakening.render;

import java.util.ArrayList;
import java.util.List;
import linksawakening.gpu.GPU;

/** Session-local configurable layers for ordinary overworld frames. Lists and layers never escape ownership. */
final class ReusableWorldLayers {
    final List<RenderLayer> layers = new ArrayList<>(12);
    private final RoomRenderLayer room = new RoomRenderLayer();
    private EntityRenderLayer entities;
    private final LinkRenderLayer link = new LinkRenderLayer(null, null);
    private final InventoryRenderLayer inventory = new InventoryRenderLayer(null, null);
    private final DialogRenderLayer dialog = new DialogRenderLayer(null, null, null, 0);
    private final TransientVfxRenderLayer vfx = new TransientVfxRenderLayer(null, null, null, null);
    private final DroppableRupeeRenderLayer rupees = new DroppableRupeeRenderLayer(null, null);
    private final StoryLayer shadow = new StoryLayer(false), grass = new StoryLayer(true);

    boolean supports(ReusableGameFrameState state) {
        return state.screen() == RenderScreen.OVERWORLD
                && state.roomInputs.tiles != null
                && state.roomInputs.warp == null
                && state.roomInputs.heart == null
                && (state.link() == null
                        || !state.link().status().isPassingOut()
                                && !state.link().animation().isMarinWakeUpBedVisible()
                                && !state.link().animation().isTarinShieldPresentationVisible()
                                && state.link().animation().presentedDialogGotItem() == 0)
                && (state.inventoryController() == null
                        || !state.inventoryController().transition().blocksWorld());
    }

    void fill(ReusableGameFrameState state, DialogWindowPainter painter) {
        layers.clear();
        var data = state.roomInputs;
        data.refreshSelection();
        room.configure(
                data.tiles, data.attributes, data.palettes, state.scrollController(), state.transitionController());
        layers.add(room);
        boolean visible = state.inventoryController() == null
                || state.inventoryController().transition().worldSpritesVisible();
        if (visible && data.entities != null && data.entities.spriteSelection() != null) {
            if (entities == null)
                entities = new EntityRenderLayer(data.entities, data.objectPalettes, state.scrollController());
            entities.configure(
                    data.entities,
                    data.objectPalettes,
                    state.scrollController(),
                    state.frameCounter(),
                    state.dialogController() != null && state.dialogController().isActive()
                            ? state.dialogController().boxPosition()
                            : null);
            layers.add(entities);
        }
        if (visible) {
            if (state.link() != null) {
                link.configure(state.link(), state.scrollController());
                layers.add(link);
                if (data.entities != null
                        && data.entities.spriteSelection() != null
                        && !data.entities.sideScrolling()) {
                    shadow.state = state;
                    grass.state = state;
                    layers.add(shadow);
                    layers.add(grass);
                }
            }
            if (state.transientVfxSystem() != null && state.cutLeavesEffectRenderer() != null) {
                vfx.configure(
                        state.transientVfxSystem(),
                        state.cutLeavesEffectRenderer(),
                        state.scrollController(),
                        state.transientVfxPalette(),
                        state.frameCounter());
                layers.add(vfx);
            }
            if (state.droppableRupeeSystem() != null) {
                rupees.configure(state.droppableRupeeSystem(), state.scrollController());
                layers.add(rupees);
            }
        }
        if (state.inventoryController() != null) {
            inventory.configure(state.inventoryController(), data.palettes);
            layers.add(inventory);
        }
        if (state.dialogController() != null) {
            dialog.windowPainter = painter;
            dialog.configure(state.dialogController(), data.palettes[1], data.objectPalettes, state.frameCounter());
            layers.add(dialog);
        }
    }

    private static final class StoryLayer implements HardwareRenderLayer {
        private final boolean grass;
        private ReusableGameFrameState state;

        StoryLayer(boolean grass) {
            this.grass = grass;
        }

        @Override
        public void submit(HardwareFrame frame, GPU gpu) {
            if (grass)
                StorySprites.submitLinkGrass(
                        state.link(),
                        state.roomInputs.objectPalettes,
                        state.scrollController(),
                        state.roomInputs.grassPalette,
                        frame,
                        gpu);
            else
                StorySprites.submitLinkShadow(
                        state.link(),
                        state.roomInputs.objectPalettes[0],
                        state.scrollController(),
                        state.frameCounter(),
                        frame,
                        gpu);
        }
    }
}
