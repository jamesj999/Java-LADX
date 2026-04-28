package linksawakening.equipment;

import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.state.PlayerState;

public final class EquipmentController {

    private final InputState inputState;
    private final InputConfig inputConfig;
    private final PlayerState playerState;
    private final ItemRegistry itemRegistry;

    public EquipmentController(InputState inputState, InputConfig inputConfig,
                               PlayerState playerState, ItemRegistry itemRegistry) {
        this.inputState = inputState;
        this.inputConfig = inputConfig;
        this.playerState = playerState;
        this.itemRegistry = itemRegistry;
    }

    public void dispatchButtonEdges() {
        dispatchButtonEdges(inputConfig.aKey(), playerState.itemA());
        dispatchButtonEdges(inputConfig.bKey(), playerState.itemB());
    }

    public void tickEquippedItems() {
        boolean aHeld = inputState.isDown(inputConfig.aKey());
        boolean bHeld = inputState.isDown(inputConfig.bKey());
        itemRegistry.tickAll(aHeld, bHeld, playerState.itemA(), playerState.itemB());
    }

    public Sword activeSword() {
        EquippedItem a = itemRegistry.lookup(playerState.itemA());
        if (a instanceof Sword sword) {
            return sword;
        }
        EquippedItem b = itemRegistry.lookup(playerState.itemB());
        if (b instanceof Sword sword) {
            return sword;
        }
        return null;
    }

    private void dispatchButtonEdges(int glfwKey, int inventoryId) {
        EquippedItem item = itemRegistry.lookup(inventoryId);
        if (item == null) {
            return;
        }
        if (inputState.wasPressed(glfwKey)) {
            item.onPress();
        }
        if (inputState.wasReleased(glfwKey)) {
            item.onRelease();
        }
    }
}
