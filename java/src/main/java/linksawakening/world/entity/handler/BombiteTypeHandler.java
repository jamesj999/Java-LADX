package linksawakening.world.entity.handler;

import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.event.EntityCombatEvent;

import linksawakening.world.entity.motion.BombiteMotion;

import static linksawakening.world.entity.EntitySnapshots.withVariant;
import static linksawakening.world.entity.EntityTypeRules.isBombiteType;

/** ROM handler stages for BombiteType. */
final class BombiteTypeHandler {
  static void install(EntityHandlerRegistration registry) {
    for (int type = 0; type < 256; type++) {
      if (((isBombiteType(type)))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_BOMBITE_TYPE,
            state -> {
              {
                state.runtime.bombiteMotion.initialize(state.entity.slot());
                state.runtime.slowTimerInitialized[state.entity.slot()] = true;
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if ((isBombiteType(type))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_3_BOMBITE_TYPE,
            state -> {
              if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
                BombiteMotion.Update bombiteUpdate =
                    state.runtime.bombiteMotion.advance(
                        state.entity,
                        state.frame,
                        state.linkEntityX,
                        state.linkEntityY,
                        state.slot().transitionCountdown(state.entity.slot()),
                        state.runtime.slowTransitionCountdown[state.entity.slot()],
                        state.runtime.bombPrivateCountdown1[state.entity.slot()],
                        state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()],
                        state.runtime.linkState.runningWithPegasusBoots,
                        state.randomByteSupplier,
                        state.backgroundCollision);
                state.updated = withVariant(bombiteUpdate.entity(), bombiteUpdate.spriteVariant());
                state.preserveBombitePresentation = true;
                state.slot().setTransitionCountdown(state.entity.slot(), bombiteUpdate.transitionCountdown());
                state.runtime.slowTransitionCountdown[state.entity.slot()] =
                    bombiteUpdate.slowTransitionCountdown();
                state.runtime.bombPrivateCountdown1[state.entity.slot()] =
                    bombiteUpdate.privateCountdown1();
                if (bombiteUpdate.bumpJingle()) {
                  state.emit(
                      new EntityCombatEvent(
                          state.entity.slot(),
                          state.entity.type(),
                          0,
                          false,
                          EntityCombatEvent.SoundChannel.JINGLE,
                          0x09));
                }
                boolean entityCollisionExplosion =
                    state.entity.type() == state.runtime.ENTITY_BOUNCING_BOMBITE
                        && state.runtime.bombiteMotion.state(state.entity.slot()) == 2
                        && state.runtime.projectiles.collideBouncingBombiteWithEntities(
                            state.updated, state.frame);
                if (bombiteUpdate.explode() || entityCollisionExplosion) {
                  // BombiteExplode writes the already-positioned source
                  // coordinates into a fresh type-$02 enemy bomb and then
                  // lets ConfigureNewEntity supply z=0 and ignore-hits=1.
                  int bombSlot =
                      state.runtime.spawnEnemyBomb(
                          state.updated.x(),
                          state.updated.y(),
                          0,
                          state.runtime.BOMBITE_EXPLOSION_COUNTDOWN);
                  if (bombSlot >= 0) {
                    state.runtime.enemyIgnoreHitsCountdown[bombSlot] = 0x01;
                    state.emit(
                        new EntityCombatEvent(
                            bombSlot,
                            state.runtime.ENTITY_BOMB,
                            0,
                            false,
                            EntityCombatEvent.SoundChannel.NOISE,
                            state.runtime.BOMBITE_EXPLOSION_SOUND_ID));
                  }
                  state.lifecycle().disableEntityWithoutPersistence(state.entity.slot());
                  return false;
                }
              }
              return true;
            });
      }
    }
  }
}
