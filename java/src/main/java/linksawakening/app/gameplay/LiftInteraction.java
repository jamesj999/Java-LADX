package linksawakening.app.gameplay;

import linksawakening.entity.Link;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.state.PlayerState;
import linksawakening.world.room.RoomSession;

import static linksawakening.world.room.RoomConstants.*;

import java.util.function.IntUnaryOperator;

/** Focused frame interactions using the current session collaborators. */
public record LiftInteraction(
        InputState inputState,
        InputConfig inputConfig,
        PlayerState playerState,
        Link link,
        RoomSession roomSession,
        IntUnaryOperator direction) {
    public boolean isPowerBraceletButtonHeld() {
        if (inputState == null || inputConfig == null || playerState == null) {
            return false;
        }
        boolean braceletOnA = playerState.itemA() == PlayerState.INVENTORY_POWER_BRACELET
            && inputState.isDown(inputConfig.aKey());
        boolean braceletOnB = playerState.itemB() == PlayerState.INVENTORY_POWER_BRACELET
            && inputState.isDown(inputConfig.bKey());
        return braceletOnA || braceletOnB;
    }

    public boolean isBombButtonHeld() {
        if (inputState == null || inputConfig == null || playerState == null) {
            return false;
        }
        if (playerState.itemB() == PlayerState.INVENTORY_BOMBS) {
            return inputState.isDown(inputConfig.bKey());
        }
        return playerState.itemA() == PlayerState.INVENTORY_BOMBS
            && inputState.isDown(inputConfig.aKey());
    }

    public void synchronizeLinkLiftedPresentation() {
        if (link == null || roomSession == null) {
            return;
        }
        var state = roomSession.liftedEntityState();
        if (state.active()) {
            link.setCarryingLiftedObjectState(state.carryState(),
                state.effectiveRomDirection());
        } else {
            if (link.isRoosterCarryActive()) {
                link.clearRoosterCarryState();
            }
            link.setCarryingLiftedObjectState(0, direction.applyAsInt(link.direction()));
        }
    }
}
