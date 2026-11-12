package linksawakening.state;

import static linksawakening.state.PlayerState.*;

/** @deprecated Temporary external API migration layer. */
@Deprecated(forRemoval = true)
abstract class PlayerResourceCompatibility extends PlayerStateComponents {
    @Deprecated(forRemoval = true)
    PlayerResourceCompatibility() {
        super();
    }

    /** @deprecated Implemented directly by the aggregate; use its composed API. */
    @Deprecated(forRemoval = true)
    public abstract Inventory inventory();

    /** @deprecated Implemented directly by the aggregate; use its composed API. */
    @Deprecated(forRemoval = true)
    public abstract Ammunition ammunition();

    /** @deprecated Implemented directly by the aggregate; use its composed API. */
    @Deprecated(forRemoval = true)
    public abstract Progression progression();

    /** @deprecated Implemented directly by the aggregate; use its composed API. */
    @Deprecated(forRemoval = true)
    public abstract EquipmentLevels equipmentLevels();

    /** @deprecated Implemented directly by the aggregate; use its composed API. */
    @Deprecated(forRemoval = true)
    public abstract Vitals vitals();

    /** @deprecated Implemented directly by the aggregate; use its composed API. */
    @Deprecated(forRemoval = true)
    public abstract Wallet wallet();

    /** @deprecated Implemented directly by the aggregate; use its composed API. */
    @Deprecated(forRemoval = true)
    public abstract PlayerSounds playerSounds();

    /** @deprecated Implemented directly by the aggregate; use its composed API. */
    @Deprecated(forRemoval = true)
    public abstract PlayerResources playerResources();

    /** @deprecated Implemented directly by the aggregate; use its composed API. */
    @Deprecated(forRemoval = true)
    public abstract PlayerRewards playerRewards();

    /** @deprecated Implemented directly by the aggregate; use its composed API. */
    @Deprecated(forRemoval = true)
    public abstract ChestRewards chestRewards();

    /** @deprecated Implemented directly by the aggregate; use its composed API. */
    @Deprecated(forRemoval = true)
    public abstract PlayerInitialization playerInitialization();

    /** @deprecated Implemented directly by the aggregate; use its composed API. */
    @Deprecated(forRemoval = true)
    public abstract PlayerSaveLoader playerSaveLoader();

    /** @deprecated Use vitals().latchFullHearts(). */
    @Deprecated(forRemoval = true)
    public void latchFullHearts() {
        vitals().latchFullHearts();
    }

    /** @deprecated Use vitals().fullHearts(). */
    @Deprecated(forRemoval = true)
    public boolean fullHearts() {
        return vitals().fullHearts();
    }

    /** @deprecated Use vitals().isOnLowHealth(). */
    @Deprecated(forRemoval = true)
    public boolean isOnLowHealth() {
        return vitals().isOnLowHealth();
    }

    /** @deprecated Use {@link #playerSounds()}. */
    @Deprecated(forRemoval = true)
    public linksawakening.gameplay.GameplaySoundEvent takeResourceSound() {
        return playerSounds()
                .takeResourceSound(sound -> linksawakening.gameplay.GameplaySoundEvent.valueOf(sound.name()));
    }

    /** @deprecated Use playerInitialization().initializeNewGame(maxArrows, maxBombs, maxMagicPowder). */
    @Deprecated(forRemoval = true)
    public void initializeNewGame(int maxArrows, int maxBombs, int maxMagicPowder) {
        playerInitialization().initializeNewGame(maxArrows, maxBombs, maxMagicPowder);
    }

    /** @deprecated Use playerInitialization().initializeDebugState(). */
    @Deprecated(forRemoval = true)
    public void initializeDebugState() {
        playerInitialization().initializeDebugState();
    }

    /** @deprecated Use playerResources().clearResourceBuffersForDebug(). */
    @Deprecated(forRemoval = true)
    public void clearResourceBuffersForDebug() {
        playerResources().clearResourceBuffersForDebug();
    }

    /** @deprecated Use wallet().rupees(). */
    @Deprecated(forRemoval = true)
    public int rupees() {
        return wallet().rupees();
    }

    /** @deprecated Use wallet().setRupees(value). */
    @Deprecated(forRemoval = true)
    public void setRupees(int value) {
        wallet().setRupees(value);
    }

    /** @deprecated Use vitals().maxHearts(). */
    @Deprecated(forRemoval = true)
    public int maxHearts() {
        return vitals().maxHearts();
    }

    /** @deprecated Use vitals().setMaxHearts(value). */
    @Deprecated(forRemoval = true)
    public void setMaxHearts(int value) {
        vitals().setMaxHearts(value);
    }

    /** @deprecated Use progression().deathCount(). */
    @Deprecated(forRemoval = true)
    public int deathCount() {
        return progression().deathCount();
    }

    /** @deprecated Use progression().setDeathCount(value). */
    @Deprecated(forRemoval = true)
    public void setDeathCount(int value) {
        progression().setDeathCount(value);
    }

    /** @deprecated Use progression().incrementDeathCount(). */
    @Deprecated(forRemoval = true)
    public void incrementDeathCount() {
        progression().incrementDeathCount();
    }

    /** @deprecated Use progression().deathCountBcd(). */
    @Deprecated(forRemoval = true)
    public int deathCountBcd() {
        return progression().deathCountBcd();
    }

    /** @deprecated Use vitals().health(). */
    @Deprecated(forRemoval = true)
    public int health() {
        return vitals().health();
    }

    /** @deprecated Use vitals().setHealth(value). */
    @Deprecated(forRemoval = true)
    public void setHealth(int value) {
        vitals().setHealth(value);
    }

    /** @deprecated Use vitals().damage(amount). */
    @Deprecated(forRemoval = true)
    public void damage(int amount) {
        vitals().damage(amount);
    }

    /** @deprecated Use vitals().applyRomEnemyDamage(nominalDamage). */
    @Deprecated(forRemoval = true)
    public int applyRomEnemyDamage(int nominalDamage) {
        return vitals().applyRomEnemyDamage(nominalDamage);
    }

    /** @deprecated Use vitals().invincibilityCounter(). */
    @Deprecated(forRemoval = true)
    public int invincibilityCounter() {
        return vitals().invincibilityCounter();
    }
}
