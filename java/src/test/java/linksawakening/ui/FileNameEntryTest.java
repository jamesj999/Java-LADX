package linksawakening.ui;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.*;

import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import org.junit.jupiter.api.Test;

class FileNameEntryTest {
    private static final InputConfig INPUT = new InputConfig(
            GLFW_KEY_ENTER, GLFW_KEY_UP, GLFW_KEY_DOWN, GLFW_KEY_LEFT, GLFW_KEY_RIGHT, GLFW_KEY_A, GLFW_KEY_B);

    @Test
    void wrapsCharacterGridAndResetsOwnedNameBuffer() {
        int[] characters = new int[64];
        characters[63] = 42;
        var entry = new FileNameEntry(characters);
        entry.tick(pressed(GLFW_KEY_LEFT, GLFW_KEY_A), INPUT, 2);
        assertEquals(63, entry.selectedCharacter());
        assertEquals(1, entry.namePosition());
        assertArrayEquals(new int[] {42, 0, 0, 0, 0}, entry.currentName());
        entry.reset();
        assertEquals(0, entry.selectedCharacter());
        assertEquals(0, entry.namePosition());
        assertArrayEquals(new int[5], entry.currentName());
    }

    @Test
    void startPrecedesEditingAndActionOwnsItsNameCopy() {
        int[] characters = new int[64];
        characters[0] = 7;
        var entry = new FileNameEntry(characters);
        entry.tick(pressed(GLFW_KEY_A), INPUT, 1);
        var result = entry.tick(pressed(GLFW_KEY_ENTER, GLFW_KEY_A, GLFW_KEY_RIGHT), INPUT, 1);
        assertEquals(FileMenuAction.Type.CREATE_FILE, result.type());
        assertEquals(1, result.selectedSlot());
        assertEquals(0, entry.selectedCharacter());
        assertEquals(1, entry.namePosition());
        entry.reset();
        assertArrayEquals(new int[] {7, 0, 0, 0, 0}, result.nameBytes());
    }

    private static InputState pressed(int... keys) {
        var input = new InputState();
        for (int key : keys) input.onKeyEvent(key, GLFW_PRESS);
        return input;
    }
}
