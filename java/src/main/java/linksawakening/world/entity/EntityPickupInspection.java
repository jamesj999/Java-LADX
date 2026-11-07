package linksawakening.world.entity;







import linksawakening.world.entity.event.BoomerangObjectRequest;
import linksawakening.world.entity.event.MagicPowderObjectRequest;
import linksawakening.world.entity.event.MagicRodObjectRequest;
import linksawakening.world.entity.event.TransientVfxRequest;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;
import linksawakening.entity.EntitySpriteSelection;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.gpu.EntitySpriteTileSnapshot;
import linksawakening.rom.RomTables;
import linksawakening.vfx.TransientVfxType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;
import static linksawakening.world.entity.RoomEntityRuntime.*;

/** Read-only inspection for Pickup. */
final class EntityPickupInspection {
    private final RoomEntityRuntime runtime;
    EntityPickupInspection(RoomEntityRuntime runtime) { this.runtime = runtime; }

    int keyDropTransitionCountdown(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.enemyTransitionCountdown[slot];
    }

    int colorShellState(int slot) {
        return runtime.colorShellMotion.state(slot);
    }

    int bombiteState(int slot) {
        return runtime.bombiteMotion.state(slot);
    }

    int bombitePrivateCountdown1(int slot) {
        return runtime.bombPrivateCountdown1[slot];
    }

    int bombiteSpeedX(int slot) {
        return runtime.bombiteMotion.speedX(slot);
    }

    int bombiteSpeedY(int slot) {
        return runtime.bombiteMotion.speedY(slot);
    }

    int colorShellPhysicsFlags(int slot) {
        return runtime.colorShellMotion.physicsFlags(slot);
    }

    int secretSeashellPrivateState3(int slot) {
        return runtime.secretSeashellMotion.privateState3(slot);
    }

    int secretSeashellPrivateState4(int slot) {
        return runtime.secretSeashellMotion.privateState4(slot);
    }

    int secretSeashellPrivateCountdown1(int slot) {
        return runtime.secretSeashellMotion.privateCountdown1(slot);
    }

    int secretSeashellSlowTransitionCountdown(int slot) {
        return runtime.secretSeashellMotion.slowTransitionCountdown(slot);
    }

    int secretSeashellSpeedX(int slot) {
        return runtime.secretSeashellMotion.speedX(slot);
    }

    int secretSeashellSpeedY(int slot) {
        return runtime.secretSeashellMotion.speedY(slot);
    }

    int secretSeashellSpeedZ(int slot) {
        return runtime.secretSeashellMotion.speedZ(slot);
    }
}
