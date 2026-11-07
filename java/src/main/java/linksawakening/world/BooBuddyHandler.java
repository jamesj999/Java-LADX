package linksawakening.world;

import static linksawakening.world.EntitySnapshots.withVariant;

/** ROM handler stages for BooBuddy. */
final class BooBuddyHandler {
  static void install(EntityHandlerRegistry registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_BOO_BUDDY,
        EntityTickStage.PHASE_1_BOO_BUDDY,
        state -> {
          if (state.initialStatus == EntityStatus.ACTIVE) {
            int slot = state.entity.slot();
            // Rendering precedes ReturnIfNonInteractive_06 and recoil.
            state.entity =
                withVariant(state.entity, state.runtime.booBuddyMotion.selectedVariant(slot));
            state.replaceEntity(state.entity);
            if (state.projectileTimersInteractive) {
              state.runtime.booBuddyMotion.tickTimers(slot);
            }
            if (!state.handlerInteractive) {
              return false;
            }
            if (state.runtime.enemyIgnoreHitsCountdown[slot] != 0) {
              // $06:64F7 consumes one ignore-hits tick, moves with recoil
              // speeds, and pops the handler return address. OPTIONS1
              // bit $20 skips background collision for Boo Buddy.
              state.runtime.enemyIgnoreHitsCountdown[slot]--;
              state.replaceEntity(state.runtime.booBuddyMotion.advanceRecoil(
                      state.entity,
                      state.runtime.enemyRecoilMotion.recoilSpeedX(slot),
                      state.runtime.enemyRecoilMotion.recoilSpeedY(slot)));
              return false;
            }
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_BOO_BUDDY,
        EntityTickStage.PHASE_3_BOO_BUDDY,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            BooBuddyMotion.Update booBuddyUpdate =
                state.runtime.booBuddyMotion.advanceAfterTimers(
                    state.entity,
                    state.frame,
                    state.linkEntityX,
                    state.linkEntityY,
                    state.runtime.enemyTransitionCountdown[state.entity.slot()],
                    state.runtime.booBuddyTriggerCount,
                    state.swordCollisionActive,
                    state.randomByteSupplier);
            state.updated = booBuddyUpdate.entity();
            state.runtime.enemyTransitionCountdown[state.entity.slot()] =
                booBuddyUpdate.transitionCountdown();
            if (booBuddyUpdate.healthOverride() >= 0) {
              state.runtime.enemyHealth[state.entity.slot()] = booBuddyUpdate.healthOverride();
            }
            if (booBuddyUpdate.unloaded()) {
              state.lifecycle().disableEntityWithoutPersistence(state.entity.slot());
              return false;
            }
          }
          return true;
        });
  }
}
