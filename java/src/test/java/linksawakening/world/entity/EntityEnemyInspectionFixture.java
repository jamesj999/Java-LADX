package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.motion.*;

final class EntityEnemyInspectionFixture {
    private final RoomEntityRuntime runtime;

    EntityEnemyInspectionFixture(RoomEntityRuntime room) {
        runtime = room;
    }

    int enemyRecoilSpeedXForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState
                .motions
                .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                .recoilSpeedX(slot);
    }

    int enemyRecoilSpeedYForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState
                .motions
                .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                .recoilSpeedY(slot);
    }

    int musicalNoteInertiaForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.projectiles.readMusicalNoteInertia(slot);
    }

    int hitboxFlagsForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.combatTables.readEnemyHitboxFlags(slot);
    }
}
