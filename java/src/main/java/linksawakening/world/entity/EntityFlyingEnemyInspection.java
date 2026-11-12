package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

/** Read-only inspection for FlyingEnemy. */
final class EntityFlyingEnemyInspection {
    private final EntityFlyingEnemyInspectionServices runtime;

    EntityFlyingEnemyInspection(EntityFlyingEnemyInspectionServices runtime) {
        this.runtime = runtime;
    }

    int keeseState(int slot) {
        return runtime.motionSet().keeseMotion.state(slot);
    }

    int keeseTransitionCountdown(int slot) {
        return runtime.motionSet().keeseMotion.transitionCountdown(slot);
    }

    int keeseAngle(int slot) {
        return runtime.motionSet().keeseMotion.angle(slot);
    }

    int keeseSpeedX(int slot) {
        return runtime.motionSet().keeseMotion.speedX(slot);
    }

    int keeseSpeedY(int slot) {
        return runtime.motionSet().keeseMotion.speedY(slot);
    }

    int pairoddState(int slot) {
        return runtime.motionSet().pairoddMotion.state(slot);
    }

    int pairoddTransitionCountdown(int slot) {
        return runtime.motionSet().pairoddMotion.transitionCountdown(slot);
    }

    int pairoddDirection(int slot) {
        return runtime.motionSet().pairoddMotion.direction(slot);
    }

    int pairoddProjectileSpeedX(int slot) {
        return runtime.motionSet().pairoddProjectileMotion.speedX(slot);
    }

    int pairoddProjectileSpeedY(int slot) {
        return runtime.motionSet().pairoddProjectileMotion.speedY(slot);
    }

    int pairoddProjectileDirection(int slot) {
        return runtime.motionSet().pairoddProjectileMotion.direction(slot);
    }

    int fishState(int slot) {
        return runtime.motionSet().fishMotion.state(slot);
    }

    int fishSpeedX(int slot) {
        return runtime.motionSet().fishMotion.speedX(slot);
    }

    int fishSpeedZ(int slot) {
        return runtime.motionSet().fishMotion.speedZ(slot);
    }

    int fishTransitionCountdown(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.physicsTables().readEnemyTransitionCountdown(slot);
    }

    int crowState(int slot) {
        return runtime.motionSet().crowMotion.state(slot);
    }

    int crowSpeedX(int slot) {
        return runtime.motionSet().crowMotion.speedX(slot);
    }

    int crowSpeedY(int slot) {
        return runtime.motionSet().crowMotion.speedY(slot);
    }

    int crowSpeedZ(int slot) {
        return runtime.motionSet().crowMotion.speedZ(slot);
    }

    int crowTransitionCountdown(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.physicsTables().readEnemyTransitionCountdown(slot);
    }

    int booBuddyState(int slot) {
        return runtime.motionSet().booBuddyMotion.state(slot);
    }

    int booBuddySpeedX(int slot) {
        return runtime.motionSet().booBuddyMotion.speedX(slot);
    }

    int booBuddySpeedY(int slot) {
        return runtime.motionSet().booBuddyMotion.speedY(slot);
    }

    int booBuddyTransitionCountdown(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.physicsTables().readEnemyTransitionCountdown(slot);
    }

    int booBuddyTriggerCount() {
        return runtime.motionSet().booBuddyMotion.readBooBuddyTriggerCount();
    }
}
