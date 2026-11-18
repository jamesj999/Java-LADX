package linksawakening.world.entity;

import linksawakening.world.entity.motion.EnemyRecoilMotion;

final class EntityEnemyInspectionFixture {
    private final RoomEntityRuntime runtime;

    EntityEnemyInspectionFixture(RoomEntityRuntime room) {
        runtime = room;
    }

    int enemyRecoilSpeedXForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.inspection()
                .motions()
                .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                .recoilSpeedX(slot);
    }

    int enemyRecoilSpeedYForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.inspection()
                .motions()
                .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                .recoilSpeedY(slot);
    }

    int musicalNoteInertiaForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.execution().projectiles().readMusicalNoteInertia(slot);
    }

    int hitboxFlagsForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.inspection().combatTables().readEnemyHitboxFlags(slot);
    }
}
