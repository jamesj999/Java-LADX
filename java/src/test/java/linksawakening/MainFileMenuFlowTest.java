package linksawakening;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_TAB;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

import linksawakening.app.*;
import linksawakening.app.screen.*;
import linksawakening.audio.music.MusicTrackIds;
import linksawakening.config.AppConfig;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.render.RenderScreen;
import org.junit.jupiter.api.Test;

final class MainFileMenuFlowTest {
    private static final GameSession game = new GameSession();

    @Test
    void titleEnterIsTheFileMenuBoundary() {
        assertTrue(InputRouter.shouldEnterFileSelection(
                new TitleScreen(new TitleScreen.Dependencies(() -> null, () -> null, () -> {})),
                GLFW_KEY_ENTER,
                GLFW_PRESS));
        assertFalse(InputRouter.shouldEnterFileSelection(
                new GameplayScreen(() -> {}, (key, action) -> {}), GLFW_KEY_ENTER, GLFW_PRESS));
    }

    @Test
    void fileMenuHasItsOwnRenderScreenAndDisassemblyMusicTrack() {
        assertEquals(
                RenderScreen.FILE_MENU,
                new FileMenuScreen(new FileMenuScreen.Dependencies(
                                () -> null,
                                () -> null,
                                () -> null,
                                () -> null,
                                slot -> {},
                                () -> {},
                                saved -> {},
                                mutation -> {},
                                () -> {}))
                        .renderScreen());
        assertEquals(MusicTrackIds.MUSIC_FILE_SELECT, linksawakening.app.MusicDirector.fileSelectionMusicTrack());
    }

    @Test
    void fileSaveChordRequiresExactlyTheFourSourceButtonsRegardlessOfLastKey() {
        InputConfig inputConfig = new InputConfig(GLFW_KEY_ENTER, 1, 2, 3, 4, 5, 6, GLFW_KEY_TAB);
        InputState inputState = new InputState();
        inputState.onKeyEvent(5, GLFW_PRESS);
        inputState.onKeyEvent(6, GLFW_PRESS);
        inputState.onKeyEvent(GLFW_KEY_ENTER, GLFW_PRESS);
        inputState.onKeyEvent(GLFW_KEY_TAB, GLFW_PRESS);

        assertTrue(InputRouter.shouldEnterFileSave(inputConfig, inputState, GLFW_KEY_TAB, GLFW_PRESS));
        assertTrue(InputRouter.shouldEnterFileSave(inputConfig, inputState, 5, GLFW_PRESS));
        inputState.onKeyEvent(1, GLFW_PRESS);
        assertFalse(InputRouter.shouldEnterFileSave(inputConfig, inputState, 5, GLFW_PRESS));
        inputState.onKeyEvent(1, GLFW_RELEASE);
        inputState.onKeyEvent(6, GLFW_RELEASE);
        assertFalse(InputRouter.shouldEnterFileSave(inputConfig, inputState, GLFW_KEY_TAB, GLFW_PRESS));
    }

    @Test
    void configuredNewGameDispatchesOnlyTheDedicatedBootstrap() {
        int[] calls = new int[2];
        AppConfig config = AppConfig.parse("""
            { "itemProfile": "NEW_GAME" }
            """);

        ScreenRouter.dispatchConfiguredGameplay(config, () -> calls[0]++, () -> calls[1]++);

        assertEquals(1, calls[0]);
        assertEquals(0, calls[1]);
    }

    @Test
    void configuredDebugProfileDispatchesOnlyTheConfiguredLocationBootstrap() {
        int[] calls = new int[2];
        AppConfig config = AppConfig.parse("""
            { "itemProfile": "DEBUG_ALL_ITEMS" }
            """);

        ScreenRouter.dispatchConfiguredGameplay(config, () -> calls[0]++, () -> calls[1]++);

        assertEquals(0, calls[0]);
        assertEquals(1, calls[1]);
    }
}
