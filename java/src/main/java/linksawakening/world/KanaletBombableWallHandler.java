package linksawakening.world;



/** ROM handler stages for KanaletBombableWall. */
final class KanaletBombableWallHandler {
  static void installPhase1(EntityHandlerRegistry registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_KANALET_BOMBABLE_WALL,
        EntityTickStage.PHASE_1_KANALET_BOMBABLE_WALL,
        state -> {
          {
            if ((state.runtime.entityRoomStatus & 0x10) != 0) {
              state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
              return false;
            }
            state.runtime.droppedItemBySlot[state.entity.slot()] =
                state.runtime.ENTITY_HIDING_SLIME_KEY;
            if (state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()] != 0) {
              if (state.runtime.entityEnemySpawns.spawnKanaletWallMoblin(state.entity)) {
                state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
                return false;
              }
            }
          }
          return true;
        });
  }
}
