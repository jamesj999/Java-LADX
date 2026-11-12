package linksawakening.app.gameplay;

import java.util.Objects;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.state.InventoryItem;
import linksawakening.state.PlayerState;

/** Frame-edge and B-slot priority policy for lifting and throwing. */
public final class LiftButtons {
    private final InputState inputState;
    private final InputConfig inputConfig;
    private final PlayerState playerState;

    public LiftButtons(InputState inputState, InputConfig inputConfig, PlayerState playerState) {
        this.inputState = Objects.requireNonNull(inputState);
        this.inputConfig = Objects.requireNonNull(inputConfig);
        this.playerState = Objects.requireNonNull(playerState);
    }

    public boolean isPowerBraceletButtonHeld() {
        if (playerState.itemB() == InventoryItem.POWER_BRACELET.romValue()) {
            return inputState.isDown(inputConfig.bKey());
        }
        return playerState.itemA() == InventoryItem.POWER_BRACELET.romValue() && inputState.isDown(inputConfig.aKey());
    }

    /** $14:537D uses newly pressed buttons for throws, with B-slot priority. */
    public boolean isThrowButtonPressed(boolean bomb) {
        int item = bomb ? InventoryItem.BOMBS.romValue() : InventoryItem.POWER_BRACELET.romValue();
        if (playerState.itemB() == item) return inputState.wasPressed(inputConfig.bKey());
        if (playerState.itemA() == item) return inputState.wasPressed(inputConfig.aKey());
        return inputState.wasPressed(inputConfig.aKey()) || inputState.wasPressed(inputConfig.bKey());
    }

    public boolean isBombButtonHeld() {
        if (playerState.itemB() == InventoryItem.BOMBS.romValue()) {
            return inputState.isDown(inputConfig.bKey());
        }
        return playerState.itemA() == InventoryItem.BOMBS.romValue() && inputState.isDown(inputConfig.aKey());
    }
}
