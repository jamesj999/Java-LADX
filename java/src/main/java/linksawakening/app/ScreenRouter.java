package linksawakening.app;

import static linksawakening.world.room.RoomConstants.*;

import java.util.function.Consumer;
import java.util.function.Supplier;
import linksawakening.app.screen.Screen;
import linksawakening.config.AppConfig;
import linksawakening.cutscene.CutsceneManager;
import linksawakening.cutscene.IntroCutsceneScript;
import linksawakening.entity.Link;
import linksawakening.gpu.GPU;
import linksawakening.input.InputState;
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

public record ScreenRouter(
        GPU gpu,
        byte[] romData,
        RoomSession roomSession,
        int ROOM_PIXEL_WIDTH,
        int ROOM_PIXEL_HEIGHT,
        InputState inputState,
        PlayerState playerState,
        BackgroundSceneLoader backgroundSceneLoader,
        MusicDirector musicDirector,
        CutsceneManager cutsceneManager,
        SaveRamStore saveRamStore,
        Link link,
        Screen titleScreen,
        Screen cutsceneScreen,
        Screen fileMenuScreen,
        Screen gameplayScreen,
        AppConfig config,
        Supplier<Screen> screen,
        Consumer<Screen> showScreen,
        Consumer<BackgroundScene> background,
        Consumer<FileMenuController> setFileMenu,
        Runnable clearSaveSlot,
        Runnable resetDebug,
        Runnable startNewGame,
        Runnable clearFileMenuController,
        Runnable clearFileSaveController,
        Runnable clearNewGameWakeUpMotion,
        Runnable clearNewGameTarinShieldMotion) {
    public void startIntroCutscene() {
        clearFileMenuController.run();
        clearFileSaveController.run();
        showScreen.accept(cutsceneScreen);
        musicDirector.playDirectMusic(MusicDirector.introCutsceneMusicTrack());
        gpu.loadIntroSequenceTiles(romData);
        cutsceneManager.startIntro(romData, this::loadIntroBackgroundScene);
    }

    public BackgroundScene loadIntroBackgroundScene(String sceneId) {
        BackgroundSceneSpec spec = BackgroundSceneCatalog.forCutsceneScene(sceneId);
        if (spec == null) {
            throw new IllegalArgumentException("No intro background scene for " + sceneId);
        }
        return backgroundSceneLoader.load(spec);
    }

    public void startTitleScreenWithoutIntro() {
        clearFileMenuController.run();
        clearFileSaveController.run();
        showScreen.accept(titleScreen);
        musicDirector.playDirectMusic(MusicDirector.directTitleScreenMusicTrack());
    }

    public void startFileSelection() {
        showScreen.accept(fileMenuScreen);
        clearFileSaveController.run();
        gpu.loadMenuTiles(romData);
        setFileMenu.accept(new FileMenuController(
                new FileMenuRomData(romData),
                this::loadFileMenuBackgroundScene,
                saveRamStore.saveFilesMask(),
                saveRamStore.savedNames(),
                new int[] {
                    saveRamStore.readSlot(0).maxHearts(),
                    saveRamStore.readSlot(1).maxHearts(),
                    saveRamStore.readSlot(2).maxHearts()
                },
                new int[] {
                    saveRamStore.readSlot(0).deathCountBcd(),
                    saveRamStore.readSlot(1).deathCountBcd(),
                    saveRamStore.readSlot(2).deathCountBcd()
                }));
        musicDirector.playDirectMusic(MusicDirector.fileSelectionMusicTrack());
        // The Enter event that leaves the title must not also activate the
        // first empty file. Its edge belongs to the title screen.
        inputState.tickEdges();
    }

    public BackgroundScene loadFileMenuBackgroundScene(String sceneId) {
        BackgroundSceneSpec spec = BackgroundSceneCatalog.forFileMenuScene(sceneId);
        if (spec == null) {
            throw new IllegalArgumentException("No file-menu background scene for " + sceneId);
        }
        return backgroundSceneLoader.load(spec);
    }

    public void setCutsceneScene(String sceneId) {
        if (BackgroundSceneCatalog.requiresTitleTileset(sceneId)) {
            gpu.loadTitleScreenTiles(romData);
        }
        if (IntroCutsceneScript.SCENE_TITLE.equals(sceneId) && screen.get() == cutsceneScreen) {
            musicDirector.playDirectMusic(MusicDirector.naturalIntroTitleMusicTrack());
        }
        BackgroundSceneSpec spec = BackgroundSceneCatalog.forCutsceneScene(sceneId);
        if (spec != null) {
            applyBackgroundScene(backgroundSceneLoader.load(spec));
        }
    }

    public void applyBackgroundScene(BackgroundScene scene) {
        background.accept(scene);
    }

    public void startConfiguredGameplay() {
        dispatchConfiguredGameplay(config, () -> startNewGame.run(), this::startConfiguredLocationGameplay);
    }

    public void startDebugGameplay() {
        if (playerState == null || roomSession == null || link == null) {
            return;
        }
        if (screen.get() == cutsceneScreen && cutsceneManager != null) {
            cutsceneManager.skipIntroToTitle();
        }
        clearFileMenuController.run();
        clearFileSaveController.run();
        clearNewGameWakeUpMotion.run();
        clearNewGameTarinShieldMotion.run();
        clearSaveSlot.run();
        link.resetTransientStateForDebug();
        playerState.initializeDebugState();
        roomSession.initializeNewGameWorldState();
        roomSession.setBowWowState(0);
        roomSession.setBirdKeyOwned(true);
        roomSession.setTailKeyOwned(true);
        showScreen.accept(gameplayScreen);
        resetDebug.run();
        link.setDirection(Link.DIRECTION_DOWN);
        gpu.loadBaseTiles(romData);
        loadOverworldScreen();
        System.out.println("Debug gameplay started (F3 toggles no-clip)");
    }

    public void startConfiguredLocationGameplay() {
        clearFileMenuController.run();
        clearFileSaveController.run();
        clearSaveSlot.run();
        clearNewGameWakeUpMotion.run();
        clearNewGameTarinShieldMotion.run();
        showScreen.accept(gameplayScreen);
        applyConfiguredItemProfile(config, playerState);
        if (config.playIntroStory()) {
            System.err.println("Intro story startup is not implemented yet; spawning at start location");
        }
        loadOverworldScreen();
    }

    public void loadOverworldScreen() {
        roomSession.loadInitialOverworld(StartupCoordinator.gameplayStartRoomId(config));
        // Center of Mabe Village Square has the statue (solid). Spawn Link two
        // tiles to the left of the room center so he lands on walkable ground.
        int startX = ROOM_PIXEL_WIDTH / 2 - Link.SPRITE_SIZE / 2 - 32 + 10;
        int startY = ROOM_PIXEL_HEIGHT / 2 - Link.SPRITE_SIZE / 2 - 10;
        link.setRoomEntryPixelPosition(startX, startY);
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
