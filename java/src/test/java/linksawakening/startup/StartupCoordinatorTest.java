package linksawakening.startup;

import linksawakening.config.AppConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StartupCoordinatorTest {

    @Test
    void configuredLocationUsesConfiguredDebugRoom() {
        AppConfig config = AppConfig.parse("""
            { "startLocation": { "type": "OVERWORLD", "roomId": "0xA1" } }
            """);

        assertEquals(0xA1, StartupCoordinator.gameplayStartRoomId(config));
    }

    @Test
    void normalStartupUsesCanonicalNewGameRoom() {
        AppConfig config = AppConfig.parse("""
            {
              "startMode": "NORMAL",
              "startLocation": { "type": "OVERWORLD", "roomId": "0xA1" }
            }
            """);

        assertEquals(0x92, StartupCoordinator.gameplayStartRoomId(config));
    }

    @Test
    void introCutsceneRequiresIntroStoryAndTitleScreen() {
        assertTrue(StartupCoordinator.shouldStartIntroCutscene(AppConfig.parse("""
            { "startMode": "CONFIGURED_LOCATION", "showTitleScreen": true, "playIntroStory": true }
            """)));
        assertFalse(StartupCoordinator.shouldStartIntroCutscene(AppConfig.parse("""
            { "startMode": "NORMAL", "showTitleScreen": true, "playIntroStory": false }
            """)));
        assertFalse(StartupCoordinator.shouldStartIntroCutscene(AppConfig.parse("""
            { "startMode": "NORMAL", "showTitleScreen": false, "playIntroStory": true }
            """)));
    }
}
