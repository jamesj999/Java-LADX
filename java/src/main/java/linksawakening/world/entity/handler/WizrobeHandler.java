package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_WIZROBE;
import static linksawakening.world.entity.EntitySnapshots.withVariant;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.motion.WizrobeMotion;

/** ROM handler stages for Wizrobe. */
final class WizrobeHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(ENTITY_WIZROBE, EntityTickStage.PHASE_2_WIZROBE, WizrobeHandler::phase2Wizrobe);
        registry.registerTick(ENTITY_WIZROBE, EntityTickStage.PHASE_2_WIZROBE_2, WizrobeHandler::phase2Wizrobe2);
        registry.registerTick(ENTITY_WIZROBE, EntityTickStage.PHASE_3_WIZROBE, WizrobeHandler::phase3Wizrobe);
    }

    private static boolean phase2Wizrobe(EntityFrameExecution state) {
        if (state.wasInitializing) {
            // EntityInitWizrobe decrements the initial display-list variant
            // before the first active handler dispatch.
            state.updated = withVariant(state.entity, -1);
        }
        return true;
    }

    private static boolean phase2Wizrobe2(EntityFrameExecution state) {
        state.wizrobeMotion().initializeFromRoom(state.entity.slot(), state.entity.spriteVariant());
        state.slot().setTransitionCountdown(state.entity.slot(), 0x80);
        state.preserveWizrobePresentation = true;

        return true;
    }

    private static boolean phase3Wizrobe(EntityFrameExecution state) {
        if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            WizrobeMotion.Update wizrobeUpdate = state.wizrobeMotion()
                    .advance(
                            state.entity,
                            state.linkEntityX,
                            state.linkEntityY,
                            state.randomByteSupplier,
                            state.slot().transitionCountdown(state.entity.slot()),
                            state.slot().physicsFlags(state.entity.slot()));
            state.updated = wizrobeUpdate.entity();
            state.slot().setTransitionCountdown(state.entity.slot(), wizrobeUpdate.transitionCountdown());
            state.slot().setPhysicsFlags(state.entity.slot(), wizrobeUpdate.physicsFlags());
            state.preserveWizrobePresentation = true;
            if (wizrobeUpdate.projectileSpawn() != null) {
                state.enemySpawns().spawnWizrobeProjectile(state.updated, wizrobeUpdate.projectileSpawn());
            }
        }
        return true;
    }
}
