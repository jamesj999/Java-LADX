package linksawakening.render;

import java.util.ArrayList;
import java.util.List;
import linksawakening.cutscene.IntroFrameSnapshot;
import linksawakening.cutscene.IntroSprite;
import linksawakening.cutscene.TitleReveal;
import linksawakening.gpu.Framebuffer;
import linksawakening.ui.FileMenuFrameSnapshot;

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
            int[] dialogPalette = state.room() == null ? null : state.room().palettes()[1];
            int[][] dialogObjectPalettes = state.room() == null
                            || state.room().entities() == null
                            || state.room().entities().spriteSelection() == null
                    ? null
                    : state.room().entities().spriteSelection().objectPalettes();
            layers.add(new DialogRenderLayer(
                    state.dialogController(), dialogPalette, dialogObjectPalettes, state.frameCounter()));
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
            java.util.Collections.reverse(heartSprites);
            layers.add(new SpriteListRenderLayer(heartSprites, palettes));
        }
        if (state.link() != null && state.link().isPassingOut() && state.screen() == RenderScreen.OVERWORLD) {
            for (int i = 0; i < layers.size(); i++) {
                RenderLayer layer = layers.get(i);
                if (!(layer instanceof LinkRenderLayer)) {
                    layers.set(
                            i, new PassOutPaletteRenderLayer(layer, state.link().passOutPaletteFrames()));
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
        return FrameScene.of(layers);
    }

    private static void addOverworldLayers(List<RenderLayer> layers, GameFrameState state) {
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
        List<RenderLayer> sprites = new ArrayList<>();
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
                sprites.add(new LinkRenderLayer(state.link(), state.scrollController()));
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

    private static void addBackgroundSceneLayers(List<RenderLayer> layers, GameFrameState state) {
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

        if (fileMenu == null && snapshot != null && snapshot.animatedTiles() != null) {
            byte[] data = snapshot.animatedTiles();
            layers.add(new TileUploadRenderLayer(data, snapshot.animatedTilesDestination()));
        }
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
                var entries = new java.util.ArrayList<SpriteListRenderLayer.Sprite>();
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
