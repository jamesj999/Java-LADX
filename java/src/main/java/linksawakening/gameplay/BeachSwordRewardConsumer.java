package linksawakening.gameplay;

import java.util.Objects;

import linksawakening.state.PlayerState;
import linksawakening.world.RoomSession;

/** Applies pending beach-sword room rewards at the gameplay state boundary. */
public final class BeachSwordRewardConsumer {
    private BeachSwordRewardConsumer() {
    }

    public static void consume(RoomSession roomSession, PlayerState playerState) {
        Objects.requireNonNull(roomSession, "roomSession");
        Objects.requireNonNull(playerState, "playerState");

        for (var ignored : roomSession.consumeSwordPickupRewards()) {
            playerState.applyBeachSwordReward();
        }
    }
}
