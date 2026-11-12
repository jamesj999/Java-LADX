package linksawakening.state;

import static linksawakening.state.PlayerNumbers.*;
import static linksawakening.state.PlayerState.*;

import linksawakening.world.entity.event.FloatingItemPickupEffect;

/** PlayerRewards owns its player-state fields and their update rules. */
public final class PlayerRewards {

    private final Ammunition ammunition;
    private final EquipmentLevels equipmentLevels;
    private final Inventory inventory;
    private final Progression progression;
    private final Vitals vitals;
    private final Wallet wallet;

    PlayerRewards(
            Ammunition ammunition,
            EquipmentLevels equipmentLevels,
            Inventory inventory,
            Progression progression,
            Vitals vitals,
            Wallet wallet) {
        this.ammunition = ammunition;
        this.equipmentLevels = equipmentLevels;
        this.inventory = inventory;
        this.progression = progression;
        this.vitals = vitals;
        this.wallet = wallet;
    }

    public void applyEntityPickup(int entityType) {
        switch (entityType & 0xFF) {
            case 0x2D -> vitals.addHealthBuffer = Math.min(0xFF, vitals.addHealthBuffer + HP_PER_HEART);
                // PickDroppableFairy ($03:64C6) adds six hearts to the byte buffer.
            case 0x2F -> vitals.addHealthBuffer = (vitals.addHealthBuffer + 0x30) & 0xFF;
            case 0x2E -> wallet.addRupeeBuffer = Math.min(0xFFFF, wallet.addRupeeBuffer + 1);
            case 0x33 -> equipmentLevels.setPowerUp(PowerUp.PIECE_OF_POWER);
            case 0x34 -> equipmentLevels.setPowerUp(PowerUp.GUARDIAN_ACORN);
            case 0x37 -> ammunition.arrowCount = incrementUpTo(ammunition.arrowCount, ammunition.maxArrows);
            case 0x38 -> {
                inventory.giveInventoryItem(InventoryItem.BOMBS.romValue());
                ammunition.bombCount = incrementUpTo(ammunition.bombCount, ammunition.maxBombs);
            }
            case 0x3B -> {
                inventory.giveInventoryItem(InventoryItem.MAGIC_POWDER.romValue());
                ammunition.magicPowderCount = incrementUpTo(ammunition.magicPowderCount, ammunition.maxMagicPowder);
            }
            case 0x3D -> progression.seashells = Math.min(99, progression.seashells + 1);
            default -> {
                // Transition-driven pickups are advanced by their entity
                // handlers; their room event still reaches this method when
                // they need a future-specific state implementation.
            }
        }
    }

    public void applyBeachSwordReward() {
        inventory.giveInventoryItem(InventoryItem.SWORD.romValue());
        equipmentLevels.setSwordLevel(1);
    }

    public void applyToadstoolReward() {
        inventory.giveInventoryItem(InventoryItem.MAGIC_POWDER.romValue());
        ammunition.hasToadstool = true;
    }

    public void beginWitchToadstoolExchange(int inventorySlot) {
        if (inventorySlot == 0) {
            inventory.itemB = InventoryItem.EMPTY.romValue();
        } else if (inventorySlot == 1) {
            inventory.itemA = InventoryItem.EMPTY.romValue();
        } else {
            throw new IllegalArgumentException("Inventory slot must be B (0) or A (1): " + inventorySlot);
        }
        ammunition.hasToadstool = false;
    }

    public void applyWitchMagicPowderReward() {
        inventory.giveInventoryItem(InventoryItem.MAGIC_POWDER.romValue());
        // The player model stores quantities as decimal values. WitchEntityHandler
        // adds BCD $20 with DAA and does not clamp against wMaxMagicPowder.
        ammunition.magicPowderCount = Math.min(99, ammunition.magicPowderCount + 20);
    }

    public void applyFloatingItemPickup(int entityType, int sourceVariant) {
        switch (FloatingItemPickupEffect.pickupEffect(entityType, sourceVariant)) {
            case TEN_RUPEES -> wallet.addRupeeBuffer = 10;
            case MAGIC_POWDER -> {
                inventory.giveInventoryItem(InventoryItem.MAGIC_POWDER.romValue());
                ammunition.magicPowderCount =
                        incrementByTenUpTo(ammunition.magicPowderCount, ammunition.maxMagicPowder);
            }
            case TEN_BOMBS -> ammunition.bombCount = incrementByTenUpTo(ammunition.bombCount, ammunition.maxBombs);
            case HEALTH_18 -> vitals.addHealthBuffer = Math.min(0xFF, vitals.addHealthBuffer + 0x18);
                // FloatingArrowsHandler adds BCD $10 and performs no capacity
                // check. PlayerState stores semantic decimal counts, so the
                // equivalent is decimal modulo-100 addition.
            case TEN_ARROWS -> ammunition.arrowCount = (ammunition.arrowCount + 10) % 100;
        }
    }
}
