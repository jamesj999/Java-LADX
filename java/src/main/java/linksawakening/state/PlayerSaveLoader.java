package linksawakening.state;

import static linksawakening.state.PlayerNumbers.*;
import static linksawakening.state.PlayerState.*;

import java.util.Arrays;
import java.util.Objects;
import linksawakening.save.SaveSlotState;

/** PlayerSaveLoader owns its player-state fields and their update rules. */
public final class PlayerSaveLoader {

    private final Ammunition ammunition;
    private final EquipmentLevels equipmentLevels;
    private final Inventory inventory;
    private final PlayerSounds playerSounds;
    private final Progression progression;
    private final Vitals vitals;
    private final Wallet wallet;

    PlayerSaveLoader(
            Ammunition ammunition,
            EquipmentLevels equipmentLevels,
            Inventory inventory,
            PlayerSounds playerSounds,
            Progression progression,
            Vitals vitals,
            Wallet wallet) {
        this.ammunition = ammunition;
        this.equipmentLevels = equipmentLevels;
        this.inventory = inventory;
        this.playerSounds = playerSounds;
        this.progression = progression;
        this.vitals = vitals;
        this.wallet = wallet;
    }

    public void applySavedGame(SaveSlotState saved) {
        Objects.requireNonNull(saved, "saved");
        vitals.setMaxHearts(saved.maxHearts());
        int deaths = saved.deathCountBcd();
        progression.setDeathCount((deaths >>> 8 & 15) * 100 + (deaths >>> 4 & 15) * 10 + (deaths & 15));
        vitals.setHealth(saved.health());
        wallet.setRupees(saved.rupees());
        progression.setHeartPieces(saved.heartPieces());
        progression.setSeashells(saved.seashells());
        progression.setHasFlippers(saved.hasFlippers() != 0);
        progression.setHasMedicine(saved.hasMedicine() != 0);
        progression.setTradeSequenceItem(saved.tradeSequenceItem());
        progression.setMedicineCount(saved.medicineCount());
        progression.setTailKeyCount(saved.tailKeyCount());
        progression.setAnglerKeyCount(saved.anglerKeyCount());
        progression.setFaceKeyCount(saved.faceKeyCount());
        progression.setBirdKeyCount(saved.birdKeyCount());
        progression.setGoldenLeavesCount(saved.goldenLeavesCount());
        equipmentLevels.setPowerBraceletLevel(saved.powerBraceletLevel());
        equipmentLevels.setSwordLevel(saved.swordLevel());
        equipmentLevels.setShieldLevel(saved.shieldLevel());
        inventory.setItemA(saved.itemA());
        inventory.setItemB(saved.itemB());
        inventory.setSubscreenItems(saved.subscreen());
        // Source pickups may exceed normal capacity (floating arrows / witch powder).
        // A load copies those saved counts; it must not apply pickup-capacity clamps.
        ammunition.setMaxArrows(saved.maxArrows());
        ammunition.arrowCount = clamp(saved.arrowCount(), 0, 99);
        ammunition.setMaxBombs(saved.maxBombs());
        ammunition.bombCount = clamp(saved.bombCount(), 0, 99);
        ammunition.setMaxMagicPowder(saved.maxMagicPowder());
        ammunition.magicPowderCount = clamp(saved.magicPowderCount(), 0, 99);
        ammunition.setHasToadstool(saved.hasToadstool() != 0);
        progression.setOcarinaSongFlags(saved.ocarinaSongFlags());
        progression.setSelectedSongIndex(saved.selectedSongIndex());
        equipmentLevels.setTunicType(saved.tunicType());

        vitals.invincibilityCounter = 0;
        equipmentLevels.activePowerUp = PowerUp.NONE;
        equipmentLevels.runningWithPegasusBoots = false;
        vitals.addHealthBuffer = 0;
        vitals.subtractHealthBuffer = 0;
        wallet.addRupeeBuffer = 0;
        wallet.rupeeUpdateDelay = 0;
        playerSounds.clear();
        vitals.onLowHealth = false;
        vitals.timeToNextLowHealthSound = 0;
        equipmentLevels.powerUpHits = 0;
        Arrays.fill(progression.chestItemCounts, 0);
    }
}
