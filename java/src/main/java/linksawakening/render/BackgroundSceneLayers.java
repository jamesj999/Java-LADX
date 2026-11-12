package linksawakening.render;

import java.util.ArrayList;
import java.util.List;
import linksawakening.cutscene.IntroFrameSnapshot;
import linksawakening.cutscene.IntroSprite;
import linksawakening.cutscene.TitleReveal;
import linksawakening.gpu.Framebuffer;
import linksawakening.ui.FileMenuFrameSnapshot;

/** Assembles immutable menu and intro snapshots into indexed layers. */
final class BackgroundSceneLayers {
    private static final int BG_MAP_WIDTH = 32;
    private static final int BG_MAP_HEIGHT = 32;
    private static final int VIEWPORT_TILE_WIDTH = 20;
    private static final int VIEWPORT_TILE_HEIGHT = 18;

    static void add(List<RenderLayer> layers, GameFrameState state) {
        FileMenuFrameSnapshot fileMenu = state.fileMenuFrame();
        IntroFrameSnapshot snapshot = state.introFrameSnapshot();
        int scrollX = fileMenu != null
                ? 0
                : snapshot != null
                        ? snapshot.scrollX()
                        : state.cutsceneManager() != null
                                ? state.cutsceneManager().scrollX()
                                : 0;
        int scrollY = fileMenu != null
                ? 0
                : snapshot != null
                        ? snapshot.scrollY()
                        : state.cutsceneManager() != null
                                ? state.cutsceneManager().scrollY()
                                : 0;
        int[] tilemap = fileMenu != null
                ? fileMenu.tilemap()
                : snapshot != null ? snapshot.tilemap() : titleRevealTilemap(state);
        int[] attrmap = fileMenu != null ? fileMenu.attrmap() : snapshot != null ? snapshot.attrmap() : state.attrmap();
        int[][] palettes = fileMenu != null
                ? fileMenu.bgPalettes()
                : snapshot != null ? snapshot.bgPalettes() : state.bgPalettes();
        int[] lineScrollX = fileMenu != null
                ? null
                : snapshot != null
                        ? resizeLineScroll(snapshot.lineScrollX(), Framebuffer.HEIGHT)
                        : state.cutsceneManager() != null
                                ? state.cutsceneManager().lineScrollX(Framebuffer.HEIGHT)
                                : null;
        addTileUpload(layers, fileMenu, snapshot);
        if (fileMenu == null && snapshot != null && snapshot.lineScrollY() != null) {
            int[] lineY = snapshot.lineScrollY();
            layers.add(new LineScrollBackgroundRenderLayer(tilemap, attrmap, palettes, lineScrollX, scrollY, lineY));
        } else if (lineScrollX != null) {
            layers.add(BackgroundRenderLayer.lineScrolled(
                    tilemap,
                    attrmap,
                    palettes,
                    BG_MAP_WIDTH,
                    BG_MAP_HEIGHT,
                    VIEWPORT_TILE_WIDTH,
                    VIEWPORT_TILE_HEIGHT,
                    lineScrollX,
                    scrollY));
        } else {
            layers.add(BackgroundRenderLayer.scrolling(
                    tilemap,
                    attrmap,
                    palettes,
                    BG_MAP_WIDTH,
                    BG_MAP_HEIGHT,
                    VIEWPORT_TILE_WIDTH,
                    VIEWPORT_TILE_HEIGHT,
                    scrollX,
                    scrollY));
        }
        addSprites(layers, state, fileMenu, snapshot);
    }

    private static void addTileUpload(
            List<RenderLayer> layers, FileMenuFrameSnapshot fileMenu, IntroFrameSnapshot snapshot) {
        if (fileMenu == null && snapshot != null && snapshot.animatedTiles() != null) {
            byte[] data = snapshot.animatedTiles();
            layers.add(new TileUploadRenderLayer(data, snapshot.animatedTilesDestination()));
        }
    }

    private static void addSprites(
            List<RenderLayer> layers,
            GameFrameState state,
            FileMenuFrameSnapshot fileMenu,
            IntroFrameSnapshot snapshot) {
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
                var entries = new ArrayList<SpriteListRenderLayer.Sprite>();
                for (IntroSprite sprite : sprites) {
                    entries.add(new SpriteListRenderLayer.Sprite(
                            sprite.tileIndex(),
                            sprite.x(),
                            sprite.y(),
                            sprite.paletteIndex(),
                            sprite.flipX(),
                            sprite.flipY(),
                            sprite.height()));
                }
                layers.add(new SpriteListRenderLayer(entries, objectPalettes));
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
        return TitleReveal.maskedTilemap(
                state.tilemap(), state.cutsceneManager().titleRevealRows(), BG_MAP_WIDTH);
    }
}
