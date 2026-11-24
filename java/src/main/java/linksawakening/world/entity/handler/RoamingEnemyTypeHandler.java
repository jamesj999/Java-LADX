package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_IRON_MASK;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_MOBLIN;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_OCTOROK;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistry;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.motion.EnemyRecoilMotion;
import linksawakening.world.entity.motion.RoamingEnemyMotion;
import linksawakening.world.entity.motion.UnmaskedIronMaskMotion;

/** ROM handler stages for RoamingEnemyType. */
final class RoamingEnemyTypeHandler {
    static void install(EntityHandlerRegistry registry) {
        for (int type : new int[] {ENTITY_OCTOROK, ENTITY_MOBLIN, ENTITY_IRON_MASK}) {
            registry.registerTick(
                    type, EntityTickStage.PHASE_1_AFTER_STATUS_TIMERS, 0, RoamingEnemyTypeHandler::tickTimers);
            registry.registerTick(
                    type, EntityTickStage.PHASE_3_BEFORE_LASER, 1, RoamingEnemyTypeHandler::phase3RoamingEnemyType);
        }
    }

    private static boolean tickTimers(EntityFrameExecution frame) {
        if (frame.gates().projectileTimersInteractive()) {
            frame.motion(RoamingEnemyMotion.class, RoamingEnemyMotion::new)
                    .tickTimers(frame.slotFrame().entity().slot());
        }
        return true;
    }

    private static boolean phase3RoamingEnemyType(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && state.gates().handlerInteractive()) {
            // $03:7FF0 skips the rest of AnimateRoamingEnemy for incoming recoil.
            if (slotState.entity().type() != ENTITY_IRON_MASK
                    && slotState.ignoreHitsBeforeRecoil() > 0
                    && state.motion(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                            .isActive(slotState.entity().slot())) return true;
            if (slotState.entity().type() == ENTITY_IRON_MASK
                    && state.slot().ironMaskRemovalState(slotState.entity().slot()) != 0) {
                UnmaskedIronMaskMotion.Update unmaskedUpdate = state.motion(
                                UnmaskedIronMaskMotion.class, UnmaskedIronMaskMotion::new)
                        .advance(
                                slotState.entity(),
                                frameInput.frame(),
                                frameInput.randomByteSupplier(),
                                frameInput.hinoxBackgroundInteraction(),
                                frameInput.backgroundCollision(),
                                state.slot().ignoreHits(slotState.entity().slot()),
                                frameInput.frame());
                slotState.updated(unmaskedUpdate.entity());
                state.slot().setTransitionCountdown(slotState.entity().slot(), unmaskedUpdate.transitionCountdown());
            } else {
                var roaming = state.motion(RoamingEnemyMotion.class, RoamingEnemyMotion::new);
                // The lethal hit clears the state table at $03:7341. The current
                // dispatch still uses hActiveEntityState at $03:585E.
                boolean deathWhilePaused = slotState.finishingLethalSwordHit()
                        && roaming.state(slotState.entity().slot()) != 0;
                RoamingEnemyMotion.Update roamingUpdate = roaming.advanceAfterTimers(
                        slotState.entity(),
                        frameInput.linkEntityX(),
                        frameInput.linkEntityY(),
                        frameInput.randomByteSupplier(),
                        frameInput.hinoxBackgroundInteraction(),
                        state.slot().ignoreHits(slotState.entity().slot()),
                        frameInput.frame(),
                        frameInput.creditsGameplay());
                slotState.updated(roamingUpdate.entity());
                if (deathWhilePaused)
                    roaming.finishLethalSwordHitWhilePaused(slotState.entity().slot());
                if (roamingUpdate.launchRequest() != null) {
                    state.emit(roamingUpdate.launchRequest());
                    state.enemySpawns().spawnEnemyProjectile(slotState.entity(), roamingUpdate.launchRequest());
                }
            }
        }
        return true;
    }
}
