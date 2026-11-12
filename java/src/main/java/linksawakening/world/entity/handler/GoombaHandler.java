package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_GOOMBA;

import java.util.function.IntSupplier;
import linksawakening.world.entity.*;
import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.GoombaMotion;

/** ROM handler stages for Goomba. */
final class GoombaHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerInitializer(ENTITY_GOOMBA, (entity, random) -> initialize(registry, entity, random));
        registry.registerTick(ENTITY_GOOMBA, EntityTickStage.PHASE_3_AFTER_LASER, 12, GoombaHandler::phase3Goomba);
    }

    private static boolean phase3Goomba(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            GoombaMotion.Update goombaUpdate = state.motion(GoombaMotion.class, GoombaMotion::new)
                    .advance(
                            slotState.entity(),
                            frameInput.frame(),
                            frameInput.linkEntityX(),
                            frameInput.linkEntityY(),
                            frameInput.randomByteSupplier(),
                            frameInput.backgroundCollision(),
                            state.slot().transitionCountdown(slotState.entity().slot()),
                            state.background().groundInteractionSideScrolling());
            slotState.updated(goombaUpdate.entity());
            state.slot().setTransitionCountdown(slotState.entity().slot(), goombaUpdate.transitionCountdown());
            if (goombaUpdate.startDying()) {
                // func_007_666B writes ENTITY_DROPPABLE_HEART and the
                // private-countdown-3 death window before setting status
                // DYING and physics flags to 4. Keep both the source
                // table and the runtime death countdown synchronized;
                // the latter drives the shared Java death presentation.
                state.lifecycle().setDroppedItemBySlot(slotState.entity().slot(), 0x2D);
                state.lifecycle().setDropPrivateCountdown3(slotState.entity().slot(), 0x0C);
                state.lifecycle().setDyingCountdown(slotState.entity().slot(), 0x0C);
                state.slot().setPhysicsFlags(slotState.entity().slot(), 0x04);
                slotState.status(EntityStatus.DYING);
            }
        }
        return true;
    }

    private static void initialize(EntityHandlerRegistration registry, RoomEntity entity, IntSupplier random) {
        registry.motion(GoombaMotion.class, GoombaMotion::new).initialize(entity.slot());
    }
}
