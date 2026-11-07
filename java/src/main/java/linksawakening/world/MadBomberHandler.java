package linksawakening.world;

import linksawakening.world.entity.motion.MadBomberMotion;

import static linksawakening.world.EntitySnapshots.withVariant;

/** ROM handler stages for MadBomber. */
final class MadBomberHandler {
  static void install(EntityHandlerRegistry registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_MAD_BOMBER,
        EntityTickStage.PHASE_2_MAD_BOMBER,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            // MadBomberEntityHandler rewrites this handler-owned drop
            // byte on every interactive pass before its state dispatch.
            state.runtime.droppedItemBySlot[state.entity.slot()] = 0x3C;
            MadBomberMotion.Update madBomberUpdate =
                state.runtime.madBomberMotion.advance(
                    state.entity,
                    state.runtime.enemyTransitionCountdown[state.entity.slot()],
                    state.linkEntityX,
                    state.linkEntityY,
                    state.runtime.enemyFlashCountdown[state.entity.slot()],
                    state.randomByteSupplier);
            state.updated = withVariant(madBomberUpdate.entity(), madBomberUpdate.spriteVariant());
            state.preserveMadBomberPresentation = true;
            state.runtime.enemyTransitionCountdown[state.entity.slot()] =
                madBomberUpdate.transitionCountdown();
            if (madBomberUpdate.bombSpawn() != null) {
              MadBomberMotion.BombSpawn spawn = madBomberUpdate.bombSpawn();
              int bombSlot =
                  state.runtime.spawnEnemyBomb(
                      spawn.x(),
                      spawn.y(),
                      spawn.z(),
                      spawn.transitionCountdown(),
                      spawn.speedX(),
                      spawn.speedY(),
                      spawn.speedZ());
              if (bombSlot >= 0 && madBomberUpdate.playJingle()) {
                state.emit(
                    new EntityCombatEvent(
                        bombSlot,
                        state.runtime.ENTITY_BOMB,
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        state.runtime.BOMBER_THROW_JINGLE_ID));
              }
            }
          }
          return true;
        });
  }
}
