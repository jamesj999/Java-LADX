package linksawakening.cutscene;

import linksawakening.scene.BackgroundScene;
import linksawakening.scene.BackgroundSceneCatalog;
import linksawakening.scene.BackgroundSceneLoader;
import linksawakening.scene.BackgroundSceneSpec;
import linksawakening.rom.RomBank;
import linksawakening.world.RomRandomByteSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Domain-specific runtime for the shipped opening sequence.
 *
 * <p>This follows the intro handlers' state and timer boundaries without
 * pretending to execute the Game Boy CPU. ROM tables provide all entity
 * records and animation data; this class only models the state that affects
 * the frame renderer.</p>
 */
public final class IntroSequence {
    public static final int SEA_SCROLL_FRAMES = (0xC0 - 0x50) * 8 + 1;
    public static final int LINK_FACE_FRAMES = 0xA0;
    public static final int SEA_FADE_FRAMES = 0x58 - 1;
    public static final int BEACH_FADE_FRAMES = 0xFF;
    public static final int BEACH_MARIN_WALK_FRAMES = (0xB0 - 0x47) * 4;
    public static final int BEACH_MARIN_WAIT_FRAMES = 0x40;
    public static final int TITLE_REVEAL_STEP_FRAMES = 1;
    public static final int TITLE_HOLD_FRAMES = 160;

    private static final int INITIAL_SHIP_X = 0xC0;
    private static final int INITIAL_SHIP_Y = 0x4E;
    private static final int LINK_FACE_SCREAM_FRAME = 0x80;
    private static final int LINK_FACE_LIGHTNING_FRAME = 0x90;
    private static final int BEACH_MARIN_START_X = 0xB0;
    private static final int BEACH_LINK_START_X = 0xFE;
    private static final int BEACH_LINK_Y = 0x6E;
    private static final int[] SEA_LIGHTNING_SCROLLS = { 0x10, 0x30, 0x38, 0x58, 0x5A, 0x69 };

    private enum Stage {
        SEA,
        LINK_FACE,
        SEA_FADE,
        BEACH_LOAD,
        BEACH_FADE,
        BEACH,
        TITLE_REVEAL,
        TITLE_WAIT,
        TITLE_DX,
        COMPLETE
    }

    private record Lightning(int variant, int x, int y, int remaining) {
    }

    private final IntroRomData rom;
    private final Function<String, BackgroundScene> backgroundProvider;
    private final RomRandomByteSource random = new RomRandomByteSource(0xA2);
    private final List<IntroRomData.TitleRow> titleRows;
    private final List<IntroRomData.SpritePair> marinVariants;
    private final List<IntroRomData.SpritePair> inertLinkVariants;
    private final List<IntroRomData.SpritePair> sparkleVariants;
    private final List<List<IntroRomData.OamEntry>> lightningTiles;
    private final int[] lightningPositions;
    private final int[] lightningStatuses;
    private final int[] shipHeave;
    private final int[] verticalOffsets;
    private final int[] colorModifiers;
    private final IntroRomData.PaletteBlock dxFadeInPalette;
    private final List<IntroRomData.OamEntry> shipTiles;
    private final List<IntroRomData.OamEntry> additionalShipTiles;

    private Stage stage = Stage.SEA;
    private int frameCounter;
    private int stageFrame;
    // hFrameCounter is reset during the beach sequence and stalls during LCD-off loads.
    // frameCounter is the monotonic presentation clock used for captures.
    private int clock;
    private int loadDelay;
    private int fraction4, fraction5, fractionD;
    private int marinVariant, linkVariant;
    private int marinY = 0x68, linkY = BEACH_LINK_Y;
    private int linkState, linkTimer, panCounter, panRows;
    private byte[] waveTiles;
    private int waveDestination = 0x900;
    private boolean skipped;
    private int fadeSteps;
    private int[][] fadeBg, fadeObj;
    private int[][] visibleBg, visibleObj;
    private byte[] visibleWave;
    private int scrollX;
    private int scrollY;
    private int verticalWaveOffset;
    private int shipX = INITIAL_SHIP_X;
    private final int shipY = INITIAL_SHIP_Y;
    private int seaFadeTimer = 1;
    private int beachFadeTimer = BEACH_FADE_FRAMES;
    private int linkFaceTimer;
    private int marinState;
    private int marinX = BEACH_MARIN_START_X;
    private int marinInertia = 1;
    private int marinTimer;
    private int linkX = BEACH_LINK_START_X;
    private boolean linkVisible;
    private int beachScrollX;
    private int titleRowIndex;
    private int titleRevealRows;
    private int titleHoldFrame;
    private int titleEffectFrame;
    private int visibleScrollY;
    private int topLineScroll;
    private boolean drawCommandQueued;
    private boolean beachScrollStoppedThisFrame;
    private int paletteLightningCountdown;
    private int[] visibleTilemap, visibleAttrmap;
    private final int[] sectionScrollOffsets = new int[5];
    private final List<Lightning> lightning = new ArrayList<>();
    private int lightningVisibleCountdown;
    private int[] tilemap;
    private int[] attrmap;
    private int[][] bgPalettes;
    private int[][] objPalettes;
    private List<IntroSprite> sprites = List.of();
    private List<IntroSprite> pendingSprites;
    private IntroFrameSnapshot snapshot;
    private IntroFrameSnapshot transitionFrame;

