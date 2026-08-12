package linksawakening.render;

import linksawakening.cutscene.IntroFrameSnapshot;
import linksawakening.cutscene.IntroSprite;
import linksawakening.cutscene.TitleReveal;
import linksawakening.gpu.Framebuffer;
import linksawakening.ui.FileMenuFrameSnapshot;

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
        } else if (state.fileMenuFrame() != null
            || (state.introFrameSnapshot() != null || state.tilemap() != null)
            && (state.introFrameSnapshot() != null || state.attrmap() != null)
            && (state.introFrameSnapshot() != null || state.bgPalettes() != null)) {
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
            if (state.link().isMarinWakeUpBedVisible()) {
                if (state.room().entities() != null
                    && state.room().entities().spriteTiles() != null
                    && state.room().entities().spriteSelection() != null) {
                    layers.add(new MarinWakeUpBedRenderLayer(state.link(),
                        state.room().entities().spriteTiles(),
                        state.room().entities().spriteSelection().objectPalettes(),
                        state.scrollController()));
                }
            } else {
                layers.add(new LinkRenderLayer(state.link(), state.scrollController()));
                if (state.link().isTarinShieldPresentationVisible()
                    && state.room().entities() != null
                    && state.room().entities().spriteSelection() != null) {
                    layers.add(new TarinShieldRenderLayer(state.link(), state.scrollController()));
                }
            }
        }
        if (state.room().entities() != null
            && state.room().entities().spriteSelection() != null) {
            layers.add(new EntityRenderLayer(state.room().entities(),
                state.room().entities().spriteSelection().objectPalettes(),
                state.scrollController(), state.frameCounter()));
        }
        if (state.transientVfxSystem() != null && state.cutLeavesEffectRenderer() != null) {
            layers.add(new TransientVfxRenderLayer(state.transientVfxSystem(),
                state.cutLeavesEffectRenderer(), state.scrollController(), state.transientVfxPalette(),
                state.frameCounter()));
        }
        if (state.droppableRupeeSystem() != null) {
            layers.add(new DroppableRupeeRenderLayer(state.droppableRupeeSystem(), state.scrollController()));
        }
        if (state.inventoryController() != null) {
            layers.add(new InventoryRenderLayer(state.inventoryController()));
        }
    }

    private static void addBackgroundSceneLayers(List<RenderLayer> layers, GameFrameState state) {
        FileMenuFrameSnapshot fileMenu = state.fileMenuFrame();
        IntroFrameSnapshot snapshot = state.introFrameSnapshot();
        int scrollX = fileMenu != null ? 0 : snapshot != null
            ? snapshot.scrollX()
            : state.cutsceneManager() != null ? state.cutsceneManager().scrollX() : 0;
        int scrollY = fileMenu != null ? 0 : snapshot != null
            ? snapshot.scrollY() + snapshot.verticalWaveOffset()
            : state.cutsceneManager() != null ? state.cutsceneManager().scrollY() : 0;
        int[] tilemap = fileMenu != null
            ? fileMenu.tilemap()
            : snapshot != null ? snapshot.tilemap() : titleRevealTilemap(state);
        int[] attrmap = fileMenu != null
            ? fileMenu.attrmap()
            : snapshot != null ? snapshot.attrmap() : state.attrmap();
        int[][] palettes = fileMenu != null
            ? fileMenu.bgPalettes()
            : snapshot != null ? snapshot.bgPalettes() : state.bgPalettes();
        int[] lineScrollX = fileMenu != null ? null : snapshot != null
            ? resizeLineScroll(snapshot.lineScrollX(), Framebuffer.HEIGHT)
            : state.cutsceneManager() != null
                ? state.cutsceneManager().lineScrollX(Framebuffer.HEIGHT)
                : null;

        if (lineScrollX != null) {
            layers.add(BackgroundRenderLayer.lineScrolled(tilemap, attrmap, palettes,
                BG_MAP_WIDTH, BG_MAP_HEIGHT, VIEWPORT_TILE_WIDTH, VIEWPORT_TILE_HEIGHT,
                lineScrollX, scrollY));
        } else {
            layers.add(BackgroundRenderLayer.scrolling(tilemap, attrmap, palettes,
                BG_MAP_WIDTH, BG_MAP_HEIGHT, VIEWPORT_TILE_WIDTH, VIEWPORT_TILE_HEIGHT,
                scrollX, scrollY));
        }
        int[][] objectPalettes = fileMenu != null
            ? fileMenu.objectPalettes()
            : snapshot != null ? snapshot.objPalettes() : state.objPalettes();
        if (objectPalettes != null) {
            Iterable<IntroSprite> sprites = fileMenu != null
                ? fileMenu.sprites()
                : snapshot != null
                ? snapshot.sprites()
                : state.cutsceneManager() != null
                    ? state.cutsceneManager().sprites()
                    : List.of();
            if (fileMenu != null || snapshot != null || state.cutsceneManager() != null) {
                layers.add(new CutsceneSpriteRenderLayer(sprites, objectPalettes));
            }
        }
    }

    private static int[] resizeLineScroll(int[] lineScroll, int height) {
        if (lineScroll == null || lineScroll.length == 0) {
            return null;
        }
        int[] resized = new int[height];
        for (int index = 0; index < height; index++) {
            resized[index] = lineScroll[Math.min(index, lineScroll.length - 1)];
        }
        return resized;
    }

    private static int[] titleRevealTilemap(GameFrameState state) {
        if (state.cutsceneManager() == null || !state.cutsceneManager().isShowingTitleScene()) {
            return state.tilemap();
        }
        return TitleReveal.maskedTilemap(state.tilemap(), state.cutsceneManager().titleRevealRows(), BG_MAP_WIDTH);
    }
}
