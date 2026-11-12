package linksawakening.app;

import static linksawakening.world.room.RoomConstants.*;

import linksawakening.app.gameplay.BushInteraction;
import linksawakening.app.gameplay.DialogInteractions;
import linksawakening.app.gameplay.GameplayFrame;
import linksawakening.app.gameplay.GameplaySimulation;
import linksawakening.app.gameplay.LiftInteraction;
import linksawakening.app.gameplay.TransitionPhase;
import linksawakening.app.host.DesktopHost;
import linksawakening.app.host.HeadlessHost;
import linksawakening.app.host.Host;
import linksawakening.app.screen.CutsceneScreen;
import linksawakening.app.screen.FileMenuScreen;
import linksawakening.app.screen.FileSaveScreen;
import linksawakening.app.screen.GameplayScreen;
import linksawakening.app.screen.Screen;
import linksawakening.app.screen.TitleScreen;
import linksawakening.app.story.OpeningStory;
import linksawakening.config.AppConfig;
import linksawakening.entity.Link;
import linksawakening.gameplay.DialogSoundRouter;
import linksawakening.gameplay.GameplayDialogInput;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.gameplay.OverworldDialogBlockers;
import linksawakening.gpu.GPU;
import linksawakening.render.GameFrameSceneBuilder;
import linksawakening.save.SaveRamStore;
import linksawakening.startup.MarinWakeUpMotion;
import linksawakening.startup.StartupCoordinator;
import linksawakening.startup.TarinShieldMotion;
import linksawakening.world.entity.RomRandomByteSource;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;

public class GameSession {
    private final ScreenState screens = new ScreenState();
    private final DebugState debug = new DebugState();
    private final SceneState scene = new SceneState();

    private final RomRandomByteSource random;

    public GameSession() {
        this(new RomRandomByteSource());
    }

    public GameSession(RomRandomByteSource random) {
        this.random = java.util.Objects.requireNonNull(random);
    }

    private linksawakening.gameplay.PcmSoundOutput headlessPcm;

    public GameSession(linksawakening.gameplay.PcmSoundOutput output) {
        this();
        headlessPcm = java.util.Objects.requireNonNull(output);
    }

    private SessionBootstrap.Collaborators services;

    private Host host = new HeadlessHost();
    private GPU gpu;
    private final GameFrameSceneBuilder frameSceneBuilder = new GameFrameSceneBuilder();
    private final TransitionController transitionController = new TransitionController();

    private boolean running = true;
    private int frameCounter;

    // Room pixel dimensions
    private final int ROOM_PIXEL_WIDTH = ROOM_TILE_WIDTH * 8; // 160
    private final int ROOM_PIXEL_HEIGHT = ROOM_TILE_HEIGHT * 8; // 128

    // Scroll state
    private final int SCROLL_SPEED = 4; // pixels per frame (original uses ~2-4px)

    private final ScrollController scrollController = new ScrollController();
    // Mirrors the first OBJ palette entry used for Link and leaf particles.
    private final int[] GREEN_OBJECTS_SPRITE_PALETTE = {
        0x00000000, 0x00000000, 0x0010A840, 0x00F8B888,
    };

