package linksawakening.cutscene;

import static linksawakening.cutscene.IntroSequence.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import linksawakening.rom.RomBank;
import linksawakening.scene.BackgroundScene;
import linksawakening.scene.BackgroundSceneCatalog;
import linksawakening.scene.BackgroundSceneLoader;
import linksawakening.scene.BackgroundSceneSpec;
import linksawakening.world.entity.RomRandomByteSource;

/**
 * Domain-specific runtime for the shipped opening sequence.
 *
 * <p>This follows the intro handlers' state and timer boundaries without
 * pretending to execute the Game Boy CPU. ROM tables provide all entity
 * records and animation data; this class only models the state that affects
 * the frame renderer.</p>
 */

/** Shared state and dependency ownership for IntroSequence. */
abstract class IntroSequenceState {

    protected static final int INITIAL_SHIP_X = 0xC0;
    protected static final int INITIAL_SHIP_Y = 0x4E;
    protected static final int LINK_FACE_SCREAM_FRAME = 0x80;
    protected static final int LINK_FACE_LIGHTNING_FRAME = 0x90;
    protected static final int BEACH_MARIN_START_X = 0xB0;
    protected static final int BEACH_LINK_START_X = 0xFE;
    protected static final int BEACH_LINK_Y = 0x6E;
    protected static final int[] SEA_LIGHTNING_SCROLLS = {0x10, 0x30, 0x38, 0x58, 0x5A, 0x69};

    protected enum Stage {
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

    protected record Lightning(int variant, int x, int y, int remaining) {}

    protected final IntroRomData rom;
    protected final Function<String, BackgroundScene> backgroundProvider;
    protected final RomRandomByteSource random = new RomRandomByteSource(0xA2);
    protected final List<IntroRomData.TitleRow> titleRows;
    protected final List<IntroRomData.SpritePair> marinVariants;
    protected final List<IntroRomData.SpritePair> inertLinkVariants;
    protected final List<IntroRomData.SpritePair> sparkleVariants;
    protected final List<List<IntroRomData.OamEntry>> lightningTiles;
    protected final int[] lightningPositions;
    protected final int[] lightningStatuses;
    protected final int[] shipHeave;
    protected final int[] verticalOffsets;
    protected final int[] colorModifiers;
    protected final IntroRomData.PaletteBlock dxFadeInPalette;
    protected final List<IntroRomData.OamEntry> shipTiles;
    protected final List<IntroRomData.OamEntry> additionalShipTiles;

    protected Stage stage = Stage.SEA;
    protected int frameCounter;
    protected int seaWaveCounter;
    protected java.util.function.IntConsumer noiseSound = id -> {};

    protected int stageFrame;
    // hFrameCounter is reset during the beach sequence and stalls during LCD-off loads.
    // frameCounter is the monotonic presentation clock used for captures.
    protected int clock;
    protected int loadDelay;
    protected int fraction4, fraction5, fractionD;
    protected int marinVariant, linkVariant;
    protected int marinY = 0x68, linkY = BEACH_LINK_Y;
    protected int linkState, linkTimer, panCounter, panRows;
    protected byte[] waveTiles;
    protected int waveDestination = 0x900;
    protected boolean skipped;
    protected int fadeSteps;
    protected int[][] fadeBg, fadeObj;
    protected int[][] visibleBg, visibleObj;
    protected byte[] visibleWave;
    protected int scrollX;
    protected int scrollY;
    protected int verticalWaveOffset;
    protected int shipX = INITIAL_SHIP_X;
    protected final int shipY = INITIAL_SHIP_Y;
    protected int seaFadeTimer = 1;
    protected int beachFadeTimer = BEACH_FADE_FRAMES;
    protected int linkFaceTimer;
    protected int marinState;
    protected int marinX = BEACH_MARIN_START_X;
    protected int marinInertia = 1;
    protected int marinTimer;
    protected int linkX = BEACH_LINK_START_X;
    protected boolean linkVisible;
    protected int beachScrollX;
    protected int titleRowIndex;
    protected int titleRevealRows;
    protected int titleHoldFrame;
    protected int titleEffectFrame;
    protected int visibleScrollY;
    protected int topLineScroll;
    protected boolean drawCommandQueued;
    protected boolean beachScrollStoppedThisFrame;
    protected int paletteLightningCountdown;
    protected int[] visibleTilemap, visibleAttrmap;
    protected final int[] sectionScrollOffsets = new int[5];
    protected final List<Lightning> lightning = new ArrayList<>();
    protected int lightningVisibleCountdown;
    protected int[] tilemap;
    protected int[] attrmap;
    protected int[][] bgPalettes;
    protected int[][] objPalettes;
    protected List<IntroSprite> sprites = List.of();
    protected List<IntroSprite> pendingSprites;
    protected IntroFrameSnapshot snapshot;
    protected IntroFrameSnapshot transitionFrame;

