package linksawakening.app;

import static linksawakening.world.room.RoomConstants.*;

import java.util.Objects;
import linksawakening.app.debug.*;
import linksawakening.app.gameplay.*;
import linksawakening.app.host.*;
import linksawakening.app.screen.*;
import linksawakening.app.screen.Screen;
import linksawakening.app.story.*;
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
import linksawakening.world.transition.*;

public final class ScreenRouter {
    private final RomServices rom;
    private final WorldServices world;
    private final PlayerServices player;
    private final UiServices ui;
    private final InputServices input;
    private final SessionState state;
    private final ScreenActions actions;

    public ScreenRouter(
            RomServices rom,
            WorldServices world,
            PlayerServices player,
            UiServices ui,
            InputServices input,
            SessionState state,
            ScreenActions actions) {
        this.rom = Objects.requireNonNull(rom);
        this.world = Objects.requireNonNull(world);
        this.player = Objects.requireNonNull(player);
        this.ui = Objects.requireNonNull(ui);
        this.input = Objects.requireNonNull(input);
        this.state = Objects.requireNonNull(state);
        this.actions = Objects.requireNonNull(actions);
    }

    public GPU gpu() {
        return rom.gpu();
    }

    public Rom romData() {
        return rom.romData();
    }

    public RoomSession roomSession() {
        return world.roomSession();
    }

    public int ROOM_PIXEL_WIDTH() {
        return 160;
    }

    public int ROOM_PIXEL_HEIGHT() {
        return 128;
    }

    public InputState inputState() {
        return input.inputState();
    }

    public PlayerState playerState() {
        return player.playerState();
    }

    public BackgroundSceneLoader backgroundSceneLoader() {
        return rom.backgroundSceneLoader();
    }

    public MusicDirector musicDirector() {
        return actions.music();
    }

    public CutsceneManager cutsceneManager() {
        return ui.cutsceneManager();
    }

    public SaveRamStore saveRamStore() {
        return actions.saveState().saveRamStore;
    }

    public Link link() {
        return player.link();
    }

    public Screen titleScreen() {
        return actions.navigator().screen(ScreenKind.TITLE);
    }

    public Screen cutsceneScreen() {
        return actions.navigator().screen(ScreenKind.CUTSCENE);
    }

    public Screen fileMenuScreen() {
        return actions.navigator().screen(ScreenKind.FILE_MENU);
    }

    public Screen gameplayScreen() {
        return actions.navigator().screen(ScreenKind.GAMEPLAY);
    }

    public AppConfig config() {
        return state.options().config();
    }

    private void setFileMenuController(FileMenuController controller) {
        state.screens().fileMenuController = controller;
    }

    public void startIntroCutscene() {
        state.screens().fileMenuController = null;
        state.screens().fileSaveController = null;
        actions.navigator().show(linksawakening.app.screen.ScreenKind.CUTSCENE);
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
        state.screens().fileMenuController = null;
        state.screens().fileSaveController = null;
        actions.navigator().show(linksawakening.app.screen.ScreenKind.TITLE);
        musicDirector().playDirectMusic(MusicDirector.directTitleScreenMusicTrack());
    }

    public void startFileSelection() {
        actions.navigator().show(linksawakening.app.screen.ScreenKind.FILE_MENU);
        state.screens().fileSaveController = null;
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
        if (IntroCutsceneScript.SCENE_TITLE.equals(sceneId)
                && actions.navigator().current() == cutsceneScreen()) {
            musicDirector().playDirectMusic(MusicDirector.naturalIntroTitleMusicTrack());
        }
        BackgroundSceneSpec spec = BackgroundSceneCatalog.forCutsceneScene(sceneId);
        if (spec != null) {
            applyBackgroundScene(backgroundSceneLoader().load(spec));
        }
    }

    public void applyBackgroundScene(BackgroundScene scene) {
        state.scene().apply(scene);
    }

    public void startConfiguredGameplay() {
        dispatchConfiguredGameplay(
                config(), () -> actions.saves().startNewGame(), this::startConfiguredLocationGameplay);
    }

    public void startDebugGameplay() {
        if (link() == null) {
            return;
        }
        if (actions.navigator().current() == cutsceneScreen()) {
            cutsceneManager().skipIntroToTitle();
        }
        state.screens().fileMenuController = null;
        state.screens().fileSaveController = null;
        state.opening().wakeUp(null);
        state.opening().shield(null);
        state.screens().currentSaveSlot = -1;
        link().resetTransientStateForDebug();
        playerState().initializeDebugState();
        roomSession().story().initializeNewGameWorldState();
        roomSession().story().setBowWowState(0);
        roomSession().story().setBirdKeyOwned(true);
        roomSession().story().setTailKeyOwned(true);
        actions.navigator().show(linksawakening.app.screen.ScreenKind.GAMEPLAY);
        state.debug().debugNoClip = false;
        link().setDirection(Link.DIRECTION_DOWN);
        gpu().loadBaseTiles(romData());
        loadOverworldScreen();
        DebugLog.log("Debug gameplay started (F3 toggles no-clip)");
    }

    public void startConfiguredLocationGameplay() {
        state.screens().fileMenuController = null;
        state.screens().fileSaveController = null;
        state.screens().currentSaveSlot = -1;
        state.opening().wakeUp(null);
        state.opening().shield(null);
        actions.navigator().show(linksawakening.app.screen.ScreenKind.GAMEPLAY);
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
