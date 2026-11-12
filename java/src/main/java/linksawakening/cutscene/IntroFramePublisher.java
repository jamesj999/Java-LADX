package linksawakening.cutscene;

import static linksawakening.cutscene.IntroPaletteMath.*;
import static linksawakening.cutscene.IntroSequence.*;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import linksawakening.scene.BackgroundScene;

/** Owns palette sources, delayed VBlank snapshots and immutable frame publication; reads only phase, video, water and actor components. */
final class IntroFramePublisher {

    private final IntroFrameSnapshot.Builder snapshotBuilder = new IntroFrameSnapshot.Builder();
    private final Function<String, BackgroundScene> backgroundProvider;
    private final int[] colorModifiers;
    private final IntroRomData.PaletteBlock dxFadeInPalette;
    private int[][] fadeBg, fadeObj;
    private int[][] visibleBg, visibleObj;
    private byte[] visibleWave;
    private int visibleScrollY;
    private int[] visibleTilemap, visibleAttrmap;
    private int[][] bgPalettes;
    private int[][] objPalettes;
    private List<IntroSprite> sprites = List.of();
    private List<IntroSprite> pendingSprites;
    private IntroFrameSnapshot snapshot;
    private IntroFrameSnapshot transitionFrame;

    private final IntroRomData rom;
    private final IntroPhaseState phase;
    private final IntroVideoMemory video;
    private final IntroWater water;
    private final IntroBeachActors actors;
    private final IntroSpriteComposer spriteComposer;

    record PaletteSources(int[] colorModifiers, IntroRomData.PaletteBlock dxFadeInPalette) {}

    IntroFramePublisher(
            IntroRomData rom,
            PaletteSources paletteSources,
            Function<String, BackgroundScene> backgroundProvider,
            IntroPhaseState phase,
            IntroVideoMemory video,
            IntroWater water,
            IntroBeachActors actors,
            IntroSpriteComposer spriteComposer) {
        this.rom = rom;
        this.colorModifiers = paletteSources.colorModifiers();
        this.dxFadeInPalette = paletteSources.dxFadeInPalette();
        this.backgroundProvider = backgroundProvider;
        this.phase = phase;
        this.video = video;
        this.water = water;
        this.actors = actors;
        this.spriteComposer = spriteComposer;
    }

    public int scrollX() {
        return phase.sceneId().equals(IntroCutsceneScript.SCENE_BEACH) ? video.beachScrollX() : video.scrollX();
    }

    void refreshSnapshot(int shipX, int shipY, int seaFadeTimer, List<IntroSequence.Lightning> lightning, int clock) {
        if (snapshot == null) sprites = renderSprites(shipX, shipY, seaFadeTimer, lightning, clock);
        boolean previousScene = phase.stageFrame() == 0
                && transitionFrame != null
                && (phase.stage() == Stage.LINK_FACE || phase.stage() == Stage.BEACH_LOAD);
        int blank = phase.stage() == Stage.SEA && phase.frameCounter() <= 3
                ? 0xFFFFFF
                : phase.stage() == Stage.LINK_FACE && phase.stageFrame() > 0 && phase.stageFrame() <= 3
                        ? 0
                        : phase.stage() == Stage.SEA_FADE && phase.stageFrame() <= 2
                                ? 0xFFFFFF
                                : phase.stage() == Stage.BEACH_LOAD && phase.stageFrame() > 0 ? 0xFFFFFF : -1;
        int[][] backgroundColors = this.visibleBg() == null ? dynamicBgPalettes() : this.visibleBg();
        int[][] objectColors = visibleObj == null ? dynamicObjPalettes() : visibleObj;
        if (blank >= 0) {
            backgroundColors = solidPalettes(blank);
            objectColors = solidPalettes(blank);
        }
        snapshot = snapshotBuilder
                .sceneId(phase.sceneId())
                .substate(substate())
                .frameCounter(phase.frameCounter())
                .scrollX(
                        previousScene && phase.stage() == Stage.LINK_FACE
                                ? (transitionFrame.scrollX() + water.sectionScrollOffsets()[0]) & 255
                                : previousScene ? transitionFrame.scrollX() : scrollX())
                .scrollY(previousScene ? transitionFrame.scrollY() : visibleScrollY)
                .lineScrollX(
                        previousScene && phase.stage() == Stage.LINK_FACE
                                ? null
                                : previousScene ? transitionFrame.lineScrollX() : lineScrollMap())
                .verticalWaveOffset(water.verticalWaveOffset())
                .tilemap(
                        previousScene
                                ? transitionFrame.tilemap()
                                : this.visibleTilemap() == null ? video.tilemap() : this.visibleTilemap())
                .attrmap(
                        previousScene
                                ? transitionFrame.attrmap()
                                : visibleAttrmap == null ? video.attrmap() : visibleAttrmap)
                .bgPalettes(backgroundColors)
                .objPalettes(objectColors)
                .sprites(sprites)
                .titleRevealRows(phase.titleRevealRows())
                .lineScrollY(
                        previousScene && phase.stage() == Stage.LINK_FACE
                                ? null
                                : previousScene ? transitionFrame.lineScrollY() : lineScrollYMap())
                .animatedTiles(
                        phase.skipped()
                                ? null
                                : previousScene
                                        ? transitionFrame.animatedTiles()
                                        : visibleWave == null ? water.waveTiles() : visibleWave)
                .animatedTilesDestination(
                        previousScene ? transitionFrame.animatedTilesDestination() : water.waveDestination())
                .build();
    }

