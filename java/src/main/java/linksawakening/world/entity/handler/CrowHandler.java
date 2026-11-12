package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_CROW;

import java.util.function.IntSupplier;
import linksawakening.world.entity.*;
import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.CrowMotion;

/** ROM handler stages for Crow. */
final class CrowHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerInitializer(ENTITY_CROW, (entity, random) -> initialize(registry, entity, random));
        registry.registerTick(ENTITY_CROW, EntityTickStage.PHASE_1_LEAF_PRODUCER, 0, CrowHandler::phase1Crow);
        registry.registerTick(ENTITY_CROW, EntityTickStage.PHASE_3_AFTER_LASER, 4, CrowHandler::phase3Crow);
    }

    private static boolean phase1Crow(EntityFrameExecution state) {
        var slotState = state.slotFrame();

        if (state.slot().ignoreHits(slotState.entity().slot()) >= 0x10) {
            state.slot().shiftIgnoreHitsRight(slotState.entity().slot(), 2);
        }
        return true;
    }

    private static boolean phase3Crow(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            CrowMotion.Update crowUpdate = state.motion(CrowMotion.class, CrowMotion::new)
                    .advance(
                            slotState.entity(),
                            state.slot().transitionCountdown(slotState.entity().slot()),
                            frameInput.frame(),
                            frameInput.linkEntityX(),
                            frameInput.linkEntityY(),
                            state.roomInspection().entityRoomId() != 0x58,
                            state.slotProperties().kanaletCrowTakeoffTriggered(slotState.entity()));
            slotState.updated(crowUpdate.entity());
            state.slot().setTransitionCountdown(slotState.entity().slot(), crowUpdate.transitionCountdown());
            state.slot().setPhysicsFlags(slotState.entity().slot(), crowUpdate.physicsFlags());
            if (crowUpdate.boomerangSound()) {
                state.projectiles().playBoomerangSoundIfDue(slotState.updated());
            }
            if (crowUpdate.unloaded()) {
                state.lifecycle()
                        .disableEntityWithoutPersistence(slotState.entity().slot());
                return false;
            }
        }
        return true;
    }

    private static void initialize(EntityHandlerRegistration registry, RoomEntity entity, IntSupplier random) {
        registry.motion(CrowMotion.class, CrowMotion::new).initialize(entity.slot());
    }
}
