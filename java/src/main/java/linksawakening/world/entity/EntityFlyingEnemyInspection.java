package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

/** Read-only inspection for FlyingEnemy. */
final class EntityFlyingEnemyInspection {
    private final RoomEntityRuntime runtime;

    EntityFlyingEnemyInspection(RoomEntityRuntime runtime) {
        this.runtime = runtime;
    }

    int keeseState(int slot) {
        return runtime.keeseMotion.state(slot);
    }

    int keeseTransitionCountdown(int slot) {
        return runtime.keeseMotion.transitionCountdown(slot);
    }

    int keeseAngle(int slot) {
        return runtime.keeseMotion.angle(slot);
    }

    int keeseSpeedX(int slot) {
        return runtime.keeseMotion.speedX(slot);
    }

    int keeseSpeedY(int slot) {
        return runtime.keeseMotion.speedY(slot);
    }

    int pairoddState(int slot) {
        return runtime.pairoddMotion.state(slot);
    }

    int pairoddTransitionCountdown(int slot) {
        return runtime.pairoddMotion.transitionCountdown(slot);
    }

    int pairoddDirection(int slot) {
        return runtime.pairoddMotion.direction(slot);
    }

    int pairoddProjectileSpeedX(int slot) {
        return runtime.pairoddProjectileMotion.speedX(slot);
    }

    int pairoddProjectileSpeedY(int slot) {
        return runtime.pairoddProjectileMotion.speedY(slot);
    }

    int pairoddProjectileDirection(int slot) {
        return runtime.pairoddProjectileMotion.direction(slot);
    }

    int fishState(int slot) {
        return runtime.fishMotion.state(slot);
    }

    int fishSpeedX(int slot) {
        return runtime.fishMotion.speedX(slot);
    }

    int fishSpeedZ(int slot) {
        return runtime.fishMotion.speedZ(slot);
    }

    int fishTransitionCountdown(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.enemyTransitionCountdown[slot];
    }

    int crowState(int slot) {
        return runtime.crowMotion.state(slot);
    }

    int crowSpeedX(int slot) {
        return runtime.crowMotion.speedX(slot);
    }

    int crowSpeedY(int slot) {
        return runtime.crowMotion.speedY(slot);
    }

    int crowSpeedZ(int slot) {
        return runtime.crowMotion.speedZ(slot);
    }

    int crowTransitionCountdown(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.enemyTransitionCountdown[slot];
    }

    int booBuddyState(int slot) {
        return runtime.booBuddyMotion.state(slot);
    }

    int booBuddySpeedX(int slot) {
        return runtime.booBuddyMotion.speedX(slot);
    }

    int booBuddySpeedY(int slot) {
        return runtime.booBuddyMotion.speedY(slot);
    }

    int booBuddyTransitionCountdown(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.enemyTransitionCountdown[slot];
    }

    int booBuddyTriggerCount() {
        return runtime.booBuddyTriggerCount;
    }
}