    /** Compatibility constructor for older callers; runtime data still comes from the bundled ROM. */
    public IntroSequence() {
        this(defaultResources());
    }

    public IntroSequence(byte[] romData, Function<String, BackgroundScene> backgroundProvider) {
        this.rom = new IntroRomData(romData);
        this.backgroundProvider = Objects.requireNonNull(backgroundProvider, "backgroundProvider");
        this.titleRows = rom.titleRows();
        this.marinVariants = rom.marinVariants();
        this.inertLinkVariants = rom.inertLinkVariants();
        this.sparkleVariants = rom.sparkleVariants();
        this.lightningTiles = rom.lightningTiles();
        this.lightningPositions = rom.lightningEntityPositions();
        this.lightningStatuses = rom.lightningEntityStatuses();
        this.shipHeave = rom.shipHeaveTable();
        this.verticalOffsets = rom.introVerticalOffsets();
        this.colorModifiers = rom.introColorModifiers();
        this.dxFadeInPalette = rom.dxFadeInPalette();
        this.shipTiles = rom.shipTiles();
        this.additionalShipTiles = rom.additionalShipTiles();
        setScene(IntroCutsceneScript.SCENE_SEA);
        waveTiles = rom.tileBytes(0x4100, 128);
        random.beginFrame(0, 0);
        refreshSnapshot();
    }

    private IntroSequence(DefaultResources resources) {
        this(resources.romData(), resources.backgroundProvider());
    }

    public void tick() {
        if (stage == Stage.COMPLETE && skipped) {
            return;
        }

        frameCounter++;
        stageFrame++;
        if (loadDelay > 0) {
            loadDelay--;
            refreshSnapshot();
            return;
        }
        drawCommandQueued = false;
        beachScrollStoppedThisFrame = false;
        topLineScroll = (scrollX() + sectionScrollOffsets[0]) & 255;
        // Draw commands, palettes and tile uploads become visible at the next VBlank.
        // Object positions additionally precede the entity handler's movement update.
        visibleTilemap = tilemap.clone();
        visibleAttrmap = attrmap.clone();
        visibleScrollY = scrollY;
        if (stage == Stage.TITLE_REVEAL || stage == Stage.TITLE_WAIT) titleEffectFrame++;
        visibleBg = dynamicBgPalettes();
        visibleObj = dynamicObjPalettes();
        visibleWave = waveTiles.clone();
        sprites = pendingSprites == null || stage == Stage.TITLE_DX || stage == Stage.COMPLETE
            ? renderSprites() : pendingSprites;
        pendingSprites = renderSprites();
        clock++;
        random.beginFrame(clock & 0xFF, 0);
        switch (stage) {
            case SEA -> tickSea();
            case LINK_FACE -> tickLinkFace();
            case SEA_FADE -> tickSeaFade();
            case BEACH_LOAD -> {
                if (stageFrame == 4) {
                    transitionTo(Stage.BEACH_FADE);
                    loadDelay = 2;
                }
            }
            case BEACH_FADE -> tickBeachFade();
            case BEACH -> tickBeach();
            case TITLE_REVEAL -> tickTitleReveal();
            case TITLE_WAIT -> {
                if (stageFrame >= 26) { transitionTo(Stage.TITLE_DX); clock = 1; }
            }
            case TITLE_DX -> tickTitleDx();
            case COMPLETE -> tickTitleDx();
        }
        refreshSnapshot();
    }

    public boolean isActive() {
        return stage != Stage.COMPLETE;
    }

    public void skipToTitle() {
        stage = Stage.COMPLETE;
        skipped = true;
        visibleScrollY = 0;
        visibleBg = visibleObj = null;
        verticalWaveOffset = 0;
        stageFrame = 0;
        scrollX = 0;
        scrollY = 0;
        beachScrollX = 0;
        lightning.clear();
        linkVisible = false;
        titleRowIndex = titleRows.size();
        titleRevealRows = titleRows.size();
        setScene(IntroCutsceneScript.SCENE_TITLE);
        // The direct title map already contains the logo; post-beach rows belong to the scrolled map.
        sprites = List.of();
        refreshSnapshot();
    }

