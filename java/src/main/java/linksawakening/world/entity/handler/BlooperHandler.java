package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_BLOOPER;

import java.util.Objects;
import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistry;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.entity.RoomEntityGroundInteraction;
import linksawakening.world.entity.motion.BlooperMotion;

/** ROM handler stages for Blooper. */
final class BlooperHandler {
    static void install(EntityHandlerRegistry registry) {
        registry.registerInitializer(ENTITY_BLOOPER, (entity, random) -> initialize(registry, entity));
        registry.registerTick(ENTITY_BLOOPER, EntityTickStage.PHASE_3_BEFORE_LASER, 10, BlooperHandler::phase3Blooper);
    }

    private static boolean phase3Blooper(EntityFrameExecution state) {
        var resolvedBlooperMotion = state.motion(BlooperMotion.class, BlooperMotion::new);

        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();
        var presentationFlags = state.presentation();

        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && timerGates.handlerLinkCollisionEnabled()) {

            RoomEntityBackgroundInteraction blooperBackgroundInteraction = frameInput.hinoxBackgroundInteraction();

            BlooperMotion.Movement movement = resolvedBlooperMotion.move(
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
            BlooperMotion.Update blooperUpdate = resolvedBlooperMotion.finish(
                    movement, groundResult, frameInput.frame(), frameInput.linkEntityX(), frameInput.linkEntityY());
            slotState.updated(blooperUpdate.entity());
            slotState.preAppliedGroundResult(blooperUpdate.groundResult());
            presentationFlags.preserveBlooperPresentation(true);
        }
        return true;
    }

    private static void initialize(EntityHandlerRegistry registry, RoomEntity entity) {
        registry.motion(BlooperMotion.class, BlooperMotion::new).initialize(entity.slot());
    }
}
