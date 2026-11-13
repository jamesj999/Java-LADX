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
    @Override
    public boolean swordCollisionActive() {
        return sword.active();
    }

    @Override
    public int swordX() {
        return sword.x();
    }

    @Override
    public int swordWidth() {
        return sword.width();
    }

    @Override
    public int swordY() {
        return sword.y();
    }

    @Override
    public int swordHeight() {
        return sword.height();
    }
}
