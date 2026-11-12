package linksawakening.app;

import static linksawakening.world.room.RoomConstants.*;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;
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
import linksawakening.gameplay.DialogSoundRouter;
import linksawakening.gameplay.DialogTextLoader;
import linksawakening.gameplay.GameplayDialogInput;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.gameplay.OverworldDialogBlockers;
import linksawakening.gameplay.OverworldDialogInteraction;
import linksawakening.gpu.GPU;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.render.GameFrameSceneBuilder;
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

/** Live access to session-owned services and state. */
public final class SessionContext {
    private final GameSession session;

    public SessionContext(GameSession session, boolean headless) {
        this.session = java.util.Objects.requireNonNull(session);
        initialize(headless);
    }

    public PassOutController simulation_passOutController() {
        return passOutController;
    }

    private java.util.function.IntConsumer simulation_gameOverOutcome;

    public java.util.function.IntConsumer simulation_gameOverOutcome() {
        if (simulation_gameOverOutcome == null) simulation_gameOverOutcome = this::finishGameOver;
        return simulation_gameOverOutcome;
    }

    public GPU simulation_gpu() {
        return gpu;
    }

    public byte[] simulation_romData() {
        return services.romData();
    }

    public RoomSession simulation_roomSession() {
        return services.roomSession();
    }

    public TransitionController simulation_transitionController() {
        return transitionController;
    }

    public RoomTransitionCoordinator simulation_roomTransitionCoordinator() {
        return services.roomTransitionCoordinator();
    }

    public boolean simulation_debugNoClip() {
        return debug.debugNoClip;
    }

    public int simulation_frameCounter() {
        return session.frameCounter;
    }

    public int simulation_ROOM_PIXEL_HEIGHT() {
        return ROOM_PIXEL_HEIGHT;
    }

    public ScrollController simulation_scrollController() {
        return scrollController;
    }

    public InputConfig simulation_inputConfig() {
        return services.inputConfig();
    }

    public InputState simulation_inputState() {
        return services.inputState();
    }

    public PlayerState simulation_playerState() {
        return services.playerState();
    }

    public InventoryMenu simulation_inventoryMenu() {
        return services.inventoryMenu();
    }

    public InventoryController simulation_inventoryController() {
        return services.inventoryController();
    }

    public DialogController simulation_dialogController() {
        return services.dialogController();
    }

    public DialogTextLoader simulation_dialogTextLoader() {
        return services.dialogTextLoader();
    }

    public GameplaySoundSink simulation_gameplaySoundSink() {
        return session.gameplaySoundSink;
    }

    public FrameMusicPlayer simulation_musicPlayer() {
        return services.musicPlayer();
    }

    public GameplayMusicController simulation_gameplayMusicController() {
        return services.gameplayMusicController();
    }

    public MusicDirector simulation_musicDirector() {
        return musicDirector;
    }

    public TransientVfxSystem simulation_transientVfxSystem() {
        return services.transientVfxSystem();
    }

    public DroppableRupeeSystem simulation_droppableRupeeSystem() {
        return services.droppableRupeeSystem();
    }

    public ItemRegistry simulation_itemRegistry() {
        return services.itemRegistry();
    }

    public EquipmentController simulation_equipmentController() {
        return services.equipmentController();
    }

    public Link simulation_link() {
        return services.link();
    }

    public OpeningStory simulation_story() {
        return openingStory();
    }

    public LiftInteraction simulation_lift() {
        return lifting();
    }

    public BushInteraction simulation_bush() {
        return bushes();
    }

    public DialogInteractions simulation_dialogInteractions() {
        return dialogs();
    }

    private Supplier<OverworldDialogBlockers> simulation_dialogBlockers;

    public Supplier<OverworldDialogBlockers> simulation_dialogBlockers() {
        if (simulation_dialogBlockers == null) simulation_dialogBlockers = this::currentOverworldDialogBlockers;
        return simulation_dialogBlockers;
    }

    private BooleanSupplier simulation_debugScroll;

    public BooleanSupplier simulation_debugScroll() {
        if (simulation_debugScroll == null)
            simulation_debugScroll = () -> inputRouter().tryDebugScreenScroll();
        return simulation_debugScroll;
    }

