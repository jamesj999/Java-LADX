package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_CROW;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.motion.CrowMotion;

/** ROM handler stages for Crow. */
final class CrowHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(ENTITY_CROW, EntityTickStage.PHASE_1_LEAF_PRODUCER, 0, CrowHandler::phase1Crow);
        registry.registerTick(ENTITY_CROW, EntityTickStage.PHASE_3_AFTER_LASER, 4, CrowHandler::phase3Crow);
    }

    private static boolean phase1Crow(EntityFrameExecution state) {
        if (state.slot().ignoreHits(state.entity.slot()) >= 0x10) {
            state.slot().shiftIgnoreHitsRight(state.entity.slot(), 2);
        }
        return true;
    }

    private static boolean phase3Crow(EntityFrameExecution state) {
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
    }
}