    private boolean overworldDialogInputConsumedThisFrame;
    private GameplaySoundSink gameplaySoundSink = GameplaySoundSink.none();
    private MusicDirector musicDirector;
    private SaveRamStore saveRamStore;
    private MarinWakeUpMotion newGameWakeUpMotion;
    private TarinShieldMotion newGameTarinShieldMotion;
    private int[] newGameTarinShieldPalette;
    private AppConfig appConfig;
    private final Screen titleScreen = new TitleScreen(new TitleScreen.Dependencies(
            () -> services.cutsceneManager(), () -> services.inputState(), () -> router().startFileSelection()));
    private final Screen cutsceneScreen = new CutsceneScreen(new CutsceneScreen.Dependencies(
            () -> services.cutsceneManager(),
            () -> services.inputState(),
            () -> services.dialogController(),
            () -> services.inputConfig(),
            this::routeDialogSounds,
            () -> currentScreen = titleScreen,
            () -> {
                currentScreen = titleScreen;
                musicDirector.playDirectMusic(MusicDirector.directTitleScreenMusicTrack());
            }));
    private final Screen fileMenuScreen = new FileMenuScreen(new FileMenuScreen.Dependencies(
            () -> screens.fileMenuController,
            () -> services.inputState(),
            () -> services.inputConfig(),
            () -> saveRamStore,
            slot -> screens.currentSaveSlot = slot,
            () -> saves().startNewGame(),
            saved -> saves().startSavedGame(saved),
            mutation -> saves().persistFileMenuMutation(mutation),
            this::completeInputFrame));
    private final Screen fileSaveScreen = new FileSaveScreen(new FileSaveScreen.Dependencies(
            () -> screens.fileSaveController,
            () -> services.inputState(),
            () -> services.inputConfig(),
            () -> {
                screens.fileSaveController = null;
                currentScreen = this.gameplayScreen;
            },
            () -> saves().saveCurrentPlayerState(),
            () -> router().startFileSelection(),
            this::completeInputFrame));
    private final GameplayScreen gameplayScreen = new GameplayScreen(
            this::tickGameplayScreen,
            (key, action) -> inputRouter().gameplayKey(key, action),
            () -> this.passOutController.snapshot(),
            () -> inputRouter().tryPresentFileSave());
    private Screen currentScreen = titleScreen;

    private final long FRAME_DURATION_NS = 1_000_000_000L / 60L;