    public String sceneId() {
        return switch (stage) {
            case SEA, SEA_FADE -> IntroCutsceneScript.SCENE_SEA;
            case LINK_FACE -> IntroCutsceneScript.SCENE_LINK_FACE;
            case BEACH_LOAD -> stageFrame == 0 ? IntroCutsceneScript.SCENE_SEA : IntroCutsceneScript.SCENE_BEACH;
            case BEACH_FADE, BEACH -> IntroCutsceneScript.SCENE_BEACH;
            case TITLE_REVEAL, TITLE_WAIT, TITLE_DX, COMPLETE -> IntroCutsceneScript.SCENE_TITLE;
        };
    }

    public int scrollX() {
        return sceneId().equals(IntroCutsceneScript.SCENE_BEACH) ? beachScrollX : scrollX;
    }

    public int scrollY() {
        return scrollY;
    }

    public int scrollXForLine(int screenY) {
        int[] lineScroll = snapshot == null ? null : snapshot.lineScrollX();
        if (lineScroll == null || lineScroll.length == 0) {
            return scrollX();
        }
        return lineScroll[Math.max(0, Math.min(screenY, lineScroll.length - 1))];
    }

    public int[] lineScrollX(int height) {
        int[] lineScroll = snapshot == null ? null : snapshot.lineScrollX();
        if (lineScroll == null) {
            return null;
        }
        int[] resized = new int[height];
        for (int index = 0; index < height; index++) {
            resized[index] = lineScroll[Math.min(index, lineScroll.length - 1)];
        }
        return resized;
    }

    public int titleRevealRows() {
        return titleRevealRows;
    }

    public List<IntroSprite> sprites() {
        return snapshot.sprites();
    }

    public IntroFrameSnapshot snapshot() {
        return snapshot;
    }

    int shipX() {
        return shipX;
    }

    private void tickSea() {
        decrementLightning();
        if (shipX == 0x50) {
            lightning.clear();
            transitionTo(Stage.LINK_FACE);
            loadDelay = 3;
            return;
        }
        if (frameCounter <= 2) return; // initial map transfer, before the first sea handler
        updateWaveOffsets();
        animateWaves(false);
        if ((clock & 7) == 0) {
            scrollX = (scrollX + 1) & 255;
            shipX--;
            for (int i = 0; i < lightning.size(); i++) {
                Lightning l = lightning.get(i);
                lightning.set(i, new Lightning(l.variant(), l.x() - 1, l.y(), l.remaining()));
            }
            triggerSeaLightningIfNeeded();
        }
    }

    private void tickLinkFace() {
        decrementLightning();
        linkFaceTimer++;
        if (linkFaceTimer == LINK_FACE_SCREAM_FRAME) rom.applyDrawCommands(tilemap, 0x74B8);
        if (linkFaceTimer == LINK_FACE_LIGHTNING_FRAME || (clock & 0x7f) == 0)
            lightningVisibleCountdown = 0x1C; // sound/palette flash only, no bolt entity
        if (linkFaceTimer >= LINK_FACE_FRAMES) {
            lightning.clear();
            seaFadeTimer = 1;
            transitionTo(Stage.SEA_FADE);
            loadDelay = 1;
        }
    }

    private void tickSeaFade() {
        decrementLightning();
        seaFadeTimer++;
        if (seaFadeTimer >= 0x30) {
            fadeSteps++;
            fadeBg = brighten(bgPalettes, fadeSteps, 32);
            fadeObj = brighten(objPalettes, Math.max(0, seaFadeTimer - 0x47), 32);
            int[][] firstTwo = brighten(objPalettes, fadeSteps, 8);
            fadeObj[0] = firstTwo[0];
            fadeObj[1] = firstTwo[1];
        }
        if (seaFadeTimer >= 0x58) {
            beachFadeTimer = BEACH_FADE_FRAMES;
            transitionTo(Stage.BEACH_LOAD);
            loadDelay = 3;
        }
    }

    private void tickBeachFade() {
        beachFadeTimer--;
        // func_020_6AC1 restores all palettes one RGB555 step at a time.
        if (beachFadeTimer < 0x34) {
            if ((beachFadeTimer & 3) == 0) fadeSteps = Math.min(12, fadeSteps + 1);
            if (fadeSteps + (clock & 3) >= 6) {
                fadeBg = approachPalettes(fadeBg, bgPalettes);
                fadeObj = approachPalettes(fadeObj, objPalettes);
            }
        }
        if (beachFadeTimer == 0) {
            marinState = 0;
            marinX = BEACH_MARIN_START_X;
            marinInertia = 1;
            marinTimer = 0;
            linkVisible = false;
            transitionTo(Stage.BEACH);
        }
    }

