package linksawakening.world.entity.handler;

import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.RoomEntityCombatRules;
import linksawakening.world.entity.EntityLinkCollisionRules;

import linksawakening.world.entity.motion.BossIntroMotion;
import linksawakening.world.entity.motion.GenieMotion;

import linksawakening.world.entity.event.DialogRequest;

/** ROM handler stages for Genie. */
final class GenieHandler {
  static void install(EntityHandlerRegistration registry) {
    for (int type = 0; type < 256; type++) {
      if ((type == RoomEntityRuntime.ENTITY_GENIE)) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_1_GENIE,
            state -> {
              if (state.runtime.geniePrivateCountdown1[state.entity.slot()] > 0) {
                state.runtime.geniePrivateCountdown1[state.entity.slot()]--;
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if ((type == RoomEntityRuntime.ENTITY_GENIE)) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_1_GENIE_2,
            state -> {
              if (state.runtime.geniePrivateCountdown2[state.entity.slot()] > 0) {
                state.runtime.geniePrivateCountdown2[state.entity.slot()]--;
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if ((type == RoomEntityRuntime.ENTITY_GENIE)) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_GENIE,
            state -> {
              if (state.runtime.geniePrivateState1[state.entity.slot()] == 1) {
                state.runtime.genie.advanceGenieDestruction(
                    state.entity, state.frame, state.randomByteSupplier);
                return false;
              }
              return true;
            });
      }
    }
    registry.registerTick(
        RoomEntityRuntime.ENTITY_GENIE,
        EntityTickStage.PHASE_4_GENIE,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            BossIntroMotion.Update bossIntro =
                !state.link().inventoryAppearing() && !state.runtime.roomTransitionActive
                    ? state.runtime.bossIntroMotion.advance(
                        state.runtime.options1(state.entity.slot()),
                        state.entity.type(),
                        state.runtime.entityMapId,
                        state.runtime.transitionSequenceCounter)
                    : BossIntroMotion.Update.none();
            if (bossIntro.musicTrack() >= 0) {
              state.runtime.pendingMusicTrack = bossIntro.musicTrack();
            }
            if (bossIntro.dialogLowId() >= 0) {
              state.emit(
                  new DialogRequest(bossIntro.dialogTableId(), bossIntro.dialogLowId()));
            }
            boolean genieThresholdReached =
                state.runtime.geniePrivateState4[state.entity.slot()]
                    >= GenieMotion.JAR_HEALTH_THRESHOLD;
            if (state.runtime.geniePrivateState1[state.entity.slot()] == 0
                && !genieThresholdReached) {
              state.runtime.genieControllerSlot = state.entity.slot();
            }
            if (state.runtime.geniePrivateState1[state.entity.slot()] == 0
                && (genieThresholdReached
                    || state.runtime.roomInspection.genieState0Interactive(
                        state.creditsGameplay))) {
              state.runtime.genie.advanceGenieJar(state.entity, state.linkEntityY);
              if (!state.runtime.slots[state.entity.slot()].loaded()) {
                return false;
              }
              state.updated = state.runtime.slots[state.entity.slot()];
            }
            if (state.runtime.geniePrivateState1[state.entity.slot()] == 2
                && state.runtime.roomInspection.genieState0Interactive(state.creditsGameplay)) {
              state.runtime.genie.advanceGenieAppearance(state.entity);
              if (!state.runtime.slots[state.entity.slot()].loaded()) {
                return false;
              }
              state.updated = state.runtime.slots[state.entity.slot()];
            }
            if (state.runtime.geniePrivateState1[state.entity.slot()] == 3
                && state.runtime.roomInspection.genieState0Interactive(state.creditsGameplay)) {
              state.runtime.genie.advanceGenieSideProjectile(state.entity, state.frameCounter);
              if (!state.runtime.slots[state.entity.slot()].loaded()) {
                return false;
              }
              state.updated = state.runtime.slots[state.entity.slot()];
            }
            if (state.runtime.geniePrivateState1[state.entity.slot()] == 4
                && state.runtime.roomInspection.genieState0Interactive(state.creditsGameplay)) {
              if (state.handlerLinkCollisionEnabled
                  && RoomEntityCombatRules.overlapsLink(
                      state.entity, state.linkEntityX, state.linkEntityY)) {
                state.runtime.backgroundMechanics.requestLinkPush(
                    state.entity, EntityLinkCollisionRules.STANDARD_PUSH);
              }
              state.runtime.genie.advanceGenieFireball(state.entity, state.frameCounter);
              if (!state.runtime.slots[state.entity.slot()].loaded()) {
                return false;
              }
              state.updated = state.runtime.slots[state.entity.slot()];
            }
            if (state.runtime.geniePrivateState1[state.entity.slot()] == 1
                && state.runtime.roomInspection.genieState0Interactive(state.creditsGameplay)) {
              state.runtime.genieSharedBodySpeedX =
                  state.runtime.genieSpeedX[state.entity.slot()] & 0xFF;
              state.runtime.genieSharedBodySpeedY =
                  state.runtime.genieSpeedY[state.entity.slot()] & 0xFF;
              if (state.runtime.genieActiveState[state.entity.slot()] != 5
                  && state.handlerLinkCollisionEnabled
                  && RoomEntityCombatRules.overlapsLink(
                      state.entity, state.linkEntityX, state.linkEntityY)) {
                state.runtime.backgroundMechanics.requestLinkPush(
                    state.entity, EntityLinkCollisionRules.STANDARD_PUSH);
              }
              state.runtime.genie.advanceGenieBody(
                  state.entity,
                  state.frameCounter,
                  state.linkEntityX,
                  state.linkEntityY,
                  state.randomByteSupplier);
              if (!state.runtime.slots[state.entity.slot()].loaded()) {
                return false;
              }
              state.updated = state.runtime.slots[state.entity.slot()];
            }
          }
          return true;
        });
  }
}
