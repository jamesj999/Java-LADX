package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_BOMBER;


import static linksawakening.world.entity.EntityRomConstants.BOMBER_THROW_JINGLE_ID;


import static linksawakening.world.entity.EntityRomConstants.ENTITY_BOMB;


import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.event.EntityCombatEvent;

import linksawakening.world.entity.motion.BomberMotion;

/** ROM handler stages for Bomber. */
final class BomberHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        ENTITY_BOMBER,
        EntityTickStage.PHASE_2_BOMBER,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            BomberMotion.Update bomberUpdate =
                state.bomberMotion().advance(
                    state.entity,
                    state.slot().transitionCountdown(state.entity.slot()),
                    state.linkEntityX,
                    state.linkEntityY,
                    state.romLinkDirection,
                    state.swordCollisionActive,
                    state.randomByteSupplier,
                    state.backgroundCollision);
            state.updated = bomberUpdate.entity();
            state.slot().setTransitionCountdown(state.entity.slot(), bomberUpdate.transitionCountdown());
            if (bomberUpdate.bombSpawn() != null) {
              BomberMotion.BombSpawn spawn = bomberUpdate.bombSpawn();
              int bombSlot =
                  state.enemySpawns().spawnEnemyBomb(
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
                        ENTITY_BOMB,
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        BOMBER_THROW_JINGLE_ID));
              }
            }
          }
          return true;
        });
  }
}