    private void tickBeach() {
        // Entity slot 1 (Link) executes before slot 0 (Marin).
        if (linkVisible) tickInertLink();
        if (stage != Stage.BEACH) return;
        // Link's handler moves both entities vertically before Marin builds her OAM.
        List<IntroSprite> corrected = new ArrayList<>(pendingSprites);
        if (corrected.size() >= 2) {
            corrected.subList(corrected.size() - 2, corrected.size()).clear();
            addPair(corrected, marinVariants.get(marinVariant), marinX, marinY);
            pendingSprites = List.copyOf(corrected);
        }
        switch (marinState) {
            case 0 -> {
                updateBeachOffsets(false);
                marinVariant = (clock >>> 3) & 1;
                if (marinX < 0x48) { marinTimer = 0x40; marinState = 1; }
                if (--marinInertia == 0) { marinInertia = 4; marinX--; }
            }
            case 1 -> {
                animateWaves(true);
                marinVariant = 1;
                if (marinTimer > 0) marinTimer--;
                else {
                    marinState = 2; linkVisible = true; linkX = BEACH_LINK_START_X;
                    linkState = 0; linkTimer = 0; clock = 0;
                }
            }
            case 2 -> {
                updateBeachOffsets(false);
                linkX = (linkX - 1) & 255;
                if ((clock & 1) == 0) {
                    beachScrollX++;
                    patchMountains();
                    if (beachScrollX == 0x30) { marinTimer = 0x40; marinState = 3; }
                }
                if (marinState == 2) marinVariant = (clock >>> 2) & 1;
            }
            case 3 -> {
                if (marinTimer > 0) { marinTimer--; marinVariant = 1; animateWaves(true); }
                else {
                    updateBeachOffsets(true);
                    marinVariant = (clock >>> 4) & 1;
                    if ((clock & 1) == 0) linkX = (linkX - 1) & 255;
                    if ((clock & 3) == 0) {
                        beachScrollX++;
                        patchMountains();
                        if (beachScrollX == 0x3A) marinTimer = 0x30;
                        if (beachScrollX == 0x40) marinTimer = 0x50;
                        if (beachScrollX == 0x56) {
                            beachScrollX = 0xA0; marinTimer = 0xE0; marinState = 4;
                            beachScrollStoppedThisFrame = true;
                        }
                    }
                }
            }
            case 4 -> {
                animateWaves(true);
                if ((clock & 1) == 0) {
                    int timer = marinTimer;
                    if (marinTimer > 0) marinTimer--;
                    boolean leaning = (timer < 0xA0 && timer >= 0x90)
                        || (timer < 0x50 && timer >= 0x4A) || (timer < 0x3C && timer >= 0x36);
                    marinVariant = leaning ? 3 : 2;
                    linkVariant = leaning ? 1 : 0;
                }
            }
            default -> throw new IllegalStateException();
        }
    }

    private void tickInertLink() {
        switch (linkState) {
            case 0 -> { linkTimer = (linkTimer - 1) & 255; if (linkTimer == 0) { linkTimer = 0x90; linkState = 1; } }
            case 1 -> { if ((clock & 3) == 0 && --linkTimer == 0) linkState = 2; }
            case 2 -> {
                if (panRows == 19) { linkState = 3; linkTimer = 0x17; return; }
                if ((++panCounter & 3) != 0) return;
                marinY = Math.min(0xA0, marinY + 1);
                linkY = Math.min(0xA0, linkY + 1);
                int oldY = scrollY;
                scrollY = (scrollY - 1) & 255;
                if ((oldY & 7) == 0) {
                    int target = 0x3F4 - panRows * 32;
                    int[] rows = rom.postBeachTilemap();
                    for (int i = 0; i < 20; i++) tilemap[(target & 0x3E0) | ((target + i) & 31)] = rows[panRows * 20 + i];
                    for (int i = 0; i < 32; i++) attrmap[(target - 20 + i) & 1023] = 3;
                    panRows++;
                    drawCommandQueued = true;
                }
            }
            case 3 -> { if ((clock & 3) == 0 && --linkTimer == 0) transitionTo(Stage.TITLE_REVEAL); }
            default -> throw new IllegalStateException();
        }
    }

    private void patchMountains() {
        if (beachScrollX == 0x20) rom.applyDrawCommands(tilemap, 0x79EC);
        if (beachScrollX == 0x22) rom.applyDrawCommands(tilemap, 0x79FD);
    }

    private void updateBeachOffsets(boolean slow) {
        if ((clock & (slow ? 15 : 7)) == 0) sectionScrollOffsets[0]++;
        fraction4 += slow ? 0x28 : 0x50;
        fraction5 += slow ? 0x2C : 0x58;
        fractionD += slow ? 0x58 : 0xB0;
        sectionScrollOffsets[1] += fraction4 >>> 8; fraction4 &= 255;
        sectionScrollOffsets[2] += fraction5 >>> 8; fraction5 &= 255;
        sectionScrollOffsets[3] += fractionD >>> 8; fractionD &= 255;
        animateWaves(true);
    }

    private void animateWaves(boolean beach) {
        if ((clock & 15) < 4 || drawCommandQueued) return;
        int chunk = (clock & 3) * 32;
        byte[] bytes = rom.tileBytes(rom.animationFrameAddress(beach, clock) + chunk, 32);
        System.arraycopy(bytes, 0, waveTiles, chunk, 32);
    }

