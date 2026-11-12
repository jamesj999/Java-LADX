package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_ARMOS_STATUE;

import linksawakening.world.entity.EnemyProjectileCollision;
import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityLinkCollisionRules;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.ArmosMotion;

/** ROM handler stages for ArmosStatue. */
final class ArmosStatueHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_ARMOS_STATUE, EntityTickStage.PHASE_3_AFTER_LASER, 16, ArmosStatueHandler::phase3ArmosStatue);
    }

    private static boolean phase3ArmosStatue(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            RoomEntityBackgroundInteraction armosBackgroundInteraction =
                    state.background().interaction();
            if (armosBackgroundInteraction == null && frameInput.backgroundCollision() != null) {
                armosBackgroundInteraction =
                        RoomEntityBackgroundInteraction.fromBoolean(frameInput.backgroundCollision());
            }
            ArmosMotion.Update armosUpdate = state.motion(ArmosMotion.class, ArmosMotion::new)
                    .advance(
                            slotState.entity(),
                            frameInput.frame(),
                            frameInput.linkEntityX(),
                            frameInput.linkEntityY(),
                            frameInput.randomByteSupplier(),
                            timerGates.handlerLinkCollisionEnabled(),
                            armosBackgroundInteraction,
                            state.slot().ignoreHits(slotState.entity().slot()));
            slotState.updated(armosUpdate.entity());
            if (armosUpdate.linkFinalPositionCopyRequested()
                    && frameInput.projectileLinkState().motionState()
                            < EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE) {
                state.background().requestLinkPush(slotState.entity(), EntityLinkCollisionRules.RESTORE_ONLY);
            }
            if (armosUpdate.woke()) {
                // ArmosStatueEntityHandler's wake branch starts the same
                // $18 visible flash used by the ROM's enemy hit handlers.
                state.slot().setFlashCountdown(slotState.entity().slot(), 0x18);
            }
            if (armosUpdate.activated()) {
                // State 1 clears the harmless physics bit and enables the
                // normal hitbox/sword path for state 2.
                state.slot().andPhysicsFlags(slotState.entity().slot(), 0x7F);
                state.slot().setIgnoreHits(slotState.entity().slot(), 0);
            }
        }
        return true;
    }
}
