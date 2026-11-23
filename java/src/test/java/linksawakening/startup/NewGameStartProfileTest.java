package linksawakening.startup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import linksawakening.state.InventoryItem;
import linksawakening.state.PlayerState;
import linksawakening.state.PowerUp;
import linksawakening.state.Tunic;
import org.junit.jupiter.api.Test;

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
        playerState.wallet().setRupees(321);
        playerState.vitals().setMaxHearts(10);
        playerState.vitals().setHealth(1);
        playerState.equipmentLevels().setSwordLevel(2);
        playerState.equipmentLevels().setShieldLevel(2);
        playerState.inventory().setItemA(InventoryItem.BOW.romValue());
        playerState.inventory().setItemB(InventoryItem.HOOKSHOT.romValue());
        playerState.ammunition().setMaxArrows(2);
        playerState.ammunition().setArrowCount(2);
        playerState.ammunition().setMaxBombs(2);
        playerState.ammunition().setBombCount(2);
        playerState.ammunition().setMaxMagicPowder(2);
        playerState.ammunition().setMagicPowderCount(2);
        playerState.equipmentLevels().setTunicType(Tunic.RED.romValue());
        playerState.equipmentLevels().setRunningWithPegasusBoots(true);
        playerState.vitals().setInvincibilityCounter(0x40);
        playerState.vitals().applyRomEnemyDamage(8);
        playerState.playerRewards().applyEntityPickup(0x2D);
        playerState.playerRewards().applyEntityPickup(0x2E);
        playerState.playerRewards().applyEntityPickup(0x3D);

        NewGameStartProfile.romDefaults().initializePlayerState(playerState);

        assertEquals(0, playerState.wallet().rupees());
        assertEquals(3, playerState.vitals().maxHearts());
        assertEquals(3 * PlayerState.HP_PER_HEART, playerState.vitals().health());
        assertEquals(0, playerState.equipmentLevels().swordLevel());
        assertEquals(0, playerState.equipmentLevels().shieldLevel());
        assertEquals(InventoryItem.EMPTY.romValue(), playerState.inventory().itemA());
        assertEquals(InventoryItem.EMPTY.romValue(), playerState.inventory().itemB());
        assertEquals(30, playerState.ammunition().maxArrows());
        assertEquals(0, playerState.ammunition().arrowCount());
        assertEquals(30, playerState.ammunition().maxBombs());
        assertEquals(0, playerState.ammunition().bombCount());
        assertEquals(20, playerState.ammunition().maxMagicPowder());
        assertEquals(0, playerState.ammunition().magicPowderCount());
        assertEquals(0, playerState.progression().heartPieces());
        assertEquals(0, playerState.progression().seashells());
        assertEquals(PowerUp.NONE.romValue(), playerState.equipmentLevels().activePowerUp());
        assertEquals(Tunic.GREEN.romValue(), playerState.equipmentLevels().tunicType());
        assertEquals(0, playerState.vitals().invincibilityCounter());
        assertEquals(0, playerState.vitals().addHealthBuffer());
        assertEquals(0, playerState.vitals().subtractHealthBuffer());
        assertEquals(0, playerState.wallet().addRupeeBuffer());
        assertEquals(0, playerState.equipmentLevels().powerUpHits());
        assertEquals(false, playerState.equipmentLevels().runningWithPegasusBoots());
        for (int slot = 0; slot < PlayerState.SUBSCREEN_SLOT_COUNT; slot++) {
            assertEquals(InventoryItem.EMPTY.romValue(), playerState.inventory().subscreenItem(slot));
        }
    }
}
