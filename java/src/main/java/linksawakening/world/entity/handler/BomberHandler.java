package linksawakening.world.entity.handler;

import linksawakening.world.EntityTickStage;
import linksawakening.world.RoomEntityRuntime;
import linksawakening.world.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.EntityCombatEvent;







import linksawakening.world.entity.motion.BomberMotion;



/** ROM handler stages for Bomber. */
final class BomberHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_BOMBER,
        EntityTickStage.PHASE_2_BOMBER,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            BomberMotion.Update bomberUpdate =
                state.runtime.bomberMotion.advance(
                    state.entity,
                    state.runtime.enemyTransitionCountdown[state.entity.slot()],
                    state.linkEntityX,
                    state.linkEntityY,
                    state.romLinkDirection,
                    state.swordCollisionActive,
                    state.randomByteSupplier,
                    state.backgroundCollision);
            state.updated = bomberUpdate.entity();
            state.runtime.enemyTransitionCountdown[state.entity.slot()] =
                bomberUpdate.transitionCountdown();
            if (bomberUpdate.bombSpawn() != null) {
              BomberMotion.BombSpawn spawn = bomberUpdate.bombSpawn();
              int bombSlot =
                  state.runtime.spawnEnemyBomb(
                      spawn.x(),
                      spawn.y(),
                      spawn.z(),
                      spawn.transitionCountdown(),
                      spawn.speedX(),
                      spawn.speedY(),
                      spawn.speedZ());
              if (bombSlot >= 0 && bomberUpdate.playJingle()) {
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
