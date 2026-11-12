package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.motion.*;

/** Room entity configuration preserving source-shaped handler inputs. */
public final class EntityDebugConfiguration {
    private final EntityDebugConfigurationServices runtime;

    EntityDebugConfiguration(EntityDebugConfigurationServices runtime) {
        this.runtime = runtime;
    }

    public void setEnemyIgnoreHitsCountdown(int slot, int value) {
        EntitySlotValidation.validateCountdownTestValue(slot, value);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(slot, value);
    }

    public void setColorShellState(
            int slot, int state, int transitionCountdown, int direction, int speedX, int speedY) {
        runtime.motions()
                .get(ColorShellMotion.class, ColorShellMotion::new)
                .restoreState(slot, state, transitionCountdown, direction, speedX, speedY);
    }
}
