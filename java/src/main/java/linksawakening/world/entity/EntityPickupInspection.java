package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

/** Read-only inspection for Pickup. */
final class EntityPickupInspection {
    private final EntityPickupInspectionServices runtime;

    EntityPickupInspection(EntityPickupInspectionServices runtime) {
        this.runtime = runtime;
    }

    int keyDropTransitionCountdown(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.physicsTables().readEnemyTransitionCountdown(slot);
    }

    int colorShellState(int slot) {
        return runtime.motionSet().colorShellMotion.state(slot);
    }

    int bombiteState(int slot) {
        return runtime.motionSet().bombiteMotion.state(slot);
    }

    int bombitePrivateCountdown1(int slot) {
        return runtime.bombInteractions().readBombPrivateCountdown1(slot);
    }

    int bombiteSpeedX(int slot) {
        return runtime.motionSet().bombiteMotion.speedX(slot);
    }

    int bombiteSpeedY(int slot) {
        return runtime.motionSet().bombiteMotion.speedY(slot);
    }

    int colorShellPhysicsFlags(int slot) {
        return runtime.motionSet().colorShellMotion.physicsFlags(slot);
    }

    int secretSeashellPrivateState3(int slot) {
        return runtime.motionSet().secretSeashellMotion.privateState3(slot);
    }

    int secretSeashellPrivateState4(int slot) {
        return runtime.motionSet().secretSeashellMotion.privateState4(slot);
    }

    int secretSeashellPrivateCountdown1(int slot) {
        return runtime.motionSet().secretSeashellMotion.privateCountdown1(slot);
    }

    int secretSeashellSlowTransitionCountdown(int slot) {
        return runtime.motionSet().secretSeashellMotion.slowTransitionCountdown(slot);
    }

    int secretSeashellSpeedX(int slot) {
        return runtime.motionSet().secretSeashellMotion.speedX(slot);
    }

    int secretSeashellSpeedY(int slot) {
        return runtime.motionSet().secretSeashellMotion.speedY(slot);
    }

    int secretSeashellSpeedZ(int slot) {
        return runtime.motionSet().secretSeashellMotion.speedZ(slot);
    }
}
