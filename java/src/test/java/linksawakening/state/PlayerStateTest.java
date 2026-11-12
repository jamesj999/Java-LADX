package linksawakening.state;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import linksawakening.save.SaveRamImage;
import linksawakening.save.SaveRamLayout;
import linksawakening.save.SaveSlotState;
import org.junit.jupiter.api.Test;

final class PlayerStateTest {
    @Test
    void frameHealthFlagsStayLatchedAcrossHealthChanges() {
        PlayerState state = new PlayerState();
        state.vitals().setMaxHearts(3);
        state.vitals().setHealth(24);
        state.vitals().latchFullHearts();
        state.vitals().setHealth(4);
        assertTrue(state.vitals().fullHearts());
        state.playerResources().tickResourceBuffers(0);
        assertTrue(state.vitals().isOnLowHealth());
        state.vitals().setHealth(24);
        assertTrue(state.vitals().isOnLowHealth());
        state.playerResources().tickResourceBuffers(2);
        assertFalse(state.vitals().isOnLowHealth());
        state.vitals().setHealth(23);
        state.vitals().latchFullHearts();
        assertFalse(state.vitals().fullHearts());
        state.playerRewards().applyEntityPickup(0x2D);
        state.playerResources().tickResourceBuffers(3);
        assertEquals(24, state.vitals().health());
        assertFalse(state.vitals().fullHearts());
        state.vitals().latchFullHearts();
        assertTrue(state.vitals().fullHearts());
    }

    @Test
    void lowHealthBeepRunsOnEvenFramesAndReloadsAfterUnderflow() {
        PlayerState state = new PlayerState();
        state.vitals().setHealth(4);
        state.playerRewards().applyEntityPickup(0x2E);
        state.playerResources().tickResourceBuffers(0);
        assertTrue(state.vitals().isOnLowHealth());
        assertEquals(ResourceSound.LOW_HEARTS, state.playerSounds().takeResourceSound());
        for (int frame = 1; frame <= 48; frame++) {
            state.playerResources().tickResourceBuffers(frame);
            assertEquals(null, state.playerSounds().takeResourceSound());
        }
        state.playerResources().tickResourceBuffers(49);
        assertEquals(ResourceSound.LOW_HEARTS, state.playerSounds().takeResourceSound());
        state.vitals().setHealth(5);
        state.playerResources().tickResourceBuffers(50);
        assertFalse(state.vitals().isOnLowHealth());
    }

    @Test
    void refillChimeWinsOverLowHealthBeepInTheSameFrame() {
        PlayerState state = new PlayerState();
        state.vitals().setMaxHearts(6);
        state.vitals().setHealth(5);
        state.playerRewards().applyEntityPickup(0x2D);
        state.playerResources().tickResourceBuffers(1);
        assertEquals(6, state.vitals().health());
        assertEquals(ResourceSound.HEART_PICKED_UP, state.playerSounds().takeResourceSound());
        state.playerResources().tickResourceBuffers(3);
        assertEquals(null, state.playerSounds().takeResourceSound());
    }

    @Test
    void rupeeBufferUsesSixteenBitsAndAccelerates() {
        PlayerState state = new PlayerState();
        for (int i = 0; i < 300; i++) state.playerRewards().applyEntityPickup(0x2E);
        assertEquals(300, state.wallet().addRupeeBuffer());
        state.playerResources().tickResourceBuffers(0);
        assertEquals(9, state.wallet().rupees());
        assertEquals(291, state.wallet().addRupeeBuffer());
        assertEquals(ResourceSound.RUPEE, state.playerSounds().takeResourceSound());
    }

    @Test
    void lethalHealthTickConsumesMedicineAndQueuesRevival() {
        PlayerState state = new PlayerState();
        state.vitals().setHealth(1);
        state.progression().setHasMedicine(true);
        state.progression().setMedicineCount(1);
        state.vitals().applyRomEnemyDamage(2);
        state.playerResources().tickResourceBuffers(0);
        assertEquals(1, state.vitals().health());
        assertEquals(1, state.progression().medicineCount());
        state.playerResources().tickResourceBuffers(1);
        assertEquals(8, state.vitals().health());
        assertEquals(0x80, state.vitals().addHealthBuffer());
        assertEquals(0xA0, state.vitals().invincibilityCounter());
        assertEquals(0, state.progression().medicineCount());
        assertFalse(state.progression().hasMedicine());
        assertEquals(1, state.vitals().subtractHealthBuffer());
        state.playerResources().tickResourceBuffers(3);
        assertEquals(9, state.vitals().health());
    }

