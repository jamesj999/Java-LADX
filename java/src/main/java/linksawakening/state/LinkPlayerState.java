package linksawakening.state;

/** @deprecated Use the inventory(), vitals(), progression() and equipmentLevels() components on PlayerState. */
@Deprecated(forRemoval = true)
public interface LinkPlayerState {
    int itemA();

    int itemB();

    int shieldLevel();

    int tunicType();

    boolean hasFlippers();

    default int activePowerUp() {
        return 0;
    }

    boolean runningWithPegasusBoots();

    void setRunningWithPegasusBoots(boolean value);

    int invincibilityCounter();

    void setInvincibilityCounter(int value);

    void tickInvincibility();

    void damage(int amount);

    default void addSubtractHealthBuffer(int amount) {
        damage(amount);
    }

    void clearSubtractHealthBuffer();
}
