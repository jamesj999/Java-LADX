package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_MUSICAL_NOTE;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntity;

/** ROM handler stages for MusicalNote. */
final class MusicalNoteHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_MUSICAL_NOTE,
                EntityTickStage.PHASE_1_BEFORE_PICKUP_SEQUENCE,
                10,
                MusicalNoteHandler::phase1MusicalNote);
    }

    private static boolean phase1MusicalNote(EntityFrameExecution state) {
        var slotState = state.slotFrame();

        if (slotState.status() == EntityStatus.ACTIVE) {
            RoomEntity note = state.slotProperties().advanceMusicalNoteEntity(slotState.entity());
            if (note == null) {
                return false;
            }
            state.replaceEntity(note);
            return false;
        }
        return true;
    }
}
