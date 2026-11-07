package linksawakening.world.entity.handler;

import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.event.EntityCombatEvent;

import linksawakening.world.entity.motion.BossIntroMotion;
import linksawakening.world.entity.motion.MoldormMotion;

import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import static linksawakening.world.entity.EntitySnapshots.withDefinition;

/** ROM handler stages for Moldorm. */
final class MoldormHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_MOLDORM,
        EntityTickStage.PHASE_1_MOLDORM,
        state -> {
          if (!state.wasInitializing

              && !state.runtime.roomTransitionActive
              && !state.link().inventoryAppearing()) {
            BossIntroMotion.Update intro =
                state.runtime.bossIntroMotion.advance(
                    state.runtime.options1(state.entity.slot()),
                    state.entity.type(),
                    state.runtime.entityMapId,
                    state.lifecycle().transitionSequenceCounter());
            if (intro.musicTrack() >= 0) state.runtime.pendingMusicTrack = intro.musicTrack();
            if (intro.dialogLowId() >= 0) {
              state.emit(
                  new DialogRequest(intro.dialogTableId(), intro.dialogLowId()));
              state.moldormIntroOpenedThisFrame = true;
            }
          }
          return true;
        });
    for (int type = 0; type < 256; type++) {
      if (((type == RoomEntityRuntime.ENTITY_ROLLING_BONES
          || type == RoomEntityRuntime.ENTITY_MOLDORM))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_1_MOLDORM_2,
            state -> {
              if (!state.wasInitializing
                  && !state.runtime.roomTransitionActive
                  && !state.runtime.freeMovementMode
                 ) {
                int dx = 0;
                int dy = 0;
                if (((state.linkEntityX - 0x11) & 0xFF) >= 0x7E) {
                  dx = state.linkEntityX < 0x50 ? 1 : -1;
                } else if (((state.linkEntityY - 0x16) & 0xFF) >= 0x5E) {
                  dy = -1;
                }
                if (dx != 0 || dy != 0) {
                  state.emit(
                      new LinkMotionBlockRequest(state.entity.slot(), dx, dy));
                  state.linkEntityX = (state.linkEntityX + dx) & 0xFF;
                  state.linkEntityY = (state.linkEntityY + dy) & 0xFF;
                  state.runtime.entityInteractionCountdown = 2;
                }
              }
              return true;
            });
      }
    }
    registry.registerTick(
        RoomEntityRuntime.ENTITY_MOLDORM,
        EntityTickStage.PHASE_2_MOLDORM,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            int slot = state.entity.slot();
            if (!state.handlerInteractive
                || state.countdownBlocksInteraction
                || state.moldormIntroOpenedThisFrame) {
              if (state.sprites() != null) {
                state.replaceEntity(withDefinition(
                        state.entity,
                        state.sprites().forMoldormState(
                            state.runtime.moldormMotion.headSpriteVariant(slot),
                            state.entity.x(),
                            (state.entity.y() - state.entity.z()) & 0xFF,
                            state.runtime.moldormMotion.currentTailSegments(slot),
                            0,
                            state.frame),
                        0));
              }
              if (state.runtime.roomTransitionActive)
                state.runtime.moldormMotion.resetHistory(slot);
              return false;
            }
            RoomEntityBackgroundInteraction moldormBackgroundInteraction =
                state.background().interaction();
            if (moldormBackgroundInteraction == null && state.backgroundCollision != null) {
              moldormBackgroundInteraction =
                  RoomEntityBackgroundInteraction.fromBoolean(state.backgroundCollision);
            }
            int transitionForMotion = state.slot().transitionCountdown(slot);
            if (transitionForMotion != 0) {
              // Common countdowns have already ticked before dispatch;
              // the direct motion API performs the same decrement.
              transitionForMotion = (transitionForMotion + 1) & 0xFF;
            }
            MoldormMotion.Update moldormUpdate =
                state.runtime.moldormMotion.advance(
                    state.entity,
                    state.frame,
                    transitionForMotion,
                    state.slot().ignoreHits(slot),
                    state.slot().flashCountdown(slot),
                    state.slot().health(slot),
                    state.randomByteSupplier,
                    moldormBackgroundInteraction);
            state.updated = moldormUpdate.entity();
            state.slot().setTransitionCountdown(slot, moldormUpdate.transitionCountdown());
            if (moldormUpdate.noiseRequested()) {
              state.emit(
                  new EntityCombatEvent(
                      slot,
                      state.entity.type(),
                      0,
                      false,
                      EntityCombatEvent.SoundChannel.NOISE,
                      0x1B));
            }
            if (state.sprites() != null) {
              MoldormMotion.Presentation presentation = moldormUpdate.presentation();
              state.updated =
                  withDefinition(
                      state.updated,
                      state.sprites().forMoldormPresentation(
                          presentation.headSpriteVariant(),
                          presentation.headX(),
                          presentation.headY(),
                          state.updated.x(),
                          (state.updated.y() - state.updated.z()) & 0xFF,
                          presentation.segments(),
                          0,
                          state.frame),
                      0);
            }
            state.slot().setPhysicsFlags(slot, 0x02);
            state.preserveMoldormPresentation = true;
          }
          return true;
        });
    for (int type = 0; type < 256; type++) {
      if (((type == RoomEntityRuntime.ENTITY_MOLDORM))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_MOLDORM_2,
            state -> {
              {
                state.runtime.bossRewards.advanceMoldormDestruction(state.entity, state.frame);
                return false;
              }
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if (((type == RoomEntityRuntime.ENTITY_MOLDORM))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_MOLDORM_3,
            state -> {
              {
                state.runtime.moldormMotion.initialize(state.entity);
              }
              return true;
            });
      }
    }
  }
}