    private IntUnaryOperator simulation_direction;

    public IntUnaryOperator simulation_direction() {
        if (simulation_direction == null) simulation_direction = this::romDirectionForLink;
        return simulation_direction;
    }

    private Runnable simulation_dialogSounds;

    public Runnable simulation_dialogSounds() {
        if (simulation_dialogSounds == null) simulation_dialogSounds = this::routeDialogSounds;
        return simulation_dialogSounds;
    }

    public RoomSession inputRouter_roomSession() {
        return services.roomSession();
    }

    public TransitionController inputRouter_transitionController() {
        return transitionController;
    }

    public RoomTransitionCoordinator inputRouter_roomTransitionCoordinator() {
        return services.roomTransitionCoordinator();
    }

    public int inputRouter_ROOM_PIXEL_WIDTH() {
        return ROOM_PIXEL_WIDTH;
    }

    public int inputRouter_ROOM_PIXEL_HEIGHT() {
        return ROOM_PIXEL_HEIGHT;
    }

    public ScrollController inputRouter_scrollController() {
        return scrollController;
    }

    public InputConfig inputRouter_inputConfig() {
        return services.inputConfig();
    }

    public InputState inputRouter_inputState() {
        return services.inputState();
    }

    public InventoryController inputRouter_inventoryController() {
        return services.inventoryController();
    }

    public DialogController inputRouter_dialogController() {
        return services.dialogController();
    }

    public RomTables inputRouter_romTables() {
        return services.romTables();
    }

    public Link inputRouter_link() {
        return services.link();
    }

    public AppConfig inputRouter_config() {
        return currentAppConfig();
    }

    private Runnable inputRouter_stop;

    public Runnable inputRouter_stop() {
        if (inputRouter_stop == null) inputRouter_stop = () -> running = false;
        return inputRouter_stop;
    }

    private Runnable inputRouter_toggleDebug;

    public Runnable inputRouter_toggleDebug() {
        if (inputRouter_toggleDebug == null)
            inputRouter_toggleDebug = () -> {
                debug.debugNoClip = !debug.debugNoClip;
                services.link().setDebugNoClip(debug.debugNoClip);
                System.out.println("Debug no-clip: " + (debug.debugNoClip ? "ON" : "OFF"));
            };
        return inputRouter_toggleDebug;
    }

    private Supplier<Screen> inputRouter_screen;

    public Supplier<Screen> inputRouter_screen() {
        if (inputRouter_screen == null) inputRouter_screen = () -> currentScreen;
        return inputRouter_screen;
    }

    private Runnable inputRouter_consumeDialogInput;

    public Runnable inputRouter_consumeDialogInput() {
        if (inputRouter_consumeDialogInput == null)
            inputRouter_consumeDialogInput = () -> overworldDialogInputConsumedThisFrame = true;
        return inputRouter_consumeDialogInput;
    }

    private Runnable inputRouter_startDebug;

    public Runnable inputRouter_startDebug() {
        if (inputRouter_startDebug == null) inputRouter_startDebug = () -> router().startDebugGameplay();
        return inputRouter_startDebug;
    }

    private Supplier<OpeningStory> inputRouter_story;

    public Supplier<OpeningStory> inputRouter_story() {
        if (inputRouter_story == null) inputRouter_story = this::openingStory;
        return inputRouter_story;
    }

    private Runnable inputRouter_fileSave;

    public Runnable inputRouter_fileSave() {
        if (inputRouter_fileSave == null) inputRouter_fileSave = () -> saves().startFileSave();
        return inputRouter_fileSave;
    }

    private Supplier<DialogInteractions> inputRouter_dialogs;

    public Supplier<DialogInteractions> inputRouter_dialogs() {
        if (inputRouter_dialogs == null) inputRouter_dialogs = this::dialogs;
        return inputRouter_dialogs;
    }

    public GPU router_gpu() {
        return gpu;
    }

    public byte[] router_romData() {
        return services.romData();
    }

    public RoomSession router_roomSession() {
        return services.roomSession();
    }

