package linksawakening.app;

import linksawakening.startup.MarinWakeUpMotion;
import linksawakening.startup.TarinShieldMotion;

/** Mutable wake-up and shield motions shared by story, loading and debug entry. */
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
        wakeUp = new MarinWakeUpMotion();
        shield = new TarinShieldMotion();
    }
}
