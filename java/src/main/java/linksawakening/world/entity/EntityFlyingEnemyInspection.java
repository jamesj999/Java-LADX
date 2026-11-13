package linksawakening.world.entity;

import linksawakening.world.entity.motion.BooBuddyMotion;
import linksawakening.world.entity.motion.CrowMotion;
import linksawakening.world.entity.motion.FishMotion;
import linksawakening.world.entity.motion.KeeseMotion;
import linksawakening.world.entity.motion.PairoddMotion;
import linksawakening.world.entity.motion.PairoddProjectileMotion;

/** Read-only inspection for FlyingEnemy. */
final class EntityFlyingEnemyInspection {
    private final EntityMotionRegistry motions;
    private final EntityPhysicsTables physicsTables;

    EntityFlyingEnemyInspection(EntityMotionRegistry motions, EntityPhysicsTables physicsTables) {
        this.motions = java.util.Objects.requireNonNull(motions, "motions");
        this.physicsTables = java.util.Objects.requireNonNull(physicsTables, "physicsTables");
    }

    int keeseState(int slot) {
        return motions.get(KeeseMotion.class, KeeseMotion::new).state(slot);
    }

    int keeseTransitionCountdown(int slot) {
        return motions.get(KeeseMotion.class, KeeseMotion::new).transitionCountdown(slot);
    }

    int keeseAngle(int slot) {
        return motions.get(KeeseMotion.class, KeeseMotion::new).angle(slot);
    }

    int keeseSpeedX(int slot) {
        return motions.get(KeeseMotion.class, KeeseMotion::new).speedX(slot);
    }

    int keeseSpeedY(int slot) {
        return motions.get(KeeseMotion.class, KeeseMotion::new).speedY(slot);
    }

    int pairoddState(int slot) {
        return motions.get(PairoddMotion.class, PairoddMotion::new).state(slot);
    }

    int pairoddTransitionCountdown(int slot) {
        return motions.get(PairoddMotion.class, PairoddMotion::new).transitionCountdown(slot);
    }

    int pairoddDirection(int slot) {
        return motions.get(PairoddMotion.class, PairoddMotion::new).direction(slot);
    }

    int pairoddProjectileSpeedX(int slot) {
        return motions.get(PairoddProjectileMotion.class, PairoddProjectileMotion::new)
                .speedX(slot);
    }

    int pairoddProjectileSpeedY(int slot) {
        return motions.get(PairoddProjectileMotion.class, PairoddProjectileMotion::new)
                .speedY(slot);
    }

    int pairoddProjectileDirection(int slot) {
        return motions.get(PairoddProjectileMotion.class, PairoddProjectileMotion::new)
                .direction(slot);
    }

    int fishState(int slot) {
        return motions.get(FishMotion.class, FishMotion::new).state(slot);
    }

    int fishSpeedX(int slot) {
        return motions.get(FishMotion.class, FishMotion::new).speedX(slot);
    }

    int fishSpeedZ(int slot) {
        return motions.get(FishMotion.class, FishMotion::new).speedZ(slot);
    }

    int fishTransitionCountdown(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return physicsTables.readEnemyTransitionCountdown(slot);
    }

    int crowState(int slot) {
        return motions.get(CrowMotion.class, CrowMotion::new).state(slot);
    }

    int crowSpeedX(int slot) {
        return motions.get(CrowMotion.class, CrowMotion::new).speedX(slot);
    }

    int crowSpeedY(int slot) {
        return motions.get(CrowMotion.class, CrowMotion::new).speedY(slot);
    }

    int crowSpeedZ(int slot) {
        return motions.get(CrowMotion.class, CrowMotion::new).speedZ(slot);
    }

    int crowTransitionCountdown(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return physicsTables.readEnemyTransitionCountdown(slot);
    }

    int booBuddyState(int slot) {
        return motions.get(BooBuddyMotion.class, BooBuddyMotion::new).state(slot);
    }

    int booBuddySpeedX(int slot) {
        return motions.get(BooBuddyMotion.class, BooBuddyMotion::new).speedX(slot);
    }

    int booBuddySpeedY(int slot) {
        return motions.get(BooBuddyMotion.class, BooBuddyMotion::new).speedY(slot);
    }

    int booBuddyTransitionCountdown(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return physicsTables.readEnemyTransitionCountdown(slot);
    }

    int booBuddyTriggerCount() {
        return motions.get(BooBuddyMotion.class, BooBuddyMotion::new).readBooBuddyTriggerCount();
    }
}
