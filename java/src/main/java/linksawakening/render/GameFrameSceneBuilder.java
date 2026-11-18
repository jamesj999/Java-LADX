package linksawakening.render;

import linksawakening.cutscene.IntroFrameSnapshot;
import linksawakening.cutscene.IntroSprite;
import linksawakening.cutscene.TitleReveal;
import linksawakening.dialog.DialogController;
import linksawakening.gpu.Framebuffer;
import linksawakening.ui.FileMenuFrameSnapshot;
import linksawakening.ui.FileLoadTransition;
import linksawakening.ui.WorldMapScreen;
import linksawakening.world.RoomPaletteFade;

import java.util.ArrayList;
import java.util.List;

public final class GameFrameSceneBuilder {
    private static final int BG_MAP_WIDTH = 32;
    private static final int BG_MAP_HEIGHT = 32;
    private static final int VIEWPORT_TILE_WIDTH = 20;
    private static final int VIEWPORT_TILE_HEIGHT = 18;

    private byte[] transitionObjects;

    public FrameScene buildWorldMap(WorldMapScreen worldMap, DialogController dialog, boolean showObjects) {
        return FrameScene.of(List.of(
            context -> worldMap.render(context.buffer(), dialog, showObjects)
        ));
    }

    public FrameScene build(GameFrameState state) {
        return build(state, true);
    }

    public FrameScene build(GameFrameState state, boolean showBackgroundSceneSprites) {
        List<RenderLayer> layers = new ArrayList<>();
        if (state.screen() == RenderScreen.OVERWORLD && state.room() != null) {
            layers.add(new RoomRenderLayer(state.room(), state.scrollController(), state.transitionController()));
            if (state.transitionController().isActive()) {
                List<RenderLayer> sprites = new ArrayList<>();
                addRoomSprites(sprites, state);
                for (RenderLayer sprite : sprites) {
                    if (sprite instanceof EntityRenderLayer) layers.add(sprite);
                    else layers.add(context -> {
                        if (transitionObjects == null || transitionObjects.length != context.buffer().length)
                            transitionObjects = new byte[context.buffer().length];
                        sprite.render(new RenderContext(transitionObjects, context.gpu()));
                        RoomPaletteFade.compositeObjects(context.buffer(), transitionObjects, state.transitionController());
                    });
                }
            } else {
                addRoomSprites(layers, state);
            }
            addInventory(layers, state);
        } else if (state.fileMenuFrame() != null
            || (state.introFrameSnapshot() != null || state.tilemap() != null)
            && (state.introFrameSnapshot() != null || state.attrmap() != null)
            && (state.introFrameSnapshot() != null || state.bgPalettes() != null)) {
            addBackgroundSceneLayers(layers, state, showBackgroundSceneSprites);
        }

        if (state.dialogController() != null) {
            layers.add(new DialogRenderLayer(state.dialogController(),
                state.screen() == RenderScreen.OVERWORLD ? state.room() : null, state.frameCounter()));
        }
        return FrameScene.of(layers);
    }

    public FrameScene buildFileLoad(GameFrameState state, FileLoadTransition transition) {
        List<RenderLayer> layers = new ArrayList<>();
        List<RenderLayer> sprites = new ArrayList<>();
        addRoomSprites(sprites, state);
        layers.add(new RoomRenderLayer(state.room(), state.scrollController(), state.transitionController()));
        layers.add(context -> {
            byte[] spriteBuffer = transition.clearSpriteBuffer(context.buffer().length);
            RenderContext objects = new RenderContext(spriteBuffer, context.gpu());
            for (RenderLayer sprite : sprites) sprite.render(objects);
            transition.compositeSprites(context.buffer());
        });
        addInventory(layers, state);
        if (state.dialogController() != null)
            layers.add(new DialogRenderLayer(state.dialogController(), state.room(), state.frameCounter()));
        return FrameScene.of(layers);
    }

    private static void addRoomSprites(List<RenderLayer> layers, GameFrameState state) {
        boolean linkInBed = state.link() != null && state.link().isMarinWakeUpBedVisible();
        if (linkInBed && state.room().entities() != null
            && state.room().entities().spriteTiles() != null
            && state.room().entities().spriteSelection() != null) {
            layers.add(new MarinWakeUpBedRenderLayer(state.link(), state.room().entities().spriteTiles(),
                state.room().entities().spriteSelection().objectPalettes(), state.scrollController()));
        }
        if (state.room().entities() != null
            && state.room().entities().spriteSelection() != null) {
            layers.add(new EntityRenderLayer(state.room().entities(),
                state.transitionController().applyObjectFade(state.room().entities().spriteSelection().objectPalettes()),
                state.scrollController(), state.frameCounter()));
        }
        // DrawLinkSprite uses fixed OAM at C008, ahead of the dynamic entity slots at C030.
        if (state.link() != null && !linkInBed) {
            layers.add(new LinkRenderLayer(state.link(), state.scrollController()));
            if (state.link().powerUpPresentationType() != 0 && state.room().entities() != null
                && state.room().entities().spriteSelection() != null) {
                boolean piece = state.link().powerUpPresentationType() == 0x33;
                int palette = piece && (state.frameCounter() & 4) == 0 ? 3 : 4;
                int[] colors = state.room().entities().spriteSelection().objectPalettes()[palette];
                layers.add(context -> IndexedRenderer.drawSpriteTile8x16(context.buffer(), context.gpu(),
                    piece ? 0x04 : 0xAE, state.link().pixelX() - state.scrollController().screenShakeHorizontal(),
                    state.link().pixelY() - state.link().romEntityZ() - (piece ? 16 : 14),
                    false, piece, colors));
            }
            if (state.link().isTarinShieldPresentationVisible() && state.room().entities() != null
                && state.room().entities().spriteSelection() != null) {
                layers.add(new TarinShieldRenderLayer(state.link(), state.scrollController()));
            }
        }
        if (state.transientVfxSystem() != null && state.cutLeavesEffectRenderer() != null) {
            layers.add(new TransientVfxRenderLayer(state.transientVfxSystem(),
                state.cutLeavesEffectRenderer(), state.scrollController(), state.transientVfxPalette(),
                state.frameCounter()));
        }
        if (state.droppableRupeeSystem() != null) {
            layers.add(new DroppableRupeeRenderLayer(state.droppableRupeeSystem(), state.scrollController()));
        }
    }

    private static void addInventory(List<RenderLayer> layers, GameFrameState state) {
        if (state.inventoryController() != null) {
            layers.add(new InventoryRenderLayer(state.inventoryController(), state.room(),
                state.transitionController(), state.frameCounter()));
        }
    }

    private static void addBackgroundSceneLayers(List<RenderLayer> layers, GameFrameState state,
                                                 boolean showSprites) {
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
        if (objectPalettes != null && showSprites) {
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
