package linksawakening.app;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;
import linksawakening.app.gameplay.BushInteraction;
import linksawakening.app.gameplay.DialogInteractions;
import linksawakening.app.gameplay.LiftInteraction;
import linksawakening.app.gameplay.PassOutController;
import linksawakening.app.screen.Screen;
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
import linksawakening.gameplay.OverworldDialogBlockers;
import linksawakening.gpu.GPU;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
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
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.world.room.DroppableRupeeSystem;
import linksawakening.world.room.RoomSession;
import linksawakening.world.room.RoomTransitionCoordinator;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;

/** Live access to session-owned services and state. */
public final class SessionContext {
    private final GameSession session;

    public SessionContext(GameSession session) {
        this.session = java.util.Objects.requireNonNull(session);
    }

    public PassOutController simulation_passOutController() {
        return session.passOutController;
    }

    private java.util.function.IntConsumer simulation_gameOverOutcome;

    public java.util.function.IntConsumer simulation_gameOverOutcome() {
        if (simulation_gameOverOutcome == null) simulation_gameOverOutcome = session::finishGameOver;
        return simulation_gameOverOutcome;
    }

    public GPU simulation_gpu() {
        return session.gpu;
    }

    public byte[] simulation_romData() {
        return session.services.romData();
    }

    public RoomSession simulation_roomSession() {
        return session.services.roomSession();
    }

    public TransitionController simulation_transitionController() {
        return session.transitionController;
    }

    public RoomTransitionCoordinator simulation_roomTransitionCoordinator() {
        return session.services.roomTransitionCoordinator();
    }

    public boolean simulation_debugNoClip() {
        return session.debug.debugNoClip;
    }

    public int simulation_frameCounter() {
        return session.frameCounter;
    }

    public int simulation_ROOM_PIXEL_HEIGHT() {
        return session.ROOM_PIXEL_HEIGHT;
    }

    public ScrollController simulation_scrollController() {
        return session.scrollController;
    }

    public InputConfig simulation_inputConfig() {
        return session.services.inputConfig();
    }

    public InputState simulation_inputState() {
        return session.services.inputState();
    }

    public PlayerState simulation_playerState() {
        return session.services.playerState();
    }

    public InventoryMenu simulation_inventoryMenu() {
        return session.services.inventoryMenu();
    }

    public InventoryController simulation_inventoryController() {
        return session.services.inventoryController();
    }

    public DialogController simulation_dialogController() {
        return session.services.dialogController();
    }

    public DialogTextLoader simulation_dialogTextLoader() {
        return session.services.dialogTextLoader();
    }

    public GameplaySoundSink simulation_gameplaySoundSink() {
        return session.gameplaySoundSink;
    }

    public FrameMusicPlayer simulation_musicPlayer() {
        return session.services.musicPlayer();
    }

    public GameplayMusicController simulation_gameplayMusicController() {
        return session.services.gameplayMusicController();
    }

    public MusicDirector simulation_musicDirector() {
        return session.musicDirector;
    }

    public TransientVfxSystem simulation_transientVfxSystem() {
        return session.services.transientVfxSystem();
    }

    public DroppableRupeeSystem simulation_droppableRupeeSystem() {
        return session.services.droppableRupeeSystem();
    }

    public ItemRegistry simulation_itemRegistry() {
        return session.services.itemRegistry();
    }

    public EquipmentController simulation_equipmentController() {
        return session.services.equipmentController();
    }

    public Link simulation_link() {
        return session.services.link();
    }

    public OpeningStory simulation_story() {
        return session.openingStory();
    }

    public LiftInteraction simulation_lift() {
        return session.lifting();
    }

    public BushInteraction simulation_bush() {
        return session.bushes();
    }

    public DialogInteractions simulation_dialogInteractions() {
        return session.dialogs();
    }

    private Supplier<OverworldDialogBlockers> simulation_dialogBlockers;

    public Supplier<OverworldDialogBlockers> simulation_dialogBlockers() {
        if (simulation_dialogBlockers == null) simulation_dialogBlockers = session::currentOverworldDialogBlockers;
        return simulation_dialogBlockers;
    }

    private BooleanSupplier simulation_debugScroll;

    public BooleanSupplier simulation_debugScroll() {
        if (simulation_debugScroll == null)
            simulation_debugScroll = () -> session.inputRouter().tryDebugScreenScroll();
        return simulation_debugScroll;
    }

    private IntUnaryOperator simulation_direction;

    public IntUnaryOperator simulation_direction() {
        if (simulation_direction == null) simulation_direction = session::romDirectionForLink;
        return simulation_direction;
    }

    private Runnable simulation_dialogSounds;

    public Runnable simulation_dialogSounds() {
        if (simulation_dialogSounds == null) simulation_dialogSounds = session::routeDialogSounds;
        return simulation_dialogSounds;
    }

    public RoomSession inputRouter_roomSession() {
        return session.services.roomSession();
    }

    public TransitionController inputRouter_transitionController() {
        return session.transitionController;
    }

