package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_HOOKSHOT_CHAIN;
import static linksawakening.world.entity.EntityRomConstants.OBJECT_HOOKSHOT_BRIDGE_PULL_DOWN;
import static linksawakening.world.entity.EntityRomConstants.OBJECT_HOOKSHOT_BRIDGE_PULL_UP;
import static linksawakening.world.entity.EntitySnapshots.withPositionAndVariant;

import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.EntityBackgroundCollisionResult;
import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityObjectSample;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.EntityProjectileEvent;
import linksawakening.world.entity.event.HookshotBridgeUpdate;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.HookshotChainMotion;

/** ROM handler stages for HookshotChain. */
final class HookshotChainHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_HOOKSHOT_CHAIN,
                EntityTickStage.PHASE_1_BEFORE_PICKUP_SEQUENCE,
                4,
                HookshotChainHandler::phase1HookshotChain);
    }

    private static boolean phase1HookshotChain(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();

        if (slotState.status() == EntityStatus.ACTIVE) {
            HookshotChainMotion.State hookshotState = state.motion(HookshotChainMotion.class, HookshotChainMotion::new)
                    .state(slotState.entity().slot());
            if (hookshotState == null) {
                state.lifecycle().clearEntity(slotState.entity().slot());
                return false;
            }
            // $7BE3 cancels the chain before rendering or the pause gate.
            if (state.link().linkPlayingOcarinaCountdown() != 0) {
                state.lifecycle().clearEntity(slotState.entity().slot());
                return false;
            }
            if (timerGates.projectileTimersInteractive()) {
                hookshotState = HookshotChainMotion.decrementTransitionCountdown(hookshotState);
                state.motion(HookshotChainMotion.class, HookshotChainMotion::new)
                        .setState(slotState.entity().slot(), hookshotState);
            }
            // Bank $18 has no credits exception in ReturnIfNonInteractive.
            if (!timerGates.handlerInteractive() || frameInput.creditsGameplay()) {
                return false;
            }

            if ((frameInput.frame() & 0x03) == 0) {
                state.emit(new EntityCombatEvent(
                        slotState.entity().slot(),
                        slotState.entity().type(),
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.NOISE,
                        0x0B));
            }

            if (hookshotState.entityState() == HookshotChainMotion.PULLING_STATE) {
                if (timerGates.handlerLinkCollisionEnabled()
                        && HookshotChainMotion.overlapsLink(
                                hookshotState, frameInput.linkEntityX(), frameInput.linkEntityY())) {
                    state.lifecycle().clearEntity(slotState.entity().slot());
                    return false;
                }
                HookshotChainMotion.PullSpeed pullSpeed = HookshotChainMotion.pullLinkSpeed(
                        hookshotState, frameInput.linkEntityX(), frameInput.linkEntityY());
                frameInput
                        .projectileEvents()
                        .add(EntityProjectileEvent.hookshotPull(
                                slotState.entity().slot(), pullSpeed.speedX(), pullSpeed.speedY()));
                state.replaceEntity(slotState.entity());
                return false;
            }

            HookshotChainMotion.Position nextPosition = HookshotChainMotion.nextPosition(hookshotState);
            HookshotChainMotion.Step step = HookshotChainMotion.advance(
                    hookshotState, frameInput.linkEntityX(), frameInput.linkEntityY(), false, false);
            if (step.returning()) {
                state.motion(HookshotChainMotion.class, HookshotChainMotion::new)
                        .setState(slotState.entity().slot(), step.state());
                if (timerGates.handlerLinkCollisionEnabled() && step.reachedLink()) {
                    state.lifecycle().clearEntity(slotState.entity().slot());
                } else {
                    state.replaceEntity(withPositionAndVariant(
                            slotState.entity(),
                            step.state().x(),
                            step.state().y(),
                            slotState.entity().spriteVariant()));
                }
                return false;
            }
            // $7C59 checks enemies before background interaction. The
            // collision pass may clear the timer, but this pass still
            // follows the already-selected outgoing handler branch.
            boolean unmasked = state.projectiles()
                    .collideHookshotWithEntities(slotState.entity(), step.state(), frameInput.frame());
            if (unmasked) {
                step = new HookshotChainMotion.Step(
                        HookshotChainMotion.completeWallPoke(step.state()),
                        step.collided(),
                        step.reachedLink(),
                        step.returning());
            }
            if (hookshotState.wallCollisionPending()) {
                state.motion(HookshotChainMotion.class, HookshotChainMotion::new)
                        .setState(slotState.entity().slot(), HookshotChainMotion.completeWallPoke(step.state()));
                state.emit(new EntityCombatEvent(
                        slotState.entity().slot(),
                        slotState.entity().type(),
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        0x07));
                state.emit(new TransientVfxRequest(
                        TransientVfxType.SWORD_POKE,
                        slotState.entity().x(),
                        (slotState.entity().y() - slotState.entity().z()) & 0xFF));
                state.replaceEntity(withPositionAndVariant(
                        slotState.entity(),
                        step.state().x(),
                        step.state().y(),
                        slotState.entity().spriteVariant()));
                return false;
            }
            EntityBackgroundCollisionResult collisionResult = null;
            boolean blocked;
            if (state.background().interaction() != null) {
                collisionResult = state.background()
                        .interaction()
                        .probe(
                                slotState.entity(),
                                hookshotState.direction(),
                                nextPosition.x(),
                                nextPosition.y(),
                                state.slot().ignoreHits(slotState.entity().slot()),
                                frameInput.frame());
                blocked = collisionResult.blocked();
            } else {
                blocked = frameInput.backgroundCollision() != null
                        && frameInput
                                .backgroundCollision()
                                .blocks(
                                        slotState.entity(),
                                        hookshotState.direction(),
                                        nextPosition.x(),
                                        nextPosition.y());
            }
            if (blocked) {
                HookshotChainMotion.State stopped =
                        HookshotChainMotion.rollbackTo(step.state(), hookshotState.x(), hookshotState.y());
                if (collisionResult != null && collisionResult.physicsFlag() == 0x60) {
                    if (HookshotChainMotion.shouldUnloadForHookshotable(
                            step.state().transitionCountdown())) {
                        state.lifecycle().clearEntity(slotState.entity().slot());
                        return false;
                    }
                    state.motion(HookshotChainMotion.class, HookshotChainMotion::new)
                            .setState(slotState.entity().slot(), HookshotChainMotion.enterPulling(stopped));
                } else {
                    state.motion(HookshotChainMotion.class, HookshotChainMotion::new)
                            .setState(slotState.entity().slot(), HookshotChainMotion.deferWallPoke(stopped));
                }
                state.replaceEntity(slotState.entity());
                return false;
            }

            if (state.roomInspection().indoorRoom()
                    && hookshotState.speedY() != 0
                    && state.background().objectQuery() != null) {
                RoomEntity movedChain = withPositionAndVariant(
                        slotState.entity(),
                        step.state().x(),
                        step.state().y(),
                        slotState.entity().spriteVariant());
                RoomEntityObjectSample object = state.background().objectQuery().sample(movedChain);
                if (object != null) {
                    int bridgeObject = (hookshotState.speedY() & 0x80) != 0
                            ? OBJECT_HOOKSHOT_BRIDGE_PULL_DOWN
                            : OBJECT_HOOKSHOT_BRIDGE_PULL_UP;
                    if (object.objectId() == bridgeObject
                            && state.playerSpawns()
                                    .spawnHookshotBridge(
                                            object.objectLeft() + 0x08,
                                            object.objectTop() + 0x10,
                                            bridgeObject == OBJECT_HOOKSHOT_BRIDGE_PULL_DOWN ? 0 : 1,
                                            step.state().transitionCountdown())) {
                        state.emit(new HookshotBridgeUpdate(
                                object.objectLeft(),
                                object.objectTop(),
                                bridgeObject == OBJECT_HOOKSHOT_BRIDGE_PULL_DOWN ? 0 : 1));
                    }
                }
            }

            state.motion(HookshotChainMotion.class, HookshotChainMotion::new)
                    .setState(slotState.entity().slot(), step.state());
            state.replaceEntity(withPositionAndVariant(
                    slotState.entity(),
                    step.state().x(),
                    step.state().y(),
                    slotState.entity().spriteVariant()));
            return false;
        }
        return true;
    }
}
