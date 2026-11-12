package linksawakening.app;

import static linksawakening.world.room.RoomConstants.*;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import linksawakening.app.debug.DebugMenu;
import linksawakening.app.debug.DebugMenuOverlay;
import linksawakening.app.gameplay.BushInteraction;
import linksawakening.app.gameplay.DialogInteractions;
import linksawakening.app.gameplay.GameplayFrame;
import linksawakening.app.gameplay.GameplaySimulation;
import linksawakening.app.gameplay.LiftInteraction;
import linksawakening.app.gameplay.PassOutController;
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
import linksawakening.audio.music.FrameMusicPlayer;
import linksawakening.audio.music.GameplayMusicController;
import linksawakening.config.AppConfig;
import linksawakening.cutscene.CutsceneManager;
import linksawakening.dialog.DialogController;
import linksawakening.entity.Link;
import linksawakening.equipment.EquipmentController;
import linksawakening.equipment.ItemRegistry;
import linksawakening.gameplay.DialogTextLoader;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.gameplay.OverworldDialogInteraction;
import linksawakening.gpu.GPU;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.render.GameFrameSceneBuilder;
import linksawakening.rom.Rom;
import linksawakening.rom.RomTables;
import linksawakening.save.SaveRamStore;
import linksawakening.save.SaveSlotState;
import linksawakening.scene.BackgroundScene;
import linksawakening.scene.BackgroundSceneLoader;
import linksawakening.startup.MarinWakeUpMotion;
import linksawakening.startup.TarinShieldMotion;
import linksawakening.state.PlayerState;
import linksawakening.ui.FileMenuController;
import linksawakening.ui.FileSaveController;
import linksawakening.ui.InventoryController;
import linksawakening.ui.InventoryMenu;
import linksawakening.vfx.CutLeavesEffectRenderer;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.world.object.OverworldBushInteraction;
import linksawakening.world.room.DroppableRupeeSystem;
import linksawakening.world.room.RoomSession;
import linksawakening.world.room.RoomTransitionCoordinator;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;
import linksawakening.world.transition.Warp;

/** Builds and retains session services, screens and orchestration. Accessors read live mutable state. */
public final class SessionContext {
    SessionBootstrap.Collaborators services() {
        return services;
    }

    private final GameSession session;
    private final GameplayInputState gameplayInput = new GameplayInputState();
    private final linksawakening.app.gameplay.DialogBlockerPolicy dialogPolicy;
    private final FrameInputCompletion inputCompletion;

    public GameplayInputState gameplayInput() {
        return gameplayInput;
    }

    private final SessionDialogSounds dialogSoundRouting;

