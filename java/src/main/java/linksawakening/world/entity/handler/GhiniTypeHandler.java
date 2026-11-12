package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntityTypeRules.isHidingGhiniType;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;

/** ROM handler stages for GhiniType. */
final class GhiniTypeHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_HIDING_GHINI, EntityTickStage.PHASE_2_GHINI_TYPE, GhiniTypeHandler::phase2GhiniType);
        registry.registerTick(
                ENTITY_GIANT_GHINI, EntityTickStage.PHASE_2_GHINI_TYPE, GhiniTypeHandler::phase2GhiniType);
        registry.registerTick(ENTITY_GHINI, EntityTickStage.PHASE_2_GHINI_TYPE, GhiniTypeHandler::phase2GhiniType);
        registry.registerTick(
                ENTITY_HIDING_GHINI, EntityTickStage.PHASE_4_GHINI_TYPE, GhiniTypeHandler::phase4GhiniType);
        registry.registerTick(
                ENTITY_GIANT_GHINI, EntityTickStage.PHASE_4_GHINI_TYPE, GhiniTypeHandler::phase4GhiniType);
        registry.registerTick(ENTITY_GHINI, EntityTickStage.PHASE_4_GHINI_TYPE, GhiniTypeHandler::phase4GhiniType);
    }

    private static boolean phase2GhiniType(EntityFrameExecution state) {
        state.ghiniMotion().initialize(state.entity.slot(), state.entity.type());
        if (isHidingGhiniType(state.entity.type())) {
            state.preserveGhiniPresentation = true;
        }

        return true;
    }

    private static boolean phase4GhiniType(EntityFrameExecution state) {
        if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            state.updated = state.ghiniMotion()
                    .advance(
                            state.entity,
                            state.frame,
                            state.entity.type(),
                            state.linkEntityX,
                            state.linkEntityY,
                            state.romCollisionType,
                            state.randomByteSupplier);
            state.preserveGhiniPresentation = true;
        }
        return true;
    }
}
