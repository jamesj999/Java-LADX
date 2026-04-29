package linksawakening.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class PlayerStateTest {

    @Test
    void damageSubtractsHealthAndClampsAtZero() {
        PlayerState playerState = new PlayerState();
        playerState.setHealth(PlayerState.HP_PER_HEART / 2);

        playerState.damage(PlayerState.HP_PER_HEART);

        assertEquals(0, playerState.health());
    }

    @Test
    void invincibilityCounterCanBeSetToRomPitRecoveryDuration() {
        PlayerState playerState = new PlayerState();

        playerState.setInvincibilityCounter(0x40);

        assertEquals(0x40, playerState.invincibilityCounter());
    }

    @Test
    void tickInvincibilityDecrementsUntilZeroAndClampsThere() {
        PlayerState playerState = new PlayerState();
        playerState.setInvincibilityCounter(2);

        playerState.tickInvincibility();
        assertEquals(1, playerState.invincibilityCounter());

        playerState.tickInvincibility();
        assertEquals(0, playerState.invincibilityCounter());

        playerState.tickInvincibility();
        assertEquals(0, playerState.invincibilityCounter());
    }
}
