package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_MOBLIN_ARROW;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_PHYSICS_HARMLESS;
import static linksawakening.world.entity.EntitySnapshots.withDefinition;

import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.RoomEntityCombatRules;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.ScreenShakeRequest;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.MoblinKingFrame;
import linksawakening.world.entity.motion.MoblinKingMotion;
import linksawakening.world.entity.motion.RoamingEnemyMotion;

/** Reused, reentrancy-safe scratch for MoblinKingHandler.phase4MoblinKing; values are assigned in source order. */
final class MoblinKingHandlerFrameStep {
    private linksawakening.world.entity.EntityFrameExecution state;
    private linksawakening.world.entity.EntitySlotExecution slotState;
    private linksawakening.world.entity.EntityFrameInputs frameInput;
    private linksawakening.world.entity.EntityTimerGates timerGates;
    private linksawakening.world.entity.EntityPresentationFlags presentationFlags;

    MoblinKingHandlerFrameStep() {}

    boolean run() {
        slotState = state.slotFrame();
        frameInput = state.inputs();
        timerGates = state.gates();
        presentationFlags = state.presentation();

        if (!(slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing())) return true;

        if (state.story().bowWowState() != 0x80) {
            state.lifecycle().disableEntityWithoutPersistence(slotState.entity().slot());
            return false;
        }
        if (!timerGates.handlerInteractive()) {
            presentationFlags.preserveMoblinKingPresentation(true);
        } else {
            // ApplyRecoilIfNeeded_15 pops the handler return address:
            // movement/Z physics and AI wait until the recoil ends.
            if (state.motion(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                            .isActive(slotState.entity().slot())
                    && state.slot().ignoreHits(slotState.entity().slot()) != 0) {
                state.replaceEntity(state.background()
                        .applyEnemyRecoilIfNeeded(slotState.entity(), frameInput.backgroundCollision())
                        .entity());
                return false;
            }
            MoblinKingMotion.Update kingUpdate = state.motion(MoblinKingMotion.class, MoblinKingMotion::new)
                    .advance(new MoblinKingFrame(
                            slotState.entity(),
                            frameInput.linkEntityX(),
                            frameInput.linkEntityY(),
                            state.slot().transitionCountdown(slotState.entity().slot()),
                            state.slot()
                                    .slowTransitionCountdown(slotState.entity().slot()),
                            frameInput.frame(),
                            frameInput.randomByteSupplier(),
                            frameInput.backgroundCollision(),
                            timerGates.handlerLinkCollisionEnabled()
                                    && RoomEntityCombatRules.collisionCadenceMatches(
                                            frameInput.frame(),
                                            slotState.entity().slot())
                                    && RoomEntityCombatRules.overlapsLink(
                                            slotState.entity(), frameInput.linkEntityX(), frameInput.linkEntityY())));
            applyMotionUpdate(kingUpdate);
            emitMotionEffects(kingUpdate);
        }

        return true;
    }

    private void applyMotionUpdate(MoblinKingMotion.Update kingUpdate) {
        slotState.updated(kingUpdate.entity());
        state.slot().setTransitionCountdown(slotState.entity().slot(), kingUpdate.transitionCountdown());
        state.slot().setSlowTransitionCountdown(slotState.entity().slot(), kingUpdate.slowTransitionCountdown());
        state.slot().setSlowTimerInitialized(slotState.entity().slot(), true);
        state.slot()
                .setOptions1Override(
                        slotState.entity().slot(),
                        state.motion(MoblinKingMotion.class, MoblinKingMotion::new)
                                                .state(slotState.entity().slot())
                                        == 5
                                ? 0x84
                                : state.motion(MoblinKingMotion.class, MoblinKingMotion::new)
                                                        .state(slotState
                                                                .entity()
                                                                .slot())
                                                >= 2
                                        ? 0xC4
                                        : 0x40);
        if (state.motion(MoblinKingMotion.class, MoblinKingMotion::new)
                        .state(slotState.entity().slot())
                == 5) {
            state.slot().orPhysicsFlags(slotState.entity().slot(), ENTITY_PHYSICS_HARMLESS);
        } else {
            state.slot().andPhysicsFlags(slotState.entity().slot(), ~ENTITY_PHYSICS_HARMLESS);
        }
        if (state.sprites() != null) {
            slotState.updated(withDefinition(
                    slotState.updated(),
                    state.sprites()
                            .forMoblinKingState(
                                    state.motion(MoblinKingMotion.class, MoblinKingMotion::new)
                                            .bodyVariant(slotState.entity().slot()),
                                    state.motion(MoblinKingMotion.class, MoblinKingMotion::new)
                                            .weaponVariant(slotState.entity().slot()),
                                    slotState.updated().z() != 0),
                    0));
        }
        presentationFlags.preserveMoblinKingPresentation(true);
    }

    private void emitMotionEffects(MoblinKingMotion.Update kingUpdate) {
        if (kingUpdate.openIntroDialog()) {
            // $15:475C clears DIALOG_BOX_BOTTOM_BIT after opening Dialog191.
            state.emit(new DialogRequest(1, 0x91, true));
        }
        if (kingUpdate.arrowRequest() != null) {
            state.emit(new RoamingEnemyMotion.LaunchRequest(
                    slotState.entity().slot(), slotState.entity().type(), ENTITY_MOBLIN_ARROW));
            state.enemySpawns()
                    .spawnEnemyProjectile(
                            slotState.entity(),
                            new RoamingEnemyMotion.LaunchRequest(
                                    slotState.entity().slot(),
                                    slotState.entity().type(),
                                    ENTITY_MOBLIN_ARROW),
                            kingUpdate.arrowRequest().direction());
        }
        if (kingUpdate.jingleId() >= 0) {
            state.emit(new EntityCombatEvent(
                    slotState.entity().slot(),
                    slotState.entity().type(),
                    0,
                    false,
                    EntityCombatEvent.SoundChannel.JINGLE,
                    kingUpdate.jingleId()));
        }
        if (kingUpdate.screenShakeCountdown() != 0) {
            state.emit(new ScreenShakeRequest(slotState.entity().slot(), kingUpdate.screenShakeCountdown(), 0));
        }
    }

    private boolean inUse;

    boolean run(linksawakening.world.entity.EntityFrameExecution state) {
        if (inUse) {
            return new MoblinKingHandlerFrameStep().run(state);
        }
        inUse = true;
        slotState = null;
        frameInput = null;
        timerGates = null;
        presentationFlags = null;
        this.state = state;
        try {
            return run();
        } finally {
            inUse = false;
            this.state = null;
            this.slotState = null;
            this.frameInput = null;
            this.timerGates = null;
            this.presentationFlags = null;
        }
    }
}
