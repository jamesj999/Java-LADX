package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_LEEVER;
import static linksawakening.world.entity.EntitySnapshots.withVariant;

import java.util.function.IntSupplier;
import linksawakening.world.entity.*;
import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.motion.*;

/** ROM handler stages for Leever. */
final class LeeverHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerInitializer(ENTITY_LEEVER, (entity, random) -> initialize(registry, entity, random));
        registry.registerAdvance(ENTITY_LEEVER, (entity, context) -> advance(registry, entity, context));
        registry.registerTick(
                ENTITY_LEEVER, EntityTickStage.PHASE_2_AFTER_RECOIL_APPLICATION, 5, LeeverHandler::phase2Leever);
    }

    private static boolean phase2Leever(EntityFrameExecution state) {
        var slotState = state.slotFrame();

        if (slotState.wasInitializing()) {
            // EntityInitLeever calls SetEntitySpriteVariant($FF) before
            // the first active handler dispatch.
            slotState.updated(withVariant(slotState.entity(), -1));
        }
        return true;
    }

    private static void initialize(EntityHandlerRegistration registry, RoomEntity entity, IntSupplier random) {
        registry.motion(LeeverMotion.class, LeeverMotion::new).initialize(entity.slot());
    }

    private static RoomEntity advance(
            EntityHandlerRegistration registry, RoomEntity entity, EntityFrameContext context) {
        return registry.motion(LeeverMotion.class, LeeverMotion::new)
                .advance(
                        entity,
                        context.frameCounter(),
                        context.linkEntityX(),
                        context.linkEntityY(),
                        context.randomByteSupplier(),
                        context.backgroundCollision());
    }
}
