package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_PEAHAT;

import java.util.function.IntSupplier;
import linksawakening.world.entity.*;
import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.motion.*;

/** ROM handler stages for Peahat. */
final class PeahatHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerInitializer(ENTITY_PEAHAT, (entity, random) -> initialize(registry, entity, random));
        registry.registerTick(ENTITY_PEAHAT, EntityTickStage.PHASE_3_AFTER_LASER, 15, PeahatHandler::phase3Peahat);
    }

    private static boolean phase3Peahat(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            slotState.updated(state.motion(PeaHatMotion.class, PeaHatMotion::new)
                    .advance(
                            slotState.entity(),
                            frameInput.frame(),
                            frameInput.randomByteSupplier(),
                            frameInput.backgroundCollision()));
        }
        return true;
    }

    private static void initialize(EntityHandlerRegistration registry, RoomEntity entity, IntSupplier random) {
        registry.motion(PeaHatMotion.class, PeaHatMotion::new).initialize(entity.slot());
    }
}
