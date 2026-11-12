package linksawakening.state;

import static linksawakening.state.PlayerState.*;

/** Aggregate of inventory, resources, progression and equipment state. */

/** PlayerResourceCompatibility behavior on the shared PlayerState state. */
abstract class PlayerResourceCompatibility extends PlayerStateComponents {
    PlayerResourceCompatibility() {
        super();
    }

    public Inventory inventory() {
        return inventory;
    }

    public Ammunition ammunition() {
        return ammunition;
    }

    public Progression progression() {
        return progression;
    }

    public EquipmentLevels equipmentLevels() {
        return equipmentLevels;
    }

    public Vitals vitals() {
        return vitals;
    }

    public Wallet wallet() {
        return wallet;
    }

    public PlayerSounds playerSounds() {
        return playerSounds;
    }

    public PlayerResources playerResources() {
        return playerResources;
    }

    public PlayerRewards playerRewards() {
        return playerRewards;
    }

    public ChestRewards chestRewards() {
        return chestRewards;
    }

    public PlayerInitialization playerInitialization() {
        return playerInitialization;
    }

    public PlayerSaveLoader playerSaveLoader() {
        return playerSaveLoader;
    }

    /** @deprecated Use {@link #vitals()}. */
    @Deprecated(forRemoval = true)
    public void latchFullHearts() {
        vitals.latchFullHearts();
    }

    /** @deprecated Use {@link #vitals()}. */
    @Deprecated(forRemoval = true)
    public boolean fullHearts() {
        return vitals.fullHearts();
    }

    /** @deprecated Use {@link #vitals()}. */
    @Deprecated(forRemoval = true)
    public boolean isOnLowHealth() {
        return vitals.isOnLowHealth();
    }

    /** @deprecated Use {@link #playerSounds()}. */
    @Deprecated(forRemoval = true)
    public linksawakening.gameplay.GameplaySoundEvent takeResourceSound() {
        ResourceSound sound = playerSounds.takeResourceSound();
        if (sound == null) return null;
        return switch (sound) {
            case LOW_HEARTS -> linksawakening.gameplay.GameplaySoundEvent.LOW_HEARTS;
            case HEART_PICKED_UP -> linksawakening.gameplay.GameplaySoundEvent.HEART_PICKED_UP;
            case RUPEE -> linksawakening.gameplay.GameplaySoundEvent.RUPEE;
        };
    }

    /** @deprecated Use {@link #playerInitialization()}. */
    @Deprecated(forRemoval = true)
    public void initializeNewGame(int maxArrows, int maxBombs, int maxMagicPowder) {
        playerInitialization.initializeNewGame(maxArrows, maxBombs, maxMagicPowder);
    }

    /** @deprecated Use {@link #playerInitialization()}. */
    @Deprecated(forRemoval = true)
    public void initializeDebugState() {
        playerInitialization.initializeDebugState();
    }

    /** @deprecated Use {@link #playerResources()}. */
    @Deprecated(forRemoval = true)
    public void clearResourceBuffersForDebug() {
        playerResources.clearResourceBuffersForDebug();
    }

    /** @deprecated Use {@link #wallet()}. */
    @Deprecated(forRemoval = true)
    public int rupees() {
        return wallet.rupees();
    }

    /** @deprecated Use {@link #wallet()}. */
    @Deprecated(forRemoval = true)
    public void setRupees(int value) {
        wallet.setRupees(value);
    }

    /** @deprecated Use {@link #vitals()}. */
    @Deprecated(forRemoval = true)
    public int maxHearts() {
        return vitals.maxHearts();
    }

    /** @deprecated Use {@link #vitals()}. */
    @Deprecated(forRemoval = true)
    public void setMaxHearts(int value) {
        vitals.setMaxHearts(value);
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public int deathCount() {
        return progression.deathCount();
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public void setDeathCount(int value) {
        progression.setDeathCount(value);
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public void incrementDeathCount() {
        progression.incrementDeathCount();
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public int deathCountBcd() {
        return progression.deathCountBcd();
    }

    /** @deprecated Use {@link #vitals()}. */
    @Deprecated(forRemoval = true)
    public int health() {
        return vitals.health();
    }

    /** @deprecated Use {@link #vitals()}. */
    @Deprecated(forRemoval = true)
    public void setHealth(int value) {
        vitals.setHealth(value);
    }

    /** @deprecated Use {@link #vitals()}. */
    @Deprecated(forRemoval = true)
    public void damage(int amount) {
        vitals.damage(amount);
    }

    /** @deprecated Use {@link #vitals()}. */
    @Deprecated(forRemoval = true)
    public int applyRomEnemyDamage(int nominalDamage) {
        return vitals.applyRomEnemyDamage(nominalDamage);
    }

    /** @deprecated Use {@link #vitals()}. */
    @Deprecated(forRemoval = true)
    public int invincibilityCounter() {
        return vitals.invincibilityCounter();
    }
}
