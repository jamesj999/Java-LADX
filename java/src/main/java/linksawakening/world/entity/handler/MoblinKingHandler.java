package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_MOBLIN_ARROW;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_MOBLIN_KING;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_PHYSICS_HARMLESS;
import static linksawakening.world.entity.EntitySnapshots.withDefinition;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityCombatRules;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.ScreenShakeRequest;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.MoblinKingMotion;
import linksawakening.world.entity.motion.RoamingEnemyMotion;

/** ROM handler stages for MoblinKing. */
final class MoblinKingHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_MOBLIN_KING, EntityTickStage.PHASE_2_DYING, 0, MoblinKingHandler::phase2MoblinKing);
        registry.registerTick(
                ENTITY_MOBLIN_KING, EntityTickStage.PHASE_4_BEFORE_FOLLOWER, 5, MoblinKingHandler::phase4MoblinKing);
    }

    private static boolean phase2MoblinKing(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        state.bossRewards()
                .advanceMoblinKingDestruction(slotState.entity(), frameInput.frame(), frameInput.randomByteSupplier());
        return false;
    }

    private static boolean phase4MoblinKing(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();
        var presentationFlags = state.presentation();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            if (state.story().bowWowState() != 0x80) {
                state.lifecycle()
                        .disableEntityWithoutPersistence(slotState.entity().slot());
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
                        .advance(
                                slotState.entity(),
                                frameInput.linkEntityX(),
                                frameInput.linkEntityY(),
                                state.slot()
                                        .transitionCountdown(slotState.entity().slot()),
                                state.slot()
                                        .slowTransitionCountdown(
                                                slotState.entity().slot()),
                                frameInput.frame(),
                                frameInput.randomByteSupplier(),
                                frameInput.backgroundCollision(),
                                timerGates.handlerLinkCollisionEnabled()
                                        && RoomEntityCombatRules.collisionCadenceMatches(
                                                frameInput.frame(),
                                                slotState.entity().slot())
                                        && RoomEntityCombatRules.overlapsLink(
                                                slotState.entity(),
                                                frameInput.linkEntityX(),
                                                frameInput.linkEntityY()));
                slotState.updated(kingUpdate.entity());
                state.slot().setTransitionCountdown(slotState.entity().slot(), kingUpdate.transitionCountdown());
                state.slot()
                        .setSlowTransitionCountdown(slotState.entity().slot(), kingUpdate.slowTransitionCountdown());
                state.slot().setSlowTimerInitialized(slotState.entity().slot(), true);
                state.slot()
                        .setOptions1Override(
                                slotState.entity().slot(),
                                state.motion(MoblinKingMotion.class, MoblinKingMotion::new)
                                                        .state(slotState
                                                                .entity()
                                                                .slot())
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
                                                    .bodyVariant(
                                                            slotState.entity().slot()),
                                            state.motion(MoblinKingMotion.class, MoblinKingMotion::new)
                                                    .weaponVariant(
                                                            slotState.entity().slot()),
                                            slotState.updated().z() != 0),
                            0));
                }
                presentationFlags.preserveMoblinKingPresentation(true);
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
        }
        return true;
    }
}
