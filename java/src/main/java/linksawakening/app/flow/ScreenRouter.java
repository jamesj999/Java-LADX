package linksawakening.app.flow;

import static linksawakening.world.room.RoomConstants.*;

import java.util.Objects;
import linksawakening.app.audio.MusicDirector;
import linksawakening.app.debug.DebugLog;
import linksawakening.app.screen.ScreenKind;
import linksawakening.app.services.InputServices;
import linksawakening.app.services.PlayerServices;
import linksawakening.app.services.RomServices;
import linksawakening.app.services.UiServices;
import linksawakening.app.services.WorldServices;
import linksawakening.app.state.SessionState;
import linksawakening.config.AppConfig;
import linksawakening.cutscene.IntroCutsceneScript;
import linksawakening.entity.Link;
import linksawakening.scene.BackgroundScene;
import linksawakening.scene.BackgroundSceneCatalog;
import linksawakening.scene.BackgroundSceneSpec;
import linksawakening.startup.NewGameStartProfile;
import linksawakening.startup.StartupCoordinator;
import linksawakening.state.Direction;
import linksawakening.state.PlayerState;
import linksawakening.ui.FileMenuController;
import linksawakening.ui.FileMenuRomData;

/**
 * Loads screen presentations and starts configured, debug, or file-selected gameplay. One instance is
 * retained for the owning session.
 */
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

    private static final int ROOM_PIXEL_WIDTH = 160;

    private static final int ROOM_PIXEL_HEIGHT = 128;

    private void setFileMenuController(FileMenuController controller) {
        state.screens().fileMenuController = controller;
    }

    public void startIntroCutscene() {
        state.screens().fileMenuController = null;
        state.screens().fileSaveController = null;
        actions.navigator().show(ScreenKind.CUTSCENE);
        actions.music().playDirectMusic(MusicDirector.introCutsceneMusicTrack());
        rom.gpu().loadIntroSequenceTiles(rom.romData());
        ui.cutsceneManager().startIntro(rom.romData(), this::loadIntroBackgroundScene);
    }

    public BackgroundScene loadIntroBackgroundScene(String sceneId) {
        BackgroundSceneSpec spec = BackgroundSceneCatalog.forCutsceneScene(sceneId);
        if (spec == null) {
            throw new IllegalArgumentException("No intro background scene for " + sceneId);
        }
        return rom.backgroundSceneLoader().load(spec);
    }

    public void startTitleScreenWithoutIntro() {
        state.screens().fileMenuController = null;
        state.screens().fileSaveController = null;
        actions.navigator().show(ScreenKind.TITLE);
        actions.music().playDirectMusic(MusicDirector.directTitleScreenMusicTrack());
    }

    public void startFileSelection() {
        actions.navigator().show(ScreenKind.FILE_MENU);
        state.screens().fileSaveController = null;
        rom.gpu().loadMenuTiles(rom.romData());
        setFileMenuController(new FileMenuController(
                new FileMenuRomData(rom.romData()),
                this::loadFileMenuBackgroundScene,
                actions.saveState().saveRamStore.saveFilesMask(),
                actions.saveState().saveRamStore.savedNames(),
                new int[] {
                    actions.saveState().saveRamStore.readSlot(0).maxHearts(),
                    actions.saveState().saveRamStore.readSlot(1).maxHearts(),
                    actions.saveState().saveRamStore.readSlot(2).maxHearts()
                },
                new int[] {
                    actions.saveState().saveRamStore.readSlot(0).deathCountBcd(),
                    actions.saveState().saveRamStore.readSlot(1).deathCountBcd(),
                    actions.saveState().saveRamStore.readSlot(2).deathCountBcd()
                }));
        actions.music().playDirectMusic(MusicDirector.fileSelectionMusicTrack());
        // The Enter event that leaves the title must not also activate the
        // first empty file. Its edge belongs to the title screen.
        input.inputState().tickEdges();
    }

    public BackgroundScene loadFileMenuBackgroundScene(String sceneId) {
        BackgroundSceneSpec spec = BackgroundSceneCatalog.forFileMenuScene(sceneId);
        if (spec == null) {
            throw new IllegalArgumentException("No file-menu background scene for " + sceneId);
        }
        return rom.backgroundSceneLoader().load(spec);
    }

    public void setCutsceneScene(String sceneId) {
        if (BackgroundSceneCatalog.requiresTitleTileset(sceneId)) {
            rom.gpu().loadTitleScreenTiles(rom.romData());
        }
        if (IntroCutsceneScript.SCENE_TITLE.equals(sceneId)
                && actions.navigator().current() == actions.navigator().screen(ScreenKind.CUTSCENE)) {
            actions.music().playDirectMusic(MusicDirector.naturalIntroTitleMusicTrack());
        }
        BackgroundSceneSpec spec = BackgroundSceneCatalog.forCutsceneScene(sceneId);
        if (spec != null) {
            applyBackgroundScene(rom.backgroundSceneLoader().load(spec));
        }
    }

    public void applyBackgroundScene(BackgroundScene scene) {
        state.scene().apply(scene);
    }

    public void startConfiguredGameplay() {
        dispatchConfiguredGameplay(
                state.options().config(), () -> actions.saves().startNewGame(), this::startConfiguredLocationGameplay);
    }

    public void startDebugGameplay() {
        if (actions.navigator().current() == actions.navigator().screen(ScreenKind.CUTSCENE)) {
            ui.cutsceneManager().skipIntroToTitle();
        }
        state.screens().fileMenuController = null;
        state.screens().fileSaveController = null;
        state.opening().wakeUp(null);
        state.opening().shield(null);
        state.screens().currentSaveSlot = -1;
        player.link().motion().resetTransientStateForDebug();
        player.playerState().playerInitialization().initializeDebugState();
        world.roomSession().story().initializeNewGameWorldState();
        world.roomSession().story().setBowWowState(0);
        world.roomSession().story().setBirdKeyOwned(true);
        world.roomSession().story().setTailKeyOwned(true);
        actions.navigator().show(ScreenKind.GAMEPLAY);
        state.debug().debugNoClip = false;
        player.link().animation().setDirection(Direction.DOWN.javaIndex());
        rom.gpu().loadBaseTiles(rom.romData());
        loadOverworldScreen();
        DebugLog.log("Debug gameplay started (F3 toggles no-clip)");
    }

    public void startConfiguredLocationGameplay() {
        state.screens().fileMenuController = null;
        state.screens().fileSaveController = null;
        state.screens().currentSaveSlot = -1;
        state.opening().wakeUp(null);
        state.opening().shield(null);
        actions.navigator().show(ScreenKind.GAMEPLAY);
        applyConfiguredItemProfile(state.options().config(), player.playerState());
        if (state.options().config().playIntroStory()) {
            System.err.println("Intro story startup is not implemented yet; spawning at start location");
        }
        loadOverworldScreen();
    }

    public void loadOverworldScreen() {
        world.roomSession()
                .rooms()
                .loadInitialOverworld(
                        StartupCoordinator.gameplayStartRoomId(state.options().config()));
        // Center of Mabe Village Square has the statue (solid). Spawn Link two
        // tiles to the left of the room center so he lands on walkable ground.
        int startX = ROOM_PIXEL_WIDTH / 2 - Link.SPRITE_SIZE / 2 - 32 + 10;
        int startY = ROOM_PIXEL_HEIGHT / 2 - Link.SPRITE_SIZE / 2 - 10;
        player.link().position().setRoomEntryPixelPosition(startX, startY);
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
