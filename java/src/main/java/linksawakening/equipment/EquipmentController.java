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
        tickEquippedItems(null);
    }

    public void tickEquippedItems(int frameCounter) {
        tickEquippedItems(Integer.valueOf(frameCounter));
    }

    /** Finish existing animations without activating or holding equipment. */
    public void tickEquippedItemsWithoutInput(int frameCounter) {
        itemRegistry.tickWithoutInput(playerState, frameCounter);
    }

    private void tickEquippedItems(Integer frameCounter) {
        boolean aHeld = inputState.isDown(inputConfig.aKey());
        boolean bHeld = inputState.isDown(inputConfig.bKey());
        if (frameCounter == null) {
            itemRegistry.tickAll(aHeld, bHeld, playerState.itemA(), playerState.itemB());
        } else {
            itemRegistry.tickAll(aHeld, bHeld, playerState.itemA(), playerState.itemB(), frameCounter);
        }
    }

    public Sword activeSword() {
        return itemRegistry.activeSword(playerState);
    }

    private void dispatchButtonEdges(int glfwKey, int inventoryId) {
        EquipmentItem item = itemRegistry.lookup(inventoryId);
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
