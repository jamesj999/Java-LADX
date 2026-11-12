package linksawakening.app.gameplay;

import static linksawakening.world.room.RoomConstants.*;

import linksawakening.entity.Link;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.state.Direction;
import linksawakening.state.InventoryItem;
import linksawakening.state.PlayerState;
import linksawakening.world.room.RoomSession;

/** Focused frame interactions using the current session collaborators. */
public final class LiftInteraction {
    private final InputState inputState;
    private final InputConfig inputConfig;
    private final PlayerState playerState;
    private final Link link;
    private final RoomSession roomSession;

    public LiftInteraction(
            InputState inputState,
            InputConfig inputConfig,
            PlayerState playerState,
            Link link,
            RoomSession roomSession) {
        this.inputState = inputState;
        this.inputConfig = inputConfig;
        this.playerState = playerState;
        this.link = link;
        this.roomSession = roomSession;
    }

    public InputState inputState() {
        return inputState;
    }

    public InputConfig inputConfig() {
        return inputConfig;
    }

    public PlayerState playerState() {
        return playerState;
    }

    public Link link() {
        return link;
    }

    public RoomSession roomSession() {
        return roomSession;
    }

    public boolean isPowerBraceletButtonHeld() {
        if (inputState == null || inputConfig == null || playerState == null) {
            return false;
        }
        if (playerState.itemB() == InventoryItem.POWER_BRACELET.romValue()) {
            return inputState.isDown(inputConfig.bKey());
        }
        return playerState.itemA() == InventoryItem.POWER_BRACELET.romValue() && inputState.isDown(inputConfig.aKey());
    }

    /** $14:537D uses newly pressed buttons for throws, with B-slot priority. */
    public boolean isThrowButtonPressed(boolean bomb) {
        if (inputState == null || inputConfig == null || playerState == null) return false;
        int item = bomb ? InventoryItem.BOMBS.romValue() : InventoryItem.POWER_BRACELET.romValue();
        if (playerState.itemB() == item) return inputState.wasPressed(inputConfig.bKey());
        if (playerState.itemA() == item) return inputState.wasPressed(inputConfig.aKey());
        return inputState.wasPressed(inputConfig.aKey()) || inputState.wasPressed(inputConfig.bKey());
    }

    public boolean isBombButtonHeld() {
        if (inputState == null || inputConfig == null || playerState == null) {
            return false;
        }
        if (playerState.itemB() == InventoryItem.BOMBS.romValue()) {
            return inputState.isDown(inputConfig.bKey());
        }
        return playerState.itemA() == InventoryItem.BOMBS.romValue() && inputState.isDown(inputConfig.aKey());
    }

    public void synchronizeLinkLiftedPresentation() {
        if (link == null || roomSession == null) {
            return;
        }
        var state = roomSession.rooms().liftedEntityState();
        if (state.active()) {
            link.setCarryingLiftedObjectState(state.carryState(), state.effectiveRomDirection());
        } else {
            if (link.isRoosterCarryActive()) {
                link.clearRoosterCarryState();
            }
            link.setCarryingLiftedObjectState(
                    0, Direction.fromJava(link.direction()).romIndex());
        }
    }
}