    public int router_ROOM_PIXEL_WIDTH() {
        return ROOM_PIXEL_WIDTH;
    }

    public int router_ROOM_PIXEL_HEIGHT() {
        return ROOM_PIXEL_HEIGHT;
    }

    public InputState router_inputState() {
        return services.inputState();
    }

    public PlayerState router_playerState() {
        return services.playerState();
    }

    public BackgroundSceneLoader router_backgroundSceneLoader() {
        return services.backgroundSceneLoader();
    }

    public MusicDirector router_musicDirector() {
        return musicDirector;
    }

    public CutsceneManager router_cutsceneManager() {
        return services.cutsceneManager();
    }

    public SaveRamStore router_saveRamStore() {
        return saveRamStore;
    }

    public Link router_link() {
        return services.link();
    }

    public Screen router_titleScreen() {
        return titleScreen;
    }

    public Screen router_cutsceneScreen() {
        return cutsceneScreen;
    }

    public Screen router_fileMenuScreen() {
        return fileMenuScreen;
    }

    public Screen router_gameplayScreen() {
        return gameplayScreen;
    }

    public AppConfig router_config() {
        return currentAppConfig();
    }

    private Supplier<Screen> router_screen;

    public Supplier<Screen> router_screen() {
        if (router_screen == null) router_screen = () -> currentScreen;
        return router_screen;
    }

    private Consumer<Screen> router_showScreen;

    public Consumer<Screen> router_showScreen() {
        if (router_showScreen == null) router_showScreen = value -> currentScreen = value;
        return router_showScreen;
    }

    private Consumer<BackgroundScene> router_background;

    public Consumer<BackgroundScene> router_background() {
        if (router_background == null)
            router_background = backgroundValue -> {
                scene.currentTilemap = backgroundValue.tilemap();
                scene.currentAttrmap = backgroundValue.attrmap();
                scene.bgPalettes = backgroundValue.palettes();
                scene.objPalettes = backgroundValue.objectPalettes();
            };
        return router_background;
    }

    private Consumer<FileMenuController> router_setFileMenu;

    public Consumer<FileMenuController> router_setFileMenu() {
        if (router_setFileMenu == null) router_setFileMenu = value -> screens.fileMenuController = value;
        return router_setFileMenu;
    }

    private Runnable router_clearSaveSlot;

    public Runnable router_clearSaveSlot() {
        if (router_clearSaveSlot == null) router_clearSaveSlot = () -> screens.currentSaveSlot = -1;
        return router_clearSaveSlot;
    }

    private Runnable router_resetDebug;

    public Runnable router_resetDebug() {
        if (router_resetDebug == null) router_resetDebug = () -> debug.debugNoClip = false;
        return router_resetDebug;
    }

    private Runnable router_startNewGame;

    public Runnable router_startNewGame() {
        if (router_startNewGame == null) router_startNewGame = () -> saves().startNewGame();
        return router_startNewGame;
    }

    private Runnable router_clearFileMenuController;

    public Runnable router_clearFileMenuController() {
        if (router_clearFileMenuController == null)
            router_clearFileMenuController = () -> screens.fileMenuController = null;
        return router_clearFileMenuController;
    }

    private Runnable router_clearFileSaveController;

    public Runnable router_clearFileSaveController() {
        if (router_clearFileSaveController == null)
            router_clearFileSaveController = () -> screens.fileSaveController = null;
        return router_clearFileSaveController;
    }

    private Runnable router_clearNewGameWakeUpMotion;

    public Runnable router_clearNewGameWakeUpMotion() {
        if (router_clearNewGameWakeUpMotion == null)
            router_clearNewGameWakeUpMotion = () -> session.newGameWakeUpMotion = null;
        return router_clearNewGameWakeUpMotion;
    }

    private Runnable router_clearNewGameTarinShieldMotion;

    public Runnable router_clearNewGameTarinShieldMotion() {
        if (router_clearNewGameTarinShieldMotion == null)
            router_clearNewGameTarinShieldMotion = () -> session.newGameTarinShieldMotion = null;
        return router_clearNewGameTarinShieldMotion;
    }

