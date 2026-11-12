package linksawakening.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_B;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_UP;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.state.InventoryItem;
import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

final class RocsFeatherTest {

    @Test
    void constructorRejectsNullJumpTarget() {
        assertThrows(NullPointerException.class, () -> new RocsFeather(null));
    }

    @ParameterizedTest
    @EnumSource(EquippedButton.class)
    void equippedButtonPressRequestsOneTopViewJumpOnlyOnNewEdge(EquippedButton button) {
        InputState input = new InputState();
        InputConfig inputConfig = new InputConfig(
                GLFW_KEY_ENTER, GLFW_KEY_UP, GLFW_KEY_DOWN, GLFW_KEY_LEFT, GLFW_KEY_RIGHT, GLFW_KEY_A, GLFW_KEY_B);
        PlayerState player = new PlayerState();
        button.equip(player, InventoryItem.ROCS_FEATHER.romValue());
        ItemRegistry registry = new ItemRegistry();
        RecordingJumpTarget link = new RecordingJumpTarget();
        registry.register(InventoryItem.ROCS_FEATHER.romValue(), new RocsFeather(link));
        EquipmentController controller = new EquipmentController(input, inputConfig, player, registry);

        input.onKeyEvent(button.glfwKey, GLFW_PRESS);
        controller.dispatchButtonEdges();
        input.tickEdges();
        controller.dispatchButtonEdges();

        assertEquals(1, link.jumpRequests);
    }

    private enum EquippedButton {
        A(GLFW_KEY_A) {
            @Override
            void equip(PlayerState player, int inventoryId) {
                player.inventory().setItemA(inventoryId);
            }
        },
        B(GLFW_KEY_B) {
            @Override
            void equip(PlayerState player, int inventoryId) {
                player.inventory().setItemB(inventoryId);
            }
        };

        private final int glfwKey;

        EquippedButton(int glfwKey) {
            this.glfwKey = glfwKey;
        }

        abstract void equip(PlayerState player, int inventoryId);
    }

    private static final class RecordingJumpTarget implements RocsFeather.JumpTarget {
        private int jumpRequests;

        @Override
        public void useRocsFeather() {
            jumpRequests++;
        }
    }
}
