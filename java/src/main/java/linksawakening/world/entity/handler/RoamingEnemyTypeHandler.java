package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_IRON_MASK;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.RoamingEnemyMotion;
import linksawakening.world.entity.motion.UnmaskedIronMaskMotion;

/** ROM handler stages for RoamingEnemyType. */
final class RoamingEnemyTypeHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_OCTOROK,
                EntityTickStage.PHASE_3_BEFORE_LASER,
                1,
                RoamingEnemyTypeHandler::phase3RoamingEnemyType);
        registry.registerTick(
                ENTITY_MOBLIN,
                EntityTickStage.PHASE_3_BEFORE_LASER,
                1,
                RoamingEnemyTypeHandler::phase3RoamingEnemyType);
        registry.registerTick(
                ENTITY_IRON_MASK,
                EntityTickStage.PHASE_3_BEFORE_LASER,
                1,
                RoamingEnemyTypeHandler::phase3RoamingEnemyType);
    }

    private static boolean phase3RoamingEnemyType(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            if (slotState.entity().type() == ENTITY_IRON_MASK
                    && state.slot().ironMaskPrivateState2(slotState.entity().slot()) != 0) {
                UnmaskedIronMaskMotion.Update unmaskedUpdate = state.motion(
                                UnmaskedIronMaskMotion.class, UnmaskedIronMaskMotion::new)
                        .advance(
                                slotState.entity(),
                                frameInput.frame(),
                                frameInput.randomByteSupplier(),
                                state.background().interaction(),
                                frameInput.backgroundCollision(),
                                state.slot().ignoreHits(slotState.entity().slot()),
                                frameInput.frame());
                slotState.updated(unmaskedUpdate.entity());
                state.slot().setTransitionCountdown(slotState.entity().slot(), unmaskedUpdate.transitionCountdown());
            } else {
                RoamingEnemyMotion.Update roamingUpdate = state.background().interaction() == null
                        ? state.motion(RoamingEnemyMotion.class, RoamingEnemyMotion::new)
                                .advance(
                                        slotState.entity(),
                                        frameInput.linkEntityX(),
                                        frameInput.linkEntityY(),
                                        frameInput.randomByteSupplier(),
                                        frameInput.backgroundCollision(),
                                        frameInput.creditsGameplay())
                        : state.motion(RoamingEnemyMotion.class, RoamingEnemyMotion::new)
                                .advanceWithInteraction(
                                        slotState.entity(),
                                        frameInput.linkEntityX(),
                                        frameInput.linkEntityY(),
                                        frameInput.randomByteSupplier(),
                                        state.background().interaction(),
                                        state.slot()
                                                .ignoreHits(slotState.entity().slot()),
                                        frameInput.frame(),
                                        frameInput.creditsGameplay());
                slotState.updated(roamingUpdate.entity());
                if (roamingUpdate.launchRequest() != null) {
                    state.emit(roamingUpdate.launchRequest());
                    state.enemySpawns().spawnEnemyProjectile(slotState.entity(), roamingUpdate.launchRequest());
                }
            }
        }
        return true;
    }
}
