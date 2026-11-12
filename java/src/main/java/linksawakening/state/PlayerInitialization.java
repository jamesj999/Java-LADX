package linksawakening.state;

import static linksawakening.state.PlayerNumbers.*;
import static linksawakening.state.PlayerState.*;

import java.util.Arrays;

/** PlayerInitialization owns its player-state fields and their update rules. */
public final class PlayerInitialization {

    private final Ammunition ammunition;
    private final EquipmentLevels equipmentLevels;
    private final Inventory inventory;
    private final PlayerSounds playerSounds;
    private final Progression progression;
    private final Vitals vitals;
    private final Wallet wallet;

    PlayerInitialization(
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

    public void initializeNewGame(int maxArrows, int maxBombs, int maxMagicPowder) {
        wallet.rupees = 0;
        vitals.maxHearts = 3;
        vitals.health = vitals.maxHearts * HP_PER_HEART;
        vitals.invincibilityCounter = 0;
        equipmentLevels.swordLevel = 0;
        equipmentLevels.shieldLevel = 0;
        equipmentLevels.powerBraceletLevel = 0;
        inventory.itemA = InventoryItem.EMPTY.romValue();
        inventory.itemB = InventoryItem.EMPTY.romValue();
        progression.ocarinaSongFlags = 0;
        progression.selectedSongIndex = 0;
        Arrays.fill(inventory.subscreen, InventoryItem.EMPTY.romValue());
        ammunition.arrowCount = 0;
        ammunition.maxArrows = clamp(maxArrows, 0, 99);
        ammunition.bombCount = 0;
        ammunition.maxBombs = clamp(maxBombs, 0, 99);
        ammunition.magicPowderCount = 0;
        ammunition.hasToadstool = false;
        ammunition.maxMagicPowder = clamp(maxMagicPowder, 0, 99);
        progression.deathCount = 0;
        progression.heartPieces = 0;
        progression.seashells = 0;
        progression.hasFlippers = false;
        progression.hasMedicine = false;
        progression.tradeSequenceItem = 0;
        progression.medicineCount = 0;
        progression.tailKeyCount = 0;
        progression.anglerKeyCount = 0;
        progression.faceKeyCount = 0;
        progression.birdKeyCount = 0;
        progression.goldenLeavesCount = 0;
        equipmentLevels.activePowerUp = PowerUp.NONE;
        equipmentLevels.tunicType = Tunic.GREEN;
        equipmentLevels.runningWithPegasusBoots = false;
        vitals.addHealthBuffer = 0;
        vitals.subtractHealthBuffer = 0;
        wallet.addRupeeBuffer = 0;
        wallet.rupeeUpdateDelay = 0;
        playerSounds.resourceSounds.clear();
        vitals.onLowHealth = false;
        vitals.timeToNextLowHealthSound = 0;
        equipmentLevels.powerUpHits = 0;
        Arrays.fill(progression.chestItemCounts, 0);
    }

    public void initializeDebugState() {
        vitals.setMaxHearts(MAX_HEARTS);
        vitals.setHealth(MAX_HEARTS * HP_PER_HEART);
        wallet.setRupees(MAX_RUPEES);
        equipmentLevels.setSwordLevel(2);
        equipmentLevels.setShieldLevel(2);
        equipmentLevels.setPowerBraceletLevel(2);
        ammunition.setMaxArrows(99);
        ammunition.setArrowCount(99);
        ammunition.setMaxBombs(99);
        ammunition.setBombCount(99);
        ammunition.setMaxMagicPowder(99);
        ammunition.setMagicPowderCount(99);
        inventory.setSubscreenItems(new int[] {
            InventoryItem.BOMBS.romValue(), InventoryItem.POWER_BRACELET.romValue(), InventoryItem.SHIELD.romValue(),
            InventoryItem.BOW.romValue(), InventoryItem.HOOKSHOT.romValue(), InventoryItem.MAGIC_ROD.romValue(),
            InventoryItem.PEGASUS_BOOTS.romValue(), InventoryItem.OCARINA.romValue(),
                    InventoryItem.ROCS_FEATHER.romValue(),
            InventoryItem.SHOVEL.romValue()
        });
        inventory.setItemA(InventoryItem.SWORD.romValue());
        inventory.setItemB(InventoryItem.BOOMERANG.romValue());
        progression.setOcarinaSongFlags(OcarinaSong.FROGS_SONG_OF_THE_SOUL.romValue()
                | OcarinaSong.MANBO_MAMBO.romValue()
                | OcarinaSong.BALLAD_OF_THE_WIND_FISH.romValue());
        progression.setHasFlippers(true);
        progression.setHasMedicine(true);
        progression.setMedicineCount(1);
        progression.setTailKeyCount(1);
        progression.setAnglerKeyCount(1);
        progression.setFaceKeyCount(1);
        progression.setBirdKeyCount(1);
        progression.setGoldenLeavesCount(5);
        progression.setSeashells(99);
        ammunition.setHasToadstool(true);
    }
}
