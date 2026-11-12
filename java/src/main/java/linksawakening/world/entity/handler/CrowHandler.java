package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_CROW;

import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.motion.CrowMotion;

/** ROM handler stages for Crow. */
final class CrowHandler {
    static void install(EntityHandlerRegistration registry) {
        for (int type = 0; type < 256; type++) {
            if ((type == ENTITY_CROW)) {
                registry.registerTick(type, EntityTickStage.PHASE_1_CROW, state -> {
                    if (state.slot().ignoreHits(state.entity.slot()) >= 0x10) {
                        state.slot().shiftIgnoreHitsRight(state.entity.slot(), 2);
                    }
                    return true;
                });
            }
        }
        registry.registerTick(ENTITY_CROW, EntityTickStage.PHASE_3_CROW, state -> {
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
                CrowMotion.Update crowUpdate = state.crowMotion()
                        .advance(
                                state.entity,
                                state.slot().transitionCountdown(state.entity.slot()),
                                state.frame,
                                state.linkEntityX,
                                state.linkEntityY,
                                state.roomInspection().entityRoomId() != 0x58,
                                state.slotProperties().kanaletCrowTakeoffTriggered(state.entity));
                state.updated = crowUpdate.entity();
                state.slot().setTransitionCountdown(state.entity.slot(), crowUpdate.transitionCountdown());
                state.slot().setPhysicsFlags(state.entity.slot(), crowUpdate.physicsFlags());
                if (crowUpdate.boomerangSound()) {
                    state.projectiles().playBoomerangSoundIfDue(state.updated);
                }
                if (crowUpdate.unloaded()) {
                    state.lifecycle().disableEntityWithoutPersistence(state.entity.slot());
                    return false;
                }
            }
            return true;
        });
    }
}
