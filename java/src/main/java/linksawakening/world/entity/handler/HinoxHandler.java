package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_HINOX;

import java.util.function.IntSupplier;
import linksawakening.world.entity.*;
import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.HinoxMotion;

/** ROM handler stages for Hinox. */
final class HinoxHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerInitializer(ENTITY_HINOX, (entity, random) -> initialize(registry, entity, random));
        registry.registerTick(ENTITY_HINOX, EntityTickStage.PHASE_4_BEFORE_FOLLOWER, 2, HinoxHandler::phase4Hinox);
    }

    private static boolean phase4Hinox(EntityFrameExecution state) {
        return state.scratch(HinoxHandlerFrameStep.class, HinoxHandlerFrameStep::new)
                .run(state);
    }

    private static void initialize(EntityHandlerRegistration registry, RoomEntity entity, IntSupplier random) {
        registry.motion(HinoxMotion.class, HinoxMotion::new).initialize(entity.slot());
    }
}
