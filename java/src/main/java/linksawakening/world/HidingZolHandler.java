package linksawakening.world;



/** ROM handler stages for HidingZol. */
final class HidingZolHandler {
  static void installPhase3(EntityHandlerRegistry registry) {
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
              state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()] = 0;
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
              state.runtime.enemyPhysicsFlags[state.entity.slot()] = hidingZolUpdate.physicsFlags();
            }
          }
          return true;
        });
  }
}