    public PlayerState saves_playerState() {
        return services.playerState();
    }

    public RoomSession saves_roomSession() {
        return services.roomSession();
    }

    public Link saves_link() {
        return services.link();
    }

    public GPU saves_gpu() {
        return gpu;
    }

    public byte[] saves_romData() {
        return services.romData();
    }

    public SaveRamStore saves_saveRamStore() {
        return saveRamStore;
    }

    public int saves_currentSaveSlot() {
        return screens.currentSaveSlot;
    }

    public InputState saves_inputState() {
        return services.inputState();
    }

    private Runnable saves_enterGameplay;

    public Runnable saves_enterGameplay() {
        if (saves_enterGameplay == null)
            saves_enterGameplay = () -> {
                screens.fileMenuController = null;
                screens.fileSaveController = null;
                currentScreen = gameplayScreen;
            };
        return saves_enterGameplay;
    }

    private Runnable saves_clearOpening;

    public Runnable saves_clearOpening() {
        if (saves_clearOpening == null)
            saves_clearOpening = () -> {
                session.newGameWakeUpMotion = null;
                session.newGameTarinShieldMotion = null;
            };
        return saves_clearOpening;
    }

    private Runnable saves_initializeOpening;

    public Runnable saves_initializeOpening() {
        if (saves_initializeOpening == null)
            saves_initializeOpening = () -> {
                session.newGameWakeUpMotion = new MarinWakeUpMotion();
                session.newGameTarinShieldMotion = new TarinShieldMotion();
            };
        return saves_initializeOpening;
    }

    private Runnable saves_playHouseMusic;

    public Runnable saves_playHouseMusic() {
        if (saves_playHouseMusic == null)
            saves_playHouseMusic = () -> musicDirector.playDirectMusic(MusicDirector.newGameHouseMusicTrack());
        return saves_playHouseMusic;
    }

    private Runnable saves_refreshOpening;

    public Runnable saves_refreshOpening() {
        if (saves_refreshOpening == null)
            saves_refreshOpening = () -> openingStory().updateNewGameMarinPresentation();
        return saves_refreshOpening;
    }

    private Consumer<SaveSlotState> saves_restoreOpening;

    public Consumer<SaveSlotState> saves_restoreOpening() {
        if (saves_restoreOpening == null)
            saves_restoreOpening = saved -> openingStory()
                    .restoreNewGameHouseRuntimeIfNeeded(
                            saved,
                            motion -> session.newGameWakeUpMotion = motion,
                            motion -> session.newGameTarinShieldMotion = motion,
                            () -> openingStory().updateNewGameMarinPresentation());
        return saves_restoreOpening;
    }

    private Runnable saves_startFileSelection;

    public Runnable saves_startFileSelection() {
        if (saves_startFileSelection == null) saves_startFileSelection = () -> router().startFileSelection();
        return saves_startFileSelection;
    }

    private Runnable saves_enterFileSave;

    public Runnable saves_enterFileSave() {
        if (saves_enterFileSave == null)
            saves_enterFileSave = () -> {
                currentScreen = fileSaveScreen;
                screens.fileMenuController = null;
            };
        return saves_enterFileSave;
    }

    private Consumer<FileSaveController> saves_showFileSave;

    public Consumer<FileSaveController> saves_showFileSave() {
        if (saves_showFileSave == null) saves_showFileSave = controller -> screens.fileSaveController = controller;
        return saves_showFileSave;
    }

    private Function<String, BackgroundScene> saves_fileMenuBackground;

    public Function<String, BackgroundScene> saves_fileMenuBackground() {
        if (saves_fileMenuBackground == null)
            saves_fileMenuBackground = label -> router().loadFileMenuBackgroundScene(label);
        return saves_fileMenuBackground;
    }

    public GPU renderer_gpu() {
        return gpu;
    }

    public GameFrameSceneBuilder renderer_frameSceneBuilder() {
        return frameSceneBuilder;
    }

    public int[] renderer_currentTilemap() {
        return scene.currentTilemap;
    }

    public int[] renderer_currentAttrmap() {
        return scene.currentAttrmap;
    }

