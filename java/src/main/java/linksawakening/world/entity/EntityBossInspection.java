package linksawakening.world.entity;

import java.util.Objects;
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
        this.bossRewards = Objects.requireNonNull(bossRewards, "bossRewards");
        this.enemyInspection = Objects.requireNonNull(enemyInspection, "enemyInspection");
        this.motions = Objects.requireNonNull(motions, "motions");
        this.physicsTables = Objects.requireNonNull(physicsTables, "physicsTables");
    }

    int armosKnightState(int slot) {
        var armosKnightMotion = motions.get(ArmosKnightMotion.class, ArmosKnightMotion::new);
        return armosKnightMotion.state(slot);
    }

    int armosKnightTransitionCountdown(int slot) {
        return enemyInspection.transitionCountdown(slot);
    }

    int armosKnightSpeedX(int slot) {
        var armosKnightMotion = motions.get(ArmosKnightMotion.class, ArmosKnightMotion::new);
        return armosKnightMotion.speedX(slot);
    }

    int armosKnightSpeedY(int slot) {
        var armosKnightMotion = motions.get(ArmosKnightMotion.class, ArmosKnightMotion::new);
        return armosKnightMotion.speedY(slot);
    }

    int armosKnightSpeedZ(int slot) {
        var armosKnightMotion = motions.get(ArmosKnightMotion.class, ArmosKnightMotion::new);
        return armosKnightMotion.speedZ(slot);
    }

    int armosKnightPrivateCountdown1(int slot) {
        var armosKnightMotion = motions.get(ArmosKnightMotion.class, ArmosKnightMotion::new);
        return armosKnightMotion.privateCountdown1(slot);
    }

    int armosKnightInertia(int slot) {
        var armosKnightMotion = motions.get(ArmosKnightMotion.class, ArmosKnightMotion::new);
        return armosKnightMotion.inertia(slot);
    }

    int moldormHeadSpriteVariant(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        var moldormMotion = motions.get(MoldormMotion.class, MoldormMotion::new);
        return moldormMotion.headSpriteVariant(slot);
    }

    int moldormTransitionCountdown(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return physicsTables.readEnemyTransitionCountdown(slot);
    }

    int moldormPrivateCountdown2(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        var moldormMotion = motions.get(MoldormMotion.class, MoldormMotion::new);
        return moldormMotion.privateCountdown2(slot);
    }

    int moldormDestructionState(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return bossRewards.readBossDeathProducerState(slot);
    }

    int moldormInertia(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        var moldormMotion = motions.get(MoldormMotion.class, MoldormMotion::new);
        return moldormMotion.inertia(slot);
    }
}
