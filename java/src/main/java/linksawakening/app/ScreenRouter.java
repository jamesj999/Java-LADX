package linksawakening.app;

import static linksawakening.world.room.RoomConstants.*;

import java.util.Objects;
import linksawakening.app.screen.Screen;
import linksawakening.config.AppConfig;
import linksawakening.cutscene.CutsceneManager;
import linksawakening.cutscene.IntroCutsceneScript;
import linksawakening.entity.Link;
import linksawakening.gpu.GPU;
import linksawakening.input.InputState;
import linksawakening.rom.Rom;
import linksawakening.save.SaveRamStore;
import linksawakening.scene.BackgroundScene;
import linksawakening.scene.BackgroundSceneCatalog;
import linksawakening.scene.BackgroundSceneLoader;
import linksawakening.scene.BackgroundSceneSpec;
import linksawakening.startup.NewGameStartProfile;
import linksawakening.startup.StartupCoordinator;
import linksawakening.state.PlayerState;
import linksawakening.ui.FileMenuController;
import linksawakening.ui.FileMenuRomData;
import linksawakening.world.room.RoomSession;

public final class ScreenRouter {
    private final SessionContext context;

    public ScreenRouter(SessionContext context) {
        this.context = Objects.requireNonNull(context);
    }

    public GPU gpu() {
        return context.gpu();
    }

    public Rom romData() {
        return context.romData();
    }

    public RoomSession roomSession() {
        return context.roomSession();
    }

    public int ROOM_PIXEL_WIDTH() {
        return context.roomPixelWidth();
    }

    public int ROOM_PIXEL_HEIGHT() {
        return context.roomPixelHeight();
    }

    public InputState inputState() {
        return context.inputState();
    }

    public PlayerState playerState() {
        return context.playerState();
    }

    public BackgroundSceneLoader backgroundSceneLoader() {
        return context.backgroundSceneLoader();
    }

    public MusicDirector musicDirector() {
        return context.musicDirector();
    }

    public CutsceneManager cutsceneManager() {
        return context.cutsceneManager();
    }

    public SaveRamStore saveRamStore() {
        return context.saveRamStore();
    }

    public Link link() {
        return context.link();
    }

    public Screen titleScreen() {
        return context.titleScreen();
    }

    public Screen cutsceneScreen() {
        return context.cutsceneScreen();
    }

    public Screen fileMenuScreen() {
        return context.fileMenuScreen();
    }

    public Screen gameplayScreen() {
        return context.gameplayScreen();
    }

    public AppConfig config() {
        return context.config();
    }

    private void setFileMenuController(FileMenuController controller) {
        context.screens.fileMenuController = controller;
    }

    public void startIntroCutscene() {
        context.screens.fileMenuController = null;
        context.screens.fileSaveController = null;
        context.navigator.show(linksawakening.app.screen.ScreenKind.CUTSCENE);
        musicDirector().playDirectMusic(MusicDirector.introCutsceneMusicTrack());
        gpu().loadIntroSequenceTiles(romData());
        cutsceneManager().startIntro(romData().unsafeBytes(), this::loadIntroBackgroundScene);
    }

    public BackgroundScene loadIntroBackgroundScene(String sceneId) {
        BackgroundSceneSpec spec = BackgroundSceneCatalog.forCutsceneScene(sceneId);
        if (spec == null) {
            throw new IllegalArgumentException("No intro background scene for " + sceneId);
        }
        return backgroundSceneLoader().load(spec);
    }

    public void startTitleScreenWithoutIntro() {
        context.screens.fileMenuController = null;
        context.screens.fileSaveController = null;
        context.navigator.show(linksawakening.app.screen.ScreenKind.TITLE);
        musicDirector().playDirectMusic(MusicDirector.directTitleScreenMusicTrack());
    }

    public void startFileSelection() {
        context.navigator.show(linksawakening.app.screen.ScreenKind.FILE_MENU);
        context.screens.fileSaveController = null;
        gpu().loadMenuTiles(romData());
        setFileMenuController(new FileMenuController(
                new FileMenuRomData(romData().unsafeBytes()),
                this::loadFileMenuBackgroundScene,
                saveRamStore().saveFilesMask(),
                saveRamStore().savedNames(),
                new int[] {
                    saveRamStore().readSlot(0).maxHearts(),
                    saveRamStore().readSlot(1).maxHearts(),
                    saveRamStore().readSlot(2).maxHearts()
                },
                new int[] {
                    saveRamStore().readSlot(0).deathCountBcd(),
                    saveRamStore().readSlot(1).deathCountBcd(),
                    saveRamStore().readSlot(2).deathCountBcd()
                }));
        musicDirector().playDirectMusic(MusicDirector.fileSelectionMusicTrack());
        // The Enter event that leaves the title must not also activate the
        // first empty file. Its edge belongs to the title screen.
        inputState().tickEdges();
    }

