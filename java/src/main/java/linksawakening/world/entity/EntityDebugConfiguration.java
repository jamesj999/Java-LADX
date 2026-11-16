package linksawakening.world.entity;

import linksawakening.world.entity.motion.ColorShellMotion;

/** Room entity configuration preserving source-shaped handler inputs. */
public final class EntityDebugConfiguration {
    private final EntityCombatTables combatTables;
    private final EntityMotionRegistry motions;

    EntityDebugConfiguration(EntityCombatTables combatTables, EntityMotionRegistry motions) {
        this.combatTables = java.util.Objects.requireNonNull(combatTables, "combatTables");
        this.motions = java.util.Objects.requireNonNull(motions, "motions");
    }

    public void setEnemyIgnoreHitsCountdown(int slot, int value) {
        EntitySlotValidation.validateCountdownTestValue(slot, value);
        combatTables.writeEnemyIgnoreHitsCountdown(slot, value);
    }

    public void setColorShellState(
            int slot, int state, int transitionCountdown, int direction, int speedX, int speedY) {
        var colorShellMotion = motions.get(ColorShellMotion.class, ColorShellMotion::new);
        colorShellMotion.restoreState(slot, state, transitionCountdown, direction, speedX, speedY);
    }
}
