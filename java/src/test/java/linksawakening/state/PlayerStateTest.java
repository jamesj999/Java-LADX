package linksawakening.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void swordLevelCanRepresentSwordlessAndDebugSwordStates() {
        PlayerState playerState = new PlayerState();

        assertEquals(1, playerState.swordLevel());

        playerState.setSwordLevel(0);
        assertEquals(0, playerState.swordLevel());

        playerState.setSwordLevel(3);
        assertEquals(2, playerState.swordLevel());
    }

    @Test
    void shieldLevelDefaultsToTheWoodenShieldAndCanRepresentTheMirrorShield() {
        PlayerState playerState = new PlayerState();

        assertEquals(1, playerState.shieldLevel());

        playerState.setShieldLevel(2);
        assertEquals(2, playerState.shieldLevel());

        playerState.setShieldLevel(0xFF);
        assertEquals(2, playerState.shieldLevel());
    }

    @Test
    void attackTunicAndPegasusBootsStateUseRomDefaultsAndValues() {
        PlayerState playerState = new PlayerState();

        assertEquals(PlayerState.TUNIC_GREEN, playerState.tunicType());
        assertFalse(playerState.runningWithPegasusBoots());

        playerState.setTunicType(PlayerState.TUNIC_RED);
        playerState.setRunningWithPegasusBoots(true);

        assertEquals(PlayerState.TUNIC_RED, playerState.tunicType());
        assertTrue(playerState.runningWithPegasusBoots());
    }

    @Test
    void pickupBuffersAdvanceOnTheSameAlternatingFramesAsTheRom() {
        PlayerState playerState = new PlayerState();
        playerState.setHealth(0);
        playerState.setRupees(0);

        playerState.applyEntityPickup(0x2D);
        playerState.applyEntityPickup(0x2E);

        assertEquals(8, playerState.addHealthBuffer());
        assertEquals(1, playerState.addRupeeBuffer());

        playerState.tickResourceBuffers(0);
        assertEquals(0, playerState.health());
        assertEquals(1, playerState.rupees());

        playerState.tickResourceBuffers(1);
        assertEquals(1, playerState.health());
        assertEquals(7, playerState.addHealthBuffer());
        assertEquals(1, playerState.rupees());
    }

    @Test
    void boundedAmmoPickupsMatchTheRomCapacityChecks() {
        PlayerState playerState = new PlayerState();
        playerState.setMaxArrows(2);
        playerState.setArrowCount(1);
        playerState.setMaxBombs(2);
        playerState.setBombCount(1);
        playerState.setMaxMagicPowder(2);
        playerState.setMagicPowderCount(1);

        playerState.applyEntityPickup(0x37);
        playerState.applyEntityPickup(0x38);
        playerState.applyEntityPickup(0x3B);
        playerState.applyEntityPickup(0x37);
        playerState.applyEntityPickup(0x38);
        playerState.applyEntityPickup(0x3B);

        assertEquals(2, playerState.arrowCount());
        assertEquals(2, playerState.bombCount());
        assertEquals(2, playerState.magicPowderCount());
    }
}
