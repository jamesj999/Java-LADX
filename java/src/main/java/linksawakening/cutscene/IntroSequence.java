package linksawakening.cutscene;

import linksawakening.scene.BackgroundScene;
import linksawakening.scene.BackgroundSceneCatalog;
import linksawakening.scene.BackgroundSceneLoader;
import linksawakening.scene.BackgroundSceneSpec;
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
    public static final int SEA_SCROLL_FRAMES = (0xC0 - 0x50) * 8;
    public static final int LINK_FACE_FRAMES = 0xA0;
    public static final int SEA_FADE_FRAMES = 0x58 - 1;
    public static final int BEACH_FADE_FRAMES = 0xA0;
    public static final int BEACH_MARIN_WALK_FRAMES = (0xB0 - 0x47) * 4;
    public static final int BEACH_MARIN_WAIT_FRAMES = 0x40;
    public static final int TITLE_REVEAL_STEP_FRAMES = 1;
    public static final int TITLE_HOLD_FRAMES = 160;

    private static final int INITIAL_SHIP_X = 0xC0;
    private static final int INITIAL_SHIP_Y = 0x4E;
    private static final int LINK_FACE_SCREAM_FRAME = 0x80;
    private static final int LINK_FACE_LIGHTNING_FRAME = 0x90;
    private static final int BEACH_MARIN_START_X = 0xB0;
    private static final int BEACH_MARIN_STOP_X = 0x47;
    private static final int BEACH_LINK_START_X = 0xFE;
    private static final int BEACH_LINK_Y = 0x6E;
    private static final int[] SEA_LIGHTNING_SCROLLS = { 0x10, 0x30, 0x38, 0x58, 0x5A, 0x69 };
    private static final int BEACH_SCREEN_HEIGHT = 144;
    private static final int[] BEACH_SECTION_ENDS = { 0x30, 0x56, 0x68 };

    private enum Stage {
        SEA,
        LINK_FACE,
        SEA_FADE,
        BEACH_FADE,
        BEACH,
        TITLE_REVEAL,
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
    private final List<IntroRomData.OamEntry> shipTiles;
    private final List<IntroRomData.OamEntry> additionalShipTiles;

    private Stage stage = Stage.SEA;
    private int frameCounter;
    private int stageFrame;
    private int scrollX;
    private int scrollY;
    private int verticalWaveOffset;
    private int shipX = INITIAL_SHIP_X;
    private int shipY = INITIAL_SHIP_Y;
    private int seaFadeTimer = 1;
    private int beachFadeTimer = BEACH_FADE_FRAMES;
    private int linkFaceTimer;
    private int marinState;
    private int marinX = BEACH_MARIN_START_X;
    private int marinInertia = 1;
    private int marinTimer;
    private int linkX = BEACH_LINK_START_X;
    private boolean linkVisible;
    private int beachFrameCounter;
    private int beachScrollX;
    private int titleRowIndex;
    private int titleRevealRows;
    private int titleHoldFrame;
    private int[] sectionScrollOffsets = new int[4];
    private final List<Lightning> lightning = new ArrayList<>();
    private int lightningVisibleCountdown;
    private int[] tilemap;
    private int[] attrmap;
    private int[][] bgPalettes;
    private int[][] objPalettes;
    private List<IntroSprite> sprites = List.of();
    private IntroFrameSnapshot snapshot;

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
        this.shipTiles = rom.shipTiles();
        this.additionalShipTiles = rom.additionalShipTiles();
        setScene(IntroCutsceneScript.SCENE_SEA);
        random.beginFrame(0, 0);
        refreshSnapshot();
    }

    private IntroSequence(DefaultResources resources) {
        this(resources.romData(), resources.backgroundProvider());
    }

    public void tick() {
        if (stage == Stage.COMPLETE) {
            return;
        }

        frameCounter++;
        stageFrame++;
        random.beginFrame(frameCounter & 0xFF, 0);
        switch (stage) {
            case SEA -> tickSea();
            case LINK_FACE -> tickLinkFace();
            case SEA_FADE -> tickSeaFade();
            case BEACH_FADE -> tickBeachFade();
            case BEACH -> tickBeach();
            case TITLE_REVEAL -> tickTitleReveal();
            case TITLE_DX -> tickTitleDx();
            case COMPLETE -> { }
        }
        refreshSnapshot();
    }

    public boolean isActive() {
        return stage != Stage.COMPLETE;
    }

    public void skipToTitle() {
        stage = Stage.COMPLETE;
        stageFrame = 0;
        scrollX = 0;
        scrollY = 0;
        beachScrollX = 0;
        lightning.clear();
        linkVisible = false;
        titleRowIndex = titleRows.size();
        titleRevealRows = titleRows.size();
        setScene(IntroCutsceneScript.SCENE_TITLE);
        applyAllTitleRows();
        sprites = List.of();
        refreshSnapshot();
    }

    public String sceneId() {
        return switch (stage) {
            case SEA, SEA_FADE -> IntroCutsceneScript.SCENE_SEA;
            case LINK_FACE -> IntroCutsceneScript.SCENE_LINK_FACE;
            case BEACH_FADE, BEACH -> IntroCutsceneScript.SCENE_BEACH;
            case TITLE_REVEAL, TITLE_DX, COMPLETE -> IntroCutsceneScript.SCENE_TITLE;
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
        if ((frameCounter & 0x07) == 0) {
            scrollX = (scrollX + 1) & 0xFF;
            shipX = (shipX - 1) & 0xFF;
            triggerSeaLightningIfNeeded();
        }
        if (stageFrame >= SEA_SCROLL_FRAMES) {
            lightning.clear();
            transitionTo(Stage.LINK_FACE);
        }
    }

    private void tickLinkFace() {
        decrementLightning();
        linkFaceTimer++;
        if (linkFaceTimer == LINK_FACE_LIGHTNING_FRAME) {
            triggerLightning(0);
        }
        if (linkFaceTimer >= LINK_FACE_FRAMES) {
            lightning.clear();
            seaFadeTimer = 1;
            transitionTo(Stage.SEA_FADE);
            return;
        }
    }

    private void tickSeaFade() {
        decrementLightning();
        seaFadeTimer++;
        if (seaFadeTimer >= 0x58) {
            beachFadeTimer = BEACH_FADE_FRAMES;
            transitionTo(Stage.BEACH_FADE);
        }
    }

    private void tickBeachFade() {
        beachFadeTimer--;
        updateWaveOffsets();
        if (beachFadeTimer <= 0) {
            marinState = 0;
            marinX = BEACH_MARIN_START_X;
            marinInertia = 1;
            marinTimer = 0;
            linkVisible = false;
            beachScrollX = 0;
            beachFrameCounter = 0;
            transitionTo(Stage.BEACH);
        }
    }

    private void tickBeach() {
        beachFrameCounter++;
        updateWaveOffsets();
        switch (marinState) {
            case 0 -> tickMarinState0();
            case 1 -> tickMarinState1();
            case 2 -> tickMarinState2();
            case 3 -> tickMarinState3();
            case 4 -> tickMarinState4();
            default -> throw new IllegalStateException("Unknown Marin state " + marinState);
        }
    }

    private void tickMarinState0() {
        if (marinX < 0x48) {
            marinTimer = 0x40;
            marinState = 1;
            return;
        }
        marinInertia--;
        if (marinInertia == 0) {
            marinInertia = 4;
            marinX--;
        }
    }

    private void tickMarinState1() {
        if (marinTimer > 0) {
            marinTimer--;
            return;
        }
        marinState = 2;
        linkVisible = true;
        linkX = BEACH_LINK_START_X;
        beachFrameCounter = 0;
    }

    private void tickMarinState2() {
        linkX = (linkX - 1) & 0xFF;
        if ((beachFrameCounter & 0x01) == 0) {
            beachScrollX++;
            if (beachScrollX == 0x30) {
                marinTimer = 0x40;
                marinState = 3;
            }
        }
    }

    private void tickMarinState3() {
        if (marinTimer > 0) {
            marinTimer--;
            return;
        }
        if ((beachFrameCounter & 0x01) == 0) {
            linkX = (linkX - 1) & 0xFF;
        }
        if ((beachFrameCounter & 0x03) != 0) {
            return;
        }
        beachScrollX++;
        if (beachScrollX == 0x3A) {
            marinTimer = 0x30;
        } else if (beachScrollX == 0x40) {
            marinTimer = 0x50;
        } else if (beachScrollX == 0x56) {
            beachScrollX = 0xA0;
            marinTimer = 0xE0;
            marinState = 4;
        }
    }

    private void tickMarinState4() {
        if ((beachFrameCounter & 0x01) == 0 && marinTimer > 0) {
            marinTimer--;
        }
        if (marinTimer == 0) {
            transitionTo(Stage.TITLE_REVEAL);
        }
    }

    private void tickTitleReveal() {
        if (stageFrame % TITLE_REVEAL_STEP_FRAMES != 0) {
            return;
        }
        if (titleRowIndex < titleRows.size()) {
            applyTitleRow(titleRows.get(titleRowIndex));
            titleRowIndex++;
            titleRevealRows = titleRowIndex;
            return;
        }
        transitionTo(Stage.TITLE_DX);
    }

    private void tickTitleDx() {
        titleHoldFrame++;
        if (titleHoldFrame >= TITLE_HOLD_FRAMES) {
            stage = Stage.COMPLETE;
        }
    }

    private void transitionTo(Stage nextStage) {
        stage = nextStage;
        stageFrame = 0;
        switch (nextStage) {
            case LINK_FACE -> {
                linkFaceTimer = 0;
                scrollX = 0;
                setScene(IntroCutsceneScript.SCENE_LINK_FACE);
            }
            case SEA_FADE -> {
                scrollX = 0;
                setScene(IntroCutsceneScript.SCENE_SEA);
            }
            case BEACH_FADE -> {
                scrollX = 0;
                setScene(IntroCutsceneScript.SCENE_BEACH);
            }
            case BEACH -> { }
            case TITLE_REVEAL -> {
                titleRowIndex = 0;
                titleRevealRows = 0;
                titleHoldFrame = 0;
                scrollX = 0;
                scrollY = 0;
                setScene(IntroCutsceneScript.SCENE_TITLE);
            }
            case TITLE_DX, COMPLETE, SEA -> { }
        }
    }

    private void decrementLightning() {
        lightningVisibleCountdown = Math.max(0, lightningVisibleCountdown - 1);
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
        if ((frameCounter & 0x07) == 0) {
            sectionScrollOffsets[0]++;
        }
        if ((frameCounter & 0x0F) == 0) {
            sectionScrollOffsets[1]++;
            sectionScrollOffsets[3]++;
        }
        if ((frameCounter & 0x1F) == 0) {
            sectionScrollOffsets[2]++;
        }
        int waveIndex = ((frameCounter + 0xFC) >>> 4) & 0x07;
        verticalWaveOffset = -verticalOffsets[waveIndex];
    }

    private void refreshSnapshot() {
        sprites = renderSprites();
        snapshot = new IntroFrameSnapshot(
            sceneId(),
            substate(),
            frameCounter,
            scrollX(),
            scrollY,
            sceneId().equals(IntroCutsceneScript.SCENE_BEACH) ? lineScrollMap() : null,
            verticalWaveOffset,
            tilemap,
            attrmap,
            bgPalettes,
            objPalettes,
            sprites,
            titleRevealRows);
    }

    private String substate() {
        return switch (stage) {
            case SEA -> "SEA";
            case LINK_FACE -> linkFaceTimer >= LINK_FACE_LIGHTNING_FRAME
                ? "LINK_FACE_LIGHTNING"
                : linkFaceTimer >= LINK_FACE_SCREAM_FRAME ? "LINK_FACE_SCREAM" : "LINK_FACE";
            case SEA_FADE -> "SEA_FADE";
            case BEACH_FADE -> "BEACH_FADE";
            case BEACH -> "MARIN_STATE_" + marinState;
            case TITLE_REVEAL -> "TITLE_REVEAL";
            case TITLE_DX -> "TITLE_DX";
            case COMPLETE -> "STABLE_TITLE";
        };
    }

    private List<IntroSprite> renderSprites() {
        List<IntroSprite> rendered = new ArrayList<>();
        if (stage == Stage.SEA || stage == Stage.SEA_FADE) {
            renderRain(rendered, 0x10);
            renderShip(rendered);
            renderLightning(rendered);
        } else if (stage == Stage.LINK_FACE) {
            renderRain(rendered, 0x15);
            renderLightning(rendered);
        } else if (stage == Stage.BEACH_FADE || stage == Stage.BEACH) {
            renderBeachEntities(rendered);
        } else if (stage == Stage.TITLE_DX) {
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
            rendered.add(new IntroSprite(tile, x - 8, y - 16, 0, false, false));
            x += 0x1C;
            if (x >= 0xA0) {
                x -= 0x98;
                y += 0x25;
            }
        }
    }

    private void renderShip(List<IntroSprite> rendered) {
        int heaveIndex = stage == Stage.SEA_FADE ? 0 : (frameCounter + 0xD0) >>> 4 & 0x07;
        int heave = shipHeave[heaveIndex];
        for (IntroRomData.OamEntry entry : shipTiles) {
            rendered.add(toSprite(entry, shipX, shipY + heave));
        }
        if (stage == Stage.SEA_FADE && seaFadeTimer >= 0x10) {
            for (IntroRomData.OamEntry entry : additionalShipTiles) {
                rendered.add(toSprite(entry, shipX, shipY));
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
        int marinVariant = Math.min(marinState, marinVariants.size() - 1);
        IntroRomData.SpritePair marin = marinVariants.get(marinVariant);
        addPair(rendered, marin, marinX, 0x68);
        if (!linkVisible || linkX >= 0xF0) {
            return;
        }
        int linkVariant = Math.min(marinState == 3 ? 1 : 0, inertLinkVariants.size() - 1);
        addPair(rendered, inertLinkVariants.get(linkVariant), linkX, BEACH_LINK_Y);
    }

    private void renderTitleSparkle(List<IntroSprite> rendered) {
        int variant = frameCounter / 4 & 0x07;
        int position = frameCounter / 0x40 & 0x07;
        IntroRomData.SpritePair pair = sparkleVariants.get(variant);
        addPair(rendered, pair, rom.titleSparkleXPositions()[position],
            rom.titleSparkleYPositions()[position]);
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
        int[] lineScroll = new int[BEACH_SCREEN_HEIGHT];
        for (int y = 0; y < lineScroll.length; y++) {
            int section = y < BEACH_SECTION_ENDS[0] ? 0
                : y < BEACH_SECTION_ENDS[1] ? 1
                : y < BEACH_SECTION_ENDS[2] ? 2 : 3;
            lineScroll[y] = (beachScrollX + sectionScrollOffsets[section]) & 0xFF;
        }
        return lineScroll;
    }

    private void setScene(String sceneId) {
        BackgroundScene scene = backgroundProvider.apply(sceneId);
        if (scene == null) {
            throw new IllegalArgumentException("No background scene for " + sceneId);
        }
        tilemap = scene.tilemap().clone();
        attrmap = scene.attrmap().clone();
        bgPalettes = copyPalettes(scene.palettes());
        objPalettes = copyPalettes(scene.objectPalettes());
    }

    private void applyTitleRow(IntroRomData.TitleRow row) {
        copyDrawRow(tilemap, row.tileTargetAddress(), row.tileBytes());
        copyDrawRow(attrmap, row.attributeTargetAddress(), row.attributeBytes());
    }

    private void applyAllTitleRows() {
        for (IntroRomData.TitleRow row : titleRows) {
            applyTitleRow(row);
        }
    }

    private static void copyDrawRow(int[] map, int targetAddress, int[] values) {
        int offset = targetAddress - 0x9800;
        if (offset < 0 || offset + values.length > map.length) {
            throw new IllegalArgumentException(String.format(
                "Intro draw command target $%04X is outside the 32x32 map", targetAddress));
        }
        System.arraycopy(values, 0, map, offset, values.length);
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
