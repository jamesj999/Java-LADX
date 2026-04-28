package linksawakening.config;

import linksawakening.input.InputConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_J;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_K;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;

final class AppConfigTest {

    @Test
    void defaultsPreserveCurrentStartupBehavior() {
        AppConfig config = AppConfig.defaults();

        assertTrue(config.showTitleScreen());
        assertFalse(config.playIntroStory());
        assertTrue(config.debugEnabled());
        assertEquals(AppConfig.StartMode.CONFIGURED_LOCATION, config.startMode());
        assertEquals(AppConfig.StartLocationType.OVERWORLD, config.startLocation().type());
        assertEquals(0x92, config.startLocation().roomId());
        assertEquals(AppConfig.ItemProfile.DEBUG_ALL_ITEMS, config.itemProfile());
    }

    @Test
    void parsesBooleansStartLocationItemProfileAndControls() {
        AppConfig config = AppConfig.parse("""
            {
              "showTitleScreen": false,
              "playIntroStory": true,
              "debugEnabled": false,
              "startMode": "NORMAL",
              "startLocation": { "type": "OVERWORLD", "roomId": "0xA1" },
              "itemProfile": "NEW_GAME",
              "controls": {
                "menuOpenKey": "SPACE",
                "upKey": "W",
                "downKey": "S",
                "leftKey": "A",
                "rightKey": "D",
                "aKey": "J",
                "bKey": "K"
              }
            }
            """);

        assertFalse(config.showTitleScreen());
        assertTrue(config.playIntroStory());
        assertFalse(config.debugEnabled());
        assertEquals(AppConfig.StartMode.NORMAL, config.startMode());
        assertEquals(0xA1, config.startLocation().roomId());
        assertEquals(AppConfig.ItemProfile.DEBUG_ALL_ITEMS, config.itemProfile());

        InputConfig input = config.inputConfig();
        assertEquals(GLFW_KEY_SPACE, input.menuOpenKey());
        assertEquals(GLFW_KEY_W, input.upKey());
        assertEquals(GLFW_KEY_S, input.downKey());
        assertEquals(GLFW_KEY_A, input.leftKey());
        assertEquals(GLFW_KEY_D, input.rightKey());
        assertEquals(GLFW_KEY_J, input.aKey());
        assertEquals(GLFW_KEY_K, input.bKey());
    }

    @Test
    void decimalRoomIdsAreAccepted() {
        AppConfig config = AppConfig.parse("""
            { "startLocation": { "type": "OVERWORLD", "roomId": "146" } }
            """);

        assertEquals(146, config.startLocation().roomId());
    }
}
