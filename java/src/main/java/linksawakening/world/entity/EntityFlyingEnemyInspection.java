package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.motion.*;

/** Read-only inspection for FlyingEnemy. */
final class EntityFlyingEnemyInspection {
    private final EntityFlyingEnemyInspectionServices runtime;

    EntityFlyingEnemyInspection(EntityFlyingEnemyInspectionServices runtime) {
        this.runtime = runtime;
    }

    int keeseState(int slot) {
        return runtime.motions().get(KeeseMotion.class, KeeseMotion::new).state(slot);
    }

    int keeseTransitionCountdown(int slot) {
        return runtime.motions().get(KeeseMotion.class, KeeseMotion::new).transitionCountdown(slot);
    }

    int keeseAngle(int slot) {
        return runtime.motions().get(KeeseMotion.class, KeeseMotion::new).angle(slot);
    }

    int keeseSpeedX(int slot) {
        return runtime.motions().get(KeeseMotion.class, KeeseMotion::new).speedX(slot);
    }

    int keeseSpeedY(int slot) {
        return runtime.motions().get(KeeseMotion.class, KeeseMotion::new).speedY(slot);
    }

    int pairoddState(int slot) {
        return runtime.motions().get(PairoddMotion.class, PairoddMotion::new).state(slot);
    }

    int pairoddTransitionCountdown(int slot) {
        return runtime.motions().get(PairoddMotion.class, PairoddMotion::new).transitionCountdown(slot);
    }

    int pairoddDirection(int slot) {
        return runtime.motions().get(PairoddMotion.class, PairoddMotion::new).direction(slot);
    }

    int pairoddProjectileSpeedX(int slot) {
        return runtime.motions()
                .get(PairoddProjectileMotion.class, PairoddProjectileMotion::new)
                .speedX(slot);
    }

    int pairoddProjectileSpeedY(int slot) {
        return runtime.motions()
                .get(PairoddProjectileMotion.class, PairoddProjectileMotion::new)
                .speedY(slot);
    }

    int pairoddProjectileDirection(int slot) {
        return runtime.motions()
                .get(PairoddProjectileMotion.class, PairoddProjectileMotion::new)
                .direction(slot);
    }

    int fishState(int slot) {
        return runtime.motions().get(FishMotion.class, FishMotion::new).state(slot);
    }

    int fishSpeedX(int slot) {
        return runtime.motions().get(FishMotion.class, FishMotion::new).speedX(slot);
    }

    int fishSpeedZ(int slot) {
        return runtime.motions().get(FishMotion.class, FishMotion::new).speedZ(slot);
    }

    int fishTransitionCountdown(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.physicsTables().readEnemyTransitionCountdown(slot);
    }

    int crowState(int slot) {
        return runtime.motions().get(CrowMotion.class, CrowMotion::new).state(slot);
    }

    int crowSpeedX(int slot) {
        return runtime.motions().get(CrowMotion.class, CrowMotion::new).speedX(slot);
    }

    int crowSpeedY(int slot) {
        return runtime.motions().get(CrowMotion.class, CrowMotion::new).speedY(slot);
    }

    int crowSpeedZ(int slot) {
        return runtime.motions().get(CrowMotion.class, CrowMotion::new).speedZ(slot);
    }

    int crowTransitionCountdown(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.physicsTables().readEnemyTransitionCountdown(slot);
    }

    int booBuddyState(int slot) {
        return runtime.motions().get(BooBuddyMotion.class, BooBuddyMotion::new).state(slot);
    }

    int booBuddySpeedX(int slot) {
        return runtime.motions().get(BooBuddyMotion.class, BooBuddyMotion::new).speedX(slot);
    }

    int booBuddySpeedY(int slot) {
        return runtime.motions().get(BooBuddyMotion.class, BooBuddyMotion::new).speedY(slot);
    }

    int booBuddyTransitionCountdown(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.physicsTables().readEnemyTransitionCountdown(slot);
    }

    int booBuddyTriggerCount() {
        return runtime.motions().get(BooBuddyMotion.class, BooBuddyMotion::new).readBooBuddyTriggerCount();
    }
}
