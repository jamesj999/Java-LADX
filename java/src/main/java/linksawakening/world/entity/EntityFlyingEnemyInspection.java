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
        return runtime.entityState.keeseMotion.state(slot);
    }

    int keeseTransitionCountdown(int slot) {
        return runtime.entityState.keeseMotion.transitionCountdown(slot);
    }

    int keeseAngle(int slot) {
        return runtime.entityState.keeseMotion.angle(slot);
    }

    int keeseSpeedX(int slot) {
        return runtime.entityState.keeseMotion.speedX(slot);
    }

    int keeseSpeedY(int slot) {
        return runtime.entityState.keeseMotion.speedY(slot);
    }

    int pairoddState(int slot) {
        return runtime.entityState.pairoddMotion.state(slot);
    }

    int pairoddTransitionCountdown(int slot) {
        return runtime.entityState.pairoddMotion.transitionCountdown(slot);
    }

    int pairoddDirection(int slot) {
        return runtime.entityState.pairoddMotion.direction(slot);
    }

    int pairoddProjectileSpeedX(int slot) {
        return runtime.entityState.pairoddProjectileMotion.speedX(slot);
    }

    int pairoddProjectileSpeedY(int slot) {
        return runtime.entityState.pairoddProjectileMotion.speedY(slot);
    }

    int pairoddProjectileDirection(int slot) {
        return runtime.entityState.pairoddProjectileMotion.direction(slot);
    }

    int fishState(int slot) {
        return runtime.entityState.fishMotion.state(slot);
    }

    int fishSpeedX(int slot) {
        return runtime.entityState.fishMotion.speedX(slot);
    }

    int fishSpeedZ(int slot) {
        return runtime.entityState.fishMotion.speedZ(slot);
    }

    int fishTransitionCountdown(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.enemyTransitionCountdown[slot];
    }

    int crowState(int slot) {
        return runtime.entityState.crowMotion.state(slot);
    }

    int crowSpeedX(int slot) {
        return runtime.entityState.crowMotion.speedX(slot);
    }

    int crowSpeedY(int slot) {
        return runtime.entityState.crowMotion.speedY(slot);
    }

    int crowSpeedZ(int slot) {
        return runtime.entityState.crowMotion.speedZ(slot);
    }

    int crowTransitionCountdown(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.enemyTransitionCountdown[slot];
    }

    int booBuddyState(int slot) {
        return runtime.entityState.booBuddyMotion.state(slot);
    }

    int booBuddySpeedX(int slot) {
        return runtime.entityState.booBuddyMotion.speedX(slot);
    }

    int booBuddySpeedY(int slot) {
        return runtime.entityState.booBuddyMotion.speedY(slot);
    }

    int booBuddyTransitionCountdown(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.enemyTransitionCountdown[slot];
    }

    int booBuddyTriggerCount() {
        return runtime.entityState.booBuddyTriggerCount;
    }
}
