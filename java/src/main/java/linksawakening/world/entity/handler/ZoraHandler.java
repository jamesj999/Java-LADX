package linksawakening.world.entity.handler;

import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.RoomEntityCombatRules;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.EntityLinkCollisionRules;

import linksawakening.world.entity.motion.ZoraMotion;

import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.event.TransientVfxRequest;

/** ROM handler stages for Zora. */
final class ZoraHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_ZORA,
        EntityTickStage.PHASE_4_ZORA,
        state -> {
          if (state.status == EntityStatus.ACTIVE
              && !state.wasInitializing

              && !state.creditsGameplay
              && state.handlerLinkCollisionEnabled) {
            int slot = state.entity.slot();
            if (RoomEntityCombatRules.overlapsLink(
                state.entity, state.linkEntityX, state.linkEntityY)) {
              // Zora writes ENTITY_PHYSICS_HARMLESS immediately before
              // this collision check, then copies hLinkFinalPosition
              // and calls ResetPegasusBoots. It does not use the common
              // hookshot-chain reset helper.
              state.background().requestLinkPush(
                  state.entity, EntityLinkCollisionRules.ZORA);
            }
            ZoraMotion.Update zoraUpdate =
                state.zoraMotion().advance(
                    state.updated,
                    state.slot().transitionCountdown(slot),
                    state.slot().physicsFlags(slot),
                    state.linkEntityX,
                    state.linkEntityY,
                    state.randomByteSupplier,
                    state.background().objectQuery());
            state.updated = zoraUpdate.entity();
            state.slot().setTransitionCountdown(slot, zoraUpdate.transitionCountdown());
            state.slot().setPhysicsFlags(slot, zoraUpdate.physicsFlags());
            if (zoraUpdate.projectileSpawn() != null) {
              state.enemySpawns().spawnZoraProjectile(zoraUpdate.projectileSpawn());
            }
            if (zoraUpdate.splash()) {
              state.emit(
                  new TransientVfxRequest(
                      TransientVfxType.WATER_SPLASH,
                      state.updated.x(),
                      (state.updated.y() - state.updated.z()) & 0xFF));
              state.emit(
                  new EntityCombatEvent(
                      slot,
                      state.entity.type(),
                      0,
                      false,
                      EntityCombatEvent.SoundChannel.JINGLE,
                      0x0E));
            }
            state.preserveZoraPresentation = true;
          }
          return true;
        });
  }
}
