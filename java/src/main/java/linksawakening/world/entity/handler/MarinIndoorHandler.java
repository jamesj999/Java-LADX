package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_MARIN_INDOOR;

import linksawakening.world.entity.EntityHandlerRegistry;
import linksawakening.world.entity.EntityRoomInspection;
import linksawakening.world.entity.EntitySlotExecution;
import linksawakening.world.entity.EntitySlotLifecycle;
import linksawakening.world.entity.EntityTickStage;

/** ROM handler stages for MarinIndoor. */
final class MarinIndoorHandler {
    static void install(EntityHandlerRegistry registry) {
        registry.registerTick(
                ENTITY_MARIN_INDOOR,
                EntityTickStage.PHASE_1_BEFORE_STATUS_TIMERS,
                1,
                frame -> phase1MarinIndoor(frame.slotFrame(), frame.roomInspection(), frame.lifecycle()));
    }

    private static boolean phase1MarinIndoor(
            EntitySlotExecution slotState, EntityRoomInspection roomInspection, EntitySlotLifecycle lifecycle) {
        if (roomInspection.indoorRoom() && roomInspection.indoorMarinAbsent()) {
            lifecycle.disableEntityWithoutPersistence(slotState.entity().slot());
            return false;
        }
        return true;
    }
}
