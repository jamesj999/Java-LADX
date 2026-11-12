package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntityTypeRules.isHidingGhiniType;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.motion.*;

/** ROM handler stages for GhiniType. */
final class GhiniTypeHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_HIDING_GHINI, EntityTickStage.PHASE_2_AFTER_INITIALIZE, 8, GhiniTypeHandler::phase2GhiniType);
        registry.registerTick(
                ENTITY_GIANT_GHINI, EntityTickStage.PHASE_2_AFTER_INITIALIZE, 8, GhiniTypeHandler::phase2GhiniType);
        registry.registerTick(
                ENTITY_GHINI, EntityTickStage.PHASE_2_AFTER_INITIALIZE, 8, GhiniTypeHandler::phase2GhiniType);
        registry.registerTick(
                ENTITY_HIDING_GHINI, EntityTickStage.PHASE_4_BEFORE_FOLLOWER, 19, GhiniTypeHandler::phase4GhiniType);
        registry.registerTick(
                ENTITY_GIANT_GHINI, EntityTickStage.PHASE_4_BEFORE_FOLLOWER, 19, GhiniTypeHandler::phase4GhiniType);
        registry.registerTick(
                ENTITY_GHINI, EntityTickStage.PHASE_4_BEFORE_FOLLOWER, 19, GhiniTypeHandler::phase4GhiniType);
    }

    private static boolean phase2GhiniType(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var presentationFlags = state.presentation();

        state.motion(GhiniMotion.class, GhiniMotion::new)
                .initialize(slotState.entity().slot(), slotState.entity().type());
        if (isHidingGhiniType(slotState.entity().type())) {
            presentationFlags.preserveGhiniPresentation(true);
        }

        return true;
    }

    private static boolean phase4GhiniType(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var presentationFlags = state.presentation();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            slotState.updated(state.motion(GhiniMotion.class, GhiniMotion::new)
                    .advance(
                            slotState.entity(),
                            frameInput.frame(),
                            slotState.entity().type(),
                            frameInput.linkEntityX(),
                            frameInput.linkEntityY(),
                            frameInput.romCollisionType(),
                            frameInput.randomByteSupplier()));
            presentationFlags.preserveGhiniPresentation(true);
        }
        return true;
    }
}
