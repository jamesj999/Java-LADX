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
        EnemyAttackContext attackContext) {}
