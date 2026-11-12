package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_WIZROBE;
import static linksawakening.world.entity.EntitySnapshots.withVariant;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.WizrobeMotion;

/** ROM handler stages for Wizrobe. */
final class WizrobeHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_WIZROBE, EntityTickStage.PHASE_2_BEFORE_RECOIL_APPLICATION, 7, WizrobeHandler::phase2Wizrobe);
        registry.registerTick(
                ENTITY_WIZROBE, EntityTickStage.PHASE_2_AFTER_INITIALIZE, 6, WizrobeHandler::phase2Wizrobe2);
        registry.registerTick(ENTITY_WIZROBE, EntityTickStage.PHASE_3_AFTER_LASER, 14, WizrobeHandler::phase3Wizrobe);
    }

    private static boolean phase2Wizrobe(EntityFrameExecution state) {
        var slotState = state.slotFrame();

        if (slotState.wasInitializing()) {
            // EntityInitWizrobe decrements the initial display-list variant
            // before the first active handler dispatch.
            slotState.updated(withVariant(slotState.entity(), -1));
        }
        return true;
    }

    private static boolean phase2Wizrobe2(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var presentationFlags = state.presentation();

        state.motion(WizrobeMotion.class, WizrobeMotion::new)
                .initializeFromRoom(
                        slotState.entity().slot(), slotState.entity().spriteVariant());
        state.slot().setTransitionCountdown(slotState.entity().slot(), 0x80);
        presentationFlags.preserveWizrobePresentation(true);

        return true;
    }

    private static boolean phase3Wizrobe(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var presentationFlags = state.presentation();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            WizrobeMotion.Update wizrobeUpdate = state.motion(WizrobeMotion.class, WizrobeMotion::new)
                    .advance(
                            slotState.entity(),
                            frameInput.linkEntityX(),
                            frameInput.linkEntityY(),
                            frameInput.randomByteSupplier(),
                            state.slot().transitionCountdown(slotState.entity().slot()),
                            state.slot().physicsFlags(slotState.entity().slot()));
            slotState.updated(wizrobeUpdate.entity());
            state.slot().setTransitionCountdown(slotState.entity().slot(), wizrobeUpdate.transitionCountdown());
            state.slot().setPhysicsFlags(slotState.entity().slot(), wizrobeUpdate.physicsFlags());
            presentationFlags.preserveWizrobePresentation(true);
            if (wizrobeUpdate.projectileSpawn() != null) {
                state.enemySpawns().spawnWizrobeProjectile(slotState.updated(), wizrobeUpdate.projectileSpawn());
            }
        }
        return true;
    }
}
