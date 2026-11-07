package linksawakening.world.entity.handler;

import linksawakening.world.EntityTickStage;
import linksawakening.world.RoomEntity;
import linksawakening.world.RoomEntityRuntime;
import linksawakening.world.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;









/** ROM handler stages for MusicalNote. */
final class MusicalNoteHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_MUSICAL_NOTE,
        EntityTickStage.PHASE_1_MUSICAL_NOTE,
        state -> {
          if (state.status == EntityStatus.ACTIVE) {
            RoomEntity note = state.runtime.slotProperties.advanceMusicalNoteEntity(state.entity);
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
