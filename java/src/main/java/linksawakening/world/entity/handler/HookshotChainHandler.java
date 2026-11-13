package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_HOOKSHOT_CHAIN;
import static linksawakening.world.entity.EntityRomConstants.OBJECT_HOOKSHOT_BRIDGE_PULL_DOWN;
import static linksawakening.world.entity.EntityRomConstants.OBJECT_HOOKSHOT_BRIDGE_PULL_UP;
import static linksawakening.world.entity.EntitySnapshots.withPositionAndVariant;

import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.EntityBackgroundCollisionResult;
import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistry;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityObjectSample;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.EntityProjectileEvent;
import linksawakening.world.entity.event.HookshotBridgeUpdate;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.motion.HookshotChainMotion;

/** ROM handler stages for HookshotChain. */
final class HookshotChainHandler {
    static void install(EntityHandlerRegistry registry) {
        registry.registerTick(
                ENTITY_HOOKSHOT_CHAIN,
                EntityTickStage.PHASE_1_BEFORE_PICKUP_SEQUENCE,
                4,
                HookshotChainHandler::phase1HookshotChain);
    }

    private static boolean phase1HookshotChain(EntityFrameExecution state) {
        return state.scratch(HookshotChainHandlerFrameStep.class, HookshotChainHandlerFrameStep::new)
                .execute();
    }

    private static final class HookshotChainHandlerFrameStep {
        // Owning frame execution is stable for this cached scratch holder.
        private final linksawakening.world.entity.EntityFrameExecution state;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.EntityFrameExecution.EntitySlotExecution slotState;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.EntityFrameExecution.EntityFrameInputs frameInput;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.EntityFrameExecution.EntityTimerGates timerGates;

        HookshotChainHandlerFrameStep(linksawakening.world.entity.EntityFrameExecution state) {
            this.state = java.util.Objects.requireNonNull(state, "state");
        }

        boolean run() {
            if (!prepareChain()) return completedResult;
            if (!advancePullOrTravel()) return completedResult;
            if (!resolveBackgroundCollision()) return completedResult;
            if (!applyWallCollision()) return completedResult;
            if (!publishChain()) return completedResult;

            return false;
        }

        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.motion.HookshotChainMotion.State hookshotState;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.motion.HookshotChainMotion.Position nextPosition;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.motion.HookshotChainMotion.Step step;
        private boolean unmasked;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.EntityBackgroundCollisionResult collisionResult;
        private boolean blocked;

        private boolean completedResult;

