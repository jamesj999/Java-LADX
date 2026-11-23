package linksawakening.gameplay;

import java.util.Objects;
import linksawakening.state.PlayerState;
import linksawakening.world.entity.event.SwordPickupRewardEvent;
import linksawakening.world.room.RoomSession;

/** Stateless application of pending beach-sword room rewards to player state. */
public final class BeachSwordRewardConsumer {
    private BeachSwordRewardConsumer() {}

    public static void consume(RoomSession roomSession, PlayerState playerState) {
        Objects.requireNonNull(roomSession, "roomSession");
        Objects.requireNonNull(playerState, "playerState");

        var swordPickupRewards = roomSession.entityState().consumeSwordPickupRewards();
        for (int ignoredIndex = 0; ignoredIndex < swordPickupRewards.size(); ignoredIndex++) {
            SwordPickupRewardEvent ignored = swordPickupRewards.get(ignoredIndex);

            playerState.playerRewards().applyBeachSwordReward();
        }
    }
}
