package linksawakening;

import linksawakening.audio.music.MusicTrackIds;
import linksawakening.render.RenderScreen;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

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
}