    @Test
    void fairyPickupHealsSixHeartsThroughTheResourceBuffer() {
        PlayerState playerState = new PlayerState();
        playerState.vitals().setMaxHearts(10);
        playerState.vitals().setHealth(8);
        playerState.playerRewards().applyEntityPickup(0x2F);
        assertEquals(8, playerState.vitals().health());
        assertEquals(0x30, playerState.vitals().addHealthBuffer());
        for (int frame = 0; frame < 96; frame++) playerState.playerResources().tickResourceBuffers(frame);
        assertEquals(56, playerState.vitals().health());
        assertEquals(0, playerState.vitals().addHealthBuffer());
    }

    @Test
    void toadstoolRewardSetsItsFlagAndPlacesPowderInInventoryWithoutAddingPowder() {
        PlayerState state = new PlayerState();
        state.playerInitialization().initializeNewGame(30, 30, 20);

        state.playerRewards().applyToadstoolReward();

        assertTrue(state.ammunition().hasToadstool());
        assertEquals(PlayerState.INVENTORY_MAGIC_POWDER, state.inventory().itemB());
        assertEquals(0, state.ammunition().magicPowderCount());
    }

    @Test
    void witchExchangeClearsTheUsedPowderSlotAndToadstoolThenAwardsTwentyUses() {
        PlayerState state = new PlayerState();
        state.playerInitialization().initializeNewGame(30, 30, 20);
        state.playerRewards().applyToadstoolReward();
        assertEquals(PlayerState.INVENTORY_MAGIC_POWDER, state.inventory().itemB());

        state.playerRewards().beginWitchToadstoolExchange(0);
        assertEquals(PlayerState.INVENTORY_EMPTY, state.inventory().itemB());
        assertFalse(state.ammunition().hasToadstool());

        state.playerRewards().applyWitchMagicPowderReward();
        assertEquals(PlayerState.INVENTORY_MAGIC_POWDER, state.inventory().itemB());
        assertEquals(20, state.ammunition().magicPowderCount());
    }

    @Test
    void witchExchangeCanClearTheAButtonSlotWithoutChangingB() {
        PlayerState state = new PlayerState();
        state.inventory().setItemB(PlayerState.INVENTORY_SWORD);
        state.inventory().setItemA(PlayerState.INVENTORY_MAGIC_POWDER);
        state.ammunition().setHasToadstool(true);

        state.playerRewards().beginWitchToadstoolExchange(1);

        assertEquals(PlayerState.INVENTORY_SWORD, state.inventory().itemB());
        assertEquals(PlayerState.INVENTORY_EMPTY, state.inventory().itemA());
        assertFalse(state.ammunition().hasToadstool());
    }

    @Test
    void witchPowderRewardUsesSourceBcdAdditionWithoutCapacityClamp() {
        PlayerState state = new PlayerState();
        state.playerInitialization().initializeNewGame(30, 30, 20);
        state.ammunition().setMagicPowderCount(10);

        state.playerRewards().applyWitchMagicPowderReward();

        assertEquals(30, state.ammunition().magicPowderCount());
    }

    @Test
    void damageSubtractsHealthAndClampsAtZero() {
        PlayerState playerState = new PlayerState();
        playerState.vitals().setHealth(PlayerState.HP_PER_HEART / 2);

        playerState.vitals().damage(PlayerState.HP_PER_HEART);

        assertEquals(0, playerState.vitals().health());
    }

    @Test
    void invincibilityCounterCanBeSetToRomPitRecoveryDuration() {
        PlayerState playerState = new PlayerState();

        playerState.vitals().setInvincibilityCounter(0x40);

        assertEquals(0x40, playerState.vitals().invincibilityCounter());
    }