    private void tickTitleReveal() {
        if (stageFrame % TITLE_REVEAL_STEP_FRAMES != 0) {
            return;
        }
        if (titleRowIndex < titleRows.size()) {
            applyTitleRow(titleRows.get(titleRowIndex));
            titleRowIndex++;
            titleRevealRows = titleRowIndex;
            if (titleRowIndex == titleRows.size()) transitionTo(Stage.TITLE_WAIT);
            return;
        }
        transitionTo(Stage.TITLE_WAIT);
    }

    private void tickTitleDx() {
        titleHoldFrame++;
        if (titleHoldFrame >= TITLE_HOLD_FRAMES) {
            stage = Stage.COMPLETE;
        }
    }

    private void transitionTo(Stage nextStage) {
        transitionFrame = snapshot;
        stage = nextStage;
        stageFrame = 0;
        switch (nextStage) {
            case LINK_FACE -> {
                linkFaceTimer = 0;
                scrollX = 0;
                setScene(IntroCutsceneScript.SCENE_LINK_FACE);
                verticalWaveOffset = 0;
            }
            case SEA_FADE -> {
                scrollX = 0;
                setScene(IntroCutsceneScript.SCENE_SEA);
                verticalWaveOffset = -verticalOffsets[((0x80 + 0xFC) >>> 4) & 7];
            }
            case BEACH_LOAD -> {
                scrollX = 0;
                setScene(IntroCutsceneScript.SCENE_BEACH);
                sectionScrollOffsets[0] = sectionScrollOffsets[2] = sectionScrollOffsets[3] = 0;
                sectionScrollOffsets[1] = 0x92;
                waveDestination = 0x1300;
                waveTiles = rom.tileBytes(0x4B00, 128);
                System.arraycopy(rom.tileBytes(0x5200, 32), 0, waveTiles, 0, 32);
                visibleWave = waveTiles.clone();
                fadeBg = brighten(bgPalettes, 31, 32);
                fadeObj = brighten(objPalettes, 31, 32);
                fadeSteps = 0;
                verticalWaveOffset = 0;
            }
            case BEACH_FADE, BEACH -> { }
            case TITLE_REVEAL -> {
                titleRowIndex = 0;
                titleRevealRows = 0;
                titleHoldFrame = 0;
                // Keep the streamed beach map and its 160,111 scroll origin.
                scrollX = beachScrollX;
                verticalWaveOffset = 0;
            }
            case TITLE_DX -> {
                rom.applyDrawCommands(tilemap, 0x7364);
                rom.applyDrawCommands(attrmap, 0x73AC);
            }
            case TITLE_WAIT, COMPLETE, SEA -> { }
        }
    }

    private void decrementLightning() {
        lightningVisibleCountdown = Math.max(0, lightningVisibleCountdown - 1);
        paletteLightningCountdown = lightningVisibleCountdown;
        for (int index = lightning.size() - 1; index >= 0; index--) {
            Lightning current = lightning.get(index);
            if (current.remaining() <= 1) {
                lightning.remove(index);
            } else {
                lightning.set(index,
                    new Lightning(current.variant(), current.x(), current.y(), current.remaining() - 1));
            }
        }
    }

    private void triggerSeaLightningIfNeeded() {
        for (int index = 0; index < SEA_LIGHTNING_SCROLLS.length; index++) {
            if (scrollX == SEA_LIGHTNING_SCROLLS[index]) {
                triggerLightning(index);
                return;
            }
        }
    }

    private void triggerLightning(int index) {
        if (index < 0 || index >= lightningPositions.length) {
            return;
        }
        lightning.add(new Lightning(
            Math.max(0, lightningStatuses[index] - 1), lightningPositions[index], 0x30, 0x20));
        lightningVisibleCountdown = 0x1C;
    }

    private void updateWaveOffsets() {
        if ((clock & 0x07) == 0) {
            sectionScrollOffsets[0]++;
        }
        if ((clock & 0x0F) == 0) {
            sectionScrollOffsets[1]++;
            sectionScrollOffsets[3]++;
        }
        if ((clock & 0x1F) == 0) {
            sectionScrollOffsets[2]++;
        }
        fraction4 += 0x28;
        sectionScrollOffsets[4] += fraction4 >>> 8;
        fraction4 &= 255;
        int waveIndex = ((clock + 0xFC) >>> 4) & 0x07;
        verticalWaveOffset = -verticalOffsets[waveIndex];
    }