    public SessionContext(GameSession session, boolean headless) {
        this.session = Objects.requireNonNull(session);
        screens.currentScreen = titleScreen;

        host = headless
                ? (session.headlessPcm == null ? new HeadlessHost() : new HeadlessHost(session.headlessPcm))
                : new DesktopHost();
        gpu = new GPU();
        var built = new SessionBootstrap(
                        session.random,
                        gpu,
                        transitionController,
                        scrollController,
                        new SessionBootstrap.Callbacks(
                                (scene) -> router().setCutsceneScene(scene),
                                room -> musicDirector.selectMusicForLoadedRoom(
                                        room,
                                        services().playerState(),
                                        services().roomSession())),
                        host)
                .build(headless);
        services = built;
        session.gameplaySounds().replace(built.gameplaySoundSink());
        scene.bgPalettes = built.bgPalettes();
        scene.objPalettes = built.objPalettes();
        appConfig = built.appConfig();
        scene.currentTilemap = built.currentTilemap();
        scene.currentAttrmap = built.currentAttrmap();
        musicDirector = new MusicDirector(services().musicPlayer(), services().gameplayMusicController());
        dialogSoundRouting = new SessionDialogSounds(built.dialogController(), built.dialogSoundSink());
        dialogPolicy = new linksawakening.app.gameplay.DialogBlockerPolicy(
                built.inventoryController(),
                built.dialogController(),
                scrollController,
                transitionController,
                gameplayInput);
        inputCompletion = new FrameInputCompletion(built.inputState(), gameplayInput);
        fileMenuScreen = new FileMenuScreen(new FileMenuScreen.Dependencies(
                () -> screens.fileMenuController,
                () -> services().inputState(),
                () -> services().inputConfig(),
                () -> saveRamStore,
                slot -> screens.currentSaveSlot = slot,
                () -> saves().startNewGame(),
                saved -> saves().startSavedGame(saved),
                mutation -> saves().persistFileMenuMutation(mutation),
                inputCompletion));
        fileSaveScreen = new FileSaveScreen(new FileSaveScreen.Dependencies(
                () -> screens.fileSaveController,
                () -> services().inputState(),
                () -> services().inputConfig(),
                () -> {
                    screens.fileSaveController = null;
                    screens.currentScreen = this.gameplayScreen;
                },
                () -> saves().saveCurrentPlayerState(),
                () -> router().startFileSelection(),
                inputCompletion));
        saveRamStore = built.saveRamStore();
        newGameTarinShieldPalette = built.newGameTarinShieldPalette();
        simulationGameOverOutcome = this::finishGameOver;
        inputRouterStop = () -> running = false;
        inputRouterScreen = () -> screens.currentScreen;
        inputRouterStartDebug = () -> router().startDebugGameplay();
        inputRouterStory = this::openingStory;
        inputRouterFileSave = () -> saves().startFileSave();
        inputRouterDialogs = this::dialogs;
        routerScreen = () -> screens.currentScreen;
        routerShowScreen = value -> screens.currentScreen = value;
        routerSetFileMenu = value -> screens.fileMenuController = value;
        routerClearSaveSlot = () -> screens.currentSaveSlot = -1;
        routerResetDebug = () -> debug.debugNoClip = false;
        routerStartNewGame = () -> saves().startNewGame();
        savesStartFileSelection = () -> router().startFileSelection();
        savesShowFileSave = controller -> screens.fileSaveController = controller;
        simulationDebugScroll = () -> inputRouter().tryDebugScreenScroll();
        inputRouterToggleDebug = () -> {
            debug.debugNoClip = !debug.debugNoClip;
            services().link().setDebugNoClip(debug.debugNoClip);
            DebugLog.log("Debug no-clip: " + (debug.debugNoClip ? "ON" : "OFF"));
        };
        routerBackground = backgroundValue -> {
            scene.currentTilemap = backgroundValue.tilemap();
            scene.currentAttrmap = backgroundValue.attrmap();
            scene.bgPalettes = backgroundValue.palettes();
            scene.objPalettes = backgroundValue.objectPalettes();
        };
        routerClearFileMenuController = () -> screens.fileMenuController = null;
        routerClearFileSaveController = () -> screens.fileSaveController = null;
        routerClearNewGameWakeUpMotion = () -> session.openingState().wakeUp(null);
        routerClearNewGameTarinShieldMotion = () -> session.openingState().shield(null);
        savesEnterGameplay = () -> {
            screens.fileMenuController = null;
            screens.fileSaveController = null;
            screens.currentScreen = gameplayScreen;
        };
        savesClearOpening = () -> {
            session.openingState().wakeUp(null);
            session.openingState().shield(null);
        };
        savesInitializeOpening = () -> {
            session.openingState().wakeUp(new MarinWakeUpMotion());
            session.openingState().shield(new TarinShieldMotion());
        };
        savesPlayHouseMusic = () -> musicDirector.playDirectMusic(MusicDirector.newGameHouseMusicTrack());
        savesRefreshOpening = () -> openingStory().updateNewGameMarinPresentation();
        savesRestoreOpening = saved -> openingStory().restoreNewGameHouseRuntimeIfNeeded(saved);
        savesEnterFileSave = () -> {
            screens.currentScreen = fileSaveScreen;
            screens.fileMenuController = null;
        };
        savesFileMenuBackground = label -> router().loadFileMenuBackgroundScene(label);
        gameplayFrame = new GameplayFrame(
                new TransitionPhase(
                        scrollController, transitionController, services().link(), scrollSpeed),
                () -> simulation().tick(),
                inputCompletion);
        liftingInstance = new LiftInteraction(this);
        dialogsInstance = new DialogInteractions(this);
        bushesInstance = new BushInteraction(this);
        openingStoryInstance = new OpeningStory(this);
        rendererInstance = new RenderStateAssembler(this);
        savesInstance = new SaveCoordinator(this);
        routerInstance = new ScreenRouter(this);
        inputRouterInstance = new InputRouter(this);
        simulationInstance = new GameplaySimulation(this);
    }

