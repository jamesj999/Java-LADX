package linksawakening;

import linksawakening.config.AppConfig;
import linksawakening.cutscene.CutsceneManager;
import linksawakening.dialog.DialogController;
import linksawakening.audio.music.MusicTrackIds;
import linksawakening.scene.BackgroundSceneCatalog;
import linksawakening.scene.BackgroundSceneLoader;
import linksawakening.scene.BackgroundSceneSpec;
import linksawakening.startup.StartupCoordinator;
import linksawakening.world.IndoorRoomPointerTables;
import linksawakening.world.RoomPointerTable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F1;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F2;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

final class MainTest {

    @Test
    void colorDungeonUsesItsOwnRoomPointerTable() {
        RoomPointerTable table = IndoorRoomPointerTables.forMap(0xFF);

        assertEquals(0x0A, table.bank());
        assertEquals(0x7B77, table.address());
    }

    @Test
    void startRoomComesFromConfiguredOverworldLocation() {
        AppConfig config = AppConfig.parse("""
            { "startLocation": { "type": "OVERWORLD", "roomId": "0xA1" } }
            """);

        assertEquals(0xA1, StartupCoordinator.gameplayStartRoomId(config));
    }

    @Test
    void normalStartupIgnoresConfiguredDebugRoomWhenEnteringGameplay() {
        AppConfig config = AppConfig.parse("""
            {
              "startMode": "NORMAL",
              "startLocation": { "type": "OVERWORLD", "roomId": "0xA1" }
            }
            """);

        assertEquals(0x92, StartupCoordinator.gameplayStartRoomId(config));
    }

    @Test
    void introStartupFollowsIntroStoryConfig() {
        AppConfig introEnabled = AppConfig.parse("""
            { "startMode": "CONFIGURED_LOCATION", "showTitleScreen": true, "playIntroStory": true }
            """);
        AppConfig introDisabled = AppConfig.parse("""
            { "startMode": "NORMAL", "showTitleScreen": true, "playIntroStory": false }
            """);

        assertTrue(StartupCoordinator.shouldStartIntroCutscene(introEnabled));
        assertFalse(StartupCoordinator.shouldStartIntroCutscene(introDisabled));
    }

    @Test
    void checkedInConfigAutomaticallyStartsIntroCutscene() {
        assertTrue(StartupCoordinator.shouldStartIntroCutscene(AppConfig.loadFromResources()));
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
        BackgroundSceneLoader loader = new BackgroundSceneLoader(loadRom());

        assertEquals(32 * 32, loader.load(BackgroundSceneCatalog.TITLE).tilemap().length);
    }

    @Test
    void debugDumpRequiresDebugEnabledAndF1Press() {
        AppConfig enabled = AppConfig.parse("{ \"debugEnabled\": true }");
        AppConfig disabled = AppConfig.parse("{ \"debugEnabled\": false }");

        assertTrue(Main.shouldRunDebugDump(enabled, GLFW_KEY_F1, GLFW_PRESS));
        assertFalse(Main.shouldRunDebugDump(disabled, GLFW_KEY_F1, GLFW_PRESS));
        assertFalse(Main.shouldRunDebugDump(enabled, GLFW_KEY_F2, GLFW_PRESS));
        assertFalse(Main.shouldRunDebugDump(enabled, GLFW_KEY_F1, GLFW_RELEASE));
    }

    @Test
    void enterPressSkipsActiveIntroCutsceneToTitle() {
        List<String> loadedScenes = new ArrayList<>();
        CutsceneManager manager = new CutsceneManager(new DialogController(16), loadedScenes::add);
        manager.startIntro();

        assertTrue(Main.skipIntroCutsceneIfRequested(GLFW_KEY_ENTER, GLFW_PRESS, manager));
        assertFalse(manager.isActive());
        assertTrue(manager.isShowingTitleScene());
    }

    @Test
    void titleAndIntroMusicTracksMatchDisassemblyEntryPoints() {
        assertEquals(MusicTrackIds.MUSIC_TITLE_CUTSCENE, Main.introCutsceneMusicTrack());
        assertEquals(MusicTrackIds.MUSIC_TITLE_SCREEN, Main.naturalIntroTitleMusicTrack());
        assertEquals(MusicTrackIds.MUSIC_TITLE_SCREEN_NO_INTRO, Main.directTitleScreenMusicTrack());
    }

    private static byte[] loadRom() {
        try (var stream = MainTest.class.getClassLoader().getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return stream.readAllBytes();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load ROM", e);
        }
    }
}
