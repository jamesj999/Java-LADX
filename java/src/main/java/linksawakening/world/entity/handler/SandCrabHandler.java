package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_SAND_CRAB;

import java.util.function.IntSupplier;
import linksawakening.world.entity.*;
import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.SandCrabMotion;

/** ROM handler stages for SandCrab. */
final class SandCrabHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerInitializer(ENTITY_SAND_CRAB, (entity, random) -> initialize(registry, entity, random));
        registry.registerTick(
                ENTITY_SAND_CRAB, EntityTickStage.PHASE_4_BEFORE_FOLLOWER, 13, SandCrabHandler::phase4SandCrab);
    }

    private static boolean phase4SandCrab(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();
        var presentationFlags = state.presentation();

        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && !frameInput.creditsGameplay()
                && timerGates.handlerLinkCollisionEnabled()) {
            int slot = slotState.entity().slot();
            RoomEntityBackgroundInteraction sandCrabBackgroundInteraction =
                    state.background().interaction();
            if (sandCrabBackgroundInteraction == null && frameInput.backgroundCollision() != null) {
                sandCrabBackgroundInteraction =
                        RoomEntityBackgroundInteraction.fromBoolean(frameInput.backgroundCollision());
            }
            SandCrabMotion.Update sandCrabUpdate = state.motion(SandCrabMotion.class, SandCrabMotion::new)
                    .advance(
                            slotState.updated(),
                            frameInput.frame(),
                            state.slot().transitionCountdown(slot),
                            frameInput.randomByteSupplier(),
                            sandCrabBackgroundInteraction);
            slotState.updated(sandCrabUpdate.entity());
            state.slot().setTransitionCountdown(slot, sandCrabUpdate.transitionCountdown());
            slotState.applyGenericGroundInteraction(sandCrabUpdate.appliesBackgroundInteraction());
            presentationFlags.preserveSandCrabPresentation(true);
        }
        return true;
    }

    private static void initialize(EntityHandlerRegistration registry, RoomEntity entity, IntSupplier random) {
        registry.motion(SandCrabMotion.class, SandCrabMotion::new).initialize(entity.slot());
    }
}
