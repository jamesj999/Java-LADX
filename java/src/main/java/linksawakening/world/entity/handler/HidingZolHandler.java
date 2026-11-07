package linksawakening.world.entity.handler;

import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.event.EntityCombatEvent;

import linksawakening.world.entity.motion.HidingZolMotion;

/** ROM handler stages for HidingZol. */
final class HidingZolHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_HIDING_ZOL,
        EntityTickStage.PHASE_3_HIDING_ZOL,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            RoomEntityBackgroundInteraction hidingZolBackgroundInteraction =
                state.runtime.backgroundInteraction;
            if (hidingZolBackgroundInteraction == null && state.backgroundCollision != null) {
              hidingZolBackgroundInteraction =
                  RoomEntityBackgroundInteraction.fromBoolean(state.backgroundCollision);
            }
            HidingZolMotion.Update hidingZolUpdate =
                state.runtime.hidingZolMotion.advance(
                    state.entity,
                    state.linkEntityX,
                    state.linkEntityY,
                    state.randomByteSupplier,
                    hidingZolBackgroundInteraction,
                    state.frame);
            state.updated = hidingZolUpdate.entity();
            if (hidingZolUpdate.clearsIgnoreHitsCountdown()) {
              // func_007_73F7 temporarily writes $03 around the
              // background helper, then clears the shared byte.
              state.slot().setIgnoreHits(state.entity.slot(), 0);
            }
            if (hidingZolUpdate.jumpJingle()) {
              state.emit(
                  new EntityCombatEvent(
                      state.entity.slot(),
                      state.entity.type(),
                      0,
                      false,
                      EntityCombatEvent.SoundChannel.JINGLE,
                      0x24));
            }
            if (hidingZolUpdate.physicsFlags() >= 0) {
              state.slot().setPhysicsFlags(state.entity.slot(), hidingZolUpdate.physicsFlags());
            }
          }
          return true;
        });
  }
}
