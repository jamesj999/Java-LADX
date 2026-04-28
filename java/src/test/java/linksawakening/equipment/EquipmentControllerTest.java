package linksawakening.equipment;

import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_B;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_UP;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

final class EquipmentControllerTest {

    @Test
    void dispatchesPressReleaseAndHeldStateForEquippedButtons() {
        InputState input = new InputState();
        InputConfig inputConfig = new InputConfig(GLFW_KEY_ENTER, GLFW_KEY_UP, GLFW_KEY_DOWN,
            GLFW_KEY_LEFT, GLFW_KEY_RIGHT, GLFW_KEY_A, GLFW_KEY_B);
        PlayerState player = new PlayerState();
        player.setItemA(PlayerState.INVENTORY_SWORD);
        player.setItemB(PlayerState.INVENTORY_BOMBS);
        ItemRegistry registry = new ItemRegistry();
        RecordingItem sword = new RecordingItem();
        RecordingItem bombs = new RecordingItem();
        registry.register(PlayerState.INVENTORY_SWORD, sword);
        registry.register(PlayerState.INVENTORY_BOMBS, bombs);
        EquipmentController controller = new EquipmentController(input, inputConfig, player, registry);

        input.onKeyEvent(GLFW_KEY_A, GLFW_PRESS);
        controller.dispatchButtonEdges();
        controller.tickEquippedItems();

        assertEquals(1, sword.presses);
        assertEquals(1, sword.heldTicks);
        assertEquals(0, bombs.presses);

        input.tickEdges();
        input.onKeyEvent(GLFW_KEY_A, GLFW_RELEASE);
        input.onKeyEvent(GLFW_KEY_B, GLFW_PRESS);
        controller.dispatchButtonEdges();
        controller.tickEquippedItems();

        assertEquals(1, sword.releases);
        assertEquals(1, bombs.presses);
        assertEquals(1, bombs.heldTicks);
    }

    private static final class RecordingItem implements EquippedItem {
        private int presses;
        private int releases;
        private int heldTicks;

        @Override
        public void onPress() {
            presses++;
        }

        @Override
        public void onRelease() {
            releases++;
        }

        @Override
        public void tick(boolean buttonHeld) {
            if (buttonHeld) {
                heldTicks++;
            }
        }
    }
}
