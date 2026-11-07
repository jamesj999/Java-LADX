package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_MUSICAL_NOTE;


import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;

/** ROM handler stages for MusicalNote. */
final class MusicalNoteHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        ENTITY_MUSICAL_NOTE,
        EntityTickStage.PHASE_1_MUSICAL_NOTE,
        state -> {
          if (state.status == EntityStatus.ACTIVE) {
            RoomEntity note = state.slotProperties().advanceMusicalNoteEntity(state.entity);
            if (note == null) {
              return false;
            }
            state.replaceEntity(note);
            return false;
          }
          return true;
        });
  }
}