    @Test
    void tickInvincibilityDecrementsUntilZeroAndClampsThere() {
        PlayerState playerState = new PlayerState();
        playerState.vitals().setInvincibilityCounter(2);

        playerState.vitals().tickInvincibility();
        assertEquals(1, playerState.vitals().invincibilityCounter());

        playerState.vitals().tickInvincibility();
        assertEquals(0, playerState.vitals().invincibilityCounter());

        playerState.vitals().tickInvincibility();
        assertEquals(0, playerState.vitals().invincibilityCounter());
    }

    @Test
    void swordLevelCanRepresentSwordlessAndDebugSwordStates() {
        PlayerState playerState = new PlayerState();

        assertEquals(1, playerState.equipmentLevels().swordLevel());

        playerState.equipmentLevels().setSwordLevel(0);
        assertEquals(0, playerState.equipmentLevels().swordLevel());

        playerState.equipmentLevels().setSwordLevel(3);
        assertEquals(2, playerState.equipmentLevels().swordLevel());
    }

    @Test
    void shieldLevelDefaultsToTheWoodenShieldAndCanRepresentTheMirrorShield() {
        PlayerState playerState = new PlayerState();

        assertEquals(1, playerState.equipmentLevels().shieldLevel());

        playerState.equipmentLevels().setShieldLevel(2);
        assertEquals(2, playerState.equipmentLevels().shieldLevel());

        playerState.equipmentLevels().setShieldLevel(0xFF);
        assertEquals(2, playerState.equipmentLevels().shieldLevel());
    }

    @Test
    void attackTunicAndPegasusBootsStateUseRomDefaultsAndValues() {
        PlayerState playerState = new PlayerState();

        assertEquals(PlayerState.TUNIC_GREEN, playerState.equipmentLevels().tunicType());
        assertFalse(playerState.equipmentLevels().runningWithPegasusBoots());

        playerState.equipmentLevels().setTunicType(PlayerState.TUNIC_RED);
        playerState.equipmentLevels().setRunningWithPegasusBoots(true);

        assertEquals(PlayerState.TUNIC_RED, playerState.equipmentLevels().tunicType());
        assertTrue(playerState.equipmentLevels().runningWithPegasusBoots());
    }

    @Test
    void pickupBuffersAdvanceOnTheSameAlternatingFramesAsTheRom() {
        PlayerState playerState = new PlayerState();
        playerState.vitals().setHealth(0);
        playerState.wallet().setRupees(0);

        playerState.playerRewards().applyEntityPickup(0x2D);
        playerState.playerRewards().applyEntityPickup(0x2E);

        assertEquals(8, playerState.vitals().addHealthBuffer());
        assertEquals(1, playerState.wallet().addRupeeBuffer());

        playerState.playerResources().tickResourceBuffers(0);
        assertEquals(0, playerState.vitals().health());
        assertEquals(1, playerState.wallet().rupees());

        playerState.playerResources().tickResourceBuffers(1);
        assertEquals(1, playerState.vitals().health());
        assertEquals(7, playerState.vitals().addHealthBuffer());
        assertEquals(1, playerState.wallet().rupees());
    }

    @Test
    void fourthHeartPieceRemainsVisibleUntilDialogThenQueuesEightHeartUnits() {
        PlayerState player = new PlayerState();
        player.progression().setHeartPieces(4);
        assertEquals(4, player.progression().heartPieces());
        player.vitals().setHealth(1);
        player.vitals().completeHeartPieceContainer();
        assertEquals(0, player.progression().heartPieces());
        assertEquals(4, player.vitals().maxHearts());
        assertEquals(0x40, player.vitals().addHealthBuffer());
        assertEquals(1, player.vitals().health());
    }

    @Test
    void heartContainerRewardRaisesMaximumAndQueuesAFullHeal() {
        PlayerState playerState = new PlayerState();
        playerState.vitals().setHealth(1);

        playerState.vitals().applyHeartContainerReward();

        assertEquals(4, playerState.vitals().maxHearts());
        assertEquals(0xFF, playerState.vitals().addHealthBuffer());
    }

    @Test
    void madamMeowMeowKissQueuesAFullHealWithoutChangingMaximumHearts() {
        PlayerState playerState = new PlayerState();
        int maxHearts = playerState.vitals().maxHearts();

        playerState.vitals().requestFullHeal();

        assertEquals(maxHearts, playerState.vitals().maxHearts());
        assertEquals(0xFF, playerState.vitals().addHealthBuffer());
    }