    public BackgroundScene loadFileMenuBackgroundScene(String sceneId) {
        BackgroundSceneSpec spec = BackgroundSceneCatalog.forFileMenuScene(sceneId);
        if (spec == null) {
            throw new IllegalArgumentException("No file-menu background scene for " + sceneId);
        }
        return backgroundSceneLoader().load(spec);
    }

    public void setCutsceneScene(String sceneId) {
        if (BackgroundSceneCatalog.requiresTitleTileset(sceneId)) {
            gpu().loadTitleScreenTiles(romData());
        }
        if (IntroCutsceneScript.SCENE_TITLE.equals(sceneId) && context.navigator.current() == cutsceneScreen()) {
            musicDirector().playDirectMusic(MusicDirector.naturalIntroTitleMusicTrack());
        }
        BackgroundSceneSpec spec = BackgroundSceneCatalog.forCutsceneScene(sceneId);
        if (spec != null) {
            applyBackgroundScene(backgroundSceneLoader().load(spec));
        }
    }

    public void applyBackgroundScene(BackgroundScene scene) {
        context.scene.apply(scene);
    }

    public void startConfiguredGameplay() {
        dispatchConfiguredGameplay(
                config(), () -> context.saves().startNewGame(), this::startConfiguredLocationGameplay);
    }

    public void startDebugGameplay() {
        if (link() == null) {
            return;
        }
        if (context.navigator.current() == cutsceneScreen()) {
            cutsceneManager().skipIntroToTitle();
        }
        context.screens.fileMenuController = null;
        context.screens.fileSaveController = null;
        context.openingState().wakeUp(null);
        context.openingState().shield(null);
        context.screens.currentSaveSlot = -1;
        link().resetTransientStateForDebug();
        playerState().initializeDebugState();
        roomSession().story().initializeNewGameWorldState();
        roomSession().story().setBowWowState(0);
        roomSession().story().setBirdKeyOwned(true);
        roomSession().story().setTailKeyOwned(true);
        context.navigator.show(linksawakening.app.screen.ScreenKind.GAMEPLAY);
        context.debug.debugNoClip = false;
        link().setDirection(Link.DIRECTION_DOWN);
        gpu().loadBaseTiles(romData());
        loadOverworldScreen();
        DebugLog.log("Debug gameplay started (F3 toggles no-clip)");
    }

    public void startConfiguredLocationGameplay() {
        context.screens.fileMenuController = null;
        context.screens.fileSaveController = null;
        context.screens.currentSaveSlot = -1;
        context.openingState().wakeUp(null);
        context.openingState().shield(null);
        context.navigator.show(linksawakening.app.screen.ScreenKind.GAMEPLAY);
        applyConfiguredItemProfile(config(), playerState());
        if (config().playIntroStory()) {
            System.err.println("Intro story startup is not implemented yet; spawning at start location");
        }
        loadOverworldScreen();
    }

    public void loadOverworldScreen() {
        roomSession().rooms().loadInitialOverworld(StartupCoordinator.gameplayStartRoomId(config()));
        // Center of Mabe Village Square has the statue (solid). Spawn Link two
        // tiles to the left of the room center so he lands on walkable ground.
        int startX = ROOM_PIXEL_WIDTH() / 2 - Link.SPRITE_SIZE / 2 - 32 + 10;
        int startY = ROOM_PIXEL_HEIGHT() / 2 - Link.SPRITE_SIZE / 2 - 10;
        link().setRoomEntryPixelPosition(startX, startY);
    }

    public static void applyConfiguredItemProfile(AppConfig config, PlayerState playerState) {
        if (config.itemProfile() == AppConfig.ItemProfile.NEW_GAME) {
            NewGameStartProfile.romDefaults().initializePlayerState(playerState);
        }
    }

    public static void dispatchConfiguredGameplay(
            AppConfig config, Runnable startNewGame, Runnable startConfiguredLocation) {
        if (StartupCoordinator.shouldStartNewGameGameplay(config)) {
            startNewGame.run();
        } else {
            startConfiguredLocation.run();
        }
    }
}
