package linksawakening.world.room;

import linksawakening.world.entity.EnemyAttackContext;

public record EntityCombatInputs(
        int frameCounter,
        int linkEntityX,
        int linkEntityY,
        boolean linkAirborne,
        int linkVerticalVelocity,
        boolean linkInteractive,
        SwordBox sword,
        EnemyAttackContext attackContext)
        implements linksawakening.world.entity.EnemyCombatInputs {
    public boolean swordCollisionActive() {
        return sword.active();
    }

    public int swordX() {
        return sword.x();
    }

    public int swordWidth() {
        return sword.width();
    }

    public int swordY() {
        return sword.y();
    }

    public int swordHeight() {
        return sword.height();
    }
}
