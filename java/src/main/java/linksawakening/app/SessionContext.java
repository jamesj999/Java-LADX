package linksawakening.app;

import static linksawakening.world.room.RoomConstants.*;

import java.util.Objects;
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
import linksawakening.gameplay.OverworldDialogInteraction;
import linksawakening.gpu.GPU;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.render.GameFrameSceneBuilder;
import linksawakening.rom.Rom;
import linksawakening.rom.RomTables;
import linksawakening.save.SaveRamStore;
import linksawakening.scene.BackgroundSceneLoader;
import linksawakening.startup.MarinWakeUpMotion;
import linksawakening.startup.TarinShieldMotion;
import linksawakening.state.PlayerState;
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
    SessionServices services() {
        return services;
    }

    private final GameSession session;
    final SessionNavigator navigator;
    final linksawakening.app.screen.ScreenSet screenSet;
    private final GameplayInputState gameplayInput = new GameplayInputState();
    private final linksawakening.app.gameplay.DialogBlockerPolicy dialogPolicy;
    private final FrameInputCompletion inputCompletion;

    public GameplayInputState gameplayInput() {
        return gameplayInput;
    }

    private final SessionDialogSounds dialogSoundRouting;

    public SessionContext(GameSession session, boolean headless) {
        this.session = Objects.requireNonNull(session);
        navigator = new SessionNavigator(screens);

        host = headless
                ? (session.headlessPcm == null ? new HeadlessHost() : new HeadlessHost(session.headlessPcm))
                : new DesktopHost();
        gpu = new GPU();
        var built = SessionWiring.build(
                session.random, gpu, transitionController, scrollController, navigator, host, headless);
        services = built.services();
        session.gameplaySounds().replace(services.audio().gameplaySoundSink());
        scene = built.scene();
        appConfig = services.rom().appConfig();
        musicDirector = new MusicDirector(
                services().audio().musicPlayer(), services().audio().gameplayMusicController());
        dialogSoundRouting = new SessionDialogSounds(
                services.ui().dialogController(), services.audio().dialogSoundSink());
        dialogPolicy = new linksawakening.app.gameplay.DialogBlockerPolicy(
                services.ui().inventoryController(),
                services.ui().dialogController(),
                scrollController,
                transitionController,
                gameplayInput);
        inputCompletion = new FrameInputCompletion(services.input().inputState(), gameplayInput);

        saveState = built.saves();
        newGameTarinShieldPalette = services.visual().newGameTarinShieldPalette();

        liftingInstance = new LiftInteraction(this);
        dialogsInstance = new DialogInteractions(this);
        bushesInstance = new BushInteraction(this);
        openingStoryInstance = new OpeningStory(this);
        rendererInstance = new RenderStateAssembler(this);
        savesInstance = new SaveCoordinator(this);
        gameOverFlow = new linksawakening.app.gameplay.GameOverFlow(
                services().player().equipmentController(),
                passOutController,
                savesInstance,
                services().visual().transientVfxSystem(),
                services().visual().droppableRupeeSystem());
        routerInstance = new ScreenRouter(this);
        noClip = new NoClipControl(debug, services().player().link());
        inputRouterInstance = new InputRouter(this);
        simulationInstance = new GameplaySimulation(this);
        gameplayFrame = new GameplayFrame(
                new TransitionPhase(
                        scrollController,
                        transitionController,
                        services().player().link(),
                        scrollSpeed),
                simulationInstance,
                inputCompletion);
        screenSet = ScreenWiring.build(
                this,
                navigator,
                savesInstance,
                inputCompletion,
                new GameplayScreenAdapter(inputRouterInstance, gameplayFrame, passOutController));
        navigator.attach(
                screenSet,
                routerInstance,
                new RoomMusicSelection(
                        musicDirector,
                        services().player().playerState(),
                        services().world().roomSession()));
        screens.currentScreen = screenSet.title();
        teleporter = new DebugTeleporter(
                services().world().roomSession(),
                services().player().link(),
                session.openingState(),
                debug,
                services().visual().transientVfxSystem(),
                services().visual().droppableRupeeSystem(),
                services().input().inputState(),
                screenSet.gameplay());
    }

    public PassOutController passOutController() {
        return passOutController;
    }

    private final linksawakening.app.gameplay.GameOverFlow gameOverFlow;

    public linksawakening.app.gameplay.GameOverActions gameOverOutcome() {
        return gameOverFlow;
    }

    public GPU gpu() {
        return gpu;
    }

    public Rom romData() {
        return services().rom().romData();
    }

    public RoomSession roomSession() {
        return services().world().roomSession();
    }

    public TransitionController transitionController() {
        return transitionController;
    }

    public RoomTransitionCoordinator roomTransitionCoordinator() {
        return services().world().roomTransitionCoordinator();
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
        return services().input().inputConfig();
    }

    public InputState inputState() {
        return services().input().inputState();
    }

    public PlayerState playerState() {
        return services().player().playerState();
    }

    public InventoryMenu inventoryMenu() {
        return services().ui().inventoryMenu();
    }

    public InventoryController inventoryController() {
        return services().ui().inventoryController();
    }

    public DialogController dialogController() {
        return services().ui().dialogController();
    }

    public DialogTextLoader dialogTextLoader() {
        return services().ui().dialogTextLoader();
    }

    public GameplaySoundSink gameplaySoundSink() {
        return session.gameplaySounds().sink();
    }

    public FrameMusicPlayer musicPlayer() {
        return services().audio().musicPlayer();
    }

    public GameplayMusicController gameplayMusicController() {
        return services().audio().gameplayMusicController();
    }

    public MusicDirector musicDirector() {
        return musicDirector;
    }

    public TransientVfxSystem transientVfxSystem() {
        return services().visual().transientVfxSystem();
    }

    public DroppableRupeeSystem droppableRupeeSystem() {
        return services().visual().droppableRupeeSystem();
    }

    public ItemRegistry itemRegistry() {
        return services().player().itemRegistry();
    }

    public EquipmentController equipmentController() {
        return services().player().equipmentController();
    }

    public Link link() {
        return services().player().link();
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

    public linksawakening.app.gameplay.DebugScroll simulationDebugScroll() {
        return inputRouterInstance;
    }

    public linksawakening.app.gameplay.DialogSoundRouting dialogSounds() {
        return dialogSoundRouting;
    }

    public int roomPixelWidth() {
        return roomPixelWidth;
    }

    public RomTables romTables() {
        return services().rom().romTables();
    }

    public AppConfig config() {
        return currentAppConfig();
    }

    public linksawakening.app.screen.SessionLifecycle stop() {
        return lifecycle;
    }

    public DebugActions inputRouterToggleDebug() {
        return noClip;
    }

    public BackgroundSceneLoader backgroundSceneLoader() {
        return services().rom().backgroundSceneLoader();
    }

    public CutsceneManager cutsceneManager() {
        return services().ui().cutsceneManager();
    }

    public SaveRamStore saveRamStore() {
        return saveState.saveRamStore;
    }

    public Screen titleScreen() {
        return screenSet.title();
    }

    public Screen cutsceneScreen() {
        return screenSet.cutscene();
    }

    public Screen fileMenuScreen() {
        return screenSet.fileMenu();
    }

    public Screen gameplayScreen() {
        return screenSet.gameplay();
    }

    public int currentSaveSlot() {
        return screens.currentSaveSlot;
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
        return services().visual().indexedDisplayBuffer();
    }

    public int[] greenObjectsSpritePalette() {
        return greenObjectsSpritePalette;
    }

    public CutLeavesEffectRenderer cutLeavesEffectRenderer() {
        return services().visual().cutLeavesEffectRenderer();
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
        return services().world().overworldBushInteraction();
    }

    public OverworldDialogInteraction overworldDialogInteraction() {
        return services().ui().overworldDialogInteraction();
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
    final SceneState scene;

    final SessionServices services;

    Host host = new HeadlessHost();
    GPU gpu;
    final GameFrameSceneBuilder frameSceneBuilder = new GameFrameSceneBuilder();
    final TransitionController transitionController = new TransitionController();

    final RunState lifecycle = new RunState();
    private final NoClipControl noClip;
    final DebugTeleporter teleporter;

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
    final SaveState saveState;
    int[] newGameTarinShieldPalette;
    AppConfig appConfig;

    final PassOutController passOutController = new PassOutController();

    void composeFrameBuffer() {
        renderer().composeFrameBuffer();
        if (debug.debugMenu != null && debug.debugMenu.isOpen()) {
            if (debug.debugMenuOverlay == null) debug.debugMenuOverlay = new DebugMenuOverlay();
            debug.debugMenuOverlay.render(debug.debugMenu, services().visual().indexedDisplayBuffer());
        }
    }

    void onKeyEvent(int key, int action) {
        if (debug.debugMenu != null && debug.debugMenu.isOpen()) {
            debug.debugMenu.key(key, action);
            services().input().inputState().clear();
            screenSet.gameplay().clearPendingInput();
            return;
        }
        if (currentAppConfig().debugEnabled() && DebugMenu.isToggle(key, action)) {
            if (screens.currentScreen == screenSet.gameplay()
                    && inputRouter().canPresentFileSave()
                    && !services().world().roomSession().doors().tailCaveKeyholeSequenceActive()) {
                if (debug.debugMenu == null)
                    debug.debugMenu = new DebugMenu(
                            services().player().playerState(),
                            services().world().roomSession(),
                            teleporter);
                debug.debugMenu.open(
                        services().world().roomSession().rooms().mapCategory(),
                        services().world().roomSession().rooms().mapId(),
                        services().world().roomSession().rooms().currentRoomId(),
                        services().player().link().pixelX(),
                        services().player().link().pixelY());
                services().input().inputState().clear();
                screenSet.gameplay().clearPendingInput();
            }
            return;
        }
        inputRouter().onKeyEvent(key, action);
    }

    void debugTeleport(Warp destination) {
        teleporter.teleport(destination);
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
