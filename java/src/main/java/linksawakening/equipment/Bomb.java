package linksawakening.equipment;

import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.state.PlayerState;

import java.util.Objects;

/**
 * Edge-triggered ordinary bomb use from {@code PlaceBomb} (bank0.asm:$135A).
 * The room owns the entity slot; this item owns the inventory and sound-side
 * ordering around the spawn request.
 */
public final class Bomb implements EquippedItem {

    public interface PlacementTarget {
        boolean placeBomb();

        boolean bombActive();
    }

    private final PlayerState playerState;
    private final GameplaySoundSink soundSink;
    private final PlacementTarget target;

    public Bomb(PlayerState playerState, GameplaySoundSink soundSink, PlacementTarget target) {
        this.playerState = Objects.requireNonNull(playerState, "playerState");
        this.soundSink = Objects.requireNonNull(soundSink, "soundSink");
        this.target = Objects.requireNonNull(target, "target");
    }

    @Override
    public void onPress() {
        // PlaceBomb tests wHasPlacedBomb before reading the inventory count.
        if (target.bombActive()) {
            return;
        }
        if (playerState.bombCount() == 0) {
            soundSink.play(GameplaySoundEvent.WRONG_ANSWER);
            return;
        }

        // The ROM spends the bomb before SpawnPlayerProjectile. Do not refund
        // it if the room target cannot allocate a slot.
        playerState.setBombCount(playerState.bombCount() - 1);
        target.placeBomb();
    }
}