    @Test
    void beachSwordRewardGrantsTheInventorySwordAndLevelOne() {
        PlayerState playerState = new PlayerState();
        playerState.playerInitialization().initializeNewGame(0x30, 0x30, 0x20);

        playerState.playerRewards().applyBeachSwordReward();

        assertEquals(1, playerState.equipmentLevels().swordLevel());
        assertEquals(PlayerState.INVENTORY_SWORD, playerState.inventory().itemB());
    }

    @Test
    void blueTunicHalvesNominalDamageIntoTheRomBuffer() {
        PlayerState playerState = new PlayerState();
        playerState.equipmentLevels().setTunicType(PlayerState.TUNIC_BLUE);

        assertEquals(4, playerState.vitals().applyRomEnemyDamage(8));
        assertEquals(4, playerState.vitals().subtractHealthBuffer());
        assertEquals(0x50, playerState.vitals().invincibilityCounter());
    }

    @Test
    void blueTunicUsesTheRomRawShiftForOnePointDamage() {
        PlayerState playerState = new PlayerState();
        playerState.equipmentLevels().setTunicType(PlayerState.TUNIC_BLUE);

        assertEquals(0, playerState.vitals().applyRomEnemyDamage(1));
        assertEquals(0, playerState.vitals().subtractHealthBuffer());
    }

    @Test
    void guardianAcornNullifiesFourDamageButHalvesOtherDamage() {
        PlayerState playerState = new PlayerState();
        playerState.equipmentLevels().setActivePowerUp(PlayerState.ACTIVE_POWER_UP_GUARDIAN_ACORN);

        assertEquals(0, playerState.vitals().applyRomEnemyDamage(4));
        assertEquals(0, playerState.vitals().subtractHealthBuffer());
        assertEquals(0x50, playerState.vitals().invincibilityCounter());

        playerState.vitals().setInvincibilityCounter(0);
        assertEquals(4, playerState.vitals().applyRomEnemyDamage(8));
        assertEquals(4, playerState.vitals().subtractHealthBuffer());
    }

    @Test
    void enemyDamageDrainsOneHealthPointPerOddFrame() {
        PlayerState playerState = new PlayerState();
        playerState.vitals().setHealth(16);

        playerState.vitals().applyRomEnemyDamage(8);

        assertEquals(16, playerState.vitals().health());
        playerState.playerResources().tickResourceBuffers(0);
        assertEquals(16, playerState.vitals().health());
        playerState.playerResources().tickResourceBuffers(1);
        assertEquals(15, playerState.vitals().health());
        assertEquals(7, playerState.vitals().subtractHealthBuffer());
    }

    @Test
    void healingTakesPrecedenceOverPendingDamageWhenHealthIsNotFull() {
        PlayerState playerState = new PlayerState();
        playerState.vitals().setHealth(8);
        playerState.vitals().applyRomEnemyDamage(8);
        playerState.playerRewards().applyEntityPickup(0x2D);

        playerState.playerResources().tickResourceBuffers(1);

        assertEquals(9, playerState.vitals().health());
        assertEquals(7, playerState.vitals().addHealthBuffer());
        assertEquals(8, playerState.vitals().subtractHealthBuffer());
    }

    @Test
    void fullHealthClearsHealingAndFallsThroughToDamageReduction() {
        PlayerState playerState = new PlayerState();
        playerState.vitals().setMaxHearts(2);
        playerState.vitals().setHealth(16);
        playerState.vitals().applyRomEnemyDamage(8);
        playerState.playerRewards().applyEntityPickup(0x2D);

        playerState.playerResources().tickResourceBuffers(1);

        assertEquals(15, playerState.vitals().health());
        assertEquals(0, playerState.vitals().addHealthBuffer());
        assertEquals(7, playerState.vitals().subtractHealthBuffer());
    }