    public int[][] renderer_bgPalettes() {
        return scene.bgPalettes;
    }

    public int[][] renderer_objPalettes() {
        return scene.objPalettes;
    }

    public byte[] renderer_indexedDisplayBuffer() {
        return services.indexedDisplayBuffer();
    }

    public RoomSession renderer_roomSession() {
        return services.roomSession();
    }

    public TransitionController renderer_transitionController() {
        return transitionController;
    }

    public int renderer_frameCounter() {
        return session.frameCounter;
    }

    public ScrollController renderer_scrollController() {
        return scrollController;
    }

    public int[] renderer_GREEN_OBJECTS_SPRITE_PALETTE() {
        return GREEN_OBJECTS_SPRITE_PALETTE;
    }

    public InventoryMenu renderer_inventoryMenu() {
        return services.inventoryMenu();
    }

    public InventoryController renderer_inventoryController() {
        return services.inventoryController();
    }

    public DialogController renderer_dialogController() {
        return services.dialogController();
    }

    public CutsceneManager renderer_cutsceneManager() {
        return services.cutsceneManager();
    }

    public TransientVfxSystem renderer_transientVfxSystem() {
        return services.transientVfxSystem();
    }

    public DroppableRupeeSystem renderer_droppableRupeeSystem() {
        return services.droppableRupeeSystem();
    }

    public CutLeavesEffectRenderer renderer_cutLeavesEffectRenderer() {
        return services.cutLeavesEffectRenderer();
    }

    public Link renderer_link() {
        return services.link();
    }

    public Screen renderer_currentScreen() {
        return currentScreen;
    }

    public int openingStory_frameCounter() {
        return session.frameCounter;
    }

    public MarinWakeUpMotion openingStory_newGameWakeUpMotion() {
        return session.newGameWakeUpMotion;
    }

    public TarinShieldMotion openingStory_newGameTarinShieldMotion() {
        return session.newGameTarinShieldMotion;
    }

    public int[] openingStory_newGameTarinShieldPalette() {
        return newGameTarinShieldPalette;
    }

    public InputState openingStory_inputState() {
        return services.inputState();
    }

    public InputConfig openingStory_inputConfig() {
        return services.inputConfig();
    }

    public DialogController openingStory_dialogController() {
        return services.dialogController();
    }

    public DialogTextLoader openingStory_dialogTextLoader() {
        return services.dialogTextLoader();
    }

    public RoomSession openingStory_roomSession() {
        return services.roomSession();
    }

    public Link openingStory_link() {
        return services.link();
    }

    public PlayerState openingStory_playerState() {
        return services.playerState();
    }

    private Supplier<OverworldDialogBlockers> openingStory_blockers;

    public Supplier<OverworldDialogBlockers> openingStory_blockers() {
        if (openingStory_blockers == null) openingStory_blockers = this::currentOverworldDialogBlockers;
        return openingStory_blockers;
    }

    private IntConsumer openingStory_music;

    public IntConsumer openingStory_music() {
        if (openingStory_music == null) openingStory_music = musicDirector::playDirectMusic;
        return openingStory_music;
    }

    private IntSupplier openingStory_houseMusic;

    public IntSupplier openingStory_houseMusic() {
        if (openingStory_houseMusic == null) openingStory_houseMusic = MusicDirector::newGameHouseMusicTrack;
        return openingStory_houseMusic;
    }

    private IntUnaryOperator openingStory_romDirection;

    public IntUnaryOperator openingStory_romDirection() {
        if (openingStory_romDirection == null) openingStory_romDirection = this::romDirectionForLink;
        return openingStory_romDirection;
    }

    public RoomSession bushes_roomSession() {
        return services.roomSession();
    }

    public OverworldBushInteraction bushes_overworldBushInteraction() {
        return services.overworldBushInteraction();
    }

    public Link bushes_link() {
        return services.link();
    }

    public EquipmentController bushes_equipmentController() {
        return services.equipmentController();
    }

    public GameplaySoundSink bushes_gameplaySoundSink() {
        return session.gameplaySoundSink;
    }