    protected record DefaultResources(byte[] romData, Function<String, BackgroundScene> backgroundProvider) {}

    /** Compatibility constructor for older callers; runtime data still comes from the bundled ROM. */
    public IntroSequenceState() {
        this(defaultResources());
    }

    public IntroSequenceState(byte[] romData, Function<String, BackgroundScene> backgroundProvider) {
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

    protected IntroSequenceState(DefaultResources resources) {
        this(resources.romData(), resources.backgroundProvider());
    }

    protected static int[][] solidPalettes(int color) {
        int[][] palettes = new int[8][4];
        for (int[] palette : palettes) java.util.Arrays.fill(palette, color);
        return palettes;
    }

    protected static int[][] brighten(int[][] source, int amount, int count) {
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

    protected static int[][] approachPalettes(int[][] source, int[][] target) {
        int[][] result = copyPalettes(source);
        for (int p = 0; p < result.length; p++)
            for (int c = 0; c < 4; c++) {
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

    protected static int applyRgb555Modifier(int rgb888, int modifier) {
        int red = (rgb888 >>> 19) & 31;
        int green = (rgb888 >>> 11) & 31;
        int blue = (rgb888 >>> 3) & 31;
        int color = red | (green << 5) | (blue << 10);
        return RomBank.decodeRgb555(color | modifier);
    }

    protected static void copyDrawRow(int[] map, int targetAddress, int[] values) {
        int offset = targetAddress - 0x9800;
        if (offset < 0 || offset >= map.length || values.length > 32) {
            throw new IllegalArgumentException(
                    String.format("Intro draw command target $%04X is outside the 32x32 map", targetAddress));
        }
        for (int i = 0; i < values.length; i++) {
            map[(offset & 0x3E0) | ((offset + i) & 31)] = values[i];
        }
    }

    protected static int[][] copyPalettes(int[][] source) {
        int[][] copy = new int[source.length][];
        for (int row = 0; row < source.length; row++) {
            copy[row] = source[row].clone();
        }
        return copy;
    }

    protected static DefaultResources defaultResources() {
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

    protected static byte[] loadBundledRom() {
        try (InputStream stream = IntroSequence.class.getClassLoader().getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return stream.readAllBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load ROM", exception);
        }
    }

    public abstract void setNoiseSound(java.util.function.IntConsumer sink);

    protected abstract void tickSeaWaveSound();

    public abstract void tick();

    public abstract boolean isActive();

    public abstract void skipToTitle();

    public abstract String sceneId();

    public abstract int scrollX();

    public abstract int scrollY();

    public abstract int scrollXForLine(int screenY);

    public abstract int[] lineScrollX(int height);

    public abstract int titleRevealRows();

    public abstract List<IntroSprite> sprites();

    public abstract IntroFrameSnapshot snapshot();

    protected abstract int shipX();

    protected abstract void tickSea();

    protected abstract void tickLinkFace();

    protected abstract void tickSeaFade();

    protected abstract void tickBeachFade();

    protected abstract void tickBeach();

    protected abstract void tickInertLink();

    protected abstract void patchMountains();

    protected abstract void updateBeachOffsets(boolean slow);

    protected abstract void animateWaves(boolean beach);

    protected abstract void tickTitleReveal();

    protected abstract void tickTitleDx();

    protected abstract void transitionTo(Stage nextStage);

    protected abstract void decrementLightning();

    protected abstract void triggerSeaLightningIfNeeded();

    protected abstract void triggerLightning(int index);

    protected abstract void playThunder();

    protected abstract void updateWaveOffsets();

    protected abstract void refreshSnapshot();

    protected abstract String substate();

    protected abstract List<IntroSprite> renderSprites();

    protected abstract void renderRain(List<IntroSprite> rendered, int rows);

    protected abstract void renderShip(List<IntroSprite> rendered);

    protected abstract void renderLightning(List<IntroSprite> rendered);

    protected abstract void renderBeachEntities(List<IntroSprite> rendered);

    protected abstract void renderTitleSparkle(List<IntroSprite> rendered);

    protected abstract void addPair(List<IntroSprite> rendered, IntroRomData.SpritePair pair, int oamX, int oamY);

    protected abstract IntroSprite toSprite(IntroRomData.OamEntry entry, int oamX, int oamY);

    protected abstract int[] lineScrollMap();

    protected abstract int[] lineScrollYMap();

    protected abstract void setScene(String sceneId);

    protected abstract int[][] dynamicBgPalettes();

    protected abstract int[][] dynamicObjPalettes();

    protected abstract void applyTitleRow(IntroRomData.TitleRow row);
}