    @Test
    void activePowerUpExpiresAfterTheThirdAcceptedHitAndPickupResetsCount() {
        PlayerState playerState = new PlayerState();
        playerState.equipmentLevels().setActivePowerUp(PlayerState.ACTIVE_POWER_UP_PIECE_OF_POWER);

        playerState.vitals().applyRomEnemyDamage(8);
        playerState.vitals().setInvincibilityCounter(0);
        playerState.vitals().applyRomEnemyDamage(8);
        assertEquals(2, playerState.equipmentLevels().powerUpHits());
        assertEquals(
                PlayerState.ACTIVE_POWER_UP_PIECE_OF_POWER,
                playerState.equipmentLevels().activePowerUp());

        playerState.vitals().setInvincibilityCounter(0);
        playerState.vitals().applyRomEnemyDamage(8);
        assertEquals(3, playerState.equipmentLevels().powerUpHits());
        assertEquals(
                PlayerState.ACTIVE_POWER_UP_NONE, playerState.equipmentLevels().activePowerUp());

        playerState.equipmentLevels().setActivePowerUp(PlayerState.ACTIVE_POWER_UP_GUARDIAN_ACORN);
        assertEquals(0, playerState.equipmentLevels().powerUpHits());
    }

    @Test
    void enemyDamageAccumulatesAsAnUnsignedRomByte() {
        PlayerState playerState = new PlayerState();

        playerState.vitals().applyRomEnemyDamage(0xFF);
        playerState.vitals().setInvincibilityCounter(0);
        playerState.vitals().applyRomEnemyDamage(1);

        assertEquals(0, playerState.vitals().subtractHealthBuffer());
    }

    @Test
    void boundedAmmoPickupsMatchTheRomCapacityChecks() {
        PlayerState playerState = new PlayerState();
        playerState.ammunition().setMaxArrows(2);
        playerState.ammunition().setArrowCount(1);
        playerState.ammunition().setMaxBombs(2);
        playerState.ammunition().setBombCount(1);
        playerState.ammunition().setMaxMagicPowder(2);
        playerState.ammunition().setMagicPowderCount(1);

        playerState.playerRewards().applyEntityPickup(0x37);
        playerState.playerRewards().applyEntityPickup(0x38);
        playerState.playerRewards().applyEntityPickup(0x3B);
        playerState.playerRewards().applyEntityPickup(0x37);
        playerState.playerRewards().applyEntityPickup(0x38);
        playerState.playerRewards().applyEntityPickup(0x3B);

        assertEquals(2, playerState.ammunition().arrowCount());
        assertEquals(2, playerState.ammunition().bombCount());
        assertEquals(2, playerState.ammunition().magicPowderCount());
    }

    @Test
    void floatingItemEffectsMatchTheSixVariantDispatchEntries() {
        PlayerState playerState = new PlayerState();
        playerState.playerInitialization().initializeNewGame(99, 20, 20);
        playerState.ammunition().setMaxArrows(99);
        playerState.ammunition().setMaxBombs(20);
        playerState.ammunition().setMaxMagicPowder(20);
        playerState.vitals().setHealth(0);

        playerState.playerRewards().applyFloatingItemPickup(0x86, 0);
        assertEquals(10, playerState.wallet().addRupeeBuffer());

        playerState.playerRewards().applyFloatingItemPickup(0x86, 1);
        assertEquals(PlayerState.INVENTORY_MAGIC_POWDER, playerState.inventory().itemB());
        assertEquals(10, playerState.ammunition().magicPowderCount());

        playerState.playerRewards().applyFloatingItemPickup(0x86, 2);
        assertEquals(10, playerState.ammunition().bombCount());

        playerState.playerRewards().applyFloatingItemPickup(0x86, 3);
        assertEquals(10, playerState.wallet().addRupeeBuffer());

        playerState.playerRewards().applyFloatingItemPickup(0xE5, 4);
        assertEquals(0x18, playerState.vitals().addHealthBuffer());

        playerState.playerRewards().applyFloatingItemPickup(0xE5, 5);
        assertEquals(10, playerState.ammunition().arrowCount());
    }

    @Test
    void floatingBombAndPowderEffectsRespectTheirRomCapacityChecks() {
        PlayerState playerState = new PlayerState();
        playerState.ammunition().setMaxBombs(12);
        playerState.ammunition().setBombCount(8);
        playerState.ammunition().setMaxMagicPowder(12);
        playerState.ammunition().setMagicPowderCount(8);

        playerState.playerRewards().applyFloatingItemPickup(0x86, 2);
        playerState.playerRewards().applyFloatingItemPickup(0x86, 1);

        assertEquals(12, playerState.ammunition().bombCount());
        assertEquals(12, playerState.ammunition().magicPowderCount());
    }

