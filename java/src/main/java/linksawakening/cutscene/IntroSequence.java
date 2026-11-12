package linksawakening.cutscene;

import static linksawakening.cutscene.IntroPaletteMath.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.IntConsumer;
import linksawakening.rom.Rom;
import linksawakening.scene.BackgroundScene;

/** Ordered IntroHandler stage orchestration; composed actors, water, sprites and VBlank publication own their state. */
public final class IntroSequence {

    static final int INITIAL_SHIP_X = 0xC0;
    static final int INITIAL_SHIP_Y = 0x4E;
    static final int LINK_FACE_SCREAM_FRAME = 0x80;
    static final int LINK_FACE_LIGHTNING_FRAME = 0x90;
    static final int BEACH_MARIN_START_X = 0xB0;
    static final int BEACH_LINK_START_X = 0xFE;
    static final int BEACH_LINK_Y = 0x6E;
    static final int[] SEA_LIGHTNING_SCROLLS = {0x10, 0x30, 0x38, 0x58, 0x5A, 0x69};

    enum Stage {
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

    record Lightning(int variant, int x, int y, int remaining) {}

    private final IntroRomData rom;
    private final List<IntroRomData.TitleRow> titleRows;
    private final int[] lightningPositions;
    private final int[] lightningStatuses;
    private final int[] verticalOffsets;
    private int seaWaveCounter;
    private IntConsumer noiseSound = id -> {};
    private int loadDelay;
    private int fadeSteps;
    private int shipX = INITIAL_SHIP_X;
    private final int shipY = INITIAL_SHIP_Y;
    private int seaFadeTimer = 1;
    private int beachFadeTimer = BEACH_FADE_FRAMES;
    private int titleRowIndex;
    private final List<Lightning> lightning = new ArrayList<>();
    private int lightningVisibleCountdown;

    record DefaultResources(Rom romData, Function<String, BackgroundScene> backgroundProvider) {}

    /** Compatibility constructor for older callers; runtime data still comes from the bundled ROM. */
    public IntroSequence() {
        this(IntroResources.defaultResources());
    }

    public IntroSequence(Rom romData, Function<String, BackgroundScene> backgroundProvider) {
        this.rom = new IntroRomData(romData);
        Function<String, BackgroundScene> checkedProvider =
                Objects.requireNonNull(backgroundProvider, "backgroundProvider");
        this.titleRows = rom.titleRows();
        List<IntroRomData.SpritePair> marinVariants = rom.marinVariants();
        List<IntroRomData.SpritePair> inertLinkVariants = rom.inertLinkVariants();
        List<IntroRomData.SpritePair> sparkleVariants = rom.sparkleVariants();
        List<List<IntroRomData.OamEntry>> lightningTiles = rom.lightningTiles();
        this.lightningPositions = rom.lightningEntityPositions();
        this.lightningStatuses = rom.lightningEntityStatuses();
        int[] shipHeave = rom.shipHeaveTable();
        this.verticalOffsets = rom.introVerticalOffsets();
        int[] colorModifiers = rom.introColorModifiers();
        IntroRomData.PaletteBlock dxFadeInPalette = rom.dxFadeInPalette();
        List<IntroRomData.OamEntry> shipTiles = rom.shipTiles();
        List<IntroRomData.OamEntry> additionalShipTiles = rom.additionalShipTiles();
        spriteComposer = new IntroSpriteComposer(
                rom,
                new IntroSpriteComposer.Assets(
                        marinVariants,
                        inertLinkVariants,
                        sparkleVariants,
                        lightningTiles,
                        shipHeave,
                        shipTiles,
                        additionalShipTiles));
        water = new IntroWater(rom, video, clockState, verticalOffsets);
        actors = new IntroBeachActors(rom, video, water, clockState);
        frames = new IntroFramePublisher(
                rom,
                new IntroFramePublisher.PaletteSources(colorModifiers, dxFadeInPalette),
                checkedProvider,
                phase,
                video,
                water,
                actors,
                spriteComposer);
        frames.setScene(IntroCutsceneScript.SCENE_SEA);
        water.waveTiles(rom.tileBytes(0x4100, 128));
        spriteComposer.beginFrame(0);
        frames.refreshSnapshot(shipX, shipY, seaFadeTimer, lightning, clockState.clock());
    }

    public IntroSequence(DefaultResources resources) {
        this(resources.romData(), resources.backgroundProvider());
    }

