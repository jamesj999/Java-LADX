package linksawakening.state;

/** Inventory and health operations used by Link; persistence remains in PlayerState. */
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
