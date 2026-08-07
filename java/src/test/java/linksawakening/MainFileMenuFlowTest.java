package linksawakening;

import linksawakening.audio.music.MusicTrackIds;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.render.RenderScreen;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_TAB;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

final class MainFileMenuFlowTest {

    @Test
    void titleEnterIsTheFileMenuBoundary() {
        assertTrue(Main.shouldEnterFileSelection(Main.SCREEN_TITLE, GLFW_KEY_ENTER, GLFW_PRESS));
        assertFalse(Main.shouldEnterFileSelection(Main.SCREEN_OVERWORLD, GLFW_KEY_ENTER, GLFW_PRESS));
    }

    @Test
    void fileMenuHasItsOwnRenderScreenAndDisassemblyMusicTrack() {
        assertEquals(RenderScreen.FILE_MENU, Main.renderScreenFor(Main.SCREEN_FILE_MENU));
        assertEquals(MusicTrackIds.MUSIC_FILE_SELECT, Main.fileSelectionMusicTrack());
    }

    @Test
    void fileSaveChordRequiresAllFourSourceButtonsWithSelectPressedLast() {
        InputConfig inputConfig = new InputConfig(
            GLFW_KEY_ENTER, 1, 2, 3, 4, 5, 6, GLFW_KEY_TAB);
        InputState inputState = new InputState();
        inputState.onKeyEvent(5, GLFW_PRESS);
        inputState.onKeyEvent(6, GLFW_PRESS);
        inputState.onKeyEvent(GLFW_KEY_ENTER, GLFW_PRESS);
        inputState.onKeyEvent(GLFW_KEY_TAB, GLFW_PRESS);

        assertTrue(Main.shouldEnterFileSave(inputConfig, inputState, GLFW_KEY_TAB, GLFW_PRESS));
        inputState.onKeyEvent(6, GLFW_RELEASE);
        assertFalse(Main.shouldEnterFileSave(inputConfig, inputState, GLFW_KEY_TAB, GLFW_PRESS));
    }

    @Test
    void committingAnEmptyFileUsesTheDedicatedNewGameBootstrap() throws Exception {
        String source = Files.readString(Path.of("src/main/java/linksawakening/Main.java"));
        String branchStart = "if (action.type() == FileMenuAction.Type.START_NEW_GAME)";
        int start = source.indexOf(branchStart);
        int end = source.indexOf("} else if (action.type() == FileMenuAction.Type.LOAD_GAME)", start);

        String branch = source.substring(start, end);
        assertTrue(branch.contains("startNewGame();"));
        assertFalse(branch.contains("startConfiguredGameplay();"));
    }

    @Test
    void fileMenuStartupUsesThePersistentSaveImageForMaskAndNames() throws Exception {
        String source = Files.readString(Path.of("src/main/java/linksawakening/Main.java"));
        int start = source.indexOf("fileMenuController = new FileMenuController(");
        int end = source.indexOf("playDirectMusic(fileSelectionMusicTrack());", start);
        String constructor = source.substring(start, end);

        assertTrue(constructor.contains("saveRamStore.saveFilesMask()"));
        assertTrue(constructor.contains("saveRamStore.savedNames()"));
        assertFalse(constructor.contains("new int[3][5]"));
    }

    @Test
    void newGamePersistsTheSelectedSlotBeforeEnteringGameplay() throws Exception {
        String source = Files.readString(Path.of("src/main/java/linksawakening/Main.java"));
        int start = source.indexOf("if (action.type() == FileMenuAction.Type.START_NEW_GAME)");
        int end = source.indexOf("} else if (action.type() == FileMenuAction.Type.LOAD_GAME)", start);
        String branch = source.substring(start, end);

        assertTrue(branch.contains("saveRamStore.createNewGame(action.selectedSlot(), action.nameBytes());"));
        assertTrue(branch.contains("saveRamStore.flush();"));
    }

    @Test
    void initializedFileSelectionLoadsTheSavedRoomInsteadOfThrowing() throws Exception {
        String source = Files.readString(Path.of("src/main/java/linksawakening/Main.java"));
        int start = source.indexOf("} else if (action.type() == FileMenuAction.Type.LOAD_GAME)");
        int end = source.indexOf("inputState.tickEdges();", start);
        String branch = source.substring(start, end);

        assertTrue(branch.contains("startSavedGame"));
        assertFalse(branch.contains("UnsupportedOperationException"));
    }
}
