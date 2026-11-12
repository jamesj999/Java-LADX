package linksawakening.state;

import static linksawakening.state.PlayerNumbers.*;
import static linksawakening.state.PlayerState.*;

/** Hearts, damage and invincibility state used by ROM {@code wHealth}. */
public final class Vitals {
    int maxHearts = 3;
    int health = 3 * HP_PER_HEART;
    int invincibilityCounter;
    int addHealthBuffer;
    int subtractHealthBuffer;
    boolean onLowHealth;
    boolean fullHearts;
    int timeToNextLowHealthSound;
    private final EquipmentLevels equipmentLevels;
    private final PlayerSounds playerSounds;
    private final Progression progression;

    Vitals(EquipmentLevels equipmentLevels, PlayerSounds playerSounds, Progression progression) {
        this.equipmentLevels = equipmentLevels;
        this.playerSounds = playerSounds;
        this.progression = progression;
    }

    public void latchFullHearts() {
        fullHearts = health == maxHearts * HP_PER_HEART;
    }

    public boolean fullHearts() {
        return fullHearts;
    }

    public boolean isOnLowHealth() {
        return onLowHealth;
    }

    public int maxHearts() {
        return maxHearts;
    }

    public void setMaxHearts(int value) {
        maxHearts = clamp(value, 1, MAX_HEARTS);
        health = Math.min(health, maxHearts * HP_PER_HEART);
    }

    public int health() {
        return health;
    }

    public void setHealth(int value) {
        health = clamp(value, 0, maxHearts * HP_PER_HEART);
    }

    public void damage(int amount) {
        setHealth(health - Math.max(0, amount));
    }

    public int applyRomEnemyDamage(int nominalDamage) {
        int effectiveDamage = nominalDamage & 0xFF;
        if (equipmentLevels.tunicType == Tunic.BLUE) {
            effectiveDamage >>= 1;
        } else if (equipmentLevels.activePowerUp == PowerUp.GUARDIAN_ACORN) {
            effectiveDamage = effectiveDamage == 0x04 ? 0 : effectiveDamage >> 1;
        }

        subtractHealthBuffer = (subtractHealthBuffer + effectiveDamage) & 0xFF;
        invincibilityCounter = 0x50;
        if (equipmentLevels.activePowerUp != PowerUp.NONE) {
            equipmentLevels.powerUpHits = (equipmentLevels.powerUpHits + 1) & 0xFF;
            if (equipmentLevels.powerUpHits >= 3) {
                equipmentLevels.activePowerUp = PowerUp.NONE;
            }
        }
        return effectiveDamage;
    }

    public int invincibilityCounter() {
        return invincibilityCounter;
    }

    public void setInvincibilityCounter(int value) {
        invincibilityCounter = clamp(value, 0, 0xFF);
    }

    public void tickInvincibility() {
        if (invincibilityCounter > 0) {
            invincibilityCounter--;
        }
    }

    public int addHealthBuffer() {
        return addHealthBuffer;
    }

    public void addSubtractHealthBuffer(int amount) {
        subtractHealthBuffer = (subtractHealthBuffer + amount) & 0xFF;
    }

    public int subtractHealthBuffer() {
        return subtractHealthBuffer;
    }

    public void clearSubtractHealthBuffer() {
        subtractHealthBuffer = 0;
    }

    public void completeHeartPieceContainer() {
        progression.heartPieces = 0;
        setMaxHearts(maxHearts + 1);
        addHealthBuffer = 0x40;
    }

    public void applyHeartContainerReward() {
        setMaxHearts(maxHearts + 1);
        addHealthBuffer = 0xFF;
    }

    public void requestFullHeal() {
        addHealthBuffer = 0xFF;
    }

    public void tickHealthBuffer() {
        if (addHealthBuffer != 0) {
            int maxHealth = maxHearts * HP_PER_HEART;
            if (health >= maxHealth) {
                addHealthBuffer = 0;
            } else {
                health++;
                if ((health & 7) == 6) {
                    playerSounds.write(ResourceSound.HEART_PICKED_UP);
                }
                addHealthBuffer--;
                return;
            }
        }
        if (subtractHealthBuffer == 0) {
            return;
        }
        subtractHealthBuffer--;
        if (health > 0) {
            health--;
        }
        // UpdateHealth.noHealth ($02:63A3): consume medicine on a lethal tick.
        if (health == 0 && progression.hasMedicine) {
            progression.medicineCount = Math.max(0, progression.medicineCount - 1);
            progression.hasMedicine = progression.medicineCount != 0;
            health = HP_PER_HEART;
            addHealthBuffer = (addHealthBuffer + 0x80) & 0xFF;
            invincibilityCounter = 0xA0;
        }
    }
}
