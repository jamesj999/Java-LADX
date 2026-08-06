package linksawakening.state;

import linksawakening.save.SaveRamLayout;
import linksawakening.save.SaveRamImage;
import linksawakening.save.SaveSlotState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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

    @Test
    void initializeNewGameClearsTheDebugInventoryAndUsesThreeFullHearts() {
        PlayerState playerState = new PlayerState();
        playerState.setItemA(PlayerState.INVENTORY_BOW);
        playerState.setItemB(PlayerState.INVENTORY_HOOKSHOT);
        playerState.setMaxHearts(12);
        playerState.setHealth(1);
        playerState.setRupees(999);

        playerState.initializeNewGame(0x30, 0x30, 0x20);

        assertEquals(3, playerState.maxHearts());
        assertEquals(3 * PlayerState.HP_PER_HEART, playerState.health());
        assertEquals(0, playerState.rupees());
        assertEquals(0, playerState.swordLevel());
        assertEquals(0, playerState.shieldLevel());
        assertEquals(PlayerState.INVENTORY_EMPTY, playerState.itemA());
        assertEquals(PlayerState.INVENTORY_EMPTY, playerState.itemB());
        assertEquals(0x30, playerState.maxArrows());
        assertEquals(0x30, playerState.maxBombs());
        assertEquals(0x20, playerState.maxMagicPowder());
        for (int slot = 0; slot < PlayerState.SUBSCREEN_SLOT_COUNT; slot++) {
            assertEquals(PlayerState.INVENTORY_EMPTY, playerState.subscreenItem(slot));
        }
    }

    @Test
    void applySavedGameReplacesPersistentFieldsAndClearsTransientRuntimeState() {
        PlayerState playerState = new PlayerState();
        playerState.setActivePowerUp(PlayerState.ACTIVE_POWER_UP_PIECE_OF_POWER);
        playerState.setInvincibilityCounter(0x40);
        playerState.setRunningWithPegasusBoots(true);
        playerState.applyEntityPickup(0x2D);
        playerState.applyEntityPickup(0x2E);

        byte[] bytes = SaveRamImage.empty().bytes();
        int main = SaveRamLayout.slotOffset(0) + SaveRamLayout.mainOffset();
        bytes[main + SaveRamLayout.MAIN_ITEM_B_OFFSET] = (byte) PlayerState.INVENTORY_BOW;
        bytes[main + SaveRamLayout.MAIN_ITEM_A_OFFSET] = (byte) PlayerState.INVENTORY_HOOKSHOT;
        for (int slot = 0; slot < PlayerState.SUBSCREEN_SLOT_COUNT; slot++) {
            bytes[main + SaveRamLayout.MAIN_SUBSCREEN_OFFSET + slot] = (byte) (0x30 + slot);
        }
        bytes[main + SaveRamLayout.MAIN_HEALTH_OFFSET] = 17;
        bytes[main + SaveRamLayout.MAIN_MAX_HEARTS_OFFSET] = 5;
        bytes[main + SaveRamLayout.MAIN_HEART_PIECES_OFFSET] = 2;
        bytes[main + SaveRamLayout.MAIN_SEASHELLS_OFFSET] = 19;
        bytes[main + SaveRamLayout.MAIN_SHIELD_OFFSET] = 2;
        bytes[main + SaveRamLayout.MAIN_SWORD_OFFSET] = 2;
        bytes[main + SaveRamLayout.MAIN_ARROWS_OFFSET] = 4;
        bytes[main + SaveRamLayout.MAIN_MAX_ARROWS_OFFSET] = 12;
        bytes[main + SaveRamLayout.MAIN_BOMBS_OFFSET] = 5;
        bytes[main + SaveRamLayout.MAIN_MAX_BOMBS_OFFSET] = 10;
        bytes[main + SaveRamLayout.MAIN_MAGIC_POWDER_OFFSET] = 6;
        bytes[main + SaveRamLayout.MAIN_MAX_MAGIC_POWDER_OFFSET] = 20;
        bytes[main + SaveRamLayout.MAIN_RUPEE_HIGH_OFFSET] = 0x05;
        bytes[main + SaveRamLayout.MAIN_RUPEE_LOW_OFFSET] = 0x09;
        bytes[main + SaveRamLayout.MAIN_NAME_OFFSET] = 1;
        bytes[main + SaveRamLayout.MAIN_NAME_OFFSET + 1] = 2;
        bytes[SaveRamLayout.slotOffset(0) + SaveRamLayout.dx3Offset()] = (byte) PlayerState.TUNIC_RED;

        SaveSlotState saved = SaveRamImage.fromBytes(bytes).readSlot(0);
        playerState.applySavedGame(saved);

        assertEquals(509, playerState.rupees());
        assertEquals(17, playerState.health());
        assertEquals(5, playerState.maxHearts());
        assertEquals(2, playerState.heartPieces());
        assertEquals(19, playerState.seashells());
        assertEquals(PlayerState.INVENTORY_BOW, playerState.itemB());
        assertEquals(PlayerState.INVENTORY_HOOKSHOT, playerState.itemA());
        assertEquals(0x30, playerState.subscreenItem(0));
        assertEquals(2, playerState.shieldLevel());
        assertEquals(2, playerState.swordLevel());
        assertEquals(4, playerState.arrowCount());
        assertEquals(12, playerState.maxArrows());
        assertEquals(5, playerState.bombCount());
        assertEquals(10, playerState.maxBombs());
        assertEquals(6, playerState.magicPowderCount());
        assertEquals(20, playerState.maxMagicPowder());
        assertEquals(PlayerState.TUNIC_RED, playerState.tunicType());
        assertEquals(0, playerState.invincibilityCounter());
        assertEquals(PlayerState.ACTIVE_POWER_UP_NONE, playerState.activePowerUp());
        assertFalse(playerState.runningWithPegasusBoots());
        assertEquals(0, playerState.addHealthBuffer());
        assertEquals(0, playerState.addRupeeBuffer());
        assertEquals(0, playerState.subtractHealthBuffer());
        assertArrayEquals(new int[] {1, 2, 0, 0, 0}, saved.nameBytes());
    }
}
