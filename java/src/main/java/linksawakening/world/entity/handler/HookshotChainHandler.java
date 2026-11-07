package linksawakening.world.entity.handler;

import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.EntityBackgroundCollisionResult;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.RoomEntityObjectSample;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.EntityProjectileEvent;

import linksawakening.world.entity.motion.HookshotChainMotion;

import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.event.HookshotBridgeUpdate;
import linksawakening.world.entity.event.TransientVfxRequest;
import static linksawakening.world.entity.EntitySnapshots.withPositionAndVariant;

/** ROM handler stages for HookshotChain. */
final class HookshotChainHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_HOOKSHOT_CHAIN,
        EntityTickStage.PHASE_1_HOOKSHOT_CHAIN,
        state -> {
          if (state.status == EntityStatus.ACTIVE) {
            HookshotChainMotion.State hookshotState =
                state.runtime.hookshotChainMotion.state(state.entity.slot());
            if (hookshotState == null) {
              state.runtime.clearEntity(state.entity.slot());
              return false;
            }
            // $7BE3 cancels the chain before rendering or the pause gate.
            if (state.link().linkPlayingOcarinaCountdown() != 0) {
              state.runtime.clearEntity(state.entity.slot());
              return false;
            }
            if (state.projectileTimersInteractive) {
              hookshotState = HookshotChainMotion.decrementTransitionCountdown(hookshotState);
              state.runtime.hookshotChainMotion.setState(state.entity.slot(), hookshotState);
            }
            // Bank $18 has no credits exception in ReturnIfNonInteractive.
            if (!state.handlerInteractive || state.creditsGameplay) {
              return false;
            }

            if ((state.frame & 0x03) == 0) {
              state.emit(
                  new EntityCombatEvent(
                      state.entity.slot(),
                      state.entity.type(),
                      0,
                      false,
                      EntityCombatEvent.SoundChannel.NOISE,
                      0x0B));
            }

            if (hookshotState.entityState() == HookshotChainMotion.PULLING_STATE) {
              if (state.handlerLinkCollisionEnabled
                  && HookshotChainMotion.overlapsLink(
                      hookshotState, state.linkEntityX, state.linkEntityY)) {
                state.runtime.clearEntity(state.entity.slot());
                return false;
              }
              HookshotChainMotion.PullSpeed pullSpeed =
                  HookshotChainMotion.pullLinkSpeed(
                      hookshotState, state.linkEntityX, state.linkEntityY);
              state.projectileEvents.add(
                  EntityProjectileEvent.hookshotPull(
                      state.entity.slot(), pullSpeed.speedX(), pullSpeed.speedY()));
              state.replaceEntity(state.entity);
              return false;
            }

            HookshotChainMotion.Position nextPosition =
                HookshotChainMotion.nextPosition(hookshotState);
            HookshotChainMotion.Step step =
                HookshotChainMotion.advance(
                    hookshotState, state.linkEntityX, state.linkEntityY, false, false);
            if (step.returning()) {
              state.runtime.hookshotChainMotion.setState(state.entity.slot(), step.state());
              if (state.handlerLinkCollisionEnabled && step.reachedLink()) {
                state.runtime.clearEntity(state.entity.slot());
              } else {
                state.replaceEntity(withPositionAndVariant(
                        state.entity,
                        step.state().x(),
                        step.state().y(),
                        state.entity.spriteVariant()));
              }
              return false;
            }
            // $7C59 checks enemies before background interaction. The
            // collision pass may clear the timer, but this pass still
            // follows the already-selected outgoing handler branch.
            boolean unmasked =
                state.runtime.projectiles.collideHookshotWithEntities(
                    state.entity, step.state(), state.frame);
            if (unmasked) {
              step =
                  new HookshotChainMotion.Step(
                      HookshotChainMotion.completeWallPoke(step.state()),
                      step.collided(),
                      step.reachedLink(),
                      step.returning());
            }
            if (hookshotState.wallCollisionPending()) {
              state.runtime.hookshotChainMotion.setState(
                  state.entity.slot(), HookshotChainMotion.completeWallPoke(step.state()));
              state.emit(
                  new EntityCombatEvent(
                      state.entity.slot(),
                      state.entity.type(),
                      0,
                      false,
                      EntityCombatEvent.SoundChannel.JINGLE,
                      0x07));
              state.emit(
                  new TransientVfxRequest(
                      TransientVfxType.SWORD_POKE,
                      state.entity.x(),
                      (state.entity.y() - state.entity.z()) & 0xFF));
              state.replaceEntity(withPositionAndVariant(
                      state.entity,
                      step.state().x(),
                      step.state().y(),
                      state.entity.spriteVariant()));
              return false;
            }
            EntityBackgroundCollisionResult collisionResult = null;
            boolean blocked;
            if (state.runtime.backgroundInteraction != null) {
              collisionResult =
                  state.runtime.backgroundInteraction.probe(
                      state.entity,
                      hookshotState.direction(),
                      nextPosition.x(),
                      nextPosition.y(),
                      state.slot().ignoreHits(state.entity.slot()),
                      state.frame);
              blocked = collisionResult.blocked();
            } else {
              blocked =
                  state.backgroundCollision != null
                      && state.backgroundCollision.blocks(
                          state.entity,
                          hookshotState.direction(),
                          nextPosition.x(),
                          nextPosition.y());
            }
            if (blocked) {
              HookshotChainMotion.State stopped =
                  HookshotChainMotion.rollbackTo(
                      step.state(), hookshotState.x(), hookshotState.y());
              if (collisionResult != null && collisionResult.physicsFlag() == 0x60) {
                if (HookshotChainMotion.shouldUnloadForHookshotable(
                    step.state().transitionCountdown())) {
                  state.runtime.clearEntity(state.entity.slot());
                  return false;
                }
                state.runtime.hookshotChainMotion.setState(
                    state.entity.slot(), HookshotChainMotion.enterPulling(stopped));
              } else {
                state.runtime.hookshotChainMotion.setState(
                    state.entity.slot(), HookshotChainMotion.deferWallPoke(stopped));
              }
              state.replaceEntity(state.entity);
              return false;
            }

            if (state.runtime.indoorRoom
                && hookshotState.speedY() != 0
                && state.runtime.objectQuery != null) {
              RoomEntity movedChain =
                  withPositionAndVariant(
                      state.entity,
                      step.state().x(),
                      step.state().y(),
                      state.entity.spriteVariant());
              RoomEntityObjectSample object = state.runtime.objectQuery.sample(movedChain);
              if (object != null) {
                int bridgeObject =
                    (hookshotState.speedY() & 0x80) != 0
                        ? state.runtime.OBJECT_HOOKSHOT_BRIDGE_PULL_DOWN
                        : state.runtime.OBJECT_HOOKSHOT_BRIDGE_PULL_UP;
                if (object.objectId() == bridgeObject
                    && state.playerSpawns().spawnHookshotBridge(
                        object.objectLeft() + 0x08,
                        object.objectTop() + 0x10,
                        bridgeObject == state.runtime.OBJECT_HOOKSHOT_BRIDGE_PULL_DOWN ? 0 : 1,
                        step.state().transitionCountdown())) {
                  state.runtime.events.hookshotBridgeUpdates.add(
                      new HookshotBridgeUpdate(
                          object.objectLeft(),
                          object.objectTop(),
                          bridgeObject == state.runtime.OBJECT_HOOKSHOT_BRIDGE_PULL_DOWN ? 0 : 1));
                }
              }
            }

            state.runtime.hookshotChainMotion.setState(state.entity.slot(), step.state());
            state.replaceEntity(withPositionAndVariant(
                    state.entity, step.state().x(), step.state().y(), state.entity.spriteVariant()));
            return false;
          }
          return true;
        });
  }
}
