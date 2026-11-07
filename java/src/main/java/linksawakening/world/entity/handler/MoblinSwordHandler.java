package linksawakening.world.entity.handler;

import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;

import linksawakening.world.entity.motion.MoblinSwordMotion;

import linksawakening.world.entity.event.DialogRequest;

/** ROM handler stages for MoblinSword. */
final class MoblinSwordHandler {
  static void install(EntityHandlerRegistration registry) {
    for (int type = 0; type < 256; type++) {
      if (((type == RoomEntityRuntime.ENTITY_MOBLIN_SWORD))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_MOBLIN_SWORD,
            state -> {
              {
                state.runtime.moblinSwordMotion.initialize(state.entity.slot(), state.entity.x());
              }
              return true;
            });
      }
    }
    registry.registerTick(
        RoomEntityRuntime.ENTITY_MOBLIN_SWORD,
        EntityTickStage.PHASE_3_MOBLIN_SWORD,
        state -> {
          if (state.status == EntityStatus.ACTIVE
              && !state.wasInitializing
              && !state.runtime.dialogActive
              && !state.runtime.inventoryAppearing
              && !state.runtime.roomTransitionActive
              && state.runtime.gameplayWorld
              && state.runtime.transitionSequenceCounter == 0x04
              && !state.runtime.inspection().witchGotItemPresentationActive()
             ) {
            RoomEntityBackgroundInteraction swordBackgroundInteraction =
                state.runtime.backgroundInteraction;
            if (swordBackgroundInteraction == null && state.backgroundCollision != null) {
              swordBackgroundInteraction =
                  RoomEntityBackgroundInteraction.fromBoolean(state.backgroundCollision);
            }
            MoblinSwordMotion.Update moblinSwordUpdate =
                state.runtime.moblinSwordMotion.advance(
                    state.entity,
                    state.linkEntityX,
                    state.linkEntityY,
                    swordBackgroundInteraction,
                    state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()],
                    state.runtime.swordMoblinAlertingSoundCounter,
                    state.runtime.entityMapId,
                    state.runtime.transitionSequenceCounter,
                    state.frame);
            state.updated = moblinSwordUpdate.entity();
            if (moblinSwordUpdate.dialogRequested()) {
              state.emit(
                  new DialogRequest(1, state.runtime.DIALOG_MOBLIN));
            }
          }
          return true;
        });
  }
}
