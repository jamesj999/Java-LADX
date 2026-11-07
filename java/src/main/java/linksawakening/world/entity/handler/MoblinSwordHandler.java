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
                state.moblinSwordMotion().initialize(state.entity.slot(), state.entity.x());
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
              && !state.link().dialogActive()
              && !state.link().inventoryAppearing()
              && !state.roomInspection().roomTransitionActive()
              && state.roomInspection().gameplayWorld()
              && state.lifecycle().transitionSequenceCounter() == 0x04
              && !state.roomInspection().witchGotItemPresentationActive()
             ) {
            RoomEntityBackgroundInteraction swordBackgroundInteraction =
                state.background().interaction();
            if (swordBackgroundInteraction == null && state.backgroundCollision != null) {
              swordBackgroundInteraction =
                  RoomEntityBackgroundInteraction.fromBoolean(state.backgroundCollision);
            }
            MoblinSwordMotion.Update moblinSwordUpdate =
                state.moblinSwordMotion().advance(
                    state.entity,
                    state.linkEntityX,
                    state.linkEntityY,
                    swordBackgroundInteraction,
                    state.slot().ignoreHits(state.entity.slot()),
                    state.runtime.swordMoblinAlertingSoundCounter,
                    state.roomInspection().entityMapId(),
                    state.lifecycle().transitionSequenceCounter(),
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
