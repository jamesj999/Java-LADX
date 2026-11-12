package linksawakening.state;

import static linksawakening.state.PlayerNumbers.*;
import static linksawakening.state.PlayerState.*;

/** ChestRewards owns its player-state fields and their update rules. */
public final class ChestRewards {

    private final Ammunition ammunition;
    private final EquipmentLevels equipmentLevels;
    private final Inventory inventory;
    private final Progression progression;
    private final Wallet wallet;

    ChestRewards(
            Ammunition ammunition,
            EquipmentLevels equipmentLevels,
            Inventory inventory,
            Progression progression,
            Wallet wallet) {
        this.ammunition = ammunition;
        this.equipmentLevels = equipmentLevels;
        this.inventory = inventory;
        this.progression = progression;
        this.wallet = wallet;
    }

    public void applyChestReward(int chestItem) {
        if (chestItem >= ChestItemValues.CHEST_RUPEES_50 && chestItem <= ChestItemValues.CHEST_RUPEES_500)
            wallet.rupeeUpdateDelay = 0x18;
        if (chestItem < 0 || chestItem > ChestItemValues.CHEST_ZOL) {
            throw new IllegalArgumentException("Chest item must be an unsigned chest variant: " + chestItem);
        }
        if (chestItem < progression.chestItemCounts.length) {
            progression.chestItemCounts[chestItem] = Math.min(0xFF, progression.chestItemCounts[chestItem] + 1);
        }
        switch (chestItem) {
            case ChestItemValues.CHEST_POWER_BRACELET -> {
                equipmentLevels.powerBraceletLevel = Math.min(2, equipmentLevels.powerBraceletLevel + 1);
                inventory.giveInventoryItem(InventoryItem.POWER_BRACELET.romValue());
            }
            case ChestItemValues.CHEST_SHIELD -> {
                equipmentLevels.setShieldLevel(equipmentLevels.shieldLevel + 1);
                inventory.giveInventoryItem(InventoryItem.SHIELD.romValue());
            }
            case ChestItemValues.CHEST_BOW -> inventory.giveInventoryItem(InventoryItem.BOW.romValue());
            case ChestItemValues.CHEST_HOOKSHOT -> inventory.giveInventoryItem(InventoryItem.HOOKSHOT.romValue());
            case ChestItemValues.CHEST_MAGIC_ROD -> inventory.giveInventoryItem(InventoryItem.MAGIC_ROD.romValue());
            case ChestItemValues.CHEST_PEGASUS_BOOTS -> inventory.giveInventoryItem(
                    InventoryItem.PEGASUS_BOOTS.romValue());
            case ChestItemValues.CHEST_OCARINA -> inventory.giveInventoryItem(InventoryItem.OCARINA.romValue());
            case ChestItemValues.CHEST_FEATHER -> inventory.giveInventoryItem(InventoryItem.ROCS_FEATHER.romValue());
            case ChestItemValues.CHEST_SHOVEL -> inventory.giveInventoryItem(InventoryItem.SHOVEL.romValue());
            case ChestItemValues.CHEST_FLIPPERS -> progression.hasFlippers = true;
            case ChestItemValues.CHEST_MAGIC_POWDER_BAG -> {
                inventory.giveInventoryItem(InventoryItem.MAGIC_POWDER.romValue());
                ammunition.magicPowderCount = incrementUpTo(ammunition.magicPowderCount, ammunition.maxMagicPowder);
            }
            case ChestItemValues.CHEST_BOMB -> {
                inventory.giveInventoryItem(InventoryItem.BOMBS.romValue());
                ammunition.bombCount = incrementUpTo(ammunition.bombCount, ammunition.maxBombs);
            }
            case ChestItemValues.CHEST_SWORD -> inventory.giveInventoryItem(InventoryItem.SWORD.romValue());
            case ChestItemValues.CHEST_MEDICINE -> {
                progression.hasMedicine = true;
                progression.medicineCount = incrementByte(progression.medicineCount);
            }
            case ChestItemValues.CHEST_TAIL_KEY -> progression.tailKeyCount = incrementByte(progression.tailKeyCount);
            case ChestItemValues.CHEST_ANGLER_KEY -> progression.anglerKeyCount =
                    incrementByte(progression.anglerKeyCount);
            case ChestItemValues.CHEST_FACE_KEY -> progression.faceKeyCount = incrementByte(progression.faceKeyCount);
            case ChestItemValues.CHEST_BIRD_KEY -> progression.birdKeyCount = incrementByte(progression.birdKeyCount);
            case ChestItemValues.CHEST_GOLD_LEAF -> progression.goldenLeavesCount =
                    incrementByte(progression.goldenLeavesCount);
            case ChestItemValues.CHEST_RUPEES_50 -> wallet.addRupeeBuffer =
                    Math.min(0xFFFF, wallet.addRupeeBuffer + 50);
            case ChestItemValues.CHEST_RUPEES_20 -> wallet.addRupeeBuffer =
                    Math.min(0xFFFF, wallet.addRupeeBuffer + 20);
            case ChestItemValues.CHEST_RUPEES_100 -> wallet.addRupeeBuffer =
                    Math.min(0xFFFF, wallet.addRupeeBuffer + 100);
            case ChestItemValues.CHEST_RUPEES_200 -> wallet.addRupeeBuffer =
                    Math.min(0xFFFF, wallet.addRupeeBuffer + 200);
            case ChestItemValues.CHEST_RUPEES_500 -> wallet.addRupeeBuffer =
                    Math.min(0xFFFF, wallet.addRupeeBuffer + 500);
            case ChestItemValues.CHEST_SEASHELL -> progression.setSeashells(progression.seashells + 1);
            case ChestItemValues.CHEST_MESSAGE, ChestItemValues.CHEST_ZOL -> {
                // The source marks these rooms complete without applying a
                // normal inventory reward.
            }
            default -> {
                // Flippers, lens, keys, leaves, and dungeon items have their
                // source-specific WRAM counters in the room/dungeon bridges;
                // chestItemCounts retains the immediate collection fact until
                // those dedicated state fields are modeled.
            }
        }
    }
}