    private String substate() {
        return switch (phase.stage()) {
            case SEA -> "SEA";
            case LINK_FACE -> phase.linkFaceTimer() >= LINK_FACE_LIGHTNING_FRAME
                    ? "LINK_FACE_LIGHTNING"
                    : phase.linkFaceTimer() >= LINK_FACE_SCREAM_FRAME ? "LINK_FACE_SCREAM" : "LINK_FACE";
            case SEA_FADE -> "SEA_FADE";
            case BEACH_LOAD -> "BEACH_LOAD";
            case BEACH_FADE -> "BEACH_FADE";
            case BEACH -> "MARIN_STATE_" + actors.marinState();
            case TITLE_REVEAL -> "TITLE_REVEAL";
            case TITLE_WAIT -> "TITLE_WAIT";
            case TITLE_DX -> "TITLE_DX";
            case COMPLETE -> "STABLE_TITLE";
        };
    }

    List<IntroSprite> renderSprites(
            int shipX, int shipY, int seaFadeTimer, List<IntroSequence.Lightning> lightning, int clock) {
        List<IntroSprite> rendered = spriteComposer.begin();
        rendered.clear();
        if (phase.stage() == Stage.SEA || phase.stage() == Stage.SEA_FADE) {
            spriteComposer.renderShip(rendered, phase.stage() == Stage.SEA_FADE, clock, shipX, shipY, seaFadeTimer);
            spriteComposer.renderLightning(rendered, lightning);
            spriteComposer.renderRain(rendered, 0x10);
        } else if (phase.stage() == Stage.LINK_FACE) {
            spriteComposer.renderRain(rendered, 0x15);
            spriteComposer.renderLightning(rendered, lightning);
        } else if (phase.stage() == Stage.BEACH) {
            spriteComposer.renderBeachEntities(
                    rendered,
                    actors.linkVisible(),
                    actors.linkX(),
                    actors.linkY(),
                    actors.linkVariant(),
                    actors.marinVariant(),
                    actors.marinX(),
                    actors.marinY());
        } else if (phase.stage() == Stage.TITLE_DX || (phase.stage() == Stage.COMPLETE && !phase.skipped())) {
            spriteComposer.renderTitleSparkle(rendered, clock, phase.titleHoldFrame());
        }
        return List.copyOf(rendered);
    }

    private int[] lineScrollMap() {
        if (video.beachScrollStoppedThisFrame()) {
            int[] lines = new int[144];
            Arrays.fill(lines, video.beachScrollX());
            // The CGB handler's direct SCX write (01:774E) reaches the LCD on
            // scanline 9; the preceding scanlines retain the first section's SCX.
            Arrays.fill(lines, 0, 9, phase.topLineScroll());
            return lines;
        }
        boolean sea = phase.stage() == Stage.SEA || phase.stage() == Stage.SEA_FADE;
        boolean beach = phase.stage() == Stage.BEACH_FADE || (phase.stage() == Stage.BEACH && actors.marinState() < 4);
        if (phase.stage() == Stage.TITLE_REVEAL || phase.stage() == Stage.TITLE_WAIT) {
            int[] lines = new int[144];
            Arrays.fill(lines, scrollX());
            return lines;
        }
        if (!sea && !beach) return null;
        int[] lines = new int[144];
        for (int y = 0; y < 144; y++) {
            int section = sea
                    ? (y < 32 ? 0 : y < 48 ? 1 : y < 64 ? 2 : y < 96 - water.verticalWaveOffset() ? 3 : 4)
                    : (y < 48 ? 0 : y < 86 ? 1 : y < 104 ? 2 : 3);
            lines[y] =
                    beach && y < 48 ? phase.topLineScroll() : (scrollX() + water.sectionScrollOffsets()[section]) & 255;
        }
        return lines;
    }

