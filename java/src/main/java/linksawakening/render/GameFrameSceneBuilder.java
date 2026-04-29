package linksawakening.render;

import linksawakening.cutscene.TitleReveal;
import linksawakening.gpu.Framebuffer;

import java.util.ArrayList;
import java.util.List;

public final class GameFrameSceneBuilder {
    private static final int BG_MAP_WIDTH = 32;
    private static final int BG_MAP_HEIGHT = 32;
    private static final int VIEWPORT_TILE_WIDTH = 20;
    private static final int VIEWPORT_TILE_HEIGHT = 18;

    public FrameScene build(GameFrameState state) {
        List<RenderLayer> layers = new ArrayList<>();
        if (state.screen() == RenderScreen.OVERWORLD && state.room() != null) {
            addOverworldLayers(layers, state);
        } else if (state.tilemap() != null && state.attrmap() != null && state.bgPalettes() != null) {
            addBackgroundSceneLayers(layers, state);
        }

        if (state.dialogController() != null) {
            layers.add(new DialogRenderLayer(state.dialogController()));
        }
        return FrameScene.of(layers);
    }

    private static void addOverworldLayers(List<RenderLayer> layers, GameFrameState state) {
        layers.add(new RoomRenderLayer(state.room(), state.scrollController(), state.transitionController()));
        if (state.link() != null) {
            layers.add(new LinkRenderLayer(state.link(), state.scrollController()));
        }
        if (state.transientVfxSystem() != null && state.cutLeavesEffectRenderer() != null) {
            layers.add(new TransientVfxRenderLayer(state.transientVfxSystem(),
                state.cutLeavesEffectRenderer(), state.scrollController(), state.transientVfxPalette()));
        }
        if (state.droppableRupeeSystem() != null) {
            layers.add(new DroppableRupeeRenderLayer(state.droppableRupeeSystem(), state.scrollController()));
        }
        if (state.inventoryController() != null) {
            layers.add(new InventoryRenderLayer(state.inventoryController()));
        }
    }

    private static void addBackgroundSceneLayers(List<RenderLayer> layers, GameFrameState state) {
        int scrollX = state.cutsceneManager() != null ? state.cutsceneManager().scrollX() : 0;
        int scrollY = state.cutsceneManager() != null ? state.cutsceneManager().scrollY() : 0;
        int[] tilemap = titleRevealTilemap(state);
        int[] lineScrollX = state.cutsceneManager() != null
            ? state.cutsceneManager().lineScrollX(Framebuffer.HEIGHT)
            : null;

        if (lineScrollX != null) {
            layers.add(BackgroundRenderLayer.lineScrolled(tilemap, state.attrmap(), state.bgPalettes(),
                BG_MAP_WIDTH, BG_MAP_HEIGHT, VIEWPORT_TILE_WIDTH, VIEWPORT_TILE_HEIGHT,
                lineScrollX, scrollY));
        } else {
            layers.add(BackgroundRenderLayer.scrolling(tilemap, state.attrmap(), state.bgPalettes(),
                BG_MAP_WIDTH, BG_MAP_HEIGHT, VIEWPORT_TILE_WIDTH, VIEWPORT_TILE_HEIGHT,
                scrollX, scrollY));
        }
        if (state.cutsceneManager() != null && state.objPalettes() != null) {
            layers.add(new CutsceneSpriteRenderLayer(state.cutsceneManager().sprites(), state.objPalettes()));
        }
    }

    private static int[] titleRevealTilemap(GameFrameState state) {
        if (state.cutsceneManager() == null || !state.cutsceneManager().isShowingTitleScene()) {
            return state.tilemap();
        }
        return TitleReveal.maskedTilemap(state.tilemap(), state.cutsceneManager().titleRevealRows(), BG_MAP_WIDTH);
    }
}
