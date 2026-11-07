package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_BOMB;


import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.event.EntityCombatEvent;

import linksawakening.world.entity.motion.MadBomberMotion;

import static linksawakening.world.entity.EntitySnapshots.withVariant;

/** ROM handler stages for MadBomber. */
final class MadBomberHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_MAD_BOMBER,
        EntityTickStage.PHASE_2_MAD_BOMBER,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            // MadBomberEntityHandler rewrites this handler-owned drop
            // byte on every interactive pass before its state dispatch.
            state.runtime.droppedItemBySlot[state.entity.slot()] = 0x3C;
            MadBomberMotion.Update madBomberUpdate =
                state.madBomberMotion().advance(
                    state.entity,
                    state.slot().transitionCountdown(state.entity.slot()),
                    state.linkEntityX,
                    state.linkEntityY,
                    state.slot().flashCountdown(state.entity.slot()),
                    state.randomByteSupplier);
            state.updated = withVariant(madBomberUpdate.entity(), madBomberUpdate.spriteVariant());
            state.preserveMadBomberPresentation = true;
            state.slot().setTransitionCountdown(state.entity.slot(), madBomberUpdate.transitionCountdown());
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
                        ENTITY_BOMB,
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
