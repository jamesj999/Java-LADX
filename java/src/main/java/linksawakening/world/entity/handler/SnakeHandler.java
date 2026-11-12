package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_SNAKE;

import java.util.function.IntSupplier;
import linksawakening.world.entity.*;
import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.SnakeMotion;

/** ROM handler stages for Snake. */
final class SnakeHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerInitializer(ENTITY_SNAKE, (entity, random) -> initialize(registry, entity, random));
        registry.registerTick(ENTITY_SNAKE, EntityTickStage.PHASE_3_AFTER_LASER, 13, SnakeHandler::phase3Snake);
    }

    private static boolean phase3Snake(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var presentationFlags = state.presentation();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            SnakeMotion.Update snakeUpdate = state.motion(SnakeMotion.class, SnakeMotion::new)
                    .advance(
                            slotState.entity(),
                            frameInput.frame(),
                            frameInput.linkEntityX(),
                            frameInput.linkEntityY(),
                            frameInput.randomByteSupplier(),
                            frameInput.backgroundCollision(),
                            state.slot().transitionCountdown(slotState.entity().slot()));
            slotState.updated(snakeUpdate.entity());
            state.slot().setTransitionCountdown(slotState.entity().slot(), snakeUpdate.transitionCountdown());
            presentationFlags.preserveSnakePresentation(true);
        }
        return true;
    }

    private static void initialize(EntityHandlerRegistration registry, RoomEntity entity, IntSupplier random) {
        registry.motion(SnakeMotion.class, SnakeMotion::new).initialize(entity.slot());
    }
}