    private static void copyDrawRow(int[] map, int targetAddress, int[] values) {
        int offset = targetAddress - 0x9800;
        if (offset < 0 || offset >= map.length || values.length > 32) {
            throw new IllegalArgumentException(
                    String.format("Intro draw command target $%04X is outside the 32x32 map", targetAddress));
        }
        for (int i = 0; i < values.length; i++) {
            map[(offset & 0x3E0) | ((offset + i) & 31)] = values[i];
        }
    }

    public void setNoiseSound(IntConsumer sink) {
        noiseSound = Objects.requireNonNull(sink);
    }

    private void tickSeaWaveSound() {
        // intro.asm func_001_71C7: called by stage 6 and Marin, not the sea handler.
        if (actors.linkState() < 2 && ++seaWaveCounter == 0xA0) {
            noiseSound.accept(0x0F);
            seaWaveCounter = 0;
        }
    }

    public void tick() {
        if (phase.stage() == Stage.COMPLETE && phase.skipped()) {
            return;
        }

        phase.frameCounter(phase.frameCounter() + 1);
        phase.stageFrame(phase.stageFrame() + 1);
        if (loadDelay > 0) {
            loadDelay--;
            frames.refreshSnapshot(shipX, shipY, seaFadeTimer, lightning, clockState.clock());
            return;
        }
        video.drawCommandQueued(false);
        video.beachScrollStoppedThisFrame(false);
        phase.topLineScroll((scrollX() + water.sectionScrollOffsets()[0]) & 255);
        // Draw commands, palettes and tile uploads become visible at the next VBlank.
        // Object positions additionally precede the entity handler's movement update.
        frames.visibleTilemap(video.tilemap().clone());
        frames.visibleAttrmap(video.attrmap().clone());
        frames.visibleScrollY(video.scrollY());
        if (phase.stage() == Stage.TITLE_REVEAL || phase.stage() == Stage.TITLE_WAIT)
            phase.titleEffectFrame(phase.titleEffectFrame() + 1);
        frames.visibleBg(frames.dynamicBgPalettes());
        frames.visibleObj(frames.dynamicObjPalettes());
        frames.visibleWave(water.waveTiles().clone());
        frames.sprites(
                frames.pendingSprites() == null || phase.stage() == Stage.TITLE_DX || phase.stage() == Stage.COMPLETE
                        ? frames.renderSprites(shipX, shipY, seaFadeTimer, lightning, clockState.clock())
                        : frames.pendingSprites());
        frames.pendingSprites(frames.renderSprites(shipX, shipY, seaFadeTimer, lightning, clockState.clock()));
        clockState.clock(clockState.clock() + 1);
        spriteComposer.beginFrame(clockState.clock() & 0xFF);
        switch (phase.stage()) {
            case SEA -> tickSea();
            case LINK_FACE -> tickLinkFace();
            case SEA_FADE -> tickSeaFade();
            case BEACH_LOAD -> {
                if (phase.stageFrame() == 4) {
                    transitionTo(Stage.BEACH_FADE);
                    loadDelay = 2;
                }
            }
            case BEACH_FADE -> tickBeachFade();
            case BEACH -> tickBeach();
            case TITLE_REVEAL -> tickTitleReveal();
            case TITLE_WAIT -> {
                if (phase.stageFrame() >= 26) {
                    transitionTo(Stage.TITLE_DX);
                    clockState.clock(1);
                }
            }
            case TITLE_DX -> tickTitleDx();
            case COMPLETE -> tickTitleDx();
        }
        frames.refreshSnapshot(shipX, shipY, seaFadeTimer, lightning, clockState.clock());
    }

    public boolean isActive() {
        return phase.stage() != Stage.COMPLETE;
    }

    public void skipToTitle() {
        phase.stage(Stage.COMPLETE);
        phase.skipped(true);
        frames.visibleScrollY(0);
        frames.visibleBg(frames.visibleObj(null));
        water.verticalWaveOffset(0);
        phase.stageFrame(0);
        video.scrollX(0);
        video.scrollY(0);
        video.beachScrollX(0);
        lightning.clear();
        actors.linkVisible(false);
        titleRowIndex = titleRows.size();
        phase.titleRevealRows(titleRows.size());
        frames.setScene(IntroCutsceneScript.SCENE_TITLE);
        // The direct title map already contains the logo; post-beach rows belong to the scrolled map.
        frames.sprites(List.of());
        frames.refreshSnapshot(shipX, shipY, seaFadeTimer, lightning, clockState.clock());
    }

    public int scrollY() {
        return video.scrollY();
    }

    public int scrollXForLine(int screenY) {
        int[] lineScroll = frames.snapshot() == null ? null : frames.snapshot().lineScrollX();
        if (lineScroll == null || lineScroll.length == 0) {
            return scrollX();
        }
        return lineScroll[Math.max(0, Math.min(screenY, lineScroll.length - 1))];
    }

