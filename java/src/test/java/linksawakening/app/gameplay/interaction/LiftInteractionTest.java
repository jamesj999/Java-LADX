package linksawakening.app.gameplay.interaction;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.*;

import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

final class LiftInteractionTest {
    @Test
    void holdingBraceletDoesNotThrowUntilANewPressAndBSlotHasPriority() {
        var input = new InputState();
        var config = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        var player = new PlayerState();
        player.inventory().setItemA(linksawakening.state.InventoryItem.POWER_BRACELET.romValue());
        player.inventory().setItemB(linksawakening.state.InventoryItem.POWER_BRACELET.romValue());
        var lift = new LiftButtons(input, config, player);
        input.onKeyEvent(config.aKey(), GLFW_PRESS);
        assertFalse(lift.isPowerBraceletButtonHeld());
        assertFalse(lift.isThrowButtonPressed(false));
        input.onKeyEvent(config.bKey(), GLFW_PRESS);
        assertTrue(lift.isPowerBraceletButtonHeld());
        assertTrue(lift.isThrowButtonPressed(false));
        input.tickEdges();
        assertTrue(lift.isPowerBraceletButtonHeld());
        assertFalse(lift.isThrowButtonPressed(false));
        input.onKeyEvent(config.bKey(), GLFW_RELEASE);
        input.onKeyEvent(config.bKey(), GLFW_PRESS);
        assertTrue(lift.isThrowButtonPressed(false));
    }
}
