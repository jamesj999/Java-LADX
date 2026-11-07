package linksawakening.app;



import linksawakening.app.host.HeadlessHost;
import linksawakening.app.host.Host;
import linksawakening.app.host.DesktopHost;
import linksawakening.app.gameplay.BushInteraction;
import linksawakening.app.gameplay.GameplayFrame;
import linksawakening.app.gameplay.DialogInteractions;
import linksawakening.app.gameplay.GameplaySimulation;
import linksawakening.app.gameplay.LiftInteraction;
import linksawakening.app.gameplay.TransitionPhase;

import linksawakening.app.screen.FileMenuScreen;
import linksawakening.app.screen.TitleScreen;
import linksawakening.app.screen.CutsceneScreen;
import linksawakening.app.screen.FileSaveScreen;
import linksawakening.app.screen.GameplayScreen;
import linksawakening.app.screen.Screen;
import linksawakening.app.SessionBootstrap;
import linksawakening.app.SaveCoordinator;
import linksawakening.app.MusicDirector;
import linksawakening.app.story.OpeningStory;

import linksawakening.config.AppConfig;
import linksawakening.cutscene.CutsceneManager;
import linksawakening.dialog.DialogController;
import linksawakening.entity.Link;
import linksawakening.entity.LinkSpriteSheet;
import linksawakening.equipment.EquipmentController;
import linksawakening.equipment.ItemRegistry;
import linksawakening.audio.music.GameplayMusicController;
import linksawakening.audio.music.FrameMusicPlayer;
import linksawakening.gameplay.DialogTextLoader;
import linksawakening.gameplay.DialogSoundRouter;
import linksawakening.gameplay.DialogSoundSink;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.gameplay.GameplayDialogInput;
import linksawakening.gameplay.OverworldDialogBlockers;
import linksawakening.gameplay.OverworldDialogInteraction;
import linksawakening.gameplay.PcmSoundOutput;
import linksawakening.gpu.GPU;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.render.GameFrameSceneBuilder;
import linksawakening.rom.RomTables;
import linksawakening.save.SaveRamStore;
import linksawakening.scene.BackgroundSceneLoader;
import linksawakening.state.PlayerState;
import linksawakening.startup.MarinWakeUpMotion;
import linksawakening.startup.StartupCoordinator;
import linksawakening.startup.TarinShieldMotion;
import linksawakening.ui.InventoryMenu;
import linksawakening.ui.InventoryController;
import linksawakening.ui.FileMenuController;
import linksawakening.ui.FileSaveController;
import linksawakening.vfx.CutLeavesEffectRenderer;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.world.room.DroppableRupeeSystem;
import linksawakening.world.object.OverworldBushInteraction;
import linksawakening.world.room.RoomSession;
import linksawakening.world.room.RoomTransitionCoordinator;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;

import linksawakening.world.entity.RomRandomByteSource;

import static linksawakening.world.room.RoomConstants.*;

public class GameSession {

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

    private Host host = new HeadlessHost();
    private GPU gpu;
    private final GameFrameSceneBuilder frameSceneBuilder = new GameFrameSceneBuilder();
    private byte[] romData;
    private int[] currentTilemap;
    private int[] currentAttrmap;
    private int[][] bgPalettes;
    private int[][] objPalettes;
    private byte[] indexedDisplayBuffer;
    private RoomSession roomSession;
    private final TransitionController transitionController = new TransitionController();
    private RoomTransitionCoordinator roomTransitionCoordinator;

    private linksawakening.app.debug.DebugMenu debugMenu;
    private linksawakening.app.debug.DebugMenuOverlay debugMenuOverlay;
    private boolean running = true;
    private boolean debugNoClip;
    private int frameCounter;

    // Room pixel dimensions
    private final int ROOM_PIXEL_WIDTH = ROOM_TILE_WIDTH * 8;   // 160
    private final int ROOM_PIXEL_HEIGHT = ROOM_TILE_HEIGHT * 8; // 128

