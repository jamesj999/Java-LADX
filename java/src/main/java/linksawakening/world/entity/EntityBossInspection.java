package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.motion.*;

/** Read-only inspection for Boss. */
final class EntityBossInspection {
    private final EntityBossInspectionServices runtime;

    EntityBossInspection(EntityBossInspectionServices runtime) {
        this.runtime = runtime;
    }

    int armosKnightState(int slot) {
        return runtime.motions()
                .get(ArmosKnightMotion.class, ArmosKnightMotion::new)
                .state(slot);
    }

    int armosKnightTransitionCountdown(int slot) {
        return runtime.enemyInspection().transitionCountdown(slot);
    }

    int armosKnightSpeedX(int slot) {
        return runtime.motions()
                .get(ArmosKnightMotion.class, ArmosKnightMotion::new)
                .speedX(slot);
    }

    int armosKnightSpeedY(int slot) {
        return runtime.motions()
                .get(ArmosKnightMotion.class, ArmosKnightMotion::new)
                .speedY(slot);
    }

    int armosKnightSpeedZ(int slot) {
        return runtime.motions()
                .get(ArmosKnightMotion.class, ArmosKnightMotion::new)
                .speedZ(slot);
    }

    int armosKnightPrivateCountdown1(int slot) {
        return runtime.motions()
                .get(ArmosKnightMotion.class, ArmosKnightMotion::new)
                .privateCountdown1(slot);
    }

    int armosKnightInertia(int slot) {
        return runtime.motions()
                .get(ArmosKnightMotion.class, ArmosKnightMotion::new)
                .inertia(slot);
    }

    int moldormHeadSpriteVariant(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.motions().get(MoldormMotion.class, MoldormMotion::new).headSpriteVariant(slot);
    }

    int moldormTransitionCountdown(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.physicsTables().readEnemyTransitionCountdown(slot);
    }

    int moldormPrivateCountdown2(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.motions().get(MoldormMotion.class, MoldormMotion::new).privateCountdown2(slot);
    }

    int moldormDestructionState(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.bossRewards().readBossDeathProducerState(slot);
    }

    int moldormInertia(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.motions().get(MoldormMotion.class, MoldormMotion::new).inertia(slot);
    }
}