    public PassOutController passOutController() {
        return passOutController;
    }

    private final IntConsumer simulationGameOverOutcome;

    public IntConsumer gameOverOutcome() {
        return simulationGameOverOutcome;
    }

    public GPU gpu() {
        return gpu;
    }

    public Rom romData() {
        return services().romData();
    }

    public RoomSession roomSession() {
        return services().roomSession();
    }

    public TransitionController transitionController() {
        return transitionController;
    }

    public RoomTransitionCoordinator roomTransitionCoordinator() {
        return services().roomTransitionCoordinator();
    }

    public boolean debugNoClip() {
        return debug.debugNoClip;
    }

    public int frameCounter() {
        return session.frameClock().value();
    }

    public int roomPixelHeight() {
        return roomPixelHeight;
    }

    public ScrollController scrollController() {
        return scrollController;
    }

    public InputConfig inputConfig() {
        return services().inputConfig();
    }

    public InputState inputState() {
        return services().inputState();
    }

    public PlayerState playerState() {
        return services().playerState();
    }

    public InventoryMenu inventoryMenu() {
        return services().inventoryMenu();
    }

    public InventoryController inventoryController() {
        return services().inventoryController();
    }

    public DialogController dialogController() {
        return services().dialogController();
    }

    public DialogTextLoader dialogTextLoader() {
        return services().dialogTextLoader();
    }

    public GameplaySoundSink gameplaySoundSink() {
        return session.gameplaySounds().sink();
    }

    public FrameMusicPlayer musicPlayer() {
        return services().musicPlayer();
    }

    public GameplayMusicController gameplayMusicController() {
        return services().gameplayMusicController();
    }

    public MusicDirector musicDirector() {
        return musicDirector;
    }

    public TransientVfxSystem transientVfxSystem() {
        return services().transientVfxSystem();
    }

    public DroppableRupeeSystem droppableRupeeSystem() {
        return services().droppableRupeeSystem();
    }

    public ItemRegistry itemRegistry() {
        return services().itemRegistry();
    }

    public EquipmentController equipmentController() {
        return services().equipmentController();
    }

    public Link link() {
        return services().link();
    }

    public OpeningStory story() {
        return openingStory();
    }

    public LiftInteraction lift() {
        return lifting();
    }

    public BushInteraction bush() {
        return bushes();
    }

    public DialogInteractions dialogInteractions() {
        return dialogs();
    }

    public linksawakening.app.gameplay.DialogBlockers dialogBlockers() {
        return dialogPolicy;
    }

    private final BooleanSupplier simulationDebugScroll;

    public BooleanSupplier simulationDebugScroll() {
        return simulationDebugScroll;
    }

    public linksawakening.app.gameplay.DialogSoundRouting dialogSounds() {
        return dialogSoundRouting;
    }

    public int roomPixelWidth() {
        return roomPixelWidth;
    }

    public RomTables romTables() {
        return services().romTables();
    }

    public AppConfig config() {
        return currentAppConfig();
    }

    private final Runnable inputRouterStop;

    public Runnable stop() {
        return inputRouterStop;
    }

    private final Runnable inputRouterToggleDebug;

    public Runnable inputRouterToggleDebug() {
        return inputRouterToggleDebug;
    }

    private final Supplier<Screen> inputRouterScreen;

    public Supplier<Screen> screen() {
        return inputRouterScreen;
    }

