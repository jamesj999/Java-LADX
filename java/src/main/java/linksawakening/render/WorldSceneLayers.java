package linksawakening.render;

import java.util.ArrayList;
import java.util.List;

/** Assembles ordered room, gameplay sprite and inventory layers. */
final class WorldSceneLayers {
    private final List<RenderLayer> sprites = new ArrayList<>();
    private linksawakening.entity.Link cachedLink;
    private linksawakening.world.transition.ScrollController cachedScroll;
    private LinkRenderLayer linkLayer;

    void add(List<RenderLayer> layers, GameFrameState state) {
        var warp = state.room().instrumentWarp();
        int[][] backgroundPalettes = warp == null
                ? state.room().palettes()
                : warp.backgroundPalettes(state.room().palettes());
        var room = new linksawakening.world.room.RoomRenderSnapshot(
                state.room().tileIds(),
                state.room().tileAttrs(),
                backgroundPalettes,
                state.room().entities());
        layers.add(new RoomRenderLayer(room, state.scrollController(), state.transitionController()));
        sprites.clear();
        boolean showWorldSprites = state.inventoryController() == null
                || state.inventoryController().transition().worldSpritesVisible();
        if (showWorldSprites
                && state.room().entities() != null
                && state.room().entities().spriteSelection() != null) {
            int[][] objectPalettes = state.room().entities().spriteSelection().objectPalettes();
            layers.add(new EntityRenderLayer(
                    state.room().entities(),
                    warp == null ? objectPalettes : warp.objectPalettes(objectPalettes),
                    state.scrollController(),
                    state.frameCounter(),
                    state.dialogController() != null && state.dialogController().isActive()
                            ? state.dialogController().boxPosition()
                            : null));
        }
        addLinkSprites(state);
        if (state.transientVfxSystem() != null && state.cutLeavesEffectRenderer() != null) {
            sprites.add(new TransientVfxRenderLayer(
                    state.transientVfxSystem(),
                    state.cutLeavesEffectRenderer(),
                    state.scrollController(),
                    state.transientVfxPalette(),
                    state.frameCounter()));
        }
        if (state.droppableRupeeSystem() != null) {
            sprites.add(new DroppableRupeeRenderLayer(state.droppableRupeeSystem(), state.scrollController()));
        }
        if (showWorldSprites && warp == null) {
            layers.addAll(sprites);
        } else if (showWorldSprites) {
            layers.add(new InstrumentWarpRenderLayer(sprites, warp.objectFade()));
        }
        if (state.inventoryController() != null && (warp == null || warp.commandBatches() == 0)) {
            layers.add(new InventoryRenderLayer(state.inventoryController(), backgroundPalettes));
        }
        if (warp != null
                && state.room().entities() != null
                && state.room().entities().spriteSelection() != null) {
            layers.add(InstrumentWarpRenderLayer.swirl(warp, state.room().entities(), state.frameCounter()));
        }
        if (warp != null && warp.commandBatches() > 0) {
            layers.add(InstrumentWarpRenderLayer.fullscreen(warp, state.room().palettes()));
        }
    }

    private void addLinkSprites(GameFrameState state) {
        if (state.link() != null) {
            if (state.link().isMarinWakeUpBedVisible()) {
                if (state.room().entities() != null
                        && state.room().entities().spriteTiles() != null
                        && state.room().entities().spriteSelection() != null) {
                    sprites.add(StorySprites.marinBed(
                            state.link(),
                            state.room().entities().spriteTiles(),
                            state.room().entities().spriteSelection().objectPalettes(),
                            state.scrollController()));
                }
            } else {
                if (cachedLink != state.link() || cachedScroll != state.scrollController()) {
                    cachedLink = state.link();
                    cachedScroll = state.scrollController();
                    linkLayer = new LinkRenderLayer(cachedLink, cachedScroll);
                }
                sprites.add(linkLayer);
                if (state.room().entities() != null
                        && state.room().entities().spriteSelection() != null
                        && !state.room().entities().sideScrolling()) {
                    sprites.add(StorySprites.linkShadow(
                            state.link(),
                            state.room().entities().spriteSelection().objectPalettes()[0],
                            state.scrollController(),
                            state.frameCounter()));
                    sprites.add(StorySprites.linkGrass(
                            state.link(),
                            state.room().entities().spriteSelection().objectPalettes(),
                            state.scrollController(),
                            state.room().linkGrassPalette()));
                }
                if (state.link().presentedDialogGotItem() != 0
                        && state.room().entities() != null
                        && state.room().entities().spriteSelection() != null) {
                    sprites.add(StorySprites.dialogGotItem(
                            state.link(),
                            state.room().entities().spriteSelection().objectPalettes(),
                            state.scrollController()));
                }
                if (state.link().isTarinShieldPresentationVisible()
                        && state.room().entities() != null
                        && state.room().entities().spriteSelection() != null) {
                    sprites.add(StorySprites.tarinShield(state.link(), state.scrollController()));
                }
            }
        }
    }
}
