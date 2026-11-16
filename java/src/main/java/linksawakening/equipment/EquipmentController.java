package linksawakening.equipment;

import java.util.Objects;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.state.PlayerState;

/** Dispatches held and edge input in CheckItemsToUse order. */
public final class EquipmentController {

    private final InputState inputState;
    private final InputConfig inputConfig;
    private final PlayerState playerState;
    private final ItemRegistry itemRegistry;

    public EquipmentController(
            InputState inputState, InputConfig inputConfig, PlayerState playerState, ItemRegistry itemRegistry) {
        this.inputState = Objects.requireNonNull(inputState, "inputState");
        this.inputConfig = Objects.requireNonNull(inputConfig, "inputConfig");
        this.playerState = Objects.requireNonNull(playerState, "playerState");
        this.itemRegistry = Objects.requireNonNull(itemRegistry, "itemRegistry");
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

    public Sword activeSword() {
        return itemRegistry.activeSword(playerState);
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

    private void dispatchButtonEdges(int glfwKey, int inventoryId) {
        EquipmentItem item = itemRegistry.item(inventoryId);
        if (inputState.wasPressed(glfwKey)) {
            item.onPress();
        }
        if (inputState.wasReleased(glfwKey)) {
            item.onRelease();
        }
    }
}
