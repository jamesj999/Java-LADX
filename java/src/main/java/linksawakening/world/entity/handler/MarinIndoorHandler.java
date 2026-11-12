package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_MARIN_INDOOR;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityTickStage;

/** ROM handler stages for MarinIndoor. */
final class MarinIndoorHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_MARIN_INDOOR,
                EntityTickStage.PHASE_1_BEFORE_STATUS_TIMERS,
                1,
                MarinIndoorHandler::phase1MarinIndoor);
    }

    private static boolean phase1MarinIndoor(EntityFrameExecution state) {
        var slotState = state.slotFrame();

        if (state.roomInspection().indoorRoom() && state.roomInspection().indoorMarinAbsent()) {
            state.lifecycle().disableEntityWithoutPersistence(slotState.entity().slot());
            return false;
        }
        return true;
    }
}
