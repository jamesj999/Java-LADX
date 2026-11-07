package linksawakening.world;



/** ROM handler stages for MusicalNote. */
final class MusicalNoteHandler {
  static void installPhase1(EntityHandlerRegistry registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_MUSICAL_NOTE,
        EntityTickStage.PHASE_1_MUSICAL_NOTE,
        state -> {
          if (state.status == EntityStatus.ACTIVE) {
            RoomEntity note = state.runtime.slotProperties.advanceMusicalNoteEntity(state.entity);
            if (note == null) {
              return false;
            }
            state.runtime.slots[state.index] = note;
            return false;
          }
          return true;
        });
  }
}
