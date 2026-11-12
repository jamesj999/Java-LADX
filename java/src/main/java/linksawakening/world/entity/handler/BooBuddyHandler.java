package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_BOO_BUDDY;
import static linksawakening.world.entity.EntitySnapshots.withVariant;

import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.motion.BooBuddyMotion;

/** ROM handler stages for BooBuddy. */
final class BooBuddyHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(ENTITY_BOO_BUDDY, EntityTickStage.PHASE_1_BOO_BUDDY, state -> {
            if (state.initialStatus == EntityStatus.ACTIVE) {
                int slot = state.entity.slot();
                // Rendering precedes ReturnIfNonInteractive_06 and recoil.
                state.entity = withVariant(state.entity, state.booBuddyMotion().selectedVariant(slot));
                state.replaceEntity(state.entity);
                if (state.projectileTimersInteractive) {
                    state.booBuddyMotion().tickTimers(slot);
                }
                if (!state.handlerInteractive) {
                    return false;
                }
                if (state.slot().ignoreHits(slot) != 0) {
                    // $06:64F7 consumes one ignore-hits tick, moves with recoil
                    // speeds, and pops the handler return address. OPTIONS1
                    // bit $20 skips background collision for Boo Buddy.
                    state.slot().decrementIgnoreHits(slot);
                    state.replaceEntity(state.booBuddyMotion()
                            .advanceRecoil(
                                    state.entity,
                                    state.enemyRecoilMotion().recoilSpeedX(slot),
                                    state.enemyRecoilMotion().recoilSpeedY(slot)));
                    return false;
                }
            }
            return true;
        });
        registry.registerTick(ENTITY_BOO_BUDDY, EntityTickStage.PHASE_3_BOO_BUDDY, state -> {
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
                BooBuddyMotion.Update booBuddyUpdate = state.booBuddyMotion()
                        .advanceAfterTimers(
                                state.entity,
                                state.frame,
                                state.linkEntityX,
                                state.linkEntityY,
                                state.slot().transitionCountdown(state.entity.slot()),
                                state.story().booBuddyTriggerCount(),
                                state.swordCollisionActive,
                                state.randomByteSupplier);
                state.updated = booBuddyUpdate.entity();
                state.slot().setTransitionCountdown(state.entity.slot(), booBuddyUpdate.transitionCountdown());
                if (booBuddyUpdate.healthOverride() >= 0) {
                    state.slot().setHealth(state.entity.slot(), booBuddyUpdate.healthOverride());
                }
                if (booBuddyUpdate.unloaded()) {
                    state.lifecycle().disableEntityWithoutPersistence(state.entity.slot());
                    return false;
                }
            }
            return true;
        });
    }
}
