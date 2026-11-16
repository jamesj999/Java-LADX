package linksawakening.world.entity;

import java.util.Objects;
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
        this.motions = Objects.requireNonNull(motions, "motions");
        this.physicsTables = Objects.requireNonNull(physicsTables, "physicsTables");
    }

    int keeseState(int slot) {
        var keeseMotion = motions.get(KeeseMotion.class, KeeseMotion::new);
        return keeseMotion.state(slot);
    }

    int keeseTransitionCountdown(int slot) {
        var keeseMotion = motions.get(KeeseMotion.class, KeeseMotion::new);
        return keeseMotion.transitionCountdown(slot);
    }

    int keeseAngle(int slot) {
        var keeseMotion = motions.get(KeeseMotion.class, KeeseMotion::new);
        return keeseMotion.angle(slot);
    }

    int keeseSpeedX(int slot) {
        var keeseMotion = motions.get(KeeseMotion.class, KeeseMotion::new);
        return keeseMotion.speedX(slot);
    }

    int keeseSpeedY(int slot) {
        var keeseMotion = motions.get(KeeseMotion.class, KeeseMotion::new);
        return keeseMotion.speedY(slot);
    }

    int pairoddState(int slot) {
        var pairoddMotion = motions.get(PairoddMotion.class, PairoddMotion::new);
        return pairoddMotion.state(slot);
    }

    int pairoddTransitionCountdown(int slot) {
        var pairoddMotion = motions.get(PairoddMotion.class, PairoddMotion::new);
        return pairoddMotion.transitionCountdown(slot);
    }

    int pairoddDirection(int slot) {
        var pairoddMotion = motions.get(PairoddMotion.class, PairoddMotion::new);
        return pairoddMotion.direction(slot);
    }

    int pairoddProjectileSpeedX(int slot) {
        var pairoddProjectileMotion = motions.get(PairoddProjectileMotion.class, PairoddProjectileMotion::new);
        return pairoddProjectileMotion.speedX(slot);
    }

    int pairoddProjectileSpeedY(int slot) {
        var pairoddProjectileMotion = motions.get(PairoddProjectileMotion.class, PairoddProjectileMotion::new);
        return pairoddProjectileMotion.speedY(slot);
    }

    int pairoddProjectileDirection(int slot) {
        var pairoddProjectileMotion = motions.get(PairoddProjectileMotion.class, PairoddProjectileMotion::new);
        return pairoddProjectileMotion.direction(slot);
    }

    int fishState(int slot) {
        var fishMotion = motions.get(FishMotion.class, FishMotion::new);
        return fishMotion.state(slot);
    }

    int fishSpeedX(int slot) {
        var fishMotion = motions.get(FishMotion.class, FishMotion::new);
        return fishMotion.speedX(slot);
    }

    int fishSpeedZ(int slot) {
        var fishMotion = motions.get(FishMotion.class, FishMotion::new);
        return fishMotion.speedZ(slot);
    }

    int fishTransitionCountdown(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return physicsTables.readEnemyTransitionCountdown(slot);
    }

    int crowState(int slot) {
        var crowMotion = motions.get(CrowMotion.class, CrowMotion::new);
        return crowMotion.state(slot);
    }

    int crowSpeedX(int slot) {
        var crowMotion = motions.get(CrowMotion.class, CrowMotion::new);
        return crowMotion.speedX(slot);
    }

    int crowSpeedY(int slot) {
        var crowMotion = motions.get(CrowMotion.class, CrowMotion::new);
        return crowMotion.speedY(slot);
    }

    int crowSpeedZ(int slot) {
        var crowMotion = motions.get(CrowMotion.class, CrowMotion::new);
        return crowMotion.speedZ(slot);
    }

    int crowTransitionCountdown(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return physicsTables.readEnemyTransitionCountdown(slot);
    }

    int booBuddyState(int slot) {
        var booBuddyMotion = motions.get(BooBuddyMotion.class, BooBuddyMotion::new);
        return booBuddyMotion.state(slot);
    }

    int booBuddySpeedX(int slot) {
        var booBuddyMotion = motions.get(BooBuddyMotion.class, BooBuddyMotion::new);
        return booBuddyMotion.speedX(slot);
    }

    int booBuddySpeedY(int slot) {
        var booBuddyMotion = motions.get(BooBuddyMotion.class, BooBuddyMotion::new);
        return booBuddyMotion.speedY(slot);
    }

    int booBuddyTransitionCountdown(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return physicsTables.readEnemyTransitionCountdown(slot);
    }

    int booBuddyTriggerCount() {
        var booBuddyMotion = motions.get(BooBuddyMotion.class, BooBuddyMotion::new);
        return booBuddyMotion.readBooBuddyTriggerCount();
    }
}
