package linksawakening.state;

import static linksawakening.state.PlayerState.*;

/** Aggregate of inventory, resources, progression and equipment state. */

/** PlayerProgressionCompatibility behavior on the shared PlayerState state. */
abstract class PlayerProgressionCompatibility extends PlayerResourceCompatibility {
    PlayerProgressionCompatibility() {
        super();
    }

    /** @deprecated Use {@link #vitals()}. */
    @Deprecated(forRemoval = true)
    public void setInvincibilityCounter(int value) {
        vitals.setInvincibilityCounter(value);
    }

    /** @deprecated Use {@link #vitals()}. */
    @Deprecated(forRemoval = true)
    public void tickInvincibility() {
        vitals.tickInvincibility();
    }

    /** @deprecated Use {@link #vitals()}. */
    @Deprecated(forRemoval = true)
    public int addHealthBuffer() {
        return vitals.addHealthBuffer();
    }

    /** @deprecated Use {@link #vitals()}. */
    @Deprecated(forRemoval = true)
    public void addSubtractHealthBuffer(int amount) {
        vitals.addSubtractHealthBuffer(amount);
    }

    /** @deprecated Use {@link #vitals()}. */
    @Deprecated(forRemoval = true)
    public int subtractHealthBuffer() {
        return vitals.subtractHealthBuffer();
    }

    /** @deprecated Use {@link #vitals()}. */
    @Deprecated(forRemoval = true)
    public void clearSubtractHealthBuffer() {
        vitals.clearSubtractHealthBuffer();
    }

    /** @deprecated Use {@link #wallet()}. */
    @Deprecated(forRemoval = true)
    public int addRupeeBuffer() {
        return wallet.addRupeeBuffer();
    }

    /** @deprecated Use {@link #playerResources()}. */
    @Deprecated(forRemoval = true)
    public void tickResourceBuffers(int frameCounter) {
        playerResources.tickResourceBuffers(frameCounter);
    }

    /** @deprecated Use {@link #playerRewards()}. */
    @Deprecated(forRemoval = true)
    public void applyEntityPickup(int entityType) {
        playerRewards.applyEntityPickup(entityType);
    }

    /** @deprecated Use {@link #vitals()}. */
    @Deprecated(forRemoval = true)
    public void completeHeartPieceContainer() {
        vitals.completeHeartPieceContainer();
    }

    /** @deprecated Use {@link #vitals()}. */
    @Deprecated(forRemoval = true)
    public void applyHeartContainerReward() {
        vitals.applyHeartContainerReward();
    }

    /** @deprecated Use {@link #vitals()}. */
    @Deprecated(forRemoval = true)
    public void requestFullHeal() {
        vitals.requestFullHeal();
    }

    /** @deprecated Use {@link #playerRewards()}. */
    @Deprecated(forRemoval = true)
    public void applyBeachSwordReward() {
        playerRewards.applyBeachSwordReward();
    }

    /** @deprecated Use {@link #playerRewards()}. */
    @Deprecated(forRemoval = true)
    public void applyToadstoolReward() {
        playerRewards.applyToadstoolReward();
    }

    /** @deprecated Use {@link #ammunition()}. */
    @Deprecated(forRemoval = true)
    public boolean hasToadstool() {
        return ammunition.hasToadstool();
    }

    /** @deprecated Use {@link #ammunition()}. */
    @Deprecated(forRemoval = true)
    public void setHasToadstool(boolean value) {
        ammunition.setHasToadstool(value);
    }

    /** @deprecated Use {@link #playerRewards()}. */
    @Deprecated(forRemoval = true)
    public void beginWitchToadstoolExchange(int inventorySlot) {
        playerRewards.beginWitchToadstoolExchange(inventorySlot);
    }

    /** @deprecated Use {@link #playerRewards()}. */
    @Deprecated(forRemoval = true)
    public void applyWitchMagicPowderReward() {
        playerRewards.applyWitchMagicPowderReward();
    }

    /** @deprecated Use {@link #playerRewards()}. */
    @Deprecated(forRemoval = true)
    public void applyFloatingItemPickup(int entityType, int sourceVariant) {
        playerRewards.applyFloatingItemPickup(entityType, sourceVariant);
    }

    /** @deprecated Use {@link #chestRewards()}. */
    @Deprecated(forRemoval = true)
    public void applyChestReward(int chestItem) {
        chestRewards.applyChestReward(chestItem);
    }

    /** @deprecated Use {@link #equipmentLevels()}. */
    @Deprecated(forRemoval = true)
    public int swordLevel() {
        return equipmentLevels.swordLevel();
    }

    /** @deprecated Use {@link #equipmentLevels()}. */
    @Deprecated(forRemoval = true)
    public void setSwordLevel(int value) {
        equipmentLevels.setSwordLevel(value);
    }

    /** @deprecated Use {@link #equipmentLevels()}. */
    @Deprecated(forRemoval = true)
    public int shieldLevel() {
        return equipmentLevels.shieldLevel();
    }

    /** @deprecated Use {@link #equipmentLevels()}. */
    @Deprecated(forRemoval = true)
    public void setShieldLevel(int value) {
        equipmentLevels.setShieldLevel(value);
    }

    /** @deprecated Use {@link #equipmentLevels()}. */
    @Deprecated(forRemoval = true)
    public int powerBraceletLevel() {
        return equipmentLevels.powerBraceletLevel();
    }

    /** @deprecated Use {@link #equipmentLevels()}. */
    @Deprecated(forRemoval = true)
    public void setPowerBraceletLevel(int value) {
        equipmentLevels.setPowerBraceletLevel(value);
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public boolean hasMedicine() {
        return progression.hasMedicine();
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public void setHasMedicine(boolean value) {
        progression.setHasMedicine(value);
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public boolean hasFlippers() {
        return progression.hasFlippers();
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public void setHasFlippers(boolean value) {
        progression.setHasFlippers(value);
    }
}
