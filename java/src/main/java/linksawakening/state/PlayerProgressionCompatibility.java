package linksawakening.state;

import static linksawakening.state.PlayerState.*;

/** @deprecated Temporary external API migration layer. */
@Deprecated(forRemoval = true)
abstract class PlayerProgressionCompatibility extends PlayerResourceCompatibility {
    @Deprecated(forRemoval = true)
    PlayerProgressionCompatibility() {
        super();
    }

    /** @deprecated Use vitals().setInvincibilityCounter(value). */
    @Deprecated(forRemoval = true)
    public void setInvincibilityCounter(int value) {
        vitals().setInvincibilityCounter(value);
    }

    /** @deprecated Use vitals().tickInvincibility(). */
    @Deprecated(forRemoval = true)
    public void tickInvincibility() {
        vitals().tickInvincibility();
    }

    /** @deprecated Use vitals().addHealthBuffer(). */
    @Deprecated(forRemoval = true)
    public int addHealthBuffer() {
        return vitals().addHealthBuffer();
    }

    /** @deprecated Use vitals().addSubtractHealthBuffer(amount). */
    @Deprecated(forRemoval = true)
    public void addSubtractHealthBuffer(int amount) {
        vitals().addSubtractHealthBuffer(amount);
    }

    /** @deprecated Use vitals().subtractHealthBuffer(). */
    @Deprecated(forRemoval = true)
    public int subtractHealthBuffer() {
        return vitals().subtractHealthBuffer();
    }

    /** @deprecated Use vitals().clearSubtractHealthBuffer(). */
    @Deprecated(forRemoval = true)
    public void clearSubtractHealthBuffer() {
        vitals().clearSubtractHealthBuffer();
    }

    /** @deprecated Use wallet().addRupeeBuffer(). */
    @Deprecated(forRemoval = true)
    public int addRupeeBuffer() {
        return wallet().addRupeeBuffer();
    }

    /** @deprecated Use playerResources().tickResourceBuffers(frameCounter). */
    @Deprecated(forRemoval = true)
    public void tickResourceBuffers(int frameCounter) {
        playerResources().tickResourceBuffers(frameCounter);
    }

    /** @deprecated Use playerRewards().applyEntityPickup(entityType). */
    @Deprecated(forRemoval = true)
    public void applyEntityPickup(int entityType) {
        playerRewards().applyEntityPickup(entityType);
    }

    /** @deprecated Use vitals().completeHeartPieceContainer(). */
    @Deprecated(forRemoval = true)
    public void completeHeartPieceContainer() {
        vitals().completeHeartPieceContainer();
    }

    /** @deprecated Use vitals().applyHeartContainerReward(). */
    @Deprecated(forRemoval = true)
    public void applyHeartContainerReward() {
        vitals().applyHeartContainerReward();
    }

    /** @deprecated Use vitals().requestFullHeal(). */
    @Deprecated(forRemoval = true)
    public void requestFullHeal() {
        vitals().requestFullHeal();
    }

    /** @deprecated Use playerRewards().applyBeachSwordReward(). */
    @Deprecated(forRemoval = true)
    public void applyBeachSwordReward() {
        playerRewards().applyBeachSwordReward();
    }

    /** @deprecated Use playerRewards().applyToadstoolReward(). */
    @Deprecated(forRemoval = true)
    public void applyToadstoolReward() {
        playerRewards().applyToadstoolReward();
    }

    /** @deprecated Use ammunition().hasToadstool(). */
    @Deprecated(forRemoval = true)
    public boolean hasToadstool() {
        return ammunition().hasToadstool();
    }

    /** @deprecated Use ammunition().setHasToadstool(value). */
    @Deprecated(forRemoval = true)
    public void setHasToadstool(boolean value) {
        ammunition().setHasToadstool(value);
    }

    /** @deprecated Use playerRewards().beginWitchToadstoolExchange(inventorySlot). */
    @Deprecated(forRemoval = true)
    public void beginWitchToadstoolExchange(int inventorySlot) {
        playerRewards().beginWitchToadstoolExchange(inventorySlot);
    }

    /** @deprecated Use playerRewards().applyWitchMagicPowderReward(). */
    @Deprecated(forRemoval = true)
    public void applyWitchMagicPowderReward() {
        playerRewards().applyWitchMagicPowderReward();
    }

    /** @deprecated Use playerRewards().applyFloatingItemPickup(entityType, sourceVariant). */
    @Deprecated(forRemoval = true)
    public void applyFloatingItemPickup(int entityType, int sourceVariant) {
        playerRewards().applyFloatingItemPickup(entityType, sourceVariant);
    }

    /** @deprecated Use chestRewards().applyChestReward(chestItem). */
    @Deprecated(forRemoval = true)
    public void applyChestReward(int chestItem) {
        chestRewards().applyChestReward(chestItem);
    }

    /** @deprecated Use equipmentLevels().swordLevel(). */
    @Deprecated(forRemoval = true)
    public int swordLevel() {
        return equipmentLevels().swordLevel();
    }

    /** @deprecated Use equipmentLevels().setSwordLevel(value). */
    @Deprecated(forRemoval = true)
    public void setSwordLevel(int value) {
        equipmentLevels().setSwordLevel(value);
    }

    /** @deprecated Use equipmentLevels().shieldLevel(). */
    @Deprecated(forRemoval = true)
    public int shieldLevel() {
        return equipmentLevels().shieldLevel();
    }

    /** @deprecated Use equipmentLevels().setShieldLevel(value). */
    @Deprecated(forRemoval = true)
    public void setShieldLevel(int value) {
        equipmentLevels().setShieldLevel(value);
    }

    /** @deprecated Use equipmentLevels().powerBraceletLevel(). */
    @Deprecated(forRemoval = true)
    public int powerBraceletLevel() {
        return equipmentLevels().powerBraceletLevel();
    }

    /** @deprecated Use equipmentLevels().setPowerBraceletLevel(value). */
    @Deprecated(forRemoval = true)
    public void setPowerBraceletLevel(int value) {
        equipmentLevels().setPowerBraceletLevel(value);
    }

    /** @deprecated Use progression().hasMedicine(). */
    @Deprecated(forRemoval = true)
    public boolean hasMedicine() {
        return progression().hasMedicine();
    }

    /** @deprecated Use progression().setHasMedicine(value). */
    @Deprecated(forRemoval = true)
    public void setHasMedicine(boolean value) {
        progression().setHasMedicine(value);
    }

    /** @deprecated Use progression().hasFlippers(). */
    @Deprecated(forRemoval = true)
    public boolean hasFlippers() {
        return progression().hasFlippers();
    }

    /** @deprecated Use progression().setHasFlippers(value). */
    @Deprecated(forRemoval = true)
    public void setHasFlippers(boolean value) {
        progression().setHasFlippers(value);
    }
}