    private void refreshSnapshot() {
        if (snapshot == null) sprites = renderSprites();
        boolean previousScene = stageFrame == 0 && transitionFrame != null
            && (stage == Stage.LINK_FACE || stage == Stage.BEACH_LOAD);
        int blank = stage == Stage.SEA && frameCounter <= 3 ? 0xFFFFFF
            : stage == Stage.LINK_FACE && stageFrame > 0 && stageFrame <= 3 ? 0
            : stage == Stage.SEA_FADE && stageFrame <= 2 ? 0xFFFFFF
            : stage == Stage.BEACH_LOAD && stageFrame > 0 ? 0xFFFFFF : -1;
        int[][] backgroundColors = visibleBg == null ? dynamicBgPalettes() : visibleBg;
        int[][] objectColors = visibleObj == null ? dynamicObjPalettes() : visibleObj;
        if (blank >= 0) {
            backgroundColors = solidPalettes(blank);
            objectColors = solidPalettes(blank);
        }
        snapshot = new IntroFrameSnapshot(
            sceneId(), substate(), frameCounter,
            previousScene && stage == Stage.LINK_FACE
                ? (transitionFrame.scrollX() + sectionScrollOffsets[0]) & 255
                : previousScene ? transitionFrame.scrollX() : scrollX(),
            previousScene ? transitionFrame.scrollY() : visibleScrollY,
            previousScene && stage == Stage.LINK_FACE ? null
                : previousScene ? transitionFrame.lineScrollX() : lineScrollMap(),
            verticalWaveOffset,
            previousScene ? transitionFrame.tilemap() : visibleTilemap == null ? tilemap : visibleTilemap,
            previousScene ? transitionFrame.attrmap() : visibleAttrmap == null ? attrmap : visibleAttrmap,
            backgroundColors, objectColors, sprites, titleRevealRows,
            previousScene && stage == Stage.LINK_FACE ? null
                : previousScene ? transitionFrame.lineScrollY() : lineScrollYMap(),
            skipped ? null : previousScene ? transitionFrame.animatedTiles()
                : visibleWave == null ? waveTiles : visibleWave,
            previousScene ? transitionFrame.animatedTilesDestination() : waveDestination);
    }

    private static int[][] solidPalettes(int color) {
        int[][] palettes = new int[8][4];
        for (int[] palette : palettes) java.util.Arrays.fill(palette, color);
        return palettes;
    }

    private String substate() {
        return switch (stage) {
            case SEA -> "SEA";
            case LINK_FACE -> linkFaceTimer >= LINK_FACE_LIGHTNING_FRAME
                ? "LINK_FACE_LIGHTNING"
                : linkFaceTimer >= LINK_FACE_SCREAM_FRAME ? "LINK_FACE_SCREAM" : "LINK_FACE";
            case SEA_FADE -> "SEA_FADE";
            case BEACH_LOAD -> "BEACH_LOAD";
            case BEACH_FADE -> "BEACH_FADE";
            case BEACH -> "MARIN_STATE_" + marinState;
            case TITLE_REVEAL -> "TITLE_REVEAL";
            case TITLE_WAIT -> "TITLE_WAIT";
            case TITLE_DX -> "TITLE_DX";
            case COMPLETE -> "STABLE_TITLE";
        };
    }

    private List<IntroSprite> renderSprites() {
        List<IntroSprite> rendered = new ArrayList<>();
        if (stage == Stage.SEA || stage == Stage.SEA_FADE) {
            renderShip(rendered);
            renderLightning(rendered);
            renderRain(rendered, 0x10);
        } else if (stage == Stage.LINK_FACE) {
            renderRain(rendered, 0x15);
            renderLightning(rendered);
        } else if (stage == Stage.BEACH) {
            renderBeachEntities(rendered);
        } else if (stage == Stage.TITLE_DX || (stage == Stage.COMPLETE && !skipped)) {
            renderTitleSparkle(rendered);
        }
        return List.copyOf(rendered);
    }

    private void renderRain(List<IntroSprite> rendered, int rows) {
        int y = (random.getAsInt() & 0x18) + 0x10;
        int x = (random.getAsInt() & 0x18) + 0x10;
        for (int row = 0; row < rows; row++) {
            int tile = 0x28;
            if ((random.getAsInt() & 0x01) != 0) {
                tile = (random.getAsInt() & 0x06) + 0x70;
            }
            rendered.add(new IntroSprite(tile, x - 8, y - 16, 0, false, false, 16));
            x += 0x1C;
            if (x >= 0xA0) {
                x -= 0x98;
                y += 0x25;
            }
        }
    }

    private void renderShip(List<IntroSprite> rendered) {
        int heaveIndex = stage == Stage.SEA_FADE ? 0 : (clock + 1 + 0xD0) >>> 4 & 0x07;
        int heave = shipHeave[heaveIndex];
        for (IntroRomData.OamEntry entry : shipTiles) {
            rendered.add(toSprite(entry, shipX, shipY + heave));
        }
        if (stage == Stage.SEA_FADE && seaFadeTimer >= 0x10) {
            for (IntroRomData.OamEntry entry : additionalShipTiles) {
                rendered.add(toSprite(entry, shipX, shipY + heave));
            }
        }
    }

