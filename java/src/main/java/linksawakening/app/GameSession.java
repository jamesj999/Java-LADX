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
    ScreenRouter routerInstance;
    InputRouter inputRouterInstance;
    SessionContext context;
    GameplaySimulation simulationInstance;
    final ScreenState screens = new ScreenState();
    final DebugState debug = new DebugState();
    final SceneState scene = new SceneState();

    final RomRandomByteSource random;

    public GameSession() {
        this(new RomRandomByteSource());
    }

    public GameSession(RomRandomByteSource random) {
        this.random = java.util.Objects.requireNonNull(random);
    }

    linksawakening.gameplay.PcmSoundOutput headlessPcm;

    public GameSession(linksawakening.gameplay.PcmSoundOutput output) {
        this();
        headlessPcm = java.util.Objects.requireNonNull(output);
    }

    SessionBootstrap.Collaborators services;

    Host host = new HeadlessHost();
    GPU gpu;
    final GameFrameSceneBuilder frameSceneBuilder = new GameFrameSceneBuilder();
    final TransitionController transitionController = new TransitionController();

    boolean running = true;
    int frameCounter;

    // Room pixel dimensions
    final int ROOM_PIXEL_WIDTH = ROOM_TILE_WIDTH * 8; // 160
    final int ROOM_PIXEL_HEIGHT = ROOM_TILE_HEIGHT * 8; // 128

    // Scroll state
    final int SCROLL_SPEED = 4; // pixels per frame (original uses ~2-4px)

    final ScrollController scrollController = new ScrollController();
    // Mirrors the first OBJ palette entry used for Link and leaf particles.
    final int[] GREEN_OBJECTS_SPRITE_PALETTE = {
        0x00000000, 0x00000000, 0x0010A840, 0x00F8B888,
    };

    boolean overworldDialogInputConsumedThisFrame;
    GameplaySoundSink gameplaySoundSink = GameplaySoundSink.none();
    MusicDirector musicDirector;
    SaveRamStore saveRamStore;
    MarinWakeUpMotion newGameWakeUpMotion;
    TarinShieldMotion newGameTarinShieldMotion;
    int[] newGameTarinShieldPalette;
    AppConfig appConfig;
    final Screen titleScreen = new TitleScreen(new TitleScreen.Dependencies(
            () -> services.cutsceneManager(), () -> services.inputState(), () -> router().startFileSelection()));
    final Screen cutsceneScreen = new CutsceneScreen(new CutsceneScreen.Dependencies(
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
    final Screen fileMenuScreen = new FileMenuScreen(new FileMenuScreen.Dependencies(
            () -> screens.fileMenuController,
            () -> services.inputState(),
            () -> services.inputConfig(),
            () -> saveRamStore,
            slot -> screens.currentSaveSlot = slot,
            () -> saves().startNewGame(),
            saved -> saves().startSavedGame(saved),
            mutation -> saves().persistFileMenuMutation(mutation),
            this::completeInputFrame));
    final Screen fileSaveScreen = new FileSaveScreen(new FileSaveScreen.Dependencies(
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
    final GameplayScreen gameplayScreen = new GameplayScreen(
            this::tickGameplayScreen,
            (key, action) -> inputRouter().gameplayKey(key, action),
            () -> this.passOutController.snapshot(),
            () -> inputRouter().tryPresentFileSave());
    Screen currentScreen = titleScreen;

    final long FRAME_DURATION_NS = 1_000_000_000L / 60L;

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

    void composeFrameBuffer() {
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

    void debugTeleport(linksawakening.world.transition.Warp destination) {
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

    void update() {
        if (debug.debugMenu != null && debug.debugMenu.isOpen()) return;
        frameCounter = (frameCounter + 1) & 0xFF;
        // The engine's frame boundary uses scanline zero for non-entity draws.
        random.beginFrame(frameCounter, 0);

        musicDirector.tick();

        currentScreen.tick();
    }

    void tickGameplayScreen() {
        new GameplayFrame(
                        new TransitionPhase(scrollController, transitionController, services.link(), SCROLL_SPEED),
                        () -> simulation().tick(),
                        this::completeInputFrame)
                .tick();
    }

    void completeInputFrame() {
        // Run last so all consumers observe this frame's input edges.
        services.inputState().tickEdges();
        overworldDialogInputConsumedThisFrame = false;
    }

    void routeDialogSounds() {
        DialogSoundRouter.routePending(services.dialogController(), services.dialogSoundSink());
    }

    int romDirectionForLink(int javaDirection) {
        return switch (javaDirection) {
            case Link.DIRECTION_RIGHT -> 0;
            case Link.DIRECTION_LEFT -> 1;
            case Link.DIRECTION_UP -> 2;
            case Link.DIRECTION_DOWN -> 3;
            default -> throw new IllegalArgumentException("Link direction out of range: " + javaDirection);
        };
    }

    OverworldDialogBlockers currentOverworldDialogBlockers() {
        return new OverworldDialogBlockers(
                services.inventoryController() != null
                        && services.inventoryController().shouldBlockOverworldInput(),
                scrollController.isActive(),
                transitionController.isInputBlocked(),
                GameplayDialogInput.blocksGameplay(services.dialogController()),
                overworldDialogInputConsumedThisFrame);
    }

    AppConfig currentAppConfig() {
        return appConfig != null ? appConfig : AppConfig.defaults();
    }

    /** Applies the configured player profile before a direct gameplay launch. */

    /** Mirrors the source's wSpawnLocationData fields copied by SaveGameToFile. */
    void cleanup() {
        host.releasePresentation();
        if (services.dialogSoundOutput() != null) {
            services.dialogSoundOutput().close();
        }
        if (services.musicPlayer() != null) {
            services.musicPlayer().close();
        }

        host.close();
    }

    OpeningStory openingStory() {
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

    void initialize(boolean headless) {
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
        context = new SessionContext(this);
        routerInstance = new ScreenRouter(context);
        inputRouterInstance = new InputRouter(context);
        simulationInstance = new GameplaySimulation(context);
    }

    SaveCoordinator saves() {
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

    BushInteraction bushes() {
        return new BushInteraction(
                services.roomSession(),
                services.overworldBushInteraction(),
                services.link(),
                services.equipmentController(),
                gameplaySoundSink,
                services.transientVfxSystem(),
                services.droppableRupeeSystem());
    }

    DialogInteractions dialogs() {
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

    LiftInteraction lifting() {
        return new LiftInteraction(
                services.inputState(),
                services.inputConfig(),
                services.playerState(),
                services.link(),
                services.roomSession(),
                this::romDirectionForLink);
    }

    final linksawakening.app.gameplay.PassOutController passOutController =
            new linksawakening.app.gameplay.PassOutController();

    void finishGameOver(int option) {
        var sword = services.equipmentController().activeSword();
        if (sword != null) sword.resetAfterGameOver();
        passOutController.clear();
        saves().finishGameOver(option);
        services.transientVfxSystem().clear();
        services.droppableRupeeSystem().clear();
    }

    GameplaySimulation simulation() {
        return simulationInstance;
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

    RenderStateAssembler renderer() {
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

    InputRouter inputRouter() {
        return inputRouterInstance;
    }

    ScreenRouter router() {
        return routerInstance;
    }
}
