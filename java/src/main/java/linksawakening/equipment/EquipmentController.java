package linksawakening.equipment;

import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.state.PlayerState;

public final class EquipmentController {

    private final InputState inputState;
    private final InputConfig inputConfig;
    private final PlayerState playerState;
    private final ItemRegistry itemRegistry;

    public EquipmentController(
            InputState inputState, InputConfig inputConfig, PlayerState playerState, ItemRegistry itemRegistry) {
        this.inputState = inputState;
        this.inputConfig = inputConfig;
        this.playerState = playerState;
        this.itemRegistry = itemRegistry;
    }

    public void dispatchButtonEdges() {
        dispatchButtonEdges(inputConfig.aKey(), playerState.inventory().itemA());
        dispatchButtonEdges(inputConfig.bKey(), playerState.inventory().itemB());
    }

    public void tickEquippedItems() {
        tickEquippedItems(false, 0);
    }

    public void tickEquippedItems(int frameCounter) {
        tickEquippedItems(true, frameCounter);
    }

    /** Finish existing animations without activating or holding equipment. */
    public void tickEquippedItemsWithoutInput(int frameCounter) {
        itemRegistry.tickWithoutInput(playerState, frameCounter);
    }

    private void tickEquippedItems(boolean hasFrame, int frameCounter) {
        boolean aHeld = inputState.isDown(inputConfig.aKey());
        boolean bHeld = inputState.isDown(inputConfig.bKey());
        if (!hasFrame) {
            itemRegistry.tickAll(
                    aHeld,
                    bHeld,
                    playerState.inventory().itemA(),
                    playerState.inventory().itemB());
        } else {
            itemRegistry.tickAll(
                    aHeld,
                    bHeld,
                    playerState.inventory().itemA(),
                    playerState.inventory().itemB(),
                    frameCounter);
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