    private void renderLightning(List<IntroSprite> rendered) {
        for (Lightning current : lightning) {
            if (current.variant() >= lightningTiles.size()) {
                continue;
            }
            for (IntroRomData.OamEntry entry : lightningTiles.get(current.variant())) {
                rendered.add(toSprite(entry, current.x(), current.y()));
            }
        }
    }

    private void renderBeachEntities(List<IntroSprite> rendered) {
        if (linkVisible && linkX < 0xF0)
            addPair(rendered, inertLinkVariants.get(linkVariant), linkX, linkY);
        addPair(rendered, marinVariants.get(marinVariant), marinX, marinY);
    }

    private void renderTitleSparkle(List<IntroSprite> rendered) {
        // TitleScreenHandler allocates a sparkle every 64 frames. Its countdown
        // runs backwards through the ROM variants, not forwards from frame zero.
        int age = clock & 63;
        if (clock >= 64 && age > 0 && age < 63) {
            int position = ((clock >>> 6) - 1) & 7;
            addPair(rendered, sparkleVariants.get((63 - age) >>> 3),
                rom.titleSparkleXPositions()[position], rom.titleSparkleYPositions()[position]);
        }
        if (titleHoldFrame >= 60) {
            for (IntroRomData.OamEntry entry : rom.dxTiles(titleHoldFrame >= 124))
                rendered.add(toSprite(entry, 0x78, 0x59));
        }
    }

    private void addPair(List<IntroSprite> rendered, IntroRomData.SpritePair pair,
                         int oamX, int oamY) {
        rendered.add(new IntroSprite(pair.firstTileIndex(), oamX - 8, oamY - 16,
            pair.firstAttributes() & 0x07, (pair.firstAttributes() & 0x20) != 0,
            (pair.firstAttributes() & 0x40) != 0, 16));
        rendered.add(new IntroSprite(pair.secondTileIndex(), oamX, oamY - 16,
            pair.secondAttributes() & 0x07, (pair.secondAttributes() & 0x20) != 0,
            (pair.secondAttributes() & 0x40) != 0, 16));
    }

    private IntroSprite toSprite(IntroRomData.OamEntry entry, int oamX, int oamY) {
        return new IntroSprite(entry.tileIndex(), oamX + entry.xOffset() - 8,
            oamY + entry.yOffset() - 16, entry.attributes() & 0x07,
            (entry.attributes() & 0x20) != 0, (entry.attributes() & 0x40) != 0, 16);
    }

    private int[] lineScrollMap() {
        if (beachScrollStoppedThisFrame) {
            int[] lines = new int[144];
            java.util.Arrays.fill(lines, beachScrollX);
            // The CGB handler's direct SCX write (01:774E) reaches the LCD on
            // scanline 9; the preceding scanlines retain the first section's SCX.
            java.util.Arrays.fill(lines, 0, 9, topLineScroll);
            return lines;
        }
        boolean sea = stage == Stage.SEA || stage == Stage.SEA_FADE;
        boolean beach = stage == Stage.BEACH_FADE || (stage == Stage.BEACH && marinState < 4);
        if (stage == Stage.TITLE_REVEAL || stage == Stage.TITLE_WAIT) {
            int[] lines = new int[144];
            java.util.Arrays.fill(lines, scrollX());
            return lines;
        }
        if (!sea && !beach) return null;
        int[] lines = new int[144];
        for (int y = 0; y < 144; y++) {
            int section = sea ? (y < 32 ? 0 : y < 48 ? 1 : y < 64 ? 2
                : y < 96 - verticalWaveOffset ? 3 : 4)
                : (y < 48 ? 0 : y < 86 ? 1 : y < 104 ? 2 : 3);
            lines[y] = beach && y < 48 ? topLineScroll
                : (scrollX() + sectionScrollOffsets[section]) & 255;
        }
        return lines;
    }

    private int[] lineScrollYMap() {
        if (stage == Stage.TITLE_REVEAL || stage == Stage.TITLE_WAIT)
            return rom.titleLineScroll(titleEffectFrame == 0 ? 16 : titleEffectFrame / 2, visibleScrollY);
        if (stage != Stage.SEA && stage != Stage.SEA_FADE) return null;
        int[] lines = new int[144];
        for (int y = 64; y < 144; y++) lines[y] = verticalWaveOffset;
        return lines;
    }

    private static int[][] brighten(int[][] source, int amount, int count) {
        int[][] result = copyPalettes(source);
        for (int i = 0; i < Math.min(count, result.length * 4); i++) {
            int color = result[i / 4][i % 4];
            int r = Math.min(31, (color >>> 19) + amount);
            int g = Math.min(31, ((color >>> 11) & 31) + amount);
            int b = Math.min(31, ((color >>> 3) & 31) + amount);
            result[i / 4][i % 4] = RomBank.decodeRgb555(r | g << 5 | b << 10);
        }
        return result;
    }

