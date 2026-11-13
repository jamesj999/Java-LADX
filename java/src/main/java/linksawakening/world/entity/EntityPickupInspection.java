package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.motion.*;

/** Read-only inspection for Pickup. */
final class EntityPickupInspection {
    private final EntityBombInteractions bombInteractions;
    private final EntityMotionRegistry motions;
    private final EntityPhysicsTables physicsTables;

    EntityPickupInspection(
            EntityBombInteractions bombInteractions, EntityMotionRegistry motions, EntityPhysicsTables physicsTables) {
        this.bombInteractions = java.util.Objects.requireNonNull(bombInteractions, "bombInteractions");
        this.motions = java.util.Objects.requireNonNull(motions, "motions");
        this.physicsTables = java.util.Objects.requireNonNull(physicsTables, "physicsTables");
    }

    int keyDropTransitionCountdown(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return physicsTables.readEnemyTransitionCountdown(slot);
    }

    int colorShellState(int slot) {
        return motions.get(ColorShellMotion.class, ColorShellMotion::new).state(slot);
    }

    int bombiteState(int slot) {
        return motions.get(BombiteMotion.class, BombiteMotion::new).state(slot);
    }

    int bombitePrivateCountdown1(int slot) {
        return bombInteractions.readBombPrivateCountdown1(slot);
    }

    int bombiteSpeedX(int slot) {
        return motions.get(BombiteMotion.class, BombiteMotion::new).speedX(slot);
    }

    int bombiteSpeedY(int slot) {
        return motions.get(BombiteMotion.class, BombiteMotion::new).speedY(slot);
    }

    int colorShellPhysicsFlags(int slot) {
        return motions.get(ColorShellMotion.class, ColorShellMotion::new).physicsFlags(slot);
    }

    int secretSeashellPrivateState3(int slot) {
        return motions.get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                .privateState3(slot);
    }

    int secretSeashellPrivateState4(int slot) {
        return motions.get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                .privateState4(slot);
    }

    int secretSeashellPrivateCountdown1(int slot) {
        return motions.get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                .privateCountdown1(slot);
    }

    int secretSeashellSlowTransitionCountdown(int slot) {
        return motions.get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                .slowTransitionCountdown(slot);
    }

    int secretSeashellSpeedX(int slot) {
        return motions.get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                .speedX(slot);
    }

    int secretSeashellSpeedY(int slot) {
        return motions.get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                .speedY(slot);
    }

    int secretSeashellSpeedZ(int slot) {
        return motions.get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                .speedZ(slot);
    }
}