    public void run(String[] args) {
        initialize(false);
        if (StartupCoordinator.shouldStartIntroCutscene(currentAppConfig())) {
            router().startIntroCutscene();
        } else if (currentAppConfig().startMode() == AppConfig.StartMode.NORMAL
                || !currentAppConfig().showTitleScreen()) {
            router().startConfiguredGameplay();
        } else {
            router().startTitleScreenWithoutIntro();
        }

        host.open(this::onKeyEvent);

        long nextFrameNs = System.nanoTime();

        while (!host.shouldClose() && running) {
            host.pollEvents();
            update();
            composeFrameBuffer();
            host.present(services.indexedDisplayBuffer());

            nextFrameNs += FRAME_DURATION_NS;
            long sleepNs = nextFrameNs - System.nanoTime();
            if (sleepNs > 0) {
                try {
                    Thread.sleep(sleepNs / 1_000_000L, (int) (sleepNs % 1_000_000L));
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            } else {
                // We're behind — drop the backlog so we don't spiral.
                nextFrameNs = System.nanoTime();
            }
        }

        cleanup();
    }

    /** Initializes this session using the desktop update, input and render paths. */
    public void initializeHeadlessNewGame() {
        if (services != null) throw new IllegalStateException("Game already initialized");
        initialize(true);
        services.dialogController().setPlayerName("A");
        saves().startNewGame();
    }

    /** Starts the production reset intro without creating a native window. */
    public void initializeHeadlessIntro() {
        if (services != null) throw new IllegalStateException("Game already initialized");
        initialize(true);
        router().startIntroCutscene();
    }

    public void headlessButton(String button, boolean pressed) {
        inputRouter().headlessButton(button, pressed);
    }

    public byte[] headlessFrame() {
        update();
        composeFrameBuffer();
        return services.indexedDisplayBuffer().clone();
    }

    void stepWithoutRender() {
        update();
    }

    private void composeFrameBuffer() {
        renderer().composeFrameBuffer();
        if (debug.debugMenu != null && debug.debugMenu.isOpen()) {
            if (debug.debugMenuOverlay == null)
                debug.debugMenuOverlay = new linksawakening.app.debug.DebugMenuOverlay();
            debug.debugMenuOverlay.render(debug.debugMenu, services.indexedDisplayBuffer());
        }
    }

    void onKeyEvent(int key, int action) {
        if (debug.debugMenu != null && debug.debugMenu.isOpen()) {
            debug.debugMenu.key(key, action);
            services.inputState().clear();
            gameplayScreen.clearPendingInput();
            return;
        }
        if (currentAppConfig().debugEnabled() && linksawakening.app.debug.DebugMenu.isToggle(key, action)) {
            if (currentScreen == gameplayScreen
                    && inputRouter().canPresentFileSave()
                    && !services.roomSession().tailCaveKeyholeSequenceActive()) {
                if (debug.debugMenu == null)
                    debug.debugMenu = new linksawakening.app.debug.DebugMenu(
                            services.playerState(), services.roomSession(), this::debugTeleport);
                debug.debugMenu.open(
                        services.roomSession().mapCategory(),
                        services.roomSession().mapId(),
                        services.roomSession().currentRoomId(),
                        services.link().pixelX(),
                        services.link().pixelY());
                services.inputState().clear();
                gameplayScreen.clearPendingInput();
            }
            return;
        }
        inputRouter().onKeyEvent(key, action);
    }

    private void debugTeleport(linksawakening.world.transition.Warp destination) {
        services.roomSession().loadWarpDestination(destination);
        newGameWakeUpMotion = null;
        newGameTarinShieldMotion = null;
        services.link().resetTransientStateForDebug();
        services.link().setDebugNoClip(debug.debugNoClip);
        services.link()
                .setSideScrolling(destination.category() == linksawakening.world.transition.Warp.CATEGORY_SIDESCROLL);
        services.link().setRoomEntryPixelPosition(destination.javaPixelX(), destination.javaPixelY());
        services.transientVfxSystem().clear();
        services.droppableRupeeSystem().clear();
        services.inputState().clear();
        gameplayScreen.clearPendingInput();
    }

    private void update() {
        if (debug.debugMenu != null && debug.debugMenu.isOpen()) return;
        frameCounter = (frameCounter + 1) & 0xFF;
        // The engine's frame boundary uses scanline zero for non-entity draws.
        random.beginFrame(frameCounter, 0);

        musicDirector.tick();

        currentScreen.tick();
    }

    private void tickGameplayScreen() {
        new GameplayFrame(
                        new TransitionPhase(scrollController, transitionController, services.link(), SCROLL_SPEED),
                        () -> simulation().tick(),
                        this::completeInputFrame)
                .tick();
    }

    private void completeInputFrame() {
        // Run last so all consumers observe this frame's input edges.
        services.inputState().tickEdges();
        overworldDialogInputConsumedThisFrame = false;
    }

    private void routeDialogSounds() {
        DialogSoundRouter.routePending(services.dialogController(), services.dialogSoundSink());
    }

    private int romDirectionForLink(int javaDirection) {
        return switch (javaDirection) {
            case Link.DIRECTION_RIGHT -> 0;
            case Link.DIRECTION_LEFT -> 1;
            case Link.DIRECTION_UP -> 2;
            case Link.DIRECTION_DOWN -> 3;
            default -> throw new IllegalArgumentException("Link direction out of range: " + javaDirection);
        };
    }

    private OverworldDialogBlockers currentOverworldDialogBlockers() {
        return new OverworldDialogBlockers(
                services.inventoryController() != null
                        && services.inventoryController().shouldBlockOverworldInput(),
                scrollController.isActive(),
                transitionController.isInputBlocked(),
                GameplayDialogInput.blocksGameplay(services.dialogController()),
                overworldDialogInputConsumedThisFrame);
    }

    private AppConfig currentAppConfig() {
        return appConfig != null ? appConfig : AppConfig.defaults();
    }

    /** Applies the configured player profile before a direct gameplay launch. */

    /** Mirrors the source's wSpawnLocationData fields copied by SaveGameToFile. */
    private void cleanup() {
        host.releasePresentation();
        if (services.dialogSoundOutput() != null) {
            services.dialogSoundOutput().close();
        }
        if (services.musicPlayer() != null) {
            services.musicPlayer().close();
        }

        host.close();
    }

    private OpeningStory openingStory() {
        return new OpeningStory(
                frameCounter,
                newGameWakeUpMotion,
                newGameTarinShieldMotion,
                newGameTarinShieldPalette,
                services.inputState(),
                services.inputConfig(),
                services.dialogController(),
                services.dialogTextLoader(),
                services.roomSession(),
                services.link(),
                services.playerState(),
                this::currentOverworldDialogBlockers,
                musicDirector::playDirectMusic,
                MusicDirector::newGameHouseMusicTrack,
                this::romDirectionForLink);
    }

    private void initialize(boolean headless) {
        host = headless
                ? (headlessPcm == null ? new HeadlessHost() : new HeadlessHost(headlessPcm))
                : new DesktopHost();
        gpu = new GPU();
        var built = new SessionBootstrap(
                        random,
                        gpu,
                        transitionController,
                        scrollController,
                        new SessionBootstrap.Callbacks(
                                (scene) -> router().setCutsceneScene(scene),
                                room -> musicDirector.selectMusicForLoadedRoom(
                                        room, services.playerState(), services.roomSession()),
                                this::romDirectionForLink),
                        host)
                .build(headless);
        services = built;
        gameplaySoundSink = built.gameplaySoundSink();
        scene.bgPalettes = built.bgPalettes();
        scene.objPalettes = built.objPalettes();
        appConfig = built.appConfig();
        scene.currentTilemap = built.currentTilemap();
        scene.currentAttrmap = built.currentAttrmap();
        musicDirector = new MusicDirector(services.musicPlayer(), services.gameplayMusicController());
        saveRamStore = built.saveRamStore();
        newGameTarinShieldPalette = built.newGameTarinShieldPalette();
    }

    private SaveCoordinator saves() {
        return new SaveCoordinator(
                services.playerState(),
                services.roomSession(),
                services.link(),
                gpu,
                services.romData(),
                saveRamStore,
                screens.currentSaveSlot,
                services.inputState(),
                () -> {
                    screens.fileMenuController = null;
                    screens.fileSaveController = null;
                    currentScreen = gameplayScreen;
                },
                () -> {
                    newGameWakeUpMotion = null;
                    newGameTarinShieldMotion = null;
                },
                () -> {
                    newGameWakeUpMotion = new MarinWakeUpMotion();
                    newGameTarinShieldMotion = new TarinShieldMotion();
                },
                () -> musicDirector.playDirectMusic(MusicDirector.newGameHouseMusicTrack()),
                () -> openingStory().updateNewGameMarinPresentation(),
                saved -> openingStory()
                        .restoreNewGameHouseRuntimeIfNeeded(
                                saved,
                                motion -> newGameWakeUpMotion = motion,
                                motion -> newGameTarinShieldMotion = motion,
                                () -> openingStory().updateNewGameMarinPresentation()),
                () -> router().startFileSelection(),
                () -> {
                    currentScreen = fileSaveScreen;
                    screens.fileMenuController = null;
                },
                controller -> screens.fileSaveController = controller,
                router()::loadFileMenuBackgroundScene);
    }

    private BushInteraction bushes() {
        return new BushInteraction(
                services.roomSession(),
                services.overworldBushInteraction(),
                services.link(),
                services.equipmentController(),
                gameplaySoundSink,
                services.transientVfxSystem(),
                services.droppableRupeeSystem());
    }

    private DialogInteractions dialogs() {
        return new DialogInteractions(
                services.inputConfig(),
                services.overworldDialogInteraction(),
                services.roomSession(),
                services.link(),
                services.playerState(),
                services.dialogController(),
                services.dialogTextLoader(),
                this::currentOverworldDialogBlockers);
    }

    private LiftInteraction lifting() {
        return new LiftInteraction(
                services.inputState(),
                services.inputConfig(),
                services.playerState(),
                services.link(),
                services.roomSession(),
                this::romDirectionForLink);
    }

    private final linksawakening.app.gameplay.PassOutController passOutController =
            new linksawakening.app.gameplay.PassOutController();

    private void finishGameOver(int option) {
        var sword = services.equipmentController().activeSword();
        if (sword != null) sword.resetAfterGameOver();
        passOutController.clear();
        saves().finishGameOver(option);
        services.transientVfxSystem().clear();
        services.droppableRupeeSystem().clear();
    }

    private GameplaySimulation simulation() {
        return new GameplaySimulation(
                passOutController,
                this::finishGameOver,
                gpu,
                services.romData(),
                services.roomSession(),
                transitionController,
                services.roomTransitionCoordinator(),
                debug.debugNoClip,
                frameCounter,
                ROOM_PIXEL_HEIGHT,
                scrollController,
                services.inputConfig(),
                services.inputState(),
                services.playerState(),
                services.inventoryMenu(),
                services.inventoryController(),
                services.dialogController(),
                services.dialogTextLoader(),
                gameplaySoundSink,
                services.musicPlayer(),
                services.gameplayMusicController(),
                musicDirector,
                services.transientVfxSystem(),
                services.droppableRupeeSystem(),
                services.itemRegistry(),
                services.equipmentController(),
                services.link(),
                openingStory(),
                lifting(),
                bushes(),
                dialogs(),
                this::currentOverworldDialogBlockers,
                () -> inputRouter().tryDebugScreenScroll(),
                this::romDirectionForLink,
                this::routeDialogSounds);
    }

    public HeadlessGameDriver.Diagnostics diagnostics() {
        return new HeadlessGameDriver.Diagnostics(
                gpu,
                services.roomSession(),
                transitionController,
                frameCounter,
                scrollController,
                services.playerState(),
                services.inventoryMenu(),
                services.dialogController(),
                services.equipmentController(),
                services.link());
    }

    private RenderStateAssembler renderer() {
        return new RenderStateAssembler(
                gpu,
                frameSceneBuilder,
                scene.currentTilemap,
                scene.currentAttrmap,
                scene.bgPalettes,
                scene.objPalettes,
                services.indexedDisplayBuffer(),
                services.roomSession(),
                transitionController,
                frameCounter,
                scrollController,
                GREEN_OBJECTS_SPRITE_PALETTE,
                services.inventoryMenu(),
                services.inventoryController(),
                services.dialogController(),
                services.cutsceneManager(),
                services.transientVfxSystem(),
                services.droppableRupeeSystem(),
                services.cutLeavesEffectRenderer(),
                services.link(),
                currentScreen);
    }

    private InputRouter inputRouter() {
        return new InputRouter(
                services.roomSession(),
                transitionController,
                services.roomTransitionCoordinator(),
                ROOM_PIXEL_WIDTH,
                ROOM_PIXEL_HEIGHT,
                scrollController,
                services.inputConfig(),
                services.inputState(),
                services.inventoryController(),
                services.dialogController(),
                services.romTables(),
                services.link(),
                currentAppConfig(),
                () -> running = false,
                () -> {
                    debug.debugNoClip = !debug.debugNoClip;
                    services.link().setDebugNoClip(debug.debugNoClip);
                    System.out.println("Debug no-clip: " + (debug.debugNoClip ? "ON" : "OFF"));
                },
                () -> currentScreen,
                () -> overworldDialogInputConsumedThisFrame = true,
                () -> router().startDebugGameplay(),
                this::openingStory,
                () -> saves().startFileSave(),
                this::dialogs);
    }

    private ScreenRouter router() {
        return new ScreenRouter(
                gpu,
                services.romData(),
                services.roomSession(),
                ROOM_PIXEL_WIDTH,
                ROOM_PIXEL_HEIGHT,
                services.inputState(),
                services.playerState(),
                services.backgroundSceneLoader(),
                musicDirector,
                services.cutsceneManager(),
                saveRamStore,
                services.link(),
                titleScreen,
                cutsceneScreen,
                fileMenuScreen,
                gameplayScreen,
                currentAppConfig(),
                () -> currentScreen,
                value -> currentScreen = value,
                scene -> {
                    this.scene.currentTilemap = scene.tilemap();
                    this.scene.currentAttrmap = scene.attrmap();
                    this.scene.bgPalettes = scene.palettes();
                    this.scene.objPalettes = scene.objectPalettes();
                },
                value -> screens.fileMenuController = value,
                () -> screens.currentSaveSlot = -1,
                () -> debug.debugNoClip = false,
                () -> saves().startNewGame(),
                () -> screens.fileMenuController = null,
                () -> screens.fileSaveController = null,
                () -> newGameWakeUpMotion = null,
                () -> newGameTarinShieldMotion = null);
    }
}