    // Scroll state
    private final int SCROLL_SPEED = 4; // pixels per frame (original uses ~2-4px)

    private final ScrollController scrollController = new ScrollController();
    // Mirrors the first OBJ palette entry used for Link and leaf particles.
    private final int[] GREEN_OBJECTS_SPRITE_PALETTE = {
        0x00000000,
        0x00000000,
        0x0010A840,
        0x00F8B888,
    };

    private InputConfig inputConfig;
    private InputState inputState;
    private PlayerState playerState;
    private BackgroundSceneLoader backgroundSceneLoader;
    private InventoryMenu inventoryMenu;
    private InventoryController inventoryController;
    private DialogController dialogController;
    private DialogTextLoader dialogTextLoader;
    private OverworldDialogInteraction overworldDialogInteraction;
    private boolean overworldDialogInputConsumedThisFrame;
    private DialogSoundSink dialogSoundSink;
    private PcmSoundOutput dialogSoundOutput;
    private GameplaySoundSink gameplaySoundSink = GameplaySoundSink.none();
    private FrameMusicPlayer musicPlayer;
    private GameplayMusicController gameplayMusicController;
    private MusicDirector musicDirector;
    private CutsceneManager cutsceneManager;
    private FileMenuController fileMenuController;
    private FileSaveController fileSaveController;
    private SaveRamStore saveRamStore;
    private int currentSaveSlot = -1;
    private RomTables romTables;
    private OverworldCollision overworldCollision;
    private OverworldBushInteraction overworldBushInteraction;
    private LinkSpriteSheet linkSpriteSheet;
    private TransientVfxSystem transientVfxSystem;
    private DroppableRupeeSystem droppableRupeeSystem;
    private CutLeavesEffectRenderer cutLeavesEffectRenderer;
    private ItemRegistry itemRegistry;
    private EquipmentController equipmentController;
    private Link link;
    private MarinWakeUpMotion newGameWakeUpMotion;
    private TarinShieldMotion newGameTarinShieldMotion;
    private int[] newGameTarinShieldPalette;
    private AppConfig appConfig;
    private final Screen titleScreen = new TitleScreen(new TitleScreen.Dependencies(() -> cutsceneManager, () -> inputState, () -> router().startFileSelection()));
    private final Screen cutsceneScreen = new CutsceneScreen(new CutsceneScreen.Dependencies(() -> cutsceneManager, () -> inputState, () -> dialogController, () -> inputConfig, this::routeDialogSounds, () -> currentScreen = titleScreen, () -> { currentScreen = titleScreen; musicDirector.playDirectMusic(MusicDirector.directTitleScreenMusicTrack()); }));
    private final Screen fileMenuScreen = new FileMenuScreen(new FileMenuScreen.Dependencies(() -> fileMenuController, () -> inputState, () -> inputConfig, () -> saveRamStore, slot -> currentSaveSlot = slot, () -> saves().startNewGame(), saved -> saves().startSavedGame(saved), mutation -> saves().persistFileMenuMutation(mutation), this::completeInputFrame));
    private final Screen fileSaveScreen = new FileSaveScreen(new FileSaveScreen.Dependencies(() -> fileSaveController, () -> inputState, () -> inputConfig, () -> { fileSaveController = null; currentScreen = this.gameplayScreen; }, () -> saves().saveCurrentPlayerState(), () -> router().startFileSelection(), this::completeInputFrame));
    private final Screen gameplayScreen = new GameplayScreen(this::tickGameplayScreen, (key, action) -> inputRouter().gameplayKey(key, action));
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
            host.present(indexedDisplayBuffer);

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
        if (inputState != null) throw new IllegalStateException("Game already initialized");
        initialize(true);
        dialogController.setPlayerName("A");
        saves().startNewGame();
    }

    /** Starts the production reset intro without creating a native window. */
    public void initializeHeadlessIntro() {
        if (inputState != null) throw new IllegalStateException("Game already initialized");
        initialize(true);
        router().startIntroCutscene();
    }

    public void headlessButton(String button, boolean pressed) { inputRouter().headlessButton(button, pressed); }

    public byte[] headlessFrame() {
        update();
        composeFrameBuffer();
        return indexedDisplayBuffer.clone();
    }

    private void composeFrameBuffer() {
        renderer().composeFrameBuffer();
        if (debugMenu != null && debugMenu.isOpen()) {
            if (debugMenuOverlay == null) debugMenuOverlay = new linksawakening.app.debug.DebugMenuOverlay();
            debugMenuOverlay.render(debugMenu, indexedDisplayBuffer);
        }
    }

    void onKeyEvent(int key, int action) {
        if (debugMenu != null && debugMenu.isOpen()) {
            debugMenu.key(key, action);
            inputState.clear();
            return;
        }
        if (currentAppConfig().debugEnabled()
            && linksawakening.app.debug.DebugMenu.isToggle(key, action)) {
            if (currentScreen == gameplayScreen && inputRouter().canPresentFileSave()
                && !roomSession.tailCaveKeyholeSequenceActive()) {
                if (debugMenu == null) debugMenu = new linksawakening.app.debug.DebugMenu(
                    playerState, roomSession, this::debugTeleport);
                debugMenu.open(roomSession.mapCategory(), roomSession.mapId(),
                    roomSession.currentRoomId(), link.pixelX(), link.pixelY());
                inputState.clear();
            }
            return;
        }
        inputRouter().onKeyEvent(key, action);
    }

    private void debugTeleport(linksawakening.world.transition.Warp destination) {
        roomSession.loadWarpDestination(destination);
        newGameWakeUpMotion = null;
        newGameTarinShieldMotion = null;
        link.resetTransientStateForDebug();
        link.setDebugNoClip(debugNoClip);
        link.setSideScrolling(destination.category() == linksawakening.world.transition.Warp.CATEGORY_SIDESCROLL);
        link.setRoomEntryPixelPosition(destination.javaPixelX(), destination.javaPixelY());
        transientVfxSystem.clear();
        droppableRupeeSystem.clear();
        inputState.clear();
    }

    private void update() {
        if (debugMenu != null && debugMenu.isOpen()) return;
        frameCounter = (frameCounter + 1) & 0xFF;
        // The engine's frame boundary uses scanline zero for non-entity draws.
        random.beginFrame(frameCounter, 0);

        musicDirector.tick();

        currentScreen.tick();
    }

    private void tickGameplayScreen() {
        new GameplayFrame(
            new TransitionPhase(scrollController, transitionController, link, SCROLL_SPEED),
            () -> simulation().tick(),
            this::completeInputFrame).tick();
    }

    private void completeInputFrame() {
        // Run last so all consumers observe this frame's input edges.
        inputState.tickEdges();
        overworldDialogInputConsumedThisFrame = false;
    }

    private void routeDialogSounds() {
        DialogSoundRouter.routePending(dialogController, dialogSoundSink);
    }

    private int romDirectionForLink(int javaDirection) {
        return switch (javaDirection) {
            case Link.DIRECTION_RIGHT -> 0;
            case Link.DIRECTION_LEFT -> 1;
            case Link.DIRECTION_UP -> 2;
            case Link.DIRECTION_DOWN -> 3;
            default -> throw new IllegalArgumentException("Link direction out of range: "
                + javaDirection);
        };
    }

    private OverworldDialogBlockers currentOverworldDialogBlockers() {
        return new OverworldDialogBlockers(
            inventoryController != null && inventoryController.shouldBlockOverworldInput(),
            scrollController.isActive(),
            transitionController.isInputBlocked(),
            GameplayDialogInput.blocksGameplay(dialogController),
            overworldDialogInputConsumedThisFrame);
    }

    private AppConfig currentAppConfig() {
        return appConfig != null ? appConfig : AppConfig.defaults();
    }

    /** Applies the configured player profile before a direct gameplay launch. */

    /** Mirrors the source's wSpawnLocationData fields copied by SaveGameToFile. */

    private void cleanup() {
        host.releasePresentation();
        if (dialogSoundOutput != null) {
            dialogSoundOutput.close();
        }
        if (musicPlayer != null) {
            musicPlayer.close();
        }

        host.close();
    }

    private OpeningStory openingStory() {
        return new OpeningStory(frameCounter, newGameWakeUpMotion, newGameTarinShieldMotion, newGameTarinShieldPalette, inputState, inputConfig, dialogController, dialogTextLoader, roomSession, link, playerState,
            this::currentOverworldDialogBlockers, musicDirector::playDirectMusic,
            MusicDirector::newGameHouseMusicTrack, this::romDirectionForLink);
    }
    private void initialize(boolean headless) {
        host = headless ? (headlessPcm == null ? new HeadlessHost() : new HeadlessHost(headlessPcm)) : new DesktopHost();
        gpu = new GPU();
        var built = new SessionBootstrap(random, gpu, transitionController, scrollController,
            new SessionBootstrap.Callbacks((scene) -> router().setCutsceneScene(scene), room -> musicDirector.selectMusicForLoadedRoom(room, playerState, roomSession),
                this::romDirectionForLink), host).build(headless);
        bgPalettes = built.bgPalettes();
        objPalettes = built.objPalettes();
        appConfig = built.appConfig();
        romData = built.romData();
        currentTilemap = built.currentTilemap();
        currentAttrmap = built.currentAttrmap();
        indexedDisplayBuffer = built.indexedDisplayBuffer();
        roomSession = built.roomSession();
        roomTransitionCoordinator = built.roomTransitionCoordinator();
        inputConfig = built.inputConfig();
        inputState = built.inputState();
        playerState = built.playerState();
        backgroundSceneLoader = built.backgroundSceneLoader();
        inventoryMenu = built.inventoryMenu();
        inventoryController = built.inventoryController();
        dialogController = built.dialogController();
        dialogTextLoader = built.dialogTextLoader();
        overworldDialogInteraction = built.overworldDialogInteraction();
        dialogSoundSink = built.dialogSoundSink();
        dialogSoundOutput = built.dialogSoundOutput();
        gameplaySoundSink = built.gameplaySoundSink();
        musicPlayer = built.musicPlayer();
        gameplayMusicController = built.gameplayMusicController();
        musicDirector = new MusicDirector(musicPlayer, gameplayMusicController);
        cutsceneManager = built.cutsceneManager();
        saveRamStore = built.saveRamStore();
        romTables = built.romTables();
        overworldCollision = built.overworldCollision();
        overworldBushInteraction = built.overworldBushInteraction();
        linkSpriteSheet = built.linkSpriteSheet();
        transientVfxSystem = built.transientVfxSystem();
        droppableRupeeSystem = built.droppableRupeeSystem();
        cutLeavesEffectRenderer = built.cutLeavesEffectRenderer();
        itemRegistry = built.itemRegistry();
        equipmentController = built.equipmentController();
        link = built.link();
        newGameTarinShieldPalette = built.newGameTarinShieldPalette();
    }
    private SaveCoordinator saves() {
        return new SaveCoordinator(playerState, roomSession, link, gpu, romData, saveRamStore, currentSaveSlot, inputState,
            () -> { fileMenuController = null; fileSaveController = null; currentScreen = gameplayScreen; },
            () -> { newGameWakeUpMotion = null; newGameTarinShieldMotion = null; },
            () -> { newGameWakeUpMotion = new MarinWakeUpMotion(); newGameTarinShieldMotion = new TarinShieldMotion(); },
            () -> musicDirector.playDirectMusic(MusicDirector.newGameHouseMusicTrack()),
            () -> openingStory().updateNewGameMarinPresentation(),
            saved -> openingStory().restoreNewGameHouseRuntimeIfNeeded(saved, motion -> newGameWakeUpMotion = motion, motion -> newGameTarinShieldMotion = motion, () -> openingStory().updateNewGameMarinPresentation()),
            () -> router().startFileSelection(),
            () -> { currentScreen = fileSaveScreen; fileMenuController = null; },
            controller -> fileSaveController = controller,
            router()::loadFileMenuBackgroundScene);
    }
    private BushInteraction bushes() {
        return new BushInteraction(roomSession, overworldBushInteraction, link, equipmentController, gameplaySoundSink, transientVfxSystem, droppableRupeeSystem);
    }
    private DialogInteractions dialogs() {
        return new DialogInteractions(inputConfig, overworldDialogInteraction, roomSession, link, playerState, dialogController, dialogTextLoader, this::currentOverworldDialogBlockers);
    }
    private LiftInteraction lifting() {
        return new LiftInteraction(inputState, inputConfig, playerState, link, roomSession, this::romDirectionForLink);
    }
    private GameplaySimulation simulation() {
        return new GameplaySimulation(gpu, romData, roomSession, transitionController, roomTransitionCoordinator, debugNoClip, frameCounter, ROOM_PIXEL_HEIGHT, scrollController, inputConfig, inputState, playerState, inventoryMenu, inventoryController, dialogController, dialogTextLoader, gameplaySoundSink, musicPlayer, gameplayMusicController, musicDirector, transientVfxSystem, droppableRupeeSystem, itemRegistry, equipmentController, link,
            openingStory(), lifting(), bushes(), dialogs(), this::currentOverworldDialogBlockers, () -> inputRouter().tryDebugScreenScroll(), this::romDirectionForLink, this::routeDialogSounds);
    }
    public HeadlessGameDriver.Diagnostics diagnostics() {
        return new HeadlessGameDriver.Diagnostics(gpu, roomSession, transitionController, frameCounter, scrollController, playerState, inventoryMenu, dialogController, equipmentController, link);
    }
    private RenderStateAssembler renderer() {
        return new RenderStateAssembler(gpu, frameSceneBuilder, currentTilemap, currentAttrmap, bgPalettes, objPalettes, indexedDisplayBuffer, roomSession, transitionController, frameCounter, scrollController, GREEN_OBJECTS_SPRITE_PALETTE, inventoryMenu, inventoryController, dialogController, cutsceneManager, transientVfxSystem, droppableRupeeSystem, cutLeavesEffectRenderer, link, currentScreen);
    }
    private InputRouter inputRouter() {
        return new InputRouter(roomSession, transitionController, roomTransitionCoordinator, ROOM_PIXEL_WIDTH, ROOM_PIXEL_HEIGHT, scrollController, inputConfig, inputState, inventoryController, dialogController, romTables, link, currentAppConfig(), () -> running = false, () -> { debugNoClip = !debugNoClip; link.setDebugNoClip(debugNoClip); System.out.println("Debug no-clip: " + (debugNoClip ? "ON" : "OFF")); }, () -> currentScreen, () -> overworldDialogInputConsumedThisFrame = true, () -> router().startDebugGameplay(), this::openingStory, () -> saves().startFileSave(), this::dialogs);
    }
    private ScreenRouter router() {
        return new ScreenRouter(gpu, romData, roomSession, ROOM_PIXEL_WIDTH, ROOM_PIXEL_HEIGHT, inputState, playerState, backgroundSceneLoader, musicDirector, cutsceneManager, saveRamStore, link, titleScreen, cutsceneScreen, fileMenuScreen, gameplayScreen, currentAppConfig(), () -> currentScreen, value -> currentScreen = value, scene -> { currentTilemap = scene.tilemap(); currentAttrmap = scene.attrmap(); bgPalettes = scene.palettes(); objPalettes = scene.objectPalettes(); }, value -> fileMenuController = value, () -> currentSaveSlot = -1, () -> debugNoClip = false, () -> saves().startNewGame(), () -> fileMenuController = null, () -> fileSaveController = null, () -> newGameWakeUpMotion = null, () -> newGameTarinShieldMotion = null);
    }
}
