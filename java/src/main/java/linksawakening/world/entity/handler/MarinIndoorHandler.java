package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_MARIN_INDOOR;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityTickStage;

/** ROM handler stages for MarinIndoor. */
final class MarinIndoorHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_MARIN_INDOOR, EntityTickStage.PHASE_1_MARIN_INDOOR, MarinIndoorHandler::phase1MarinIndoor);
    }

    private static boolean phase1MarinIndoor(EntityFrameExecution state) {
        if (state.roomInspection().indoorRoom() && state.roomInspection().indoorMarinAbsent()) {
            state.lifecycle().disableEntityWithoutPersistence(state.entity.slot());
            return false;
        }
        return true;
    }
}
