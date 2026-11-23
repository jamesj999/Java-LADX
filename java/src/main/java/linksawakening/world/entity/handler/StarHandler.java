package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_STAR;

import java.util.function.IntSupplier;
import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistry;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.entity.motion.StarMotion;

/** ROM handler stages for Star. */
final class StarHandler {
    static void install(EntityHandlerRegistry registry) {
        registry.registerInitializer(ENTITY_STAR, (entity, random) -> initialize(registry, entity, random));
        registry.registerTick(ENTITY_STAR, EntityTickStage.PHASE_3_BEFORE_LASER, 9, StarHandler::phase3Star);
    }

    private static boolean phase3Star(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();
        var presentationFlags = state.presentation();

        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && timerGates.handlerLinkCollisionEnabled()) {

            RoomEntityBackgroundInteraction starBackgroundInteraction = frameInput.hinoxBackgroundInteraction();

            slotState.updated(state.motion(StarMotion.class, StarMotion::new)
                    .advance(
                            slotState.entity(),
                            frameInput.frame(),
                            frameInput.randomByteSupplier(),
                            starBackgroundInteraction,
                            state.slot().ignoreHits(slotState.entity().slot())));
            presentationFlags.preserveStarPresentation(true);
        }
        return true;
    }

    private static void initialize(EntityHandlerRegistry registry, RoomEntity entity, IntSupplier random) {
        registry.motion(StarMotion.class, StarMotion::new).initialize(entity.slot(), random);
    }
}
