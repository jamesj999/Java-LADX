package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_WINGED_OCTOROK;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.WingedOctorokMotion;

/** ROM handler stages for WingedOctorok. */
final class WingedOctorokHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_WINGED_OCTOROK,
                EntityTickStage.PHASE_3_BEFORE_LASER,
                12,
                WingedOctorokHandler::phase3WingedOctorok);
    }

    private static boolean phase3WingedOctorok(EntityFrameExecution state) {
        if (state.status == EntityStatus.ACTIVE && !state.wasInitializing && state.handlerLinkCollisionEnabled) {

            WingedOctorokMotion.Update wingedUpdate = state.wingedOctorokMotion()
                    .advance(
                            state.entity,
                            state.frame,
                            state.linkEntityX,
                            state.linkEntityY,
                            state.randomByteSupplier,
                            state.backgroundCollision,
                            state.slot().transitionCountdown(state.entity.slot()),
                            state.slot().ignoreHits(state.entity.slot()),
                            state.link().actionButtonAHeld(),
                            state.link().actionButtonBHeld(),
                            state.link().linkItemA(),
                            state.link().linkItemB());
            state.updated = wingedUpdate.entity();
            state.slot().setTransitionCountdown(state.entity.slot(), wingedUpdate.transitionCountdown());
            state.preserveWingedOctorokPresentation = true;
            state.projectiles().setWingedStateTwoThisFrame(state.entity.slot(), wingedUpdate.skippedDefaultCollision());
            if (wingedUpdate.rockSpawn() != null) {
                state.enemySpawns()
                        .spawnWingedOctorokRock(
                                state.updated, wingedUpdate.rockSpawn().direction());
            }
            if (wingedUpdate.jumped()) {
                state.projectiles().setWingedSwordAttackThisFrame(state.entity.slot(), true);
                state.emit(new EntityCombatEvent(
                        state.entity.slot(),
                        state.entity.type(),
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        0x24));
            }
        }
        return true;
    }
}
