package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

/** Read-only inspection for Pickup. */
final class EntityPickupInspection {
    private final RoomEntityRuntime runtime;

    EntityPickupInspection(RoomEntityRuntime runtime) {
        this.runtime = runtime;
    }

    int keyDropTransitionCountdown(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.enemyTransitionCountdown[slot];
    }

    int colorShellState(int slot) {
        return runtime.entityState.colorShellMotion.state(slot);
    }

    int bombiteState(int slot) {
        return runtime.entityState.bombiteMotion.state(slot);
    }

    int bombitePrivateCountdown1(int slot) {
        return runtime.entityState.bombPrivateCountdown1[slot];
    }

    int bombiteSpeedX(int slot) {
        return runtime.entityState.bombiteMotion.speedX(slot);
    }

    int bombiteSpeedY(int slot) {
        return runtime.entityState.bombiteMotion.speedY(slot);
    }

    int colorShellPhysicsFlags(int slot) {
        return runtime.entityState.colorShellMotion.physicsFlags(slot);
    }

    int secretSeashellPrivateState3(int slot) {
        return runtime.entityState.secretSeashellMotion.privateState3(slot);
    }

    int secretSeashellPrivateState4(int slot) {
        return runtime.entityState.secretSeashellMotion.privateState4(slot);
    }

    int secretSeashellPrivateCountdown1(int slot) {
        return runtime.entityState.secretSeashellMotion.privateCountdown1(slot);
    }

    int secretSeashellSlowTransitionCountdown(int slot) {
        return runtime.entityState.secretSeashellMotion.slowTransitionCountdown(slot);
    }

    int secretSeashellSpeedX(int slot) {
        return runtime.entityState.secretSeashellMotion.speedX(slot);
    }

    int secretSeashellSpeedY(int slot) {
        return runtime.entityState.secretSeashellMotion.speedY(slot);
    }

    int secretSeashellSpeedZ(int slot) {
        return runtime.entityState.secretSeashellMotion.speedZ(slot);
    }
}
