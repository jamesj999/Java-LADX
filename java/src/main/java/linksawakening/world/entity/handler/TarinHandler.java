package linksawakening.world.entity.handler;

import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.RoomEntityCombatRules;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.EntityLinkCollisionRules;







import linksawakening.world.entity.motion.TarinRaccoonMotion;

import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.LinkAttackClearRequest;
import linksawakening.world.entity.event.LinkFacingRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.RoomStatusPersistenceRequest;

/** ROM handler stages for Tarin. */
final class TarinHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_TARIN,
        EntityTickStage.PHASE_1_TARIN,
        state -> {
          if (!state.runtime.indoorRoom && (state.runtime.entityRoomStatus & 0x10) != 0) {
            state.lifecycle().disableEntityWithoutPersistence(state.entity.slot());
            return false;
          }
          return true;
        });
    for (int type = 0; type < 256; type++) {
      if ((type == RoomEntityRuntime.ENTITY_TARIN)) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_4_TARIN,
            state -> {
              if (state.status == EntityStatus.ACTIVE
                  && !state.wasInitializing
                  && !state.runtime.indoorRoom

                  && (state.runtime.tarinRaccoonMotion.state(state.entity.slot()) == 0
                      || state.runtime.roomInspection.tarinTransformationHandlerInteractive())) {
                RoomEntityBackgroundInteraction tarinBackgroundInteraction =
                    state.runtime.backgroundInteraction;
                if (tarinBackgroundInteraction == null && state.backgroundCollision != null) {
                  tarinBackgroundInteraction =
                      RoomEntityBackgroundInteraction.fromBoolean(state.backgroundCollision);
                }
                TarinRaccoonMotion.Update tarinUpdate =
                    state.runtime.tarinRaccoonMotion.advance(
                        state.updated,
                        new TarinRaccoonMotion.Input(
                            state.frame,
                            state.linkEntityX,
                            state.linkEntityY,
                            state.romLinkDirection,
                            state.runtime.actionButtonAHeld,
                            state.runtime.dialogActive,
                            false,
                            state.runtime.linkAttackStepAnimationCountdown,
                            state.linkAirborne,
                            state.runtime.inventoryAppearing,
                            state.runtime.dialogCooldown,
                            state.runtime.windowY,
                            state.runtime.slowTransitionCountdown[state.entity.slot()],
                            state.runtime.enemyTransitionCountdown[state.entity.slot()]),
                        tarinBackgroundInteraction);
                state.updated = tarinUpdate.entity();
                state.runtime.slowTransitionCountdown[state.entity.slot()] =
                    tarinUpdate.slowTransitionCountdown();
                state.runtime.enemyTransitionCountdown[state.entity.slot()] =
                    tarinUpdate.transitionCountdown();
                state.preserveTarinPresentation = true;
                state.runtime.shouldGetLostInMysteriousWoods = tarinUpdate.shouldGetLost();
                if (tarinUpdate.linkMotionBlocked()) {
                  state.emit(
                      new LinkMotionBlockRequest(state.entity.slot()));
                }
                if (tarinUpdate.dialogGlobalId() >= 0) {
                  state.emit(
                      new DialogRequest(
                          tarinUpdate.dialogGlobalId() >>> 8, tarinUpdate.dialogGlobalId() & 0xFF));
                }
                if (tarinUpdate.clearLinkAttack()) {
                  state.emit(
                      new LinkAttackClearRequest(state.entity.slot()));
                }
                if (tarinUpdate.linkFacingDirection() >= 0) {
                  state.emit(
                      new LinkFacingRequest(
                          state.entity.slot(), tarinUpdate.linkFacingDirection(), true));
                }
                if (tarinUpdate.pushLink()) {
                  if (RoomEntityCombatRules.overlapsLink(
                      state.updated, state.linkEntityX, state.linkEntityY)) {
                    state.runtime.backgroundMechanics.requestLinkPush(
                        state.updated, EntityLinkCollisionRules.STANDARD_PUSH);
                  }
                }
                if (tarinUpdate.soundId() >= 0) {
                  state.emit(
                      new EntityCombatEvent(
                          state.entity.slot(),
                          state.entity.type(),
                          0,
                          false,
                          EntityCombatEvent.SoundChannel.JINGLE,
                          tarinUpdate.soundId()));
                  if (tarinUpdate.soundId() == 0x23) {
                    state.runtime.pendingMusicTrack = state.runtime.defaultMusicTrack;
                  }
                }
                if (tarinUpdate.spawnBomb()) {
                  state.enemySpawns().spawnTarinTransformationBomb(state.updated);
                }
                if (tarinUpdate.roomChanged() || tarinUpdate.tarinFlag()) {
                  state.emit(
                      new RoomStatusPersistenceRequest(state.entity.slot(), 0x10, 1));
                }
              }
              return true;
            });
      }
    }
  }
}
