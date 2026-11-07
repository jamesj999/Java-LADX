package linksawakening.world.entity.handler;

import linksawakening.world.EntityTickStage;
import linksawakening.world.RoomEntityRuntime;
import linksawakening.world.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.EntityCombatEvent;







import linksawakening.world.entity.motion.BossIntroMotion;
import linksawakening.world.entity.motion.HinoxMotion;

import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.HinoxLinkEffectRequest;
import linksawakening.world.entity.event.LinkFallenPoseRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.TransientVfxRequest;

/** ROM handler stages for Hinox. */
final class HinoxHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_HINOX,
        EntityTickStage.PHASE_4_HINOX,
        state -> {
          if (state.status == EntityStatus.ACTIVE
              && !state.wasInitializing
              && state.handlerLinkCollisionEnabled
              && state.runtime.backgroundMechanics.entityTimersInteractive()
              && state.runtime.transitionSequenceCounter == 0x04
              && !state.creditsGameplay
             ) {
            int slot = state.entity.slot();
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
            HinoxMotion.Update hinoxUpdate =
                state.runtime.hinoxMotion.advance(
                    state.entity,
                    state.runtime.enemyTransitionCountdown[slot],
                    state.frame,
                    state.linkEntityX,
                    state.linkEntityY,
                    state.linkZ,
                    state.projectileLinkState.motionState(),
                    state.randomByteSupplier,
                    state.hinoxBackgroundInteraction,
                    state.runtime.hinoxMotion.collisionFlags(slot),
                    state.runtime.enemyFlashCountdown[slot]);
            state.updated = hinoxUpdate.entity();
            state.runtime.enemyTransitionCountdown[slot] = hinoxUpdate.transitionCountdown();
            state.preserveHinoxPresentation = true;
            if (hinoxUpdate.linkMotionBlocked()) {
              state.emit(new LinkMotionBlockRequest(slot));
              state.emit(new LinkFallenPoseRequest(slot));
            }
            if (hinoxUpdate.heldLinkPositionWritten()) {
              state.emit(
                  new HinoxLinkEffectRequest(
                      slot,
                      hinoxUpdate.heldLinkX(),
                      hinoxUpdate.heldLinkY(),
                      hinoxUpdate.heldLinkZ(),
                      hinoxUpdate.linkSpeedX(),
                      hinoxUpdate.linkSpeedY(),
                      hinoxUpdate.linkVelocityZ(),
                      hinoxUpdate.linkAirborneState(),
                      hinoxUpdate.linkDamage(),
                      hinoxUpdate.linkMotionBlocked(),
                      hinoxUpdate.applyHeldLinkPose()));
            }
            if (hinoxUpdate.bombSpawn() != null) {
              HinoxMotion.BombSpawn spawn = hinoxUpdate.bombSpawn();
              state.runtime.spawnEnemyBomb(
                  spawn.x(),
                  spawn.y(),
                  spawn.z(),
                  spawn.transitionCountdown(),
                  spawn.speedX(),
                  spawn.speedY(),
                  spawn.speedZ());
            }
            if (!hinoxUpdate.heldLinkPositionWritten()
                && !hinoxUpdate.linkMotionBlocked()
                && (hinoxUpdate.linkSpeedX() != 0
                    || hinoxUpdate.linkSpeedY() != 0
                    || hinoxUpdate.linkVelocityZ() != 0
                    || hinoxUpdate.linkDamage() != 0)) {
              state.emit(
                  new HinoxLinkEffectRequest(
                      slot,
                      hinoxUpdate.heldLinkX(),
                      hinoxUpdate.heldLinkY(),
                      hinoxUpdate.heldLinkZ(),
                      hinoxUpdate.linkSpeedX(),
                      hinoxUpdate.linkSpeedY(),
                      hinoxUpdate.linkVelocityZ(),
                      hinoxUpdate.linkAirborneState(),
                      hinoxUpdate.linkDamage(),
                      hinoxUpdate.linkMotionBlocked(),
                      hinoxUpdate.applyHeldLinkPose()));
            }
            if (hinoxUpdate.dustRequested()) {
              state.emit(
                  new TransientVfxRequest(
                      TransientVfxType.PEGASUS_DUST,
                      state.updated.x(),
                      (state.updated.y() + 0x0A) & 0xFF));
            }
            if (hinoxUpdate.jingleId() >= 0) {
              state.emit(
                  new EntityCombatEvent(
                      slot,
                      state.entity.type(),
                      0,
                      false,
                      EntityCombatEvent.SoundChannel.JINGLE,
                      hinoxUpdate.jingleId()));
            }
            if (hinoxUpdate.waveId() >= 0) {
              state.emit(
                  new EntityCombatEvent(
                      slot,
                      state.entity.type(),
                      0,
                      false,
                      EntityCombatEvent.SoundChannel.WAVE,
                      hinoxUpdate.waveId()));
            }
          }
          return true;
        });
  }
}
