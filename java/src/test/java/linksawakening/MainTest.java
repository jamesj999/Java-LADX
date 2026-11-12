package linksawakening;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F1;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F2;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

import java.util.ArrayList;
import java.util.List;
import linksawakening.app.*;
import linksawakening.app.gameplay.*;
import linksawakening.app.host.*;
import linksawakening.app.screen.*;
import linksawakening.audio.music.MusicTrackIds;
import linksawakening.config.AppConfig;
import linksawakening.cutscene.CutsceneManager;
import linksawakening.dialog.DialogController;
import linksawakening.rom.Rom;
import linksawakening.scene.BackgroundSceneCatalog;
import linksawakening.scene.BackgroundSceneLoader;
import linksawakening.scene.BackgroundSceneSpec;
import linksawakening.startup.StartupCoordinator;
import linksawakening.state.PlayerState;
import linksawakening.world.room.IndoorRoomPointerTables;
import linksawakening.world.room.RoomPointerTable;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class MainTest {
    private static final GameSession game = new GameSession();

    @Test
    void colorDungeonUsesItsOwnRoomPointerTable() {
        RoomPointerTable table = IndoorRoomPointerTables.forMap(0xFF);

        assertEquals(0x0A, table.bank());
        assertEquals(0x7B77, table.address());
    }

    @Test
    void startRoomComesFromConfiguredOverworldLocation() {
        AppConfig config = AppConfig.parse(
                """
            { "startLocation": { "type": "OVERWORLD", "roomId": "0xA1" } }
            """);

        assertEquals(0xA1, StartupCoordinator.gameplayStartRoomId(config));
    }

    @Test
    void normalStartupIgnoresConfiguredDebugRoomWhenEnteringGameplay() {
        AppConfig config = AppConfig.parse(
                """
            {
              "startMode": "NORMAL",
              "startLocation": { "type": "OVERWORLD", "roomId": "0xA1" }
            }
            """);

        assertEquals(0x92, StartupCoordinator.gameplayStartRoomId(config));
    }

    @Test
    void configuredNewGameProfileClearsTheDebugInventoryBeforeRoomStartup() {
        PlayerState playerState = new PlayerState();
        playerState.inventory().setItemA(linksawakening.state.InventoryItem.BOW.romValue());
        playerState.inventory().setItemB(linksawakening.state.InventoryItem.HOOKSHOT.romValue());

        ScreenRouter.applyConfiguredItemProfile(
                AppConfig.parse("""
            { "itemProfile": "NEW_GAME" }
            """), playerState);

        assertEquals(
                linksawakening.state.InventoryItem.EMPTY.romValue(),
                playerState.inventory().itemA());
        assertEquals(
                linksawakening.state.InventoryItem.EMPTY.romValue(),
                playerState.inventory().itemB());
        assertEquals(30, playerState.ammunition().maxArrows());
        assertEquals(30, playerState.ammunition().maxBombs());
        assertEquals(20, playerState.ammunition().maxMagicPowder());
        assertEquals(
                linksawakening.state.InventoryItem.EMPTY.romValue(),
                playerState.inventory().subscreenItem(0));
    }

    @Test
    void introStartupFollowsIntroStoryConfig() {
        AppConfig introEnabled = AppConfig.parse(
                """
            { "startMode": "CONFIGURED_LOCATION", "showTitleScreen": true, "playIntroStory": true }
            """);
        AppConfig introDisabled = AppConfig.parse(
                """
            { "startMode": "NORMAL", "showTitleScreen": true, "playIntroStory": false }
            """);

        assertTrue(StartupCoordinator.shouldStartIntroCutscene(introEnabled));
        assertFalse(StartupCoordinator.shouldStartIntroCutscene(introDisabled));
    }

    @Test
    void checkedInConfigStartsIntroBeforeNewGameGameplay() {
        AppConfig config = AppConfig.loadFromResources();

        assertTrue(StartupCoordinator.shouldStartIntroCutscene(config));
        assertTrue(StartupCoordinator.shouldStartNewGameGameplay(config));
    }

    @Test
    void cutsceneSceneSpecsPointAtDisassemblyIntroBackgrounds() {
        BackgroundSceneSpec sea = BackgroundSceneCatalog.forCutsceneScene("intro-sea");
        BackgroundSceneSpec face = BackgroundSceneCatalog.forCutsceneScene("intro-link-face");
        BackgroundSceneSpec beach = BackgroundSceneCatalog.forCutsceneScene("intro-beach");
        BackgroundSceneSpec title = BackgroundSceneCatalog.forCutsceneScene("title");

        assertEquals(new BackgroundSceneSpec(0x08, 0x6C37, 0x24, 0x5D18, 0x21, 0x7536), sea);
        assertEquals(new BackgroundSceneSpec(0x08, 0x6D80, 0x24, 0x5D69, 0x21, 0x7536), face);
        assertEquals(new BackgroundSceneSpec(0x08, 0x6F8B, 0x24, 0x7BA7, 0x21, 0x7DEE), beach);
        assertEquals(new BackgroundSceneSpec(0x08, 0x710A, 0x24, 0x7BF0, 0x21, 0x7DEE), title);
    }

    @Test
    void backgroundScenesUseFullGameBoyBgMap() {
        BackgroundSceneLoader loader = new BackgroundSceneLoader(Rom.of(loadRom()));

        assertEquals(32 * 32, loader.load(BackgroundSceneCatalog.TITLE).tilemap().length);
    }

    @Test
    void debugDumpRequiresDebugEnabledAndF1Press() {
        AppConfig enabled = AppConfig.parse("{ \"debugEnabled\": true }");
        AppConfig disabled = AppConfig.parse("{ \"debugEnabled\": false }");

        assertTrue(InputRouter.shouldRunDebugDump(enabled, GLFW_KEY_F1, GLFW_PRESS));
        assertFalse(InputRouter.shouldRunDebugDump(disabled, GLFW_KEY_F1, GLFW_PRESS));
        assertFalse(InputRouter.shouldRunDebugDump(enabled, GLFW_KEY_F2, GLFW_PRESS));
        assertFalse(InputRouter.shouldRunDebugDump(enabled, GLFW_KEY_F1, GLFW_RELEASE));
    }

    @Test
    void dxInventoryBlocksEntityAnimationDuringTransitionsAndWhileOpen() {
        assertTrue(linksawakening.app.gameplay.GameplaySimulation.shouldTickRoomEntities(false, false, false, false));
        assertFalse(linksawakening.app.gameplay.GameplaySimulation.shouldTickRoomEntities(false, false, true, true));
        assertFalse(linksawakening.app.gameplay.GameplaySimulation.shouldTickRoomEntities(false, false, true, false));
        assertFalse(linksawakening.app.gameplay.GameplaySimulation.shouldTickRoomEntities(true, false, false, false));
        assertFalse(linksawakening.app.gameplay.GameplaySimulation.shouldTickRoomEntities(false, true, false, false));
    }

    @Test
    void gameplayFrameDecrementsDialogCooldownWhenEntityAnimationIsSkipped() {
        DialogController dialog = closedDialogWithCooldown();
        assertFalse(linksawakening.app.gameplay.GameplaySimulation.shouldTickRoomEntities(false, false, true, false));
        assertFalse(linksawakening.app.gameplay.GameplaySimulation.shouldTickRoomEntities(true, false, false, false));
        assertFalse(linksawakening.app.gameplay.GameplaySimulation.shouldTickRoomEntities(false, true, false, false));

        linksawakening.app.gameplay.GameplaySimulation.completeGameplayFrame(dialog);

        assertEquals(0x17, dialog.dialogCooldown());
    }

    @Test
    void gameplayFramesExposeTheFullCooldownBeforeTwentyFourPostEntityDecrements() {
        DialogController dialog = closedDialogWithCooldown();
        List<Integer> valuesSeenByOptionalEntityAnimation = new ArrayList<>();

        for (int frame = 0; frame < 0x18; frame++) {
            valuesSeenByOptionalEntityAnimation.add(dialog.dialogCooldown());
            linksawakening.app.gameplay.GameplaySimulation.completeGameplayFrame(dialog);
        }

        assertEquals(0x18, valuesSeenByOptionalEntityAnimation.getFirst());
        assertEquals(1, valuesSeenByOptionalEntityAnimation.getLast());
        assertEquals(0x18, valuesSeenByOptionalEntityAnimation.size());
        assertEquals(0, dialog.dialogCooldown());
    }

    @Test
    void enterPressSkipsActiveIntroCutsceneToTitle() {
        List<String> loadedScenes = new ArrayList<>();
        CutsceneManager manager = new CutsceneManager(new DialogController(16), loadedScenes::add);
        manager.startIntro();

        assertTrue(InputRouter.skipIntroCutsceneIfRequested(GLFW_KEY_ENTER, GLFW_PRESS, manager));
        assertFalse(manager.isActive());
        assertTrue(manager.isShowingTitleScene());
    }

    @Test
    void titleAndIntroMusicTracksMatchDisassemblyEntryPoints() {
        assertEquals(MusicTrackIds.MUSIC_TITLE_CUTSCENE, linksawakening.app.MusicDirector.introCutsceneMusicTrack());
        assertEquals(MusicTrackIds.MUSIC_TITLE_SCREEN, linksawakening.app.MusicDirector.naturalIntroTitleMusicTrack());
        assertEquals(
                MusicTrackIds.MUSIC_TITLE_SCREEN_NO_INTRO,
                linksawakening.app.MusicDirector.directTitleScreenMusicTrack());
    }

    private static byte[] loadRom() {
        try (var stream = linksawakening.testing.TestRom.open()) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return stream.readAllBytes();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load ROM", e);
        }
    }

    private static DialogController closedDialogWithCooldown() {
        DialogController dialog = new DialogController(16);
        dialog.open("A");
        dialog.advance();
        dialog.advance();
        return dialog;
    }
}
