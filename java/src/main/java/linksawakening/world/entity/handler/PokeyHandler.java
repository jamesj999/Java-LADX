package linksawakening.world.entity.handler;

import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.event.EntityCombatEvent;

import linksawakening.world.entity.motion.PokeyMotion;

import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.event.TransientVfxRequest;
import static linksawakening.world.entity.EntitySnapshots.withDefinition;

/** ROM handler stages for Pokey. */
final class PokeyHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_POKEY,
        EntityTickStage.PHASE_4_POKEY,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            int slot = state.entity.slot();
            if (!state.handlerLinkCollisionEnabled) {
              // Both Pokey branches return immediately after their
              // pre-handler display pass when Link is non-interactive.
            } else if (state.runtime.pokeyMotion.isSegment(slot)) {
              PokeyMotion.Update segmentUpdate =
                  state.runtime.pokeyMotion.advanceSegment(
                      state.updated,
                      state.runtime.entityInertia[slot],
                      state.runtime.enemyTransitionCountdown[slot],
                      state.backgroundCollision);
              state.updated = segmentUpdate.entity();
              state.runtime.entityInertia[slot] = segmentUpdate.inertia();
              state.runtime.enemyTransitionCountdown[slot] = segmentUpdate.transitionCountdown();
              if (segmentUpdate.unloadRequested()) {
                state.emit(
                    new TransientVfxRequest(
                        TransientVfxType.POOF,
                        state.updated.x(),
                        (state.updated.y() - state.updated.z()) & 0xFF));
                state.emit(
                    new EntityCombatEvent(
                        slot,
                        state.entity.type(),
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        0x2F));
                state.lifecycle().disableEntityWithoutPersistence(slot);
                return false;
              }
              if (segmentUpdate.bumpJingle()) {
                state.emit(
                    new EntityCombatEvent(
                        slot,
                        state.entity.type(),
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        0x09));
              }
            } else {
              if (state.runtime.entityInertia[slot] < 0x02) {
                // PokeyEntityHandler replaces the normal group-$00
                // health with two hit points until its body is fully
                // separated.
                state.runtime.enemyHealth[slot] = 0x02;
              }
              PokeyMotion.Update pokeyUpdate =
                  state.runtime.pokeyMotion.advanceMain(
                      state.updated,
                      state.frame,
                      state.runtime.entityInertia[slot],
                      state.runtime.enemyTransitionCountdown[slot],
                      state.runtime.enemyFlashCountdown[slot],
                      state.linkEntityX,
                      state.linkEntityY,
                      state.randomByteSupplier,
                      state.backgroundCollision);
              state.updated = pokeyUpdate.entity();
              state.runtime.entityInertia[slot] = pokeyUpdate.inertia();
              state.runtime.enemyTransitionCountdown[slot] = pokeyUpdate.transitionCountdown();
              state.runtime.enemyFlashCountdown[slot] = pokeyUpdate.flashCountdown();
              if (pokeyUpdate.segmentSpawn() != null) {
                state.enemySpawns().spawnPokeySegment(pokeyUpdate.segmentSpawn());
              }
              if (state.runtime.spriteHandlers != null) {
                state.updated =
                    withDefinition(
                        state.updated,
                        state.runtime.spriteHandlers.forPokeyState(pokeyUpdate.renderInertia()),
                        state.updated.spriteVariant());
              }
            }
            state.preservePokeyPresentation = true;
          }
          return true;
        });
  }
}
