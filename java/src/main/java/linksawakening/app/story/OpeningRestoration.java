package linksawakening.app.story;

import linksawakening.startup.MarinWakeUpMotion;
import linksawakening.startup.TarinShieldMotion;

/** Ordered writes when restoring opening-house progression from a save. */
public interface OpeningRestoration {
    void restoreWake(MarinWakeUpMotion motion);

    void restoreShield(TarinShieldMotion motion);

    void refreshPresentation();
}
