package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_WATER_TEKTITE;

import java.util.function.IntSupplier;
import linksawakening.world.entity.*;
import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.motion.*;

/** ROM handler stages for WaterTektite. */
final class WaterTektiteHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerInitializer(ENTITY_WATER_TEKTITE, (entity, random) -> initialize(registry, entity, random));
        registry.registerTick(
                ENTITY_WATER_TEKTITE, EntityTickStage.PHASE_3_AFTER_LASER, 2, WaterTektiteHandler::phase3WaterTektite);
    }

    private static boolean phase3WaterTektite(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            slotState.updated(state.motion(WaterTektiteMotion.class, WaterTektiteMotion::new)
                    .advance(
                            slotState.entity(),
                            frameInput.frame(),
                            frameInput.randomByteSupplier(),
                            frameInput.backgroundCollision()));
        }
        return true;
    }

    private static void initialize(EntityHandlerRegistration registry, RoomEntity entity, IntSupplier random) {
        registry.motion(WaterTektiteMotion.class, WaterTektiteMotion::new).initialize(entity.slot());
    }
}
