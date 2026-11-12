package linksawakening.app.gameplay;

import static linksawakening.world.room.RoomConstants.*;

import java.util.function.IntUnaryOperator;
import linksawakening.entity.Link;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.state.PlayerState;
import linksawakening.world.room.RoomSession;

/** Focused frame interactions using the current session collaborators. */
public final class LiftInteraction {
    private final InputState inputState;
    private final InputConfig inputConfig;
    private final PlayerState playerState;
    private final Link link;
    private final RoomSession roomSession;
    private final IntUnaryOperator direction;

    public LiftInteraction(
            InputState inputState,
            InputConfig inputConfig,
            PlayerState playerState,
            Link link,
            RoomSession roomSession,
            IntUnaryOperator direction) {
        this.inputState = inputState;
        this.inputConfig = inputConfig;
        this.playerState = playerState;
        this.link = link;
        this.roomSession = roomSession;
        this.direction = direction;
    }

    public LiftInteraction(linksawakening.app.SessionContext context) {
        this(
                context.lifting_inputState(),
                context.lifting_inputConfig(),
                context.lifting_playerState(),
                context.lifting_link(),
                context.lifting_roomSession(),
                context.lifting_direction());
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

    public IntUnaryOperator direction() {
        return direction;
    }

    public boolean isPowerBraceletButtonHeld() {
        if (inputState == null || inputConfig == null || playerState == null) {
            return false;
        }
        if (playerState.itemB() == PlayerState.INVENTORY_POWER_BRACELET) {
            return inputState.isDown(inputConfig.bKey());
        }
        return playerState.itemA() == PlayerState.INVENTORY_POWER_BRACELET && inputState.isDown(inputConfig.aKey());
    }

    /** $14:537D uses newly pressed buttons for throws, with B-slot priority. */
    public boolean isThrowButtonPressed(boolean bomb) {
        if (inputState == null || inputConfig == null || playerState == null) return false;
        int item = bomb ? PlayerState.INVENTORY_BOMBS : PlayerState.INVENTORY_POWER_BRACELET;
        if (playerState.itemB() == item) return inputState.wasPressed(inputConfig.bKey());
        if (playerState.itemA() == item) return inputState.wasPressed(inputConfig.aKey());
        return inputState.wasPressed(inputConfig.aKey()) || inputState.wasPressed(inputConfig.bKey());
    }

    public boolean isBombButtonHeld() {
        if (inputState == null || inputConfig == null || playerState == null) {
            return false;
        }
        if (playerState.itemB() == PlayerState.INVENTORY_BOMBS) {
            return inputState.isDown(inputConfig.bKey());
        }
        return playerState.itemA() == PlayerState.INVENTORY_BOMBS && inputState.isDown(inputConfig.aKey());
    }

    public void synchronizeLinkLiftedPresentation() {
        if (link == null || roomSession == null) {
            return;
        }
        var state = roomSession.liftedEntityState();
        if (state.active()) {
            link.setCarryingLiftedObjectState(state.carryState(), state.effectiveRomDirection());
        } else {
            if (link.isRoosterCarryActive()) {
                link.clearRoosterCarryState();
            }
            link.setCarryingLiftedObjectState(0, direction.applyAsInt(link.direction()));
        }
    }
}
