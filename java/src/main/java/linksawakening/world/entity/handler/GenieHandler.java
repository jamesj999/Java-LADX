package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_GENIE;


import linksawakening.world.entity.EntityTickStage;
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
      if ((type == ENTITY_GENIE)) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_1_GENIE,
            state -> {
              if (state.genie().geniePrivateCountdown1(state.entity.slot()) > 0) {
                state.genie().decrementGeniePrivateCountdown1(state.entity.slot());
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if ((type == ENTITY_GENIE)) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_1_GENIE_2,
            state -> {
              if (state.genie().geniePrivateCountdown2(state.entity.slot()) > 0) {
                state.genie().decrementGeniePrivateCountdown2(state.entity.slot());
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if ((type == ENTITY_GENIE)) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_GENIE,
            state -> {
              if (state.genie().geniePrivateState1(state.entity.slot()) == 1) {
                state.genie().advanceGenieDestruction(
                    state.entity, state.frame, state.randomByteSupplier);
                return false;
              }
              return true;
            });
      }
    }
    registry.registerTick(
        ENTITY_GENIE,
        EntityTickStage.PHASE_4_GENIE,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            BossIntroMotion.Update bossIntro =
                !state.link().inventoryAppearing() && !state.roomInspection().roomTransitionActive()
                    ? state.bossIntroMotion().advance(
                        state.slotProperties().options1(state.entity.slot()),
                        state.entity.type(),
                        state.roomInspection().entityMapId(),
                        state.lifecycle().transitionSequenceCounter())
                    : BossIntroMotion.Update.none();
            if (bossIntro.musicTrack() >= 0) {
              state.requestMusic(bossIntro.musicTrack());
            }
            if (bossIntro.dialogLowId() >= 0) {
              state.emit(
                  new DialogRequest(bossIntro.dialogTableId(), bossIntro.dialogLowId()));
            }
            boolean genieThresholdReached =
                state.genie().geniePrivateState4(state.entity.slot())
                    >= GenieMotion.JAR_HEALTH_THRESHOLD;
            if (state.genie().geniePrivateState1(state.entity.slot()) == 0
                && !genieThresholdReached) {
              state.genie().setGenieControllerSlot(state.entity.slot());
            }
            if (state.genie().geniePrivateState1(state.entity.slot()) == 0
                && (genieThresholdReached
                    || state.roomInspection().genieState0Interactive(
                        state.creditsGameplay))) {
              state.genie().advanceGenieJar(state.entity, state.linkEntityY);
              if (!state.entityAt(state.entity.slot()).loaded()) {
                return false;
              }
              state.updated = state.entityAt(state.entity.slot());
            }
            if (state.genie().geniePrivateState1(state.entity.slot()) == 2
                && state.roomInspection().genieState0Interactive(state.creditsGameplay)) {
              state.genie().advanceGenieAppearance(state.entity);
              if (!state.entityAt(state.entity.slot()).loaded()) {
                return false;
              }
              state.updated = state.entityAt(state.entity.slot());
            }
            if (state.genie().geniePrivateState1(state.entity.slot()) == 3
                && state.roomInspection().genieState0Interactive(state.creditsGameplay)) {
              state.genie().advanceGenieSideProjectile(state.entity, state.frameCounter);
              if (!state.entityAt(state.entity.slot()).loaded()) {
                return false;
              }
              state.updated = state.entityAt(state.entity.slot());
            }
            if (state.genie().geniePrivateState1(state.entity.slot()) == 4
                && state.roomInspection().genieState0Interactive(state.creditsGameplay)) {
              if (state.handlerLinkCollisionEnabled
                  && RoomEntityCombatRules.overlapsLink(
                      state.entity, state.linkEntityX, state.linkEntityY)) {
                state.background().requestLinkPush(
                    state.entity, EntityLinkCollisionRules.STANDARD_PUSH);
              }
              state.genie().advanceGenieFireball(state.entity, state.frameCounter);
              if (!state.entityAt(state.entity.slot()).loaded()) {
                return false;
              }
              state.updated = state.entityAt(state.entity.slot());
            }
            if (state.genie().geniePrivateState1(state.entity.slot()) == 1
                && state.roomInspection().genieState0Interactive(state.creditsGameplay)) {
              state.genie().setGenieSharedBodySpeedX(state.genie().genieSpeedX(state.entity.slot()) & 0xFF);
              state.genie().setGenieSharedBodySpeedY(state.genie().genieSpeedY(state.entity.slot()) & 0xFF);
              if (state.genie().genieActiveState(state.entity.slot()) != 5
                  && state.handlerLinkCollisionEnabled
                  && RoomEntityCombatRules.overlapsLink(
                      state.entity, state.linkEntityX, state.linkEntityY)) {
                state.background().requestLinkPush(
                    state.entity, EntityLinkCollisionRules.STANDARD_PUSH);
              }
              state.genie().advanceGenieBody(
                  state.entity,
                  state.frameCounter,
                  state.linkEntityX,
                  state.linkEntityY,
                  state.randomByteSupplier);
              if (!state.entityAt(state.entity.slot()).loaded()) {
                return false;
              }
              state.updated = state.entityAt(state.entity.slot());
            }
          }
          return true;
        });
  }
}
