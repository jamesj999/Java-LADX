package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_WITCH_RAT;

import java.util.function.IntSupplier;
import linksawakening.world.entity.*;
import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.WitchRatMotion;

/** ROM handler stages for WitchRat. */
final class WitchRatHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerInitializer(ENTITY_WITCH_RAT, (entity, random) -> initialize(registry, entity, random));
        registry.registerTick(
                ENTITY_WITCH_RAT, EntityTickStage.PHASE_4_BEFORE_FOLLOWER, 18, WitchRatHandler::phase4WitchRat);
    }

    private static boolean phase4WitchRat(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();
        var presentationFlags = state.presentation();

        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && timerGates.handlerLinkCollisionEnabled()) {

            int slot = slotState.entity().slot();
            RoomEntityBackgroundInteraction witchRatBackgroundInteraction =
                    state.background().interaction();
            if (witchRatBackgroundInteraction == null && frameInput.backgroundCollision() != null) {
                witchRatBackgroundInteraction = frameInput.hinoxBackgroundInteraction();
            }
            WitchRatMotion.Update witchRatUpdate = state.motion(WitchRatMotion.class, WitchRatMotion::new)
                    .advance(
                            slotState.updated(),
                            frameInput.frame(),
                            state.slot().transitionCountdown(slot),
                            frameInput.randomByteSupplier(),
                            witchRatBackgroundInteraction);
            slotState.updated(witchRatUpdate.entity());
            state.slot().setTransitionCountdown(slot, witchRatUpdate.transitionCountdown());
            slotState.applyGenericGroundInteraction(witchRatUpdate.appliesBackgroundInteraction());
            presentationFlags.preserveWitchRatPresentation(true);
        }
        return true;
    }

    private static void initialize(EntityHandlerRegistration registry, RoomEntity entity, IntSupplier random) {
        registry.motion(WitchRatMotion.class, WitchRatMotion::new).initialize(entity.slot());
    }
}
