package linksawakening.world;

import linksawakening.world.entity.motion.CrowMotion;



/** ROM handler stages for Crow. */
final class CrowHandler {
  static void install(EntityHandlerRegistry registry) {
    for (int type = 0; type < 256; type++) {
      if ((type == RoomEntityRuntime.ENTITY_CROW)) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_1_CROW,
            state -> {
              if (state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()] >= 0x10) {
                state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()] >>>= 2;
              }
              return true;
            });
      }
    }
    registry.registerTick(
        RoomEntityRuntime.ENTITY_CROW,
        EntityTickStage.PHASE_3_CROW,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            CrowMotion.Update crowUpdate =
                state.runtime.crowMotion.advance(
                    state.entity,
                    state.runtime.enemyTransitionCountdown[state.entity.slot()],
                    state.frame,
                    state.linkEntityX,
                    state.linkEntityY,
                    state.runtime.entityRoomId != 0x58,
                    state.runtime.slotProperties.kanaletCrowTakeoffTriggered(state.entity));
            state.updated = crowUpdate.entity();
            state.runtime.enemyTransitionCountdown[state.entity.slot()] =
                crowUpdate.transitionCountdown();
            state.runtime.enemyPhysicsFlags[state.entity.slot()] = crowUpdate.physicsFlags();
            if (crowUpdate.boomerangSound()) {
              state.runtime.projectiles.playBoomerangSoundIfDue(state.updated);
            }
            if (crowUpdate.unloaded()) {
              state.lifecycle().disableEntityWithoutPersistence(state.entity.slot());
              return false;
            }
          }
          return true;
        });
  }
}
