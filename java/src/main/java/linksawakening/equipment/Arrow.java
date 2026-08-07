package linksawakening.equipment;

import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.state.PlayerState;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Edge-triggered ordinary bow use from {@code ShootArrow} (bank0.asm:$13BD). */
public final class Arrow implements EquippedItem {
    private static final int MAX_ACTIVE_PROJECTILES = 0x02;
    private static final int SHOOTING_COUNTDOWN = 0x10;

    public interface ShootTarget {
        boolean shootArrow();

        int activeProjectileCount();
    }

    private final PlayerState playerState;
    private final GameplaySoundSink soundSink;
    private final ShootTarget target;
    private final BooleanSupplier itemUseAllowed;
    private int shootingCountdown;

    public Arrow(PlayerState playerState, GameplaySoundSink soundSink, ShootTarget target) {
        this(playerState, soundSink, target, () -> true);
    }

    public Arrow(PlayerState playerState, GameplaySoundSink soundSink, ShootTarget target,
                 BooleanSupplier itemUseAllowed) {
        this.playerState = Objects.requireNonNull(playerState, "playerState");
        this.soundSink = Objects.requireNonNull(soundSink, "soundSink");
        this.target = Objects.requireNonNull(target, "target");
        this.itemUseAllowed = Objects.requireNonNull(itemUseAllowed, "itemUseAllowed");
    }

    @Override
    public void onPress() {
        if (!itemUseAllowed.getAsBoolean() || shootingCountdown != 0
            || target.activeProjectileCount() >= MAX_ACTIVE_PROJECTILES) {
            return;
        }

        // ShootArrow starts this gate before checking the inventory count.
        shootingCountdown = SHOOTING_COUNTDOWN;
        if (playerState.arrowCount() == 0) {
            soundSink.play(GameplaySoundEvent.WRONG_ANSWER);
            return;
        }

        // The ROM spends the arrow before SpawnPlayerProjectile. A failed
        // allocation therefore does not refund the inventory item.
        playerState.setArrowCount(playerState.arrowCount() - 1);
        if (target.shootArrow()) {
            soundSink.play(GameplaySoundEvent.ARROW_SHOT);
        }
    }

    @Override
    public void tick(boolean buttonHeld, int frameCounter) {
        if (shootingCountdown > 0) {
            shootingCountdown--;
        }
    }

    int shootingCountdownForTest() {
        return shootingCountdown;
    }
}