    public int[] lineScrollX(int height) {
        int[] lineScroll = frames.snapshot() == null ? null : frames.snapshot().lineScrollX();
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
        return phase.titleRevealRows();
    }

    public List<IntroSprite> sprites() {
        return frames.snapshot().sprites();
    }

    public IntroFrameSnapshot snapshot() {
        return frames.snapshot();
    }

    private int shipX() {
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
        if (phase.frameCounter() <= 2) return; // initial map transfer, before the first sea handler
        water.updateWaveOffsets();
        water.animateWaves(false);
        if ((clockState.clock() & 7) == 0) {
            video.scrollX((video.scrollX() + 1) & 255);
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
        phase.linkFaceTimer(phase.linkFaceTimer() + 1);
        if (phase.linkFaceTimer() == LINK_FACE_SCREAM_FRAME) rom.applyDrawCommands(video.tilemap(), 0x74B8);
        if (phase.linkFaceTimer() == LINK_FACE_LIGHTNING_FRAME || (clockState.clock() & 0x7f) == 0)
            playThunder(); // sound/palette flash only, no bolt entity
        if (phase.linkFaceTimer() >= LINK_FACE_FRAMES) {
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
            frames.fadeBg(brighten(frames.bgPalettes(), fadeSteps, 32));
            frames.fadeObj(brighten(frames.objPalettes(), Math.max(0, seaFadeTimer - 0x47), 32));
            int[][] firstTwo = brighten(frames.objPalettes(), fadeSteps, 8);
            frames.fadeObj()[0] = firstTwo[0];
            frames.fadeObj()[1] = firstTwo[1];
        }
        if (seaFadeTimer >= 0x58) {
            beachFadeTimer = BEACH_FADE_FRAMES;
            transitionTo(Stage.BEACH_LOAD);
            loadDelay = 3;
        }
    }

    private void tickBeachFade() {
        tickSeaWaveSound();
        beachFadeTimer--;
        // func_020_6AC1 restores all palettes one RGB555 step at a time.
        if (beachFadeTimer < 0x34) {
            if ((beachFadeTimer & 3) == 0) fadeSteps = Math.min(12, fadeSteps + 1);
            if (fadeSteps + (clockState.clock() & 3) >= 6) {
                frames.fadeBg(approachPalettes(frames.fadeBg(), frames.bgPalettes()));
                frames.fadeObj(approachPalettes(frames.fadeObj(), frames.objPalettes()));
            }
        }
        if (beachFadeTimer == 0) {
            actors.marinState(0);
            actors.marinX(BEACH_MARIN_START_X);
            actors.marinInertia(1);
            actors.marinTimer(0);
            actors.linkVisible(false);
            transitionTo(Stage.BEACH);
        }
    }

    private void tickBeach() {
        // Entity slot 1 (Link) executes before slot 0 (Marin).
        if (actors.linkVisible() && actors.tickInertLink()) {
            transitionTo(Stage.TITLE_REVEAL);
            return;
        }
        tickSeaWaveSound();
        // Link's handler moves both entities vertically before Marin builds her OAM.
        List<IntroSprite> corrected = spriteComposer.correction();
        corrected.clear();
        corrected.addAll(frames.pendingSprites());
        if (corrected.size() >= 2) {
            corrected.subList(corrected.size() - 2, corrected.size()).clear();
            spriteComposer.addPair(
                    corrected, spriteComposer.marinPair(actors.marinVariant()), actors.marinX(), actors.marinY());
            frames.pendingSprites(List.copyOf(corrected));
        }
        actors.tickMarin();
    }

    private void tickTitleReveal() {
        if (phase.stageFrame() % TITLE_REVEAL_STEP_FRAMES != 0) {
            return;
        }
        if (titleRowIndex < titleRows.size()) {
            applyTitleRow(titleRows.get(titleRowIndex));
            titleRowIndex++;
            phase.titleRevealRows(titleRowIndex);
            if (titleRowIndex == titleRows.size()) transitionTo(Stage.TITLE_WAIT);
            return;
        }
        transitionTo(Stage.TITLE_WAIT);
    }

    private void tickTitleDx() {
        phase.titleHoldFrame(phase.titleHoldFrame() + 1);
        if (phase.titleHoldFrame() >= TITLE_HOLD_FRAMES) {
            phase.stage(Stage.COMPLETE);
        }
    }

    private void transitionTo(Stage nextStage) {
        frames.transitionFrame(frames.snapshot());
        phase.stage(nextStage);
        phase.stageFrame(0);
        switch (nextStage) {
            case LINK_FACE -> {
                phase.linkFaceTimer(0);
                video.scrollX(0);
                frames.setScene(IntroCutsceneScript.SCENE_LINK_FACE);
                water.verticalWaveOffset(0);
            }
            case SEA_FADE -> {
                video.scrollX(0);
                frames.setScene(IntroCutsceneScript.SCENE_SEA);
                water.verticalWaveOffset(-verticalOffsets[((0x80 + 0xFC) >>> 4) & 7]);
            }
            case BEACH_LOAD -> {
                video.scrollX(0);
                frames.setScene(IntroCutsceneScript.SCENE_BEACH);
                water.sectionScrollOffsets()[0] = water.sectionScrollOffsets()[2] = water.sectionScrollOffsets()[3] = 0;
                water.sectionScrollOffsets()[1] = 0x92;
                water.waveDestination(0x1300);
                water.waveTiles(rom.tileBytes(0x4B00, 128));
                System.arraycopy(rom.tileBytes(0x5200, 32), 0, water.waveTiles(), 0, 32);
                frames.visibleWave(water.waveTiles().clone());
                frames.fadeBg(brighten(frames.bgPalettes(), 31, 32));
                frames.fadeObj(brighten(frames.objPalettes(), 31, 32));
                fadeSteps = 0;
                water.verticalWaveOffset(0);
            }
            case BEACH_FADE, BEACH -> {}
            case TITLE_REVEAL -> {
                titleRowIndex = 0;
                phase.titleRevealRows(0);
                phase.titleHoldFrame(0);
                // Keep the streamed beach map and its 160,111 scroll origin.
                video.scrollX(video.beachScrollX());
                water.verticalWaveOffset(0);
            }
            case TITLE_DX -> {
                rom.applyDrawCommands(video.tilemap(), 0x7364);
                rom.applyDrawCommands(video.attrmap(), 0x73AC);
            }
            case TITLE_WAIT, COMPLETE, SEA -> {}
        }
    }

    private void decrementLightning() {
        lightningVisibleCountdown = Math.max(0, lightningVisibleCountdown - 1);
        frames.paletteLightningCountdown(lightningVisibleCountdown);
        for (int index = lightning.size() - 1; index >= 0; index--) {
            Lightning current = lightning.get(index);
            if (current.remaining() <= 1) {
                lightning.remove(index);
            } else {
                lightning.set(
                        index, new Lightning(current.variant(), current.x(), current.y(), current.remaining() - 1));
            }
        }
    }

    private void triggerSeaLightningIfNeeded() {
        for (int index = 0; index < SEA_LIGHTNING_SCROLLS.length; index++) {
            if (video.scrollX() == SEA_LIGHTNING_SCROLLS[index]) {
                triggerLightning(index);
                return;
            }
        }
    }

    private void triggerLightning(int index) {
        if (index < 0 || index >= lightningPositions.length) {
            return;
        }
        lightning.add(new Lightning(Math.max(0, lightningStatuses[index] - 1), lightningPositions[index], 0x30, 0x20));
        playThunder();
    }

    private void playThunder() {
        lightningVisibleCountdown = 0x1C;
        // RenderLightning.playSfx -> PlayBombExplosionSfx (bank0.asm 00:0C4B).
        noiseSound.accept(0x0C);
    }

    private void applyTitleRow(IntroRomData.TitleRow row) {
        copyDrawRow(video.tilemap(), row.tileTargetAddress(), row.tileBytes());
        copyDrawRow(video.attrmap(), row.attributeTargetAddress(), row.attributeBytes());
    }

    public static final int SEA_SCROLL_FRAMES = (0xC0 - 0x50) * 8 + 1;
    public static final int LINK_FACE_FRAMES = 0xA0;
    public static final int SEA_FADE_FRAMES = 0x58 - 1;
    public static final int BEACH_FADE_FRAMES = 0xFF;
    public static final int BEACH_MARIN_WALK_FRAMES = (0xB0 - 0x47) * 4;
    public static final int BEACH_MARIN_WAIT_FRAMES = 0x40;
    public static final int TITLE_REVEAL_STEP_FRAMES = 1;
    public static final int TITLE_HOLD_FRAMES = 160;
    private final IntroVideoMemory video = new IntroVideoMemory();
    private final IntroClock clockState = new IntroClock();
    private final IntroWater water;
    private final IntroBeachActors actors;
    private final IntroSpriteComposer spriteComposer;
    private final IntroPhaseState phase = new IntroPhaseState();
    private final IntroFramePublisher frames;

    public String sceneId() {
        return phase.sceneId();
    }

    public int scrollX() {
        return frames.scrollX();
    }
}
