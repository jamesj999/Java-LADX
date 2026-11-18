package linksawakening.app.state;

import linksawakening.startup.MarinWakeUpMotion;
import linksawakening.startup.TarinShieldMotion;

/**
 * Mutable wake-up and shield motions shared by story, loading and debug entry. One instance is retained for
 * the owning session.
 */
public final class OpeningStoryState {
    private MarinWakeUpMotion wakeUp;
    private TarinShieldMotion shield;

    public MarinWakeUpMotion wakeUp() {
        return wakeUp;
    }

    public TarinShieldMotion shield() {
        return shield;
    }

    public void wakeUp(MarinWakeUpMotion value) {
        wakeUp = value;
    }

    public void shield(TarinShieldMotion value) {
        shield = value;
    }

    public void clear() {
        wakeUp = null;
        shield = null;
    }

    public void initialize() {
        initialize(0, 0);
    }

    public void initialize(int swordLevel, int shieldLevel) {
        wakeUp = null;
        if (swordLevel == 0) {
            wakeUp = shieldLevel == 0 ? new MarinWakeUpMotion() : MarinWakeUpMotion.postWake(true);
        }
        shield = shieldLevel == 0 ? new TarinShieldMotion() : null;
    }
}
