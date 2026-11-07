package linksawakening.world;



/** ROM handler stages for MusicalNote. */
final class MusicalNoteHandler {
  static void install(EntityHandlerRegistry registry) {
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
