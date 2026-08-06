package linksawakening.startup;

import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class NewGameStartProfileTest {

    @Test
    void romNewGameConstantsMatchLoadSavedFileInitNewGame() {
        NewGameStartProfile profile = NewGameStartProfile.romDefaults();

        assertEquals(0x10, profile.mapId());
        assertEquals(0xA3, profile.roomId());
        assertEquals(0x50, profile.entryX());
        assertEquals(0x60, profile.entryY());
        assertEquals(0x30, profile.maxArrows());
        assertEquals(0x30, profile.maxBombs());
        assertEquals(0x20, profile.maxMagicPowder());
        assertEquals(0x03, profile.romDirection());
        assertEquals(0x00, profile.animationState());
        assertEquals(0x16, profile.wreckingBallRoom());
        assertEquals(0x50, profile.wreckingBallX());
        assertEquals(0x27, profile.wreckingBallY());
    }

    @Test
    void profileClearsDebugStateBeforeApplyingFreshGameValues() {
        PlayerState playerState = new PlayerState();
        playerState.setRupees(321);
        playerState.setMaxHearts(10);
        playerState.setHealth(1);
        playerState.setSwordLevel(2);
        playerState.setShieldLevel(2);
        playerState.setItemA(PlayerState.INVENTORY_BOW);
        playerState.setItemB(PlayerState.INVENTORY_HOOKSHOT);
        playerState.setMaxArrows(2);
        playerState.setArrowCount(2);
        playerState.setMaxBombs(2);
        playerState.setBombCount(2);
        playerState.setMaxMagicPowder(2);
        playerState.setMagicPowderCount(2);
        playerState.setTunicType(PlayerState.TUNIC_RED);
        playerState.setRunningWithPegasusBoots(true);
        playerState.setInvincibilityCounter(0x40);
        playerState.applyRomEnemyDamage(8);
        playerState.applyEntityPickup(0x2D);
        playerState.applyEntityPickup(0x2E);
        playerState.applyEntityPickup(0x3D);

        NewGameStartProfile.romDefaults().initializePlayerState(playerState);

        assertEquals(0, playerState.rupees());
        assertEquals(3, playerState.maxHearts());
        assertEquals(3 * PlayerState.HP_PER_HEART, playerState.health());
        assertEquals(0, playerState.swordLevel());
        assertEquals(0, playerState.shieldLevel());
        assertEquals(PlayerState.INVENTORY_EMPTY, playerState.itemA());
        assertEquals(PlayerState.INVENTORY_EMPTY, playerState.itemB());
        assertEquals(0x30, playerState.maxArrows());
        assertEquals(0, playerState.arrowCount());
        assertEquals(0x30, playerState.maxBombs());
        assertEquals(0, playerState.bombCount());
        assertEquals(0x20, playerState.maxMagicPowder());
        assertEquals(0, playerState.magicPowderCount());
        assertEquals(0, playerState.heartPieces());
        assertEquals(0, playerState.seashells());
        assertEquals(PlayerState.ACTIVE_POWER_UP_NONE, playerState.activePowerUp());
        assertEquals(PlayerState.TUNIC_GREEN, playerState.tunicType());
        assertEquals(0, playerState.invincibilityCounter());
        assertEquals(0, playerState.addHealthBuffer());
        assertEquals(0, playerState.subtractHealthBuffer());
        assertEquals(0, playerState.addRupeeBuffer());
        assertEquals(0, playerState.powerUpHits());
        assertEquals(false, playerState.runningWithPegasusBoots());
        for (int slot = 0; slot < PlayerState.SUBSCREEN_SLOT_COUNT; slot++) {
            assertEquals(PlayerState.INVENTORY_EMPTY, playerState.subscreenItem(slot));
        }
    }
}
