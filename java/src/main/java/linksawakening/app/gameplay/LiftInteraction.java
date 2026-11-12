package linksawakening.app.gameplay;

import java.util.Objects;
import linksawakening.entity.Link;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.state.Direction;
import linksawakening.state.PlayerState;
import linksawakening.world.room.RoomSession;

/** Synchronizes world-owned lifted-object state with Link presentation. */
public final class LiftInteraction {
    private final LiftButtons buttons;
    private final Link link;
    private final RoomSession roomSession;

    public LiftInteraction(
            InputState inputState,
            InputConfig inputConfig,
            PlayerState playerState,
            Link link,
            RoomSession roomSession) {
        buttons = new LiftButtons(inputState, inputConfig, playerState);
        this.link = Objects.requireNonNull(link);
        this.roomSession = Objects.requireNonNull(roomSession);
    }

    public boolean isPowerBraceletButtonHeld() {
        return buttons.isPowerBraceletButtonHeld();
    }

    public boolean isBombButtonHeld() {
        return buttons.isBombButtonHeld();
    }

    public boolean isThrowButtonPressed(boolean bomb) {
        return buttons.isThrowButtonPressed(bomb);
    }

    public void synchronizeLinkLiftedPresentation() {
        var state = roomSession.rooms().liftedEntityState();
        if (state.active()) {
            link.actions().setCarryingLiftedObjectState(state.carryState(), state.effectiveRomDirection());
        } else {
            if (link.actions().isRoosterCarryActive()) {
                link.actions().clearRoosterCarryState();
            }
            link.actions()
                    .setCarryingLiftedObjectState(
                            0, Direction.fromJava(link.animation().direction()).romIndex());
        }
    }
}
