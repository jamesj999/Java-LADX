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

/** Read-only inspection for FlyingEnemy. */
final class EntityFlyingEnemyInspection {
    private final RoomEntityRuntime runtime;
    EntityFlyingEnemyInspection(RoomEntityRuntime runtime) { this.runtime = runtime; }

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
