package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

/** Read-only inspection for Boss. */
final class EntityBossInspection {
    private final EntityBossInspectionServices runtime;

    EntityBossInspection(EntityBossInspectionServices runtime) {
        this.runtime = runtime;
    }

    int armosKnightState(int slot) {
        return runtime.motionSet().armosKnightMotion.state(slot);
    }

    int armosKnightTransitionCountdown(int slot) {
        return runtime.enemyInspection().transitionCountdown(slot);
    }

    int armosKnightSpeedX(int slot) {
        return runtime.motionSet().armosKnightMotion.speedX(slot);
    }

    int armosKnightSpeedY(int slot) {
        return runtime.motionSet().armosKnightMotion.speedY(slot);
    }

    int armosKnightSpeedZ(int slot) {
        return runtime.motionSet().armosKnightMotion.speedZ(slot);
    }

    int armosKnightPrivateCountdown1(int slot) {
        return runtime.motionSet().armosKnightMotion.privateCountdown1(slot);
    }

    int armosKnightInertia(int slot) {
        return runtime.motionSet().armosKnightMotion.inertia(slot);
    }

    int geniePrivateState1ForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.genie().geniePrivateState1(slot) & 0xFF;
    }

    int genieActiveStateForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.genie().genieActiveState(slot) & 0xFF;
    }

    int geniePrivateState3ForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.genie().geniePrivateState3(slot) & 0xFF;
    }

    int geniePrivateCountdown1ForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.genie().geniePrivateCountdown1(slot) & 0xFF;
    }

    int geniePrivateCountdown2ForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.genie().geniePrivateCountdown2(slot) & 0xFF;
    }

    int genieSpeedXForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.genie().genieSpeedX(slot) & 0xFF;
    }

    int genieSpeedZForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.genie().genieSpeedZ(slot) & 0xFF;
    }

    int geniePrivateState4ForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.genie().geniePrivateState4(slot) & 0xFF;
    }

    int rollingBonesBarStateForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.motionSet().rollingBonesMotion.barState(slot);
    }

    int rollingBonesBarSpeedXForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.motionSet().rollingBonesMotion.barSpeedX(slot);
    }

    int moldormHeadSpriteVariant(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.motionSet().moldormMotion.headSpriteVariant(slot);
    }

    int moldormTransitionCountdown(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.physicsTables().readEnemyTransitionCountdown(slot);
    }

    int moldormPrivateCountdown2(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.motionSet().moldormMotion.privateCountdown2(slot);
    }

    int moldormDestructionState(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.bossRewards().readBossDeathProducerState(slot);
    }

    int moldormInertia(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.motionSet().moldormMotion.inertia(slot);
    }
}
