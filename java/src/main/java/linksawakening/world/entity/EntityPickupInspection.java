package linksawakening.world.entity;

import java.util.Objects;
import linksawakening.world.entity.motion.BombiteMotion;
import linksawakening.world.entity.motion.ColorShellMotion;
import linksawakening.world.entity.motion.SecretSeashellMotion;

/** Read-only inspection for Pickup. */
final class EntityPickupInspection {
    private final EntityBombState bombInteractions;
    private final EntityMotionRegistry motions;
    private final EntityPhysicsTables physicsTables;

    EntityPickupInspection(
            EntityBombState bombInteractions, EntityMotionRegistry motions, EntityPhysicsTables physicsTables) {
        this.bombInteractions = Objects.requireNonNull(bombInteractions, "bombInteractions");
        this.motions = Objects.requireNonNull(motions, "motions");
        this.physicsTables = Objects.requireNonNull(physicsTables, "physicsTables");
    }

    int keyDropTransitionCountdown(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return physicsTables.readEnemyTransitionCountdown(slot);
    }

    int colorShellState(int slot) {
        var colorShellMotion = motions.get(ColorShellMotion.class, ColorShellMotion::new);
        return colorShellMotion.state(slot);
    }

    int bombiteState(int slot) {
        var bombiteMotion = motions.get(BombiteMotion.class, BombiteMotion::new);
        return bombiteMotion.state(slot);
    }

    int bombitePrivateCountdown1(int slot) {
        return bombInteractions.readBombCollisionGraceCountdown(slot);
    }

    int bombiteSpeedX(int slot) {
        var bombiteMotion = motions.get(BombiteMotion.class, BombiteMotion::new);
        return bombiteMotion.speedX(slot);
    }

    int bombiteSpeedY(int slot) {
        var bombiteMotion = motions.get(BombiteMotion.class, BombiteMotion::new);
        return bombiteMotion.speedY(slot);
    }

    int colorShellPhysicsFlags(int slot) {
        var colorShellMotion = motions.get(ColorShellMotion.class, ColorShellMotion::new);
        return colorShellMotion.physicsFlags(slot);
    }

    int seashellHiddenMode(int slot) {
        var secretSeashellMotion = motions.get(SecretSeashellMotion.class, SecretSeashellMotion::new);
        return secretSeashellMotion.hiddenMode(slot);
    }

    int seashellShovelRevealPending(int slot) {
        var secretSeashellMotion = motions.get(SecretSeashellMotion.class, SecretSeashellMotion::new);
        return secretSeashellMotion.shovelRevealPending(slot);
    }

    int seashellRevealCountdown(int slot) {
        var secretSeashellMotion = motions.get(SecretSeashellMotion.class, SecretSeashellMotion::new);
        return secretSeashellMotion.revealCountdown(slot);
    }

    int secretSeashellSlowTransitionCountdown(int slot) {
        var secretSeashellMotion = motions.get(SecretSeashellMotion.class, SecretSeashellMotion::new);
        return secretSeashellMotion.slowTransitionCountdown(slot);
    }

    int secretSeashellSpeedX(int slot) {
        var secretSeashellMotion = motions.get(SecretSeashellMotion.class, SecretSeashellMotion::new);
        return secretSeashellMotion.speedX(slot);
    }

    int secretSeashellSpeedY(int slot) {
        var secretSeashellMotion = motions.get(SecretSeashellMotion.class, SecretSeashellMotion::new);
        return secretSeashellMotion.speedY(slot);
    }

    int secretSeashellSpeedZ(int slot) {
        var secretSeashellMotion = motions.get(SecretSeashellMotion.class, SecretSeashellMotion::new);
        return secretSeashellMotion.speedZ(slot);
    }
}
