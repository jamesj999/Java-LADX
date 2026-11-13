package linksawakening.world.entity;

import linksawakening.world.entity.motion.ArmosKnightMotion;
import linksawakening.world.entity.motion.MoldormMotion;

/** Read-only inspection for Boss. */
final class EntityBossInspection {
    private final EntityBossState bossRewards;
    private final EntityEnemyInspection enemyInspection;
    private final EntityMotionRegistry motions;
    private final EntityPhysicsTables physicsTables;

    EntityBossInspection(
            EntityBossState bossRewards,
            EntityEnemyInspection enemyInspection,
            EntityMotionRegistry motions,
            EntityPhysicsTables physicsTables) {
        this.bossRewards = java.util.Objects.requireNonNull(bossRewards, "bossRewards");
        this.enemyInspection = java.util.Objects.requireNonNull(enemyInspection, "enemyInspection");
        this.motions = java.util.Objects.requireNonNull(motions, "motions");
        this.physicsTables = java.util.Objects.requireNonNull(physicsTables, "physicsTables");
    }

    int armosKnightState(int slot) {
        return motions.get(ArmosKnightMotion.class, ArmosKnightMotion::new).state(slot);
    }

    int armosKnightTransitionCountdown(int slot) {
        return enemyInspection.transitionCountdown(slot);
    }

    int armosKnightSpeedX(int slot) {
        return motions.get(ArmosKnightMotion.class, ArmosKnightMotion::new).speedX(slot);
    }

    int armosKnightSpeedY(int slot) {
        return motions.get(ArmosKnightMotion.class, ArmosKnightMotion::new).speedY(slot);
    }

    int armosKnightSpeedZ(int slot) {
        return motions.get(ArmosKnightMotion.class, ArmosKnightMotion::new).speedZ(slot);
    }

    int armosKnightPrivateCountdown1(int slot) {
        return motions.get(ArmosKnightMotion.class, ArmosKnightMotion::new).privateCountdown1(slot);
    }

    int armosKnightInertia(int slot) {
        return motions.get(ArmosKnightMotion.class, ArmosKnightMotion::new).inertia(slot);
    }

    int moldormHeadSpriteVariant(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return motions.get(MoldormMotion.class, MoldormMotion::new).headSpriteVariant(slot);
    }

    int moldormTransitionCountdown(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return physicsTables.readEnemyTransitionCountdown(slot);
    }

    int moldormPrivateCountdown2(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return motions.get(MoldormMotion.class, MoldormMotion::new).privateCountdown2(slot);
    }

    int moldormDestructionState(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return bossRewards.readBossDeathProducerState(slot);
    }

    int moldormInertia(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return motions.get(MoldormMotion.class, MoldormMotion::new).inertia(slot);
    }
}
