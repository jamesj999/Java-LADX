package linksawakening.world;

import linksawakening.world.entity.event.BoomerangObjectRequest;
import linksawakening.world.entity.event.MagicPowderObjectRequest;
import linksawakening.world.entity.event.MagicRodObjectRequest;
import linksawakening.world.entity.event.TransientVfxRequest;

import static linksawakening.world.EntityMotionMath.*;
import static linksawakening.world.EntitySnapshots.*;
import static linksawakening.world.EntityTypeRules.*;
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
import static linksawakening.world.RoomEntityRuntime.*;

/** Read-only inspection for Boss. */
final class EntityBossInspection {
    private final RoomEntityRuntime runtime;
    EntityBossInspection(RoomEntityRuntime runtime) { this.runtime = runtime; }

    int armosKnightState(int slot) {
        return runtime.armosKnightMotion.state(slot);
    }

    int armosKnightTransitionCountdown(int slot) {
        return runtime.enemyInspection.transitionCountdown(slot);
    }

    int armosKnightSpeedX(int slot) {
        return runtime.armosKnightMotion.speedX(slot);
    }

    int armosKnightSpeedY(int slot) {
        return runtime.armosKnightMotion.speedY(slot);
    }

    int armosKnightSpeedZ(int slot) {
        return runtime.armosKnightMotion.speedZ(slot);
    }

    int armosKnightPrivateCountdown1(int slot) {
        return runtime.armosKnightMotion.privateCountdown1(slot);
    }

    int armosKnightInertia(int slot) {
        return runtime.armosKnightMotion.inertia(slot);
    }

    int geniePrivateState1ForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.geniePrivateState1[slot] & 0xFF;
    }

    int genieActiveStateForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.genieActiveState[slot] & 0xFF;
    }

    int geniePrivateState3ForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.geniePrivateState3[slot] & 0xFF;
    }

    int geniePrivateCountdown1ForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.geniePrivateCountdown1[slot] & 0xFF;
    }

    int geniePrivateCountdown2ForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.geniePrivateCountdown2[slot] & 0xFF;
    }

    int genieSpeedXForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.genieSpeedX[slot] & 0xFF;
    }

    int genieSpeedZForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.genieSpeedZ[slot] & 0xFF;
    }

    int geniePrivateState4ForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.geniePrivateState4[slot] & 0xFF;
    }

    int rollingBonesBarStateForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.rollingBonesMotion.barState(slot);
    }

    int rollingBonesBarSpeedXForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.rollingBonesMotion.barSpeedX(slot);
    }

    int moldormHeadSpriteVariant(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.moldormMotion.headSpriteVariant(slot);
    }

    int moldormTransitionCountdown(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.enemyTransitionCountdown[slot];
    }

    int moldormPrivateCountdown2(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.moldormMotion.privateCountdown2(slot);
    }

    int moldormDestructionState(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.bossDeathProducerState[slot];
    }

    int moldormInertia(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.moldormMotion.inertia(slot);
    }
}