    private static int[][] approachPalettes(int[][] source, int[][] target) {
        int[][] result = copyPalettes(source);
        for (int p = 0; p < result.length; p++) for (int c = 0; c < 4; c++) {
            int v = 0;
            for (int channel = 0; channel < 3; channel++) {
                int shift = channel * 8 + 3;
                int a = (source[p][c] >>> shift) & 31;
                int b = (target[p][c] >>> shift) & 31;
                v |= Math.max(b, a - 1) << (channel == 0 ? 10 : channel == 1 ? 5 : 0);
            }
            result[p][c] = RomBank.decodeRgb555(v);
        }
        return result;
    }

    private void setScene(String sceneId) {
        BackgroundScene scene = backgroundProvider.apply(sceneId);
        if (scene == null) {
            throw new IllegalArgumentException("No background scene for " + sceneId);
        }
        tilemap = scene.tilemap().clone();
        attrmap = scene.attrmap().clone();
        visibleTilemap = visibleAttrmap = null;
        bgPalettes = copyPalettes(scene.palettes());
        objPalettes = copyPalettes(scene.objectPalettes());
    }

    private int[][] dynamicBgPalettes() {
        if (stage == Stage.BEACH_LOAD || stage == Stage.BEACH_FADE
                || (stage == Stage.SEA_FADE && fadeBg != null)) return copyPalettes(fadeBg);
        int[][] palettes = copyPalettes(bgPalettes);
        if (paletteLightningCountdown == 0
            || (stage != Stage.SEA && stage != Stage.LINK_FACE && stage != Stage.SEA_FADE)) {
            return palettes;
        }
        int modifierIndex = (paletteLightningCountdown >>> 1) & 0x03;
        int modifier = colorModifiers[modifierIndex * 2]
            | (colorModifiers[modifierIndex * 2 + 1] << 8);
        for (int palette = 0; palette < palettes.length; palette++) {
            for (int color = 0; color < Math.min(3, palettes[palette].length); color++) {
                palettes[palette][color] = applyRgb555Modifier(palettes[palette][color], modifier);
            }
        }
        return palettes;
    }

    private static int applyRgb555Modifier(int rgb888, int modifier) {
        int red = (rgb888 >>> 19) & 31;
        int green = (rgb888 >>> 11) & 31;
        int blue = (rgb888 >>> 3) & 31;
        int color = red | (green << 5) | (blue << 10);
        return RomBank.decodeRgb555(color | modifier);
    }

    private int[][] dynamicObjPalettes() {
        if (stage == Stage.BEACH_LOAD || stage == Stage.BEACH_FADE
                || (stage == Stage.SEA_FADE && fadeObj != null)) return copyPalettes(fadeObj);
        int[][] palettes = copyPalettes(objPalettes);
        if (stage != Stage.TITLE_DX && stage != Stage.COMPLETE) {
            return palettes;
        }
        int pair = Math.min(7, Math.max(0, titleHoldFrame - 60) / 8);
        int[][] fadeRows = dxFadeInPalette.rows();
        if (palettes.length > 6 && fadeRows.length >= pair * 2 + 2) {
            palettes[5] = fadeRows[pair * 2].clone();
            palettes[6] = fadeRows[pair * 2 + 1].clone();
        }
        return palettes;
    }

    private void applyTitleRow(IntroRomData.TitleRow row) {
        copyDrawRow(tilemap, row.tileTargetAddress(), row.tileBytes());
        copyDrawRow(attrmap, row.attributeTargetAddress(), row.attributeBytes());
    }

    private static void copyDrawRow(int[] map, int targetAddress, int[] values) {
        int offset = targetAddress - 0x9800;
        if (offset < 0 || offset >= map.length || values.length > 32) {
            throw new IllegalArgumentException(String.format(
                "Intro draw command target $%04X is outside the 32x32 map", targetAddress));
        }
        for (int i = 0; i < values.length; i++) {
            map[(offset & 0x3E0) | ((offset + i) & 31)] = values[i];
        }
    }

    private static int[][] copyPalettes(int[][] source) {
        int[][] copy = new int[source.length][];
        for (int row = 0; row < source.length; row++) {
            copy[row] = source[row].clone();
        }
        return copy;
    }

    private static DefaultResources defaultResources() {
        byte[] romData = loadBundledRom();
        BackgroundSceneLoader loader = new BackgroundSceneLoader(romData);
        return new DefaultResources(romData, sceneId -> {
            BackgroundSceneSpec spec = BackgroundSceneCatalog.forCutsceneScene(sceneId);
            if (spec == null) {
                throw new IllegalArgumentException("No cutscene background spec for " + sceneId);
            }
            return loader.load(spec);
        });
    }

    private static byte[] loadBundledRom() {
        try (InputStream stream = IntroSequence.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return stream.readAllBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load ROM", exception);
        }
    }

    private record DefaultResources(
        byte[] romData,
        Function<String, BackgroundScene> backgroundProvider
    ) {
    }
}
