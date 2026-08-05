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
    void blueTunicHalvesNominalDamageIntoTheRomBuffer() {
        PlayerState playerState = new PlayerState();
        playerState.setTunicType(PlayerState.TUNIC_BLUE);

        assertEquals(4, playerState.applyRomEnemyDamage(8));
        assertEquals(4, playerState.subtractHealthBuffer());
        assertEquals(0x50, playerState.invincibilityCounter());
    }

    @Test
    void blueTunicUsesTheRomRawShiftForOnePointDamage() {
        PlayerState playerState = new PlayerState();
        playerState.setTunicType(PlayerState.TUNIC_BLUE);

        assertEquals(0, playerState.applyRomEnemyDamage(1));
        assertEquals(0, playerState.subtractHealthBuffer());
    }

    @Test
    void guardianAcornNullifiesFourDamageButHalvesOtherDamage() {
        PlayerState playerState = new PlayerState();
        playerState.setActivePowerUp(PlayerState.ACTIVE_POWER_UP_GUARDIAN_ACORN);

        assertEquals(0, playerState.applyRomEnemyDamage(4));
        assertEquals(0, playerState.subtractHealthBuffer());
        assertEquals(0x50, playerState.invincibilityCounter());

        playerState.setInvincibilityCounter(0);
        assertEquals(4, playerState.applyRomEnemyDamage(8));
        assertEquals(4, playerState.subtractHealthBuffer());
    }

    @Test
    void enemyDamageDrainsOneHealthPointPerOddFrame() {
        PlayerState playerState = new PlayerState();
        playerState.setHealth(16);

        playerState.applyRomEnemyDamage(8);

        assertEquals(16, playerState.health());
        playerState.tickResourceBuffers(0);
        assertEquals(16, playerState.health());
        playerState.tickResourceBuffers(1);
        assertEquals(15, playerState.health());
        assertEquals(7, playerState.subtractHealthBuffer());
    }

    @Test
    void healingTakesPrecedenceOverPendingDamageWhenHealthIsNotFull() {
        PlayerState playerState = new PlayerState();
        playerState.setHealth(8);
        playerState.applyRomEnemyDamage(8);
        playerState.applyEntityPickup(0x2D);

        playerState.tickResourceBuffers(1);

        assertEquals(9, playerState.health());
        assertEquals(7, playerState.addHealthBuffer());
        assertEquals(8, playerState.subtractHealthBuffer());
    }

    @Test
    void fullHealthClearsHealingAndFallsThroughToDamageReduction() {
        PlayerState playerState = new PlayerState();
        playerState.setMaxHearts(2);
        playerState.setHealth(16);
        playerState.applyRomEnemyDamage(8);
        playerState.applyEntityPickup(0x2D);

        playerState.tickResourceBuffers(1);

        assertEquals(15, playerState.health());
        assertEquals(0, playerState.addHealthBuffer());
        assertEquals(7, playerState.subtractHealthBuffer());
    }

    @Test
    void activePowerUpExpiresAfterTheThirdAcceptedHitAndPickupResetsCount() {
        PlayerState playerState = new PlayerState();
        playerState.setActivePowerUp(PlayerState.ACTIVE_POWER_UP_PIECE_OF_POWER);

        playerState.applyRomEnemyDamage(8);
        playerState.setInvincibilityCounter(0);
        playerState.applyRomEnemyDamage(8);
        assertEquals(2, playerState.powerUpHits());
        assertEquals(PlayerState.ACTIVE_POWER_UP_PIECE_OF_POWER,
            playerState.activePowerUp());

        playerState.setInvincibilityCounter(0);
        playerState.applyRomEnemyDamage(8);
        assertEquals(3, playerState.powerUpHits());
        assertEquals(PlayerState.ACTIVE_POWER_UP_NONE, playerState.activePowerUp());

        playerState.setActivePowerUp(PlayerState.ACTIVE_POWER_UP_GUARDIAN_ACORN);
        assertEquals(0, playerState.powerUpHits());
    }

    @Test
    void enemyDamageAccumulatesAsAnUnsignedRomByte() {
        PlayerState playerState = new PlayerState();

        playerState.applyRomEnemyDamage(0xFF);
        playerState.setInvincibilityCounter(0);
        playerState.applyRomEnemyDamage(1);

        assertEquals(0, playerState.subtractHealthBuffer());
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