        private boolean prepareChain() {

            slotState = state.slotFrame();
            frameInput = state.inputs();
            timerGates = state.gates();

            if (!(slotState.status() == EntityStatus.ACTIVE)) {
                completedResult = true;
                return false;
            }

            hookshotState = state.motion(HookshotChainMotion.class, HookshotChainMotion::new)
                    .state(slotState.entity().slot());
            if (hookshotState == null) {
                state.lifecycle().clearEntity(slotState.entity().slot());

                completedResult = false;
                return false;
            }
            // $7BE3 cancels the chain before rendering or the pause gate.
            if (state.link().linkPlayingOcarinaCountdown() != 0) {
                state.lifecycle().clearEntity(slotState.entity().slot());

                completedResult = false;
                return false;
            }
            if (timerGates.projectileTimersInteractive()) {
                hookshotState = HookshotChainMotion.decrementTransitionCountdown(hookshotState);
                state.motion(HookshotChainMotion.class, HookshotChainMotion::new)
                        .setState(slotState.entity().slot(), hookshotState);
            }
            // Bank $18 has no credits exception in ReturnIfNonInteractive.
            if (!timerGates.handlerInteractive() || frameInput.creditsGameplay()) {

                completedResult = false;
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
            return true;
        }

        private boolean advancePullOrTravel() {

            if (hookshotState.entityState() == HookshotChainMotion.PULLING_STATE) {
                if (timerGates.handlerLinkCollisionEnabled()
                        && HookshotChainMotion.overlapsLink(
                                hookshotState, frameInput.linkEntityX(), frameInput.linkEntityY())) {
                    state.lifecycle().clearEntity(slotState.entity().slot());

                    completedResult = false;
                    return false;
                }
                HookshotChainMotion.PullSpeed pullSpeed = HookshotChainMotion.pullLinkSpeed(
                        hookshotState, frameInput.linkEntityX(), frameInput.linkEntityY());
                frameInput
                        .projectileEvents()
                        .add(EntityProjectileEvent.hookshotPull(
                                slotState.entity().slot(), pullSpeed.speedX(), pullSpeed.speedY()));
                state.replaceEntity(slotState.entity());

                completedResult = false;
                return false;
            }

            nextPosition = HookshotChainMotion.nextPosition(hookshotState);
            step = HookshotChainMotion.advance(
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

                completedResult = false;
                return false;
            }
            // $7C59 checks enemies before background interaction. The
            // collision pass may clear the timer, but this pass still
            // follows the already-selected outgoing handler branch.
            unmasked = state.projectiles()
                    .collideHookshotWithEntities(slotState.entity(), step.state(), frameInput.frame());
            if (unmasked) {
                step = new HookshotChainMotion.Step(
                        HookshotChainMotion.completeWallPoke(step.state()),
                        step.collided(),
                        step.reachedLink(),
                        step.returning());
            }
            return true;
        }

        private boolean resolveBackgroundCollision() {

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

                completedResult = false;
                return false;
            }
            collisionResult = EntityBackgroundCollisionResult.NONE;

            {
                collisionResult = frameInput
                        .hinoxBackgroundInteraction()
                        .probe(
                                slotState.entity(),
                                hookshotState.direction(),
                                nextPosition.x(),
                                nextPosition.y(),
                                state.slot().ignoreHits(slotState.entity().slot()),
                                frameInput.frame());
                blocked = collisionResult.blocked();
            }
            return true;
        }

        private boolean applyWallCollision() {

            if (blocked) {
                HookshotChainMotion.State stopped =
                        HookshotChainMotion.rollbackTo(step.state(), hookshotState.x(), hookshotState.y());
                if (collisionResult.physicsFlag() == 0x60) {
                    if (HookshotChainMotion.shouldUnloadForHookshotable(
                            step.state().transitionCountdown())) {
                        state.lifecycle().clearEntity(slotState.entity().slot());

                        completedResult = false;
                        return false;
                    }
                    state.motion(HookshotChainMotion.class, HookshotChainMotion::new)
                            .setState(slotState.entity().slot(), HookshotChainMotion.enterPulling(stopped));
                } else {
                    state.motion(HookshotChainMotion.class, HookshotChainMotion::new)
                            .setState(slotState.entity().slot(), HookshotChainMotion.deferWallPoke(stopped));
                }
                state.replaceEntity(slotState.entity());

                completedResult = false;
                return false;
            }

            if (state.roomInspection().indoorRoom() && hookshotState.speedY() != 0) {
                RoomEntity movedChain = withPositionAndVariant(
                        slotState.entity(),
                        step.state().x(),
                        step.state().y(),
                        slotState.entity().spriteVariant());
                RoomEntityObjectSample object = state.background().objectQuery().sampleOrAbsent(movedChain);
                if (object.sampled()) {
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
            return true;
        }

        private boolean publishChain() {

            state.replaceEntity(withPositionAndVariant(
                    slotState.entity(),
                    step.state().x(),
                    step.state().y(),
                    slotState.entity().spriteVariant()));
            return true;
        }

        private boolean inUse;

        boolean execute() {
            if (inUse) {
                return new HookshotChainHandlerFrameStep(state).execute();
            }
            inUse = true;
            slotState = null;
            frameInput = null;
            timerGates = null;
            hookshotState = null;
            nextPosition = null;
            step = null;
            unmasked = false;
            collisionResult = EntityBackgroundCollisionResult.NONE;
            blocked = false;
            completedResult = false;

            try {
                return run();
            } finally {
                inUse = false;

                this.slotState = null;
                this.frameInput = null;
                this.timerGates = null;
                this.hookshotState = null;
                this.nextPosition = null;
                this.step = null;
                this.collisionResult = EntityBackgroundCollisionResult.NONE;
            }
        }
    }
}