    private final Runnable inputRouterStartDebug;

    public Runnable startDebug() {
        return inputRouterStartDebug;
    }

    private final Supplier<OpeningStory> inputRouterStory;

    public Supplier<OpeningStory> inputRouterStory() {
        return inputRouterStory;
    }

    private final Runnable inputRouterFileSave;

    public Runnable fileSave() {
        return inputRouterFileSave;
    }

    private final Supplier<DialogInteractions> inputRouterDialogs;

    public Supplier<DialogInteractions> inputRouterDialogs() {
        return inputRouterDialogs;
    }

    public BackgroundSceneLoader backgroundSceneLoader() {
        return services().backgroundSceneLoader();
    }

    public CutsceneManager cutsceneManager() {
        return services().cutsceneManager();
    }

    public SaveRamStore saveRamStore() {
        return saveRamStore;
    }

    public Screen titleScreen() {
        return titleScreen;
    }

    public Screen cutsceneScreen() {
        return cutsceneScreen;
    }

    public Screen fileMenuScreen() {
        return fileMenuScreen;
    }

    public Screen gameplayScreen() {
        return gameplayScreen;
    }

    private final Supplier<Screen> routerScreen;

    public Supplier<Screen> routerScreen() {
        return routerScreen;
    }

    private final Consumer<Screen> routerShowScreen;

    public Consumer<Screen> showScreen() {
        return routerShowScreen;
    }

    private final Consumer<BackgroundScene> routerBackground;

    public Consumer<BackgroundScene> routerBackground() {
        return routerBackground;
    }

    private final Consumer<FileMenuController> routerSetFileMenu;

    public Consumer<FileMenuController> setFileMenu() {
        return routerSetFileMenu;
    }

    private final Runnable routerClearSaveSlot;

    public Runnable clearSaveSlot() {
        return routerClearSaveSlot;
    }

    private final Runnable routerResetDebug;

    public Runnable resetDebug() {
        return routerResetDebug;
    }

    private final Runnable routerStartNewGame;

    public Runnable startNewGame() {
        return routerStartNewGame;
    }

    private final Runnable routerClearFileMenuController;

    public Runnable routerClearFileMenuController() {
        return routerClearFileMenuController;
    }

    private final Runnable routerClearFileSaveController;

    public Runnable routerClearFileSaveController() {
        return routerClearFileSaveController;
    }

    private final Runnable routerClearNewGameWakeUpMotion;

    public Runnable routerClearNewGameWakeUpMotion() {
        return routerClearNewGameWakeUpMotion;
    }

    private final Runnable routerClearNewGameTarinShieldMotion;

    public Runnable routerClearNewGameTarinShieldMotion() {
        return routerClearNewGameTarinShieldMotion;
    }

    public int currentSaveSlot() {
        return screens.currentSaveSlot;
    }

    private final Runnable savesEnterGameplay;

    public Runnable savesEnterGameplay() {
        return savesEnterGameplay;
    }

    private final Runnable savesClearOpening;

    public Runnable savesClearOpening() {
        return savesClearOpening;
    }

    private final Runnable savesInitializeOpening;

    public Runnable savesInitializeOpening() {
        return savesInitializeOpening;
    }

    private final Runnable savesPlayHouseMusic;

    public Runnable savesPlayHouseMusic() {
        return savesPlayHouseMusic;
    }

    private final Runnable savesRefreshOpening;

    public Runnable savesRefreshOpening() {
        return savesRefreshOpening;
    }

    private final Consumer<SaveSlotState> savesRestoreOpening;

    public Consumer<SaveSlotState> savesRestoreOpening() {
        return savesRestoreOpening;
    }

    private final Runnable savesStartFileSelection;

    public Runnable startFileSelection() {
        return savesStartFileSelection;
    }

    private final Runnable savesEnterFileSave;

    public Runnable savesEnterFileSave() {
        return savesEnterFileSave;
    }

    private final Consumer<FileSaveController> savesShowFileSave;