    public RoomTransitionCoordinator inputRouter_roomTransitionCoordinator() {
        return session.services.roomTransitionCoordinator();
    }

    public int inputRouter_ROOM_PIXEL_WIDTH() {
        return session.ROOM_PIXEL_WIDTH;
    }

    public int inputRouter_ROOM_PIXEL_HEIGHT() {
        return session.ROOM_PIXEL_HEIGHT;
    }

    public ScrollController inputRouter_scrollController() {
        return session.scrollController;
    }

    public InputConfig inputRouter_inputConfig() {
        return session.services.inputConfig();
    }

    public InputState inputRouter_inputState() {
        return session.services.inputState();
    }

    public InventoryController inputRouter_inventoryController() {
        return session.services.inventoryController();
    }

    public DialogController inputRouter_dialogController() {
        return session.services.dialogController();
    }

    public RomTables inputRouter_romTables() {
        return session.services.romTables();
    }

    public Link inputRouter_link() {
        return session.services.link();
    }

    public AppConfig inputRouter_config() {
        return session.currentAppConfig();
    }

    private Runnable inputRouter_stop;

    public Runnable inputRouter_stop() {
        if (inputRouter_stop == null) inputRouter_stop = () -> session.running = false;
        return inputRouter_stop;
    }

    private Runnable inputRouter_toggleDebug;

    public Runnable inputRouter_toggleDebug() {
        if (inputRouter_toggleDebug == null)
            inputRouter_toggleDebug = () -> {
                session.debug.debugNoClip = !session.debug.debugNoClip;
                session.services.link().setDebugNoClip(session.debug.debugNoClip);
                System.out.println("Debug no-clip: " + (session.debug.debugNoClip ? "ON" : "OFF"));
            };
        return inputRouter_toggleDebug;
    }

    private Supplier<Screen> inputRouter_screen;

    public Supplier<Screen> inputRouter_screen() {
        if (inputRouter_screen == null) inputRouter_screen = () -> session.currentScreen;
        return inputRouter_screen;
    }

    private Runnable inputRouter_consumeDialogInput;

    public Runnable inputRouter_consumeDialogInput() {
        if (inputRouter_consumeDialogInput == null)
            inputRouter_consumeDialogInput = () -> session.overworldDialogInputConsumedThisFrame = true;
        return inputRouter_consumeDialogInput;
    }

    private Runnable inputRouter_startDebug;

    public Runnable inputRouter_startDebug() {
        if (inputRouter_startDebug == null)
            inputRouter_startDebug = () -> session.router().startDebugGameplay();
        return inputRouter_startDebug;
    }

    private Supplier<OpeningStory> inputRouter_story;

    public Supplier<OpeningStory> inputRouter_story() {
        if (inputRouter_story == null) inputRouter_story = session::openingStory;
        return inputRouter_story;
    }

    private Runnable inputRouter_fileSave;

    public Runnable inputRouter_fileSave() {
        if (inputRouter_fileSave == null)
            inputRouter_fileSave = () -> session.saves().startFileSave();
        return inputRouter_fileSave;
    }

    private Supplier<DialogInteractions> inputRouter_dialogs;

    public Supplier<DialogInteractions> inputRouter_dialogs() {
        if (inputRouter_dialogs == null) inputRouter_dialogs = session::dialogs;
        return inputRouter_dialogs;
    }

    public GPU router_gpu() {
        return session.gpu;
    }

    public byte[] router_romData() {
        return session.services.romData();
    }

    public RoomSession router_roomSession() {
        return session.services.roomSession();
    }

    public int router_ROOM_PIXEL_WIDTH() {
        return session.ROOM_PIXEL_WIDTH;
    }

    public int router_ROOM_PIXEL_HEIGHT() {
        return session.ROOM_PIXEL_HEIGHT;
    }

    public InputState router_inputState() {
        return session.services.inputState();
    }

    public PlayerState router_playerState() {
        return session.services.playerState();
    }

    public BackgroundSceneLoader router_backgroundSceneLoader() {
        return session.services.backgroundSceneLoader();
    }

    public MusicDirector router_musicDirector() {
        return session.musicDirector;
    }

    public CutsceneManager router_cutsceneManager() {
        return session.services.cutsceneManager();
    }

    public SaveRamStore router_saveRamStore() {
        return session.saveRamStore;
    }

    public Link router_link() {
        return session.services.link();
    }

    public Screen router_titleScreen() {
        return session.titleScreen;
    }

    public Screen router_cutsceneScreen() {
        return session.cutsceneScreen;
    }

    public Screen router_fileMenuScreen() {
        return session.fileMenuScreen;
    }

    public Screen router_gameplayScreen() {
        return session.gameplayScreen;
    }

    public AppConfig router_config() {
        return session.currentAppConfig();
    }

    private Supplier<Screen> router_screen;

    public Supplier<Screen> router_screen() {
        if (router_screen == null) router_screen = () -> session.currentScreen;
        return router_screen;
    }

    private Consumer<Screen> router_showScreen;