    public TransientVfxSystem bushes_transientVfxSystem() {
        return services.transientVfxSystem();
    }

    public DroppableRupeeSystem bushes_droppableRupeeSystem() {
        return services.droppableRupeeSystem();
    }

    public InputConfig dialogs_inputConfig() {
        return services.inputConfig();
    }

    public OverworldDialogInteraction dialogs_overworldDialogInteraction() {
        return services.overworldDialogInteraction();
    }

    public RoomSession dialogs_roomSession() {
        return services.roomSession();
    }

    public Link dialogs_link() {
        return services.link();
    }

    public PlayerState dialogs_playerState() {
        return services.playerState();
    }

    public DialogController dialogs_dialogController() {
        return services.dialogController();
    }

    public DialogTextLoader dialogs_dialogTextLoader() {
        return services.dialogTextLoader();
    }

    private Supplier<OverworldDialogBlockers> dialogs_blockers;

    public Supplier<OverworldDialogBlockers> dialogs_blockers() {
        if (dialogs_blockers == null) dialogs_blockers = this::currentOverworldDialogBlockers;
        return dialogs_blockers;
    }

    public InputState lifting_inputState() {
        return services.inputState();
    }

    public InputConfig lifting_inputConfig() {
        return services.inputConfig();
    }

    public PlayerState lifting_playerState() {
        return services.playerState();
    }

    public Link lifting_link() {
        return services.link();
    }

    public RoomSession lifting_roomSession() {
        return services.roomSession();
    }

    private IntUnaryOperator lifting_direction;

    public IntUnaryOperator lifting_direction() {
        if (lifting_direction == null) lifting_direction = this::romDirectionForLink;
        return lifting_direction;
    }

    GameplayFrame gameplayFrame;
    LiftInteraction liftingInstance;
    DialogInteractions dialogsInstance;
    BushInteraction bushesInstance;
    OpeningStory openingStoryInstance;
    RenderStateAssembler rendererInstance;
    SaveCoordinator savesInstance;
    ScreenRouter routerInstance;
    InputRouter inputRouterInstance;
    GameplaySimulation simulationInstance;
    final ScreenState screens = new ScreenState();
    final DebugState debug = new DebugState();
    final SceneState scene = new SceneState();

    SessionBootstrap.Collaborators services;

    Host host = new HeadlessHost();
    GPU gpu;
    final GameFrameSceneBuilder frameSceneBuilder = new GameFrameSceneBuilder();
    final TransitionController transitionController = new TransitionController();

    boolean running = true;

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
    MusicDirector musicDirector;
    SaveRamStore saveRamStore;
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

    /** Initializes this session using the desktop update, input and render paths. */

    /** Starts the production reset intro without creating a native window. */

    /** Applies the configured player profile before a direct gameplay launch. */

    /** Mirrors the source's wSpawnLocationData fields copied by SaveGameToFile. */
    final linksawakening.app.gameplay.PassOutController passOutController =
            new linksawakening.app.gameplay.PassOutController();

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
        session.newGameWakeUpMotion = null;
        session.newGameTarinShieldMotion = null;
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

    void tickGameplayScreen() {
        gameplayFrame.tick();
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

    OpeningStory openingStory() {
        return openingStoryInstance;
    }

    void initialize(boolean headless) {
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
                                        room, services.playerState(), services.roomSession()),
                                this::romDirectionForLink),
                        host)
                .build(headless);
        services = built;
        session.gameplaySoundSink = built.gameplaySoundSink();
        scene.bgPalettes = built.bgPalettes();
        scene.objPalettes = built.objPalettes();
        appConfig = built.appConfig();
        scene.currentTilemap = built.currentTilemap();
        scene.currentAttrmap = built.currentAttrmap();
        musicDirector = new MusicDirector(services.musicPlayer(), services.gameplayMusicController());
        saveRamStore = built.saveRamStore();
        newGameTarinShieldPalette = built.newGameTarinShieldPalette();
        gameplayFrame = new GameplayFrame(
                new TransitionPhase(scrollController, transitionController, services.link(), SCROLL_SPEED),
                () -> simulation().tick(),
                this::completeInputFrame);
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