    public Consumer<FileSaveController> showFileSave() {
        return savesShowFileSave;
    }

    private final Function<String, BackgroundScene> savesFileMenuBackground;

    public Function<String, BackgroundScene> savesFileMenuBackground() {
        return savesFileMenuBackground;
    }

    public GameFrameSceneBuilder frameSceneBuilder() {
        return frameSceneBuilder;
    }

    public int[] currentTilemap() {
        return scene.currentTilemap;
    }

    public int[] currentAttrmap() {
        return scene.currentAttrmap;
    }

    public int[][] bgPalettes() {
        return scene.bgPalettes;
    }

    public int[][] objPalettes() {
        return scene.objPalettes;
    }

    public byte[] indexedDisplayBuffer() {
        return services().indexedDisplayBuffer();
    }

    public int[] greenObjectsSpritePalette() {
        return greenObjectsSpritePalette;
    }

    public CutLeavesEffectRenderer cutLeavesEffectRenderer() {
        return services().cutLeavesEffectRenderer();
    }

    public Screen currentScreen() {
        return screens.currentScreen;
    }

    public OpeningStoryState openingState() {
        return session.openingState();
    }

    public MarinWakeUpMotion newGameWakeUpMotion() {
        return session.openingState().wakeUp();
    }

    public TarinShieldMotion newGameTarinShieldMotion() {
        return session.openingState().shield();
    }

    public int[] newGameTarinShieldPalette() {
        return newGameTarinShieldPalette;
    }

    public linksawakening.app.gameplay.DialogBlockers blockers() {
        return dialogPolicy;
    }

    public linksawakening.app.story.MusicRequests music() {
        return musicDirector;
    }

    public OverworldBushInteraction overworldBushInteraction() {
        return services().overworldBushInteraction();
    }

    public OverworldDialogInteraction overworldDialogInteraction() {
        return services().overworldDialogInteraction();
    }

    public linksawakening.app.gameplay.DialogBlockers dialogsBlockers() {
        return dialogPolicy;
    }

    final GameplayFrame gameplayFrame;
    final LiftInteraction liftingInstance;
    final DialogInteractions dialogsInstance;
    final BushInteraction bushesInstance;
    final OpeningStory openingStoryInstance;
    final RenderStateAssembler rendererInstance;
    final SaveCoordinator savesInstance;
    final ScreenRouter routerInstance;
    final InputRouter inputRouterInstance;
    final GameplaySimulation simulationInstance;
    final ScreenState screens = new ScreenState();
    final DebugState debug = new DebugState();
    final SceneState scene = new SceneState();

    final SessionBootstrap.Collaborators services;

    Host host = new HeadlessHost();
    GPU gpu;
    final GameFrameSceneBuilder frameSceneBuilder = new GameFrameSceneBuilder();
    final TransitionController transitionController = new TransitionController();

    boolean running = true;

    // Room pixel dimensions
    final int roomPixelWidth = ROOM_TILE_WIDTH * 8; // 160
    final int roomPixelHeight = ROOM_TILE_HEIGHT * 8; // 128

    // Scroll state
    final int scrollSpeed = 4; // pixels per frame (original uses ~2-4px)

    final ScrollController scrollController = new ScrollController();
    // Mirrors the first OBJ palette entry used for Link and leaf particles.
    final int[] greenObjectsSpritePalette = {
        0x00000000, 0x00000000, 0x0010A840, 0x00F8B888,
    };

    MusicDirector musicDirector;
    SaveRamStore saveRamStore;
    int[] newGameTarinShieldPalette;
    AppConfig appConfig;
    final Screen titleScreen = new TitleScreen(new TitleScreen.Dependencies(
            () -> services().cutsceneManager(), () -> services().inputState(), () -> router().startFileSelection()));
    final Screen cutsceneScreen = new CutsceneScreen(new CutsceneScreen.Dependencies(
            () -> services().cutsceneManager(),
            () -> services().inputState(),
            () -> services().dialogController(),
            () -> services().inputConfig(),
            this::routeDialogSounds,
            () -> screens.currentScreen = titleScreen,
            () -> {
                screens.currentScreen = titleScreen;
                musicDirector.playDirectMusic(MusicDirector.directTitleScreenMusicTrack());
            }));
    final Screen fileMenuScreen;
    final Screen fileSaveScreen;
    final GameplayScreen gameplayScreen = new GameplayScreen(
            this::tickGameplayScreen,
            (key, action) -> inputRouter().gameplayKey(key, action),
            () -> this.passOutController.snapshot(),
            () -> inputRouter().tryPresentFileSave());

