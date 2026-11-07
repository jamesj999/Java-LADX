package linksawakening.world;

import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.LinkFallenPoseRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.ScreenShakeRequest;
import linksawakening.world.entity.event.TransientVfxRequest;

/** ROM handler stages for ArmosKnight. */
final class ArmosKnightHandler {
  static void install(EntityHandlerRegistry registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_ARMOS_KNIGHT,
        EntityTickStage.PHASE_4_ARMOS_KNIGHT,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            BossIntroMotion.Update bossIntro =
                state.runtime.bossIntroMotion.advance(
                    state.runtime.options1(state.entity.slot()),
                    state.entity.type(),
                    state.runtime.entityMapId,
                    state.runtime.transitionSequenceCounter);
            if (bossIntro.musicTrack() >= 0) {
              state.runtime.pendingMusicTrack = bossIntro.musicTrack();
            }
            if (bossIntro.dialogLowId() >= 0) {
              state.emit(
                  new DialogRequest(bossIntro.dialogTableId(), bossIntro.dialogLowId()));
            }
            state.runtime.backgroundMechanics.requestArmosLinkPush(
                state.entity,
                state.linkEntityX,
                state.linkEntityY,
                state.projectileLinkState.motionState());
            ArmosKnightMotion.Update armosKnightUpdate =
                state.runtime.armosKnightMotion.advance(
                    state.entity,
                    state.linkEntityX,
                    state.linkEntityY,
                    state.linkZ,
                    state.runtime.enemyTransitionCountdown[state.entity.slot()],
                    state.runtime.enemyPhysicsFlags[state.entity.slot()],
                    state.runtime.enemyHitboxFlags[state.entity.slot()],
                    state.runtime.options1(state.entity.slot()),
                    state.runtime.enemyHealth[state.entity.slot()]);
            state.updated = armosKnightUpdate.entity();
            state.runtime.enemyTransitionCountdown[state.entity.slot()] =
                armosKnightUpdate.transitionCountdown();
            state.runtime.enemyPhysicsFlags[state.entity.slot()] = armosKnightUpdate.physicsFlags();
            state.runtime.enemyHitboxFlags[state.entity.slot()] = armosKnightUpdate.hitboxFlags();
            state.runtime.entityOptions1Override[state.entity.slot()] =
                armosKnightUpdate.options1();
            state.preserveArmosKnightPresentation = true;
            if (armosKnightUpdate.linkMotionBlocked()) {
              state.emit(
                  new LinkMotionBlockRequest(state.entity.slot()));
              state.emit(
                  new LinkFallenPoseRequest(state.entity.slot()));
            }
            if (armosKnightUpdate.screenShakeRequest() != null) {
              ArmosKnightMotion.ScreenShakeRequest shake = armosKnightUpdate.screenShakeRequest();
              state.emit(
                  new ScreenShakeRequest(state.entity.slot(), shake.countdown(), shake.phase()));
            }
            if (armosKnightUpdate.rubbleRequest() != null) {
              ArmosKnightMotion.RubbleRequest rubble = armosKnightUpdate.rubbleRequest();
              int rubbleSlot = state.runtime.spawnLiftableRockRubble(rubble.x(), rubble.y());
              if (rubbleSlot >= 0) {
                state.emit(
                    new TransientVfxRequest(TransientVfxType.POOF, rubble.x(), rubble.y()));
                state.emit(
                    new EntityCombatEvent(
                        rubbleSlot,
                        state.runtime.ENTITY_LIFTABLE_ROCK,
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.NOISE,
                        0x29));
              }
            }
            if (armosKnightUpdate.jingleId() >= 0) {
              state.emit(
                  new EntityCombatEvent(
                      state.entity.slot(),
                      state.entity.type(),
                      0,
                      false,
                      EntityCombatEvent.SoundChannel.JINGLE,
                      armosKnightUpdate.jingleId()));
            }
          }
          return true;
        });
  }
}
