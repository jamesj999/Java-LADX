package linksawakening.render;

import java.util.List;
import linksawakening.cutscene.IntroFrameSnapshot;
import linksawakening.cutscene.IntroSprite;
import linksawakening.cutscene.TitleReveal;
import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.GPU;
import linksawakening.ui.FileMenuFrameSnapshot;

/** Owns retained intro/menu planes, snapshot extraction and direct immutable-sprite submission. */
final class ReusableBackgroundLayers {
    private final Node upload = new Node(0), background = new Node(1), objects = new Node(2);
    private IntroFrameSnapshot capturedIntro;
    private FileMenuFrameSnapshot capturedMenu;
    private int[] introTiles, introAttrs, introX, introY, menuTiles, menuAttrs;
    private int[][] introBg, introObj, menuBg, menuObj;
    private byte[] introUpload;
    private int[] tiles, attrs, lineX, lineY;
    private int[][] bg, obj;
    private int scrollX, scrollY, uploadDestination;
    private byte[] uploadBytes;
    private List<IntroSprite> sprites;
    private int[] revealTiles;
    private static final int[][] REVEAL_MASKS = revealMasks();

    void fill(List<RenderLayer> layers, GameFrameInputs state) {
        var menu = state.fileMenuFrame();
        var intro = state.introFrameSnapshot();
        if (menu == null
                && (intro == null
                        && (state.tilemap() == null || state.attrmap() == null || state.bgPalettes() == null))) return;
        uploadBytes = null;
        lineY = null;
        if (menu != null) {
            capture(menu);
            tiles = menuTiles;
            attrs = menuAttrs;
            bg = menuBg;
            obj = menuObj;
            scrollX = scrollY = 0;
            lineX = null;
            sprites = menu.sprites();
        } else if (intro != null) {
            capture(intro);
            tiles = introTiles;
            attrs = introAttrs;
            bg = introBg;
            obj = introObj;
            scrollX = intro.scrollX();
            scrollY = intro.scrollY();
            lineX = introX;
            lineY = introY;
            sprites = intro.sprites();
            uploadBytes = introUpload;
            uploadDestination = intro.animatedTilesDestination();
        } else {
            tiles = state.tilemap();
            attrs = state.attrmap();
            bg = state.bgPalettes();
            obj = state.objPalettes();
            var cutscene = state.cutsceneManager();
            scrollX = cutscene == null ? 0 : cutscene.scrollX();
            scrollY = cutscene == null ? 0 : cutscene.scrollY();
            lineX = cutscene == null ? null : cutscene.lineScrollX(Framebuffer.HEIGHT);
            sprites = cutscene == null ? null : cutscene.sprites();
            if (cutscene != null && cutscene.isShowingTitleScene()) maskTitle(cutscene.titleRevealRows());
        }
        if (uploadBytes != null) layers.add(upload);
        layers.add(background);
        if (obj != null && (menu != null || intro != null || state.cutsceneManager() != null)) layers.add(objects);
    }

    private void capture(FileMenuFrameSnapshot value) {
        if (value == capturedMenu) return;
        capturedMenu = value;
        menuTiles = value.tilemap();
        menuAttrs = value.attrmap();
        menuBg = value.bgPalettes();
        menuObj = value.objectPalettes();
    }

    private void capture(IntroFrameSnapshot value) {
        if (value == capturedIntro) return;
        capturedIntro = value;
        introTiles = value.tilemap();
        introAttrs = value.attrmap();
        introBg = value.bgPalettes();
        introObj = value.objPalettes();
        int[] source = value.lineScrollX();
        if (source == null || source.length == 0) introX = null;
        else {
            if (introX == null) introX = new int[Framebuffer.HEIGHT];
            for (int i = 0; i < introX.length; i++) introX[i] = source[Math.min(i, source.length - 1)];
        }
        introY = value.lineScrollY();
        introUpload = value.animatedTiles();
    }

    private void maskTitle(int rows) {
        if (rows >= TitleReveal.ROW_COUNT) return;
        if (revealTiles == null || revealTiles.length != tiles.length) revealTiles = new int[tiles.length];
        System.arraycopy(tiles, 0, revealTiles, 0, tiles.length);
        int[] mask = REVEAL_MASKS[Math.max(0, rows)];
        for (int i = 0; i < mask.length; i++) if (mask[i] != 0) revealTiles[i] = mask[i];
        tiles = revealTiles;
    }

    private static int[][] revealMasks() {
        int[][] result = new int[TitleReveal.ROW_COUNT][];
        for (int i = 0; i < result.length; i++) result[i] = TitleReveal.maskedTilemap(new int[32 * 32], i, 32);
        return result;
    }

    private final class Node implements HardwareRenderLayer {
        private final int kind;

        Node(int kind) {
            this.kind = kind;
        }

        @Override
        public void submit(HardwareFrame frame, GPU gpu) {
            if (kind == 0) {
                frame.submittingLayer("TileUploadRenderLayer");
                for (int i = 0; i < uploadBytes.length; i++) gpu.writeVRAM(uploadDestination + i, uploadBytes[i]);
            } else if (kind == 1) {
                frame.submittingLayer(lineY == null ? "BackgroundRenderLayer" : "LineScrollBackgroundRenderLayer");
                if (lineY != null)
                    HardwareBackgroundRenderer.renderBackgroundLineScroll(
                            frame, gpu, tiles, attrs, bg, 32, 32, 20, 18, lineX, scrollY, lineY);
                else if (lineX != null)
                    HardwareBackgroundRenderer.renderBackgroundLineScroll(
                            frame, gpu, tiles, attrs, bg, 32, 32, 20, 18, lineX, scrollY);
                else
                    HardwareBackgroundRenderer.renderBackground(
                            frame, gpu, tiles, attrs, bg, 32, 32, 20, 18, scrollX, scrollY);
            } else {
                frame.submittingLayer("SpriteListRenderLayer");
                if (sprites != null)
                    for (int i = sprites.size() - 1; i >= 0; i--) {
                        IntroSprite sprite = sprites.get(i);
                        int[] palette = obj[Math.min(sprite.paletteIndex(), obj.length - 1)];
                        if (sprite.height() == 16)
                            OamDrawing.draw8x16(
                                    RenderContext.indexed(gpu, frame),
                                    gpu,
                                    sprite.tileIndex(),
                                    sprite.x(),
                                    sprite.y(),
                                    sprite.flipX(),
                                    sprite.flipY(),
                                    palette);
                        else
                            frame.append(
                                    gpu.getTile(sprite.tileIndex()),
                                    sprite.x(),
                                    sprite.y(),
                                    sprite.flipX(),
                                    sprite.flipY(),
                                    palette,
                                    false);
                    }
            }
        }
    }
}
