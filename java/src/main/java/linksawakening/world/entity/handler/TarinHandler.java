package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_TARIN;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.motion.*;

/** ROM handler stages for Tarin. */
final class TarinHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(ENTITY_TARIN, EntityTickStage.PHASE_1_BEFORE_STATUS_TIMERS, 0, TarinHandler::phase1Tarin);
        registry.registerTick(ENTITY_TARIN, EntityTickStage.PHASE_4_BEFORE_FOLLOWER, 15, TarinHandler::phase4Tarin);
    }

    private static boolean phase1Tarin(EntityFrameExecution state) {
        var slotState = state.slotFrame();

        if ((state.roomInspection().indoorRoom() && state.roomInspection().indoorTarinAbsent())
                || (!state.roomInspection().indoorRoom()
                        && (state.roomInspection().entityRoomStatus() & 0x10) != 0)) {
            state.lifecycle().disableEntityWithoutPersistence(slotState.entity().slot());
            return false;
        }
        return true;
    }

    private static boolean phase4Tarin(EntityFrameExecution state) {
        return state.scratch(TarinHandlerFrameStep.class, TarinHandlerFrameStep::new)
                .run(state);
    }
}