    @Test
    void chestRewardsApplyTheImmediateInventoryLevelAndBufferWrites() {
        PlayerState playerState = new PlayerState();
        playerState.playerInitialization().initializeNewGame(99, 20, 20);

        playerState.chestRewards().applyChestReward(linksawakening.world.room.ChestContentsTable.CHEST_POWER_BRACELET);
        playerState.chestRewards().applyChestReward(linksawakening.world.room.ChestContentsTable.CHEST_SHIELD);
        playerState.chestRewards().applyChestReward(linksawakening.world.room.ChestContentsTable.CHEST_BOMB);
        playerState.chestRewards().applyChestReward(linksawakening.world.room.ChestContentsTable.CHEST_RUPEES_50);
        playerState.chestRewards().applyChestReward(linksawakening.world.room.ChestContentsTable.CHEST_SEASHELL);
        playerState.chestRewards().applyChestReward(linksawakening.world.room.ChestContentsTable.CHEST_FLIPPERS);
        playerState.chestRewards().applyChestReward(linksawakening.world.room.ChestContentsTable.CHEST_MEDICINE);
        playerState.chestRewards().applyChestReward(linksawakening.world.room.ChestContentsTable.CHEST_TAIL_KEY);
        playerState.chestRewards().applyChestReward(linksawakening.world.room.ChestContentsTable.CHEST_ANGLER_KEY);
        playerState.chestRewards().applyChestReward(linksawakening.world.room.ChestContentsTable.CHEST_FACE_KEY);
        playerState.chestRewards().applyChestReward(linksawakening.world.room.ChestContentsTable.CHEST_BIRD_KEY);
        playerState.chestRewards().applyChestReward(linksawakening.world.room.ChestContentsTable.CHEST_GOLD_LEAF);

        assertEquals(1, playerState.equipmentLevels().powerBraceletLevel());
        assertEquals(1, playerState.equipmentLevels().shieldLevel());
        assertEquals(1, playerState.ammunition().bombCount());
        assertEquals(50, playerState.wallet().addRupeeBuffer());
        assertEquals(1, playerState.progression().seashells());
        assertTrue(playerState.progression().hasFlippers());
        assertTrue(playerState.progression().hasMedicine());
        assertEquals(1, playerState.progression().medicineCount());
        assertEquals(1, playerState.progression().tailKeyCount());
        assertEquals(1, playerState.progression().anglerKeyCount());
        assertEquals(1, playerState.progression().faceKeyCount());
        assertEquals(1, playerState.progression().birdKeyCount());
        assertEquals(1, playerState.progression().goldenLeavesCount());
        assertEquals(
                1,
                playerState
                        .progression()
                        .chestItemCount(linksawakening.world.room.ChestContentsTable.CHEST_POWER_BRACELET));
    }

    @Test
    void floatingArrowsUseDecimalModulo100WithoutCapacityCheck() {
        PlayerState playerState = new PlayerState();
        playerState.playerInitialization().initializeNewGame(99, 20, 20);
        playerState.ammunition().setArrowCount(95);

        playerState.playerRewards().applyFloatingItemPickup(0xE5, 5);

        assertEquals(5, playerState.ammunition().arrowCount());
    }

    @Test
    void initializeNewGameClearsTheDebugInventoryAndUsesThreeFullHearts() {
        PlayerState playerState = new PlayerState();
        playerState.inventory().setItemA(PlayerState.INVENTORY_BOW);
        playerState.inventory().setItemB(PlayerState.INVENTORY_HOOKSHOT);
        playerState.vitals().setMaxHearts(12);
        playerState.vitals().setHealth(1);
        playerState.wallet().setRupees(999);

        playerState.playerInitialization().initializeNewGame(0x30, 0x30, 0x20);

        assertEquals(3, playerState.vitals().maxHearts());
        assertEquals(3 * PlayerState.HP_PER_HEART, playerState.vitals().health());
        assertEquals(0, playerState.wallet().rupees());
        assertEquals(0, playerState.equipmentLevels().swordLevel());
        assertEquals(0, playerState.equipmentLevels().shieldLevel());
        assertEquals(PlayerState.INVENTORY_EMPTY, playerState.inventory().itemA());
        assertEquals(PlayerState.INVENTORY_EMPTY, playerState.inventory().itemB());
        assertEquals(0x30, playerState.ammunition().maxArrows());
        assertEquals(0x30, playerState.ammunition().maxBombs());
        assertEquals(0x20, playerState.ammunition().maxMagicPowder());
        for (int slot = 0; slot < PlayerState.SUBSCREEN_SLOT_COUNT; slot++) {
            assertEquals(PlayerState.INVENTORY_EMPTY, playerState.inventory().subscreenItem(slot));
        }
    }