    public Consumer<Screen> router_showScreen() {
        if (router_showScreen == null) router_showScreen = value -> session.currentScreen = value;
        return router_showScreen;
    }

    private Consumer<BackgroundScene> router_background;

    public Consumer<BackgroundScene> router_background() {
        if (router_background == null)
            router_background = backgroundValue -> {
                session.scene.currentTilemap = backgroundValue.tilemap();
                session.scene.currentAttrmap = backgroundValue.attrmap();
                session.scene.bgPalettes = backgroundValue.palettes();
                session.scene.objPalettes = backgroundValue.objectPalettes();
            };
        return router_background;
    }

    private Consumer<FileMenuController> router_setFileMenu;

    public Consumer<FileMenuController> router_setFileMenu() {
        if (router_setFileMenu == null) router_setFileMenu = value -> session.screens.fileMenuController = value;
        return router_setFileMenu;
    }

    private Runnable router_clearSaveSlot;

    public Runnable router_clearSaveSlot() {
        if (router_clearSaveSlot == null) router_clearSaveSlot = () -> session.screens.currentSaveSlot = -1;
        return router_clearSaveSlot;
    }

    private Runnable router_resetDebug;

    public Runnable router_resetDebug() {
        if (router_resetDebug == null) router_resetDebug = () -> session.debug.debugNoClip = false;
        return router_resetDebug;
    }

    private Runnable router_startNewGame;

    public Runnable router_startNewGame() {
        if (router_startNewGame == null)
            router_startNewGame = () -> session.saves().startNewGame();
        return router_startNewGame;
    }

    private Runnable router_clearFileMenuController;

    public Runnable router_clearFileMenuController() {
        if (router_clearFileMenuController == null)
            router_clearFileMenuController = () -> session.screens.fileMenuController = null;
        return router_clearFileMenuController;
    }

    private Runnable router_clearFileSaveController;

    public Runnable router_clearFileSaveController() {
        if (router_clearFileSaveController == null)
            router_clearFileSaveController = () -> session.screens.fileSaveController = null;
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
        return session.services.playerState();
    }

    public RoomSession saves_roomSession() {
        return session.services.roomSession();
    }

    public Link saves_link() {
        return session.services.link();
    }

    public GPU saves_gpu() {
        return session.gpu;
    }

    public byte[] saves_romData() {
        return session.services.romData();
    }

    public SaveRamStore saves_saveRamStore() {
        return session.saveRamStore;
    }

    public int saves_currentSaveSlot() {
        return session.screens.currentSaveSlot;
    }

    public InputState saves_inputState() {
        return session.services.inputState();
    }

    private Runnable saves_enterGameplay;

    public Runnable saves_enterGameplay() {
        if (saves_enterGameplay == null)
            saves_enterGameplay = () -> {
                session.screens.fileMenuController = null;
                session.screens.fileSaveController = null;
                session.currentScreen = session.gameplayScreen;
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
            saves_playHouseMusic = () -> session.musicDirector.playDirectMusic(MusicDirector.newGameHouseMusicTrack());
        return saves_playHouseMusic;
    }

    private Runnable saves_refreshOpening;

    public Runnable saves_refreshOpening() {
        if (saves_refreshOpening == null)
            saves_refreshOpening = () -> session.openingStory().updateNewGameMarinPresentation();
        return saves_refreshOpening;
    }

    private Consumer<SaveSlotState> saves_restoreOpening;

    public Consumer<SaveSlotState> saves_restoreOpening() {
        if (saves_restoreOpening == null)
            saves_restoreOpening = saved -> session.openingStory()
                    .restoreNewGameHouseRuntimeIfNeeded(
                            saved,
                            motion -> session.newGameWakeUpMotion = motion,
                            motion -> session.newGameTarinShieldMotion = motion,
                            () -> session.openingStory().updateNewGameMarinPresentation());
        return saves_restoreOpening;
    }

    private Runnable saves_startFileSelection;

    public Runnable saves_startFileSelection() {
        if (saves_startFileSelection == null)
            saves_startFileSelection = () -> session.router().startFileSelection();
        return saves_startFileSelection;
    }

    private Runnable saves_enterFileSave;

    public Runnable saves_enterFileSave() {
        if (saves_enterFileSave == null)
            saves_enterFileSave = () -> {
                session.currentScreen = session.fileSaveScreen;
                session.screens.fileMenuController = null;
            };
        return saves_enterFileSave;
    }

    private Consumer<FileSaveController> saves_showFileSave;

    public Consumer<FileSaveController> saves_showFileSave() {
        if (saves_showFileSave == null)
            saves_showFileSave = controller -> session.screens.fileSaveController = controller;
        return saves_showFileSave;
    }

    private Function<String, BackgroundScene> saves_fileMenuBackground;

    public Function<String, BackgroundScene> saves_fileMenuBackground() {
        if (saves_fileMenuBackground == null)
            saves_fileMenuBackground = label -> session.router().loadFileMenuBackgroundScene(label);
        return saves_fileMenuBackground;
    }
}
