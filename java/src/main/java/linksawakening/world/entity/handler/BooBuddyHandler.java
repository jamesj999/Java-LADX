package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_BOO_BUDDY;
import static linksawakening.world.entity.EntitySnapshots.withVariant;

import java.util.function.IntSupplier;
import linksawakening.world.entity.*;
import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.BooBuddyMotion;

/** ROM handler stages for BooBuddy. */
final class BooBuddyHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerInitializer(ENTITY_BOO_BUDDY, (entity, random) -> initialize(registry, entity, random));
        registry.registerTick(
                ENTITY_BOO_BUDDY, EntityTickStage.PHASE_1_BEFORE_BOSS_INTRO, 0, BooBuddyHandler::phase1BooBuddy);
        registry.registerTick(
                ENTITY_BOO_BUDDY, EntityTickStage.PHASE_3_AFTER_LASER, 6, BooBuddyHandler::phase3BooBuddy);
    }

    private static boolean phase1BooBuddy(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var timerGates = state.gates();

        if (slotState.initialStatus() == EntityStatus.ACTIVE) {
            int slot = slotState.entity().slot();
            // Rendering precedes ReturnIfNonInteractive_06 and recoil.
            slotState.entity(withVariant(
                    slotState.entity(),
                    state.motion(BooBuddyMotion.class, BooBuddyMotion::new).selectedVariant(slot)));
            state.replaceEntity(slotState.entity());
            if (timerGates.projectileTimersInteractive()) {
                state.motion(BooBuddyMotion.class, BooBuddyMotion::new).tickTimers(slot);
            }
            if (!timerGates.handlerInteractive()) {
                return false;
            }
            if (state.slot().ignoreHits(slot) != 0) {
                // $06:64F7 consumes one ignore-hits tick, moves with recoil
                // speeds, and pops the handler return address. OPTIONS1
                // bit $20 skips background collision for Boo Buddy.
                state.slot().decrementIgnoreHits(slot);
                state.replaceEntity(state.motion(BooBuddyMotion.class, BooBuddyMotion::new)
                        .advanceRecoil(
                                slotState.entity(),
                                state.motion(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                                        .recoilSpeedX(slot),
                                state.motion(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                                        .recoilSpeedY(slot)));
                return false;
            }
        }
        return true;
    }

    private static boolean phase3BooBuddy(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            BooBuddyMotion.Update booBuddyUpdate = state.motion(BooBuddyMotion.class, BooBuddyMotion::new)
                    .advanceAfterTimers(
                            slotState.entity(),
                            frameInput.frame(),
                            frameInput.linkEntityX(),
                            frameInput.linkEntityY(),
                            state.slot().transitionCountdown(slotState.entity().slot()),
                            state.story().booBuddyTriggerCount(),
                            frameInput.swordCollisionActive(),
                            frameInput.randomByteSupplier());
            slotState.updated(booBuddyUpdate.entity());
            state.slot().setTransitionCountdown(slotState.entity().slot(), booBuddyUpdate.transitionCountdown());
            if (booBuddyUpdate.healthOverride() >= 0) {
                state.slot().setHealth(slotState.entity().slot(), booBuddyUpdate.healthOverride());
            }
            if (booBuddyUpdate.unloaded()) {
                state.lifecycle()
                        .disableEntityWithoutPersistence(slotState.entity().slot());
                return false;
            }
        }
        return true;
    }

    private static void initialize(EntityHandlerRegistration registry, RoomEntity entity, IntSupplier random) {
        registry.motion(BooBuddyMotion.class, BooBuddyMotion::new).initialize(entity.slot());
    }
}