    @Test
    void initializeDebugStateProvidesMaxHeartsAndEveryUsableInventoryItem() {
        PlayerState state = new PlayerState();

        state.playerInitialization().initializeDebugState();

        assertEquals(PlayerState.MAX_HEARTS, state.vitals().maxHearts());
        assertEquals(
                PlayerState.MAX_HEARTS * PlayerState.HP_PER_HEART,
                state.vitals().health());
        assertEquals(PlayerState.MAX_RUPEES, state.wallet().rupees());
        assertEquals(PlayerState.INVENTORY_SWORD, state.inventory().itemA());
        assertEquals(PlayerState.INVENTORY_BOOMERANG, state.inventory().itemB());
        for (int slot = 0; slot < PlayerState.SUBSCREEN_SLOT_COUNT; slot++) {
            assertTrue(PlayerState.INVENTORY_EMPTY != state.inventory().subscreenItem(slot));
        }
        assertEquals(99, state.ammunition().arrowCount());
        assertEquals(99, state.ammunition().bombCount());
        assertEquals(99, state.ammunition().magicPowderCount());
        assertTrue(state.progression().hasFlippers());
        assertTrue(state.progression().hasMedicine());
        assertEquals(5, state.progression().goldenLeavesCount());
    }

    @Test
    void applySavedGameReplacesPersistentFieldsAndClearsTransientRuntimeState() {
        PlayerState playerState = new PlayerState();
        playerState.equipmentLevels().setActivePowerUp(PlayerState.ACTIVE_POWER_UP_PIECE_OF_POWER);
        playerState.vitals().setInvincibilityCounter(0x40);
        playerState.equipmentLevels().setRunningWithPegasusBoots(true);
        playerState.playerRewards().applyEntityPickup(0x2D);
        playerState.playerRewards().applyEntityPickup(0x2E);
        playerState.chestRewards().applyChestReward(linksawakening.world.room.ChestContentsTable.CHEST_POWER_BRACELET);

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
        bytes[main + SaveRamLayout.MAIN_SEASHELLS_OFFSET] = 0x19;
        bytes[main + SaveRamLayout.MAIN_FLIPPERS_OFFSET] = 1;
        bytes[main + SaveRamLayout.MAIN_POWER_BRACELET_OFFSET] = 2;
        bytes[main + SaveRamLayout.MAIN_MEDICINE_OFFSET] = 1;
        bytes[main + SaveRamLayout.MAIN_TRADE_SEQUENCE_ITEM_OFFSET] = 0x0E;
        bytes[main + SaveRamLayout.MAIN_MEDICINE_COUNT_OFFSET] = 2;
        bytes[main + SaveRamLayout.MAIN_TAIL_KEY_OFFSET] = 1;
        bytes[main + SaveRamLayout.MAIN_ANGLER_KEY_OFFSET] = 2;
        bytes[main + SaveRamLayout.MAIN_FACE_KEY_OFFSET] = 3;
        bytes[main + SaveRamLayout.MAIN_BIRD_KEY_OFFSET] = 4;
        bytes[main + SaveRamLayout.MAIN_GOLDEN_LEAVES_OFFSET] = 5;
        bytes[main + SaveRamLayout.MAIN_SHIELD_OFFSET] = 2;
        bytes[main + SaveRamLayout.MAIN_SWORD_OFFSET] = 2;
        bytes[main + SaveRamLayout.MAIN_ARROWS_OFFSET] = 4;
        bytes[main + SaveRamLayout.MAIN_MAX_ARROWS_OFFSET] = 0x12;
        bytes[main + SaveRamLayout.MAIN_BOMBS_OFFSET] = 5;
        bytes[main + SaveRamLayout.MAIN_MAX_BOMBS_OFFSET] = 0x10;
        bytes[main + SaveRamLayout.MAIN_MAGIC_POWDER_OFFSET] = 6;
        bytes[main + SaveRamLayout.MAIN_MAX_MAGIC_POWDER_OFFSET] = 0x20;
        bytes[main + SaveRamLayout.MAIN_RUPEE_HIGH_OFFSET] = 0x05;
        bytes[main + SaveRamLayout.MAIN_RUPEE_LOW_OFFSET] = 0x09;
        bytes[main + SaveRamLayout.MAIN_OCARINA_SONG_FLAGS_OFFSET] = 0x05;
        bytes[main + SaveRamLayout.MAIN_SELECTED_SONG_INDEX_OFFSET] = 0x02;
        bytes[main + SaveRamLayout.MAIN_NAME_OFFSET] = 1;
        bytes[main + SaveRamLayout.MAIN_NAME_OFFSET + 1] = 2;
        bytes[SaveRamLayout.slotOffset(0) + SaveRamLayout.dx3Offset()] = (byte) PlayerState.TUNIC_RED;

        SaveSlotState saved = SaveRamImage.fromBytes(bytes).readSlot(0);
        playerState.playerSaveLoader().applySavedGame(saved);

        assertEquals(509, playerState.wallet().rupees());
        assertEquals(17, playerState.vitals().health());
        assertEquals(5, playerState.vitals().maxHearts());
        assertEquals(2, playerState.progression().heartPieces());
        assertEquals(19, playerState.progression().seashells());
        assertTrue(playerState.progression().hasFlippers());
        assertTrue(playerState.progression().hasMedicine());
        assertEquals(0x0E, playerState.progression().tradeSequenceItem());
        assertEquals(2, playerState.progression().medicineCount());
        assertEquals(1, playerState.progression().tailKeyCount());
        assertEquals(2, playerState.progression().anglerKeyCount());
        assertEquals(3, playerState.progression().faceKeyCount());
        assertEquals(4, playerState.progression().birdKeyCount());
        assertEquals(5, playerState.progression().goldenLeavesCount());
        assertEquals(2, playerState.equipmentLevels().powerBraceletLevel());
        assertEquals(PlayerState.INVENTORY_BOW, playerState.inventory().itemB());
        assertEquals(PlayerState.INVENTORY_HOOKSHOT, playerState.inventory().itemA());
        assertEquals(0x30, playerState.inventory().subscreenItem(0));
        assertEquals(2, playerState.equipmentLevels().shieldLevel());
        assertEquals(2, playerState.equipmentLevels().swordLevel());
        assertEquals(4, playerState.ammunition().arrowCount());
        assertEquals(12, playerState.ammunition().maxArrows());
        assertEquals(5, playerState.ammunition().bombCount());
        assertEquals(10, playerState.ammunition().maxBombs());
        assertEquals(6, playerState.ammunition().magicPowderCount());
        assertEquals(20, playerState.ammunition().maxMagicPowder());
        assertEquals(0x05, playerState.progression().ocarinaSongFlags());
        assertEquals(0x02, playerState.progression().selectedSongIndex());
        assertEquals(PlayerState.TUNIC_RED, playerState.equipmentLevels().tunicType());
        assertEquals(0, playerState.vitals().invincibilityCounter());
        assertEquals(
                PlayerState.ACTIVE_POWER_UP_NONE, playerState.equipmentLevels().activePowerUp());
        assertFalse(playerState.equipmentLevels().runningWithPegasusBoots());
        assertEquals(0, playerState.vitals().addHealthBuffer());
        assertEquals(0, playerState.wallet().addRupeeBuffer());
        assertEquals(0, playerState.vitals().subtractHealthBuffer());
        assertEquals(
                0,
                playerState
                        .progression()
                        .chestItemCount(linksawakening.world.room.ChestContentsTable.CHEST_POWER_BRACELET));
        assertArrayEquals(new int[] {1, 2, 0, 0, 0}, saved.nameBytes());
    }
}
