package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_BLOOPER;

import java.util.Objects;
import java.util.function.IntSupplier;
import linksawakening.world.entity.*;
import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.entity.RoomEntityGroundInteraction;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.BlooperMotion;

/** ROM handler stages for Blooper. */
final class BlooperHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerInitializer(ENTITY_BLOOPER, (entity, random) -> initialize(registry, entity, random));
        registry.registerTick(ENTITY_BLOOPER, EntityTickStage.PHASE_3_BEFORE_LASER, 10, BlooperHandler::phase3Blooper);
    }

    private static boolean phase3Blooper(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();
        var presentationFlags = state.presentation();

        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && timerGates.handlerLinkCollisionEnabled()) {

            RoomEntityBackgroundInteraction blooperBackgroundInteraction =
                    state.background().interaction();
            if (blooperBackgroundInteraction == null && frameInput.backgroundCollision() != null) {
                blooperBackgroundInteraction =
                        RoomEntityBackgroundInteraction.fromBoolean(frameInput.backgroundCollision());
            }
            BlooperMotion.Movement movement = state.motion(BlooperMotion.class, BlooperMotion::new)
                    .move(
                            slotState.entity(),
                            frameInput.frame(),
                            blooperBackgroundInteraction,
                            state.slot().ignoreHits(slotState.entity().slot()));
            RoomEntityGroundInteraction.Result groundResult = Objects.requireNonNull(
                    state.background()
                            .groundInteraction()
                            .apply(
                                    movement.entity(),
                                    frameInput.frame(),
                                    state.background()
                                            .entityGroundStatus(
                                                    slotState.entity().slot()),
                                    state.background().verticalSpeedZ(movement.entity()),
                                    state.background().groundInteractionSideScrolling()),
                    "Room entity ground interaction returned null");
            BlooperMotion.Update blooperUpdate = state.motion(BlooperMotion.class, BlooperMotion::new)
                    .finish(
                            movement,
                            groundResult,
                            frameInput.frame(),
                            frameInput.linkEntityX(),
                            frameInput.linkEntityY());
            slotState.updated(blooperUpdate.entity());
            slotState.preAppliedGroundResult(blooperUpdate.groundResult());
            presentationFlags.preserveBlooperPresentation(true);
        }
        return true;
    }

    private static void initialize(EntityHandlerRegistration registry, RoomEntity entity, IntSupplier random) {
        registry.motion(BlooperMotion.class, BlooperMotion::new).initialize(entity.slot());
    }
}