    private int[] lineScrollYMap() {
        if (phase.stage() == Stage.TITLE_REVEAL || phase.stage() == Stage.TITLE_WAIT)
            return rom.titleLineScroll(
                    phase.titleEffectFrame() == 0 ? 16 : phase.titleEffectFrame() / 2, visibleScrollY);
        if (phase.stage() != Stage.SEA && phase.stage() != Stage.SEA_FADE) return null;
        int[] lines = new int[144];
        for (int y = 64; y < 144; y++) lines[y] = water.verticalWaveOffset();
        return lines;
    }

    void setScene(String sceneId) {
        BackgroundScene scene = backgroundProvider.apply(sceneId);
        if (scene == null) {
            throw new IllegalArgumentException("No background scene for " + sceneId);
        }
        video.tilemap(scene.tilemap().clone());
        video.attrmap(scene.attrmap().clone());
        this.visibleTilemap((visibleAttrmap = null));
        bgPalettes = copyPalettes(scene.palettes());
        objPalettes = copyPalettes(scene.objectPalettes());
    }

    int[][] dynamicBgPalettes() {
        if (phase.stage() == Stage.BEACH_LOAD
                || phase.stage() == Stage.BEACH_FADE
                || (phase.stage() == Stage.SEA_FADE && this.fadeBg() != null)) return copyPalettes(this.fadeBg());
        int[][] palettes = copyPalettes(bgPalettes);
        if (paletteLightningCountdown == 0
                || (phase.stage() != Stage.SEA
                        && phase.stage() != Stage.LINK_FACE
                        && phase.stage() != Stage.SEA_FADE)) {
            return palettes;
        }
        int modifierIndex = (paletteLightningCountdown >>> 1) & 0x03;
        int modifier = colorModifiers[modifierIndex * 2] | (colorModifiers[modifierIndex * 2 + 1] << 8);
        for (int palette = 0; palette < palettes.length; palette++) {
            for (int color = 0; color < Math.min(3, palettes[palette].length); color++) {
                palettes[palette][color] = applyRgb555Modifier(palettes[palette][color], modifier);
            }
        }
        return palettes;
    }

    int[][] dynamicObjPalettes() {
        if (phase.stage() == Stage.BEACH_LOAD
                || phase.stage() == Stage.BEACH_FADE
                || (phase.stage() == Stage.SEA_FADE && fadeObj != null)) return copyPalettes(fadeObj);
        int[][] palettes = copyPalettes(objPalettes);
        if (phase.stage() != Stage.TITLE_DX && phase.stage() != Stage.COMPLETE) {
            return palettes;
        }
        int pair = Math.min(7, Math.max(0, phase.titleHoldFrame() - 60) / 8);
        int[][] fadeRows = dxFadeInPalette.rows();
        if (palettes.length > 6 && fadeRows.length >= pair * 2 + 2) {
            palettes[5] = fadeRows[pair * 2].clone();
            palettes[6] = fadeRows[pair * 2 + 1].clone();
        }
        return palettes;
    }

    int[][] fadeBg() {
        return fadeBg;
    }

    int[][] fadeBg(int[][] value) {
        fadeBg = value;
        return value;
    }

    int[][] fadeObj() {
        return fadeObj;
    }

    int[][] fadeObj(int[][] value) {
        fadeObj = value;
        return value;
    }

    int[][] visibleBg() {
        return visibleBg;
    }

    int[][] visibleBg(int[][] value) {
        visibleBg = value;
        return value;
    }

    int[][] visibleObj(int[][] value) {
        visibleObj = value;
        return value;
    }

    byte[] visibleWave(byte[] value) {
        visibleWave = value;
        return value;
    }

    int visibleScrollY(int value) {
        visibleScrollY = value;
        return value;
    }

    int[] visibleTilemap() {
        return visibleTilemap;
    }

    int[] visibleTilemap(int[] value) {
        visibleTilemap = value;
        return value;
    }

    int[] visibleAttrmap(int[] value) {
        visibleAttrmap = value;
        return value;
    }

    int[][] bgPalettes() {
        return bgPalettes;
    }

    int[][] objPalettes() {
        return objPalettes;
    }

    List<IntroSprite> sprites(List<IntroSprite> value) {
        sprites = value;
        return value;
    }

    List<IntroSprite> pendingSprites() {
        return pendingSprites;
    }

    List<IntroSprite> pendingSprites(List<IntroSprite> value) {
        pendingSprites = value;
        return value;
    }

    IntroFrameSnapshot snapshot() {
        return snapshot;
    }

    IntroFrameSnapshot transitionFrame(IntroFrameSnapshot value) {
        transitionFrame = value;
        return value;
    }

    private int paletteLightningCountdown;

    void paletteLightningCountdown(int value) {
        paletteLightningCountdown = value;
    }
}