    final PassOutController passOutController = new PassOutController();

    void composeFrameBuffer() {
        renderer().composeFrameBuffer();
        if (debug.debugMenu != null && debug.debugMenu.isOpen()) {
            if (debug.debugMenuOverlay == null) debug.debugMenuOverlay = new DebugMenuOverlay();
            debug.debugMenuOverlay.render(debug.debugMenu, services().indexedDisplayBuffer());
        }
    }

    void onKeyEvent(int key, int action) {
        if (debug.debugMenu != null && debug.debugMenu.isOpen()) {
            debug.debugMenu.key(key, action);
            services().inputState().clear();
            gameplayScreen.clearPendingInput();
            return;
        }
        if (currentAppConfig().debugEnabled() && DebugMenu.isToggle(key, action)) {
            if (screens.currentScreen == gameplayScreen
                    && inputRouter().canPresentFileSave()
                    && !services().roomSession().doors().tailCaveKeyholeSequenceActive()) {
                if (debug.debugMenu == null)
                    debug.debugMenu =
                            new DebugMenu(services().playerState(), services().roomSession(), this::debugTeleport);
                debug.debugMenu.open(
                        services().roomSession().rooms().mapCategory(),
                        services().roomSession().rooms().mapId(),
                        services().roomSession().rooms().currentRoomId(),
                        services().link().pixelX(),
                        services().link().pixelY());
                services().inputState().clear();
                gameplayScreen.clearPendingInput();
            }
            return;
        }
        inputRouter().onKeyEvent(key, action);
    }

    void debugTeleport(Warp destination) {
        services().roomSession().transitions().loadWarpDestination(destination);
        session.openingState().wakeUp(null);
        session.openingState().shield(null);
        services().link().resetTransientStateForDebug();
        services().link().setDebugNoClip(debug.debugNoClip);
        services().link().setSideScrolling(destination.category() == Warp.CATEGORY_SIDESCROLL);
        services().link().setRoomEntryPixelPosition(destination.javaPixelX(), destination.javaPixelY());
        services().transientVfxSystem().clear();
        services().droppableRupeeSystem().clear();
        services().inputState().clear();
        gameplayScreen.clearPendingInput();
    }

    void tickGameplayScreen() {
        gameplayFrame.tick();
    }

    void completeInputFrame() {
        inputCompletion.complete();
    }

    void routeDialogSounds() {
        dialogSoundRouting.route();
    }

    AppConfig currentAppConfig() {
        return appConfig != null ? appConfig : AppConfig.defaults();
    }

    OpeningStory openingStory() {
        return openingStoryInstance;
    }

    SaveCoordinator saves() {
        return savesInstance;
    }

    BushInteraction bushes() {
        return bushesInstance;
    }

    DialogInteractions dialogs() {
        return dialogsInstance;
    }

    LiftInteraction lifting() {
        return liftingInstance;
    }

    void finishGameOver(int option) {
        var sword = services().equipmentController().activeSword();
        if (sword != null) sword.resetAfterGameOver();
        passOutController.clear();
        saves().finishGameOver(option);
        services().transientVfxSystem().clear();
        services().droppableRupeeSystem().clear();
    }

    GameplaySimulation simulation() {
        return simulationInstance;
    }

    RenderStateAssembler renderer() {
        return rendererInstance;
    }

    InputRouter inputRouter() {
        return inputRouterInstance;
    }

    ScreenRouter router() {
        return routerInstance;
    }
}
