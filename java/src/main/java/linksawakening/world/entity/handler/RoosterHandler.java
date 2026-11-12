package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_ROOSTER;
import static linksawakening.world.entity.EntitySnapshots.withVariant;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityCombatRules;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.RoosterLinkStateRequest;
import linksawakening.world.entity.motion.RoosterMotion;

/** ROM handler stages for Rooster. */
final class RoosterHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_ROOSTER, EntityTickStage.PHASE_2_LIFTED_FOLLOWER, 0, RoosterHandler::phase2Rooster);
        registry.registerTick(ENTITY_ROOSTER, EntityTickStage.PHASE_5_BEFORE_COMMIT, 0, RoosterHandler::phase5Rooster);
    }

    private static boolean phase2Rooster(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        RoosterMotion.Update roosterUpdate = RoosterMotion.advanceLifted(
                frameInput.frame(),
                frameInput.linkZ() & 0xFF,
                state.link().linkPressedButtonsMask(),
                frameInput.romCollisionType(),
                state.link().currentLinkSpeedX(),
                state.link().currentLinkSpeedY(),
                frameInput.romLinkDirection());
        slotState.lifted(withVariant(slotState.lifted(), roosterUpdate.spriteVariant()));
        state.liftThrow().setLiftedSourceDirection(slotState.entity().slot(), roosterUpdate.romDirection());
        slotState.liftedRomDirection(roosterUpdate.romDirection());
        state.emit(new RoosterLinkStateRequest(
                slotState.entity().slot(),
                roosterUpdate.positionZ(),
                roosterUpdate.velocityZ(),
                roosterUpdate.speedX(),
                roosterUpdate.speedY(),
                roosterUpdate.romDirection()));
        state.projectiles().playBoomerangSoundIfDue(slotState.entity());

        return true;
    }

    private static boolean phase5Rooster(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var presentationFlags = state.presentation();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            presentationFlags.preserveRoosterPresentation(true);
            if (frameInput.projectileLinkState().motionState() == 0
                    && (frameInput.linkZ() & 0xFF) == 0
                    && state.link().linkAttackStepAnimationCountdown() == 0
                    && state.liftThrow().cuccoPowerBraceletHeld()
                    && RoomEntityCombatRules.overlapsLink(
                            slotState.entity(), frameInput.linkEntityX(), frameInput.linkEntityY())
                    && state.liftThrow().beginLift(slotState.entity().slot(), frameInput.romLinkDirection())) {
                state.emit(new RoosterLinkStateRequest(
                        slotState.entity().slot(),
                        0x02,
                        0x00,
                        state.link().currentLinkSpeedX(),
                        state.link().currentLinkSpeedY(),
                        frameInput.romLinkDirection()));
                state.emit(new EntityCombatEvent(
                        slotState.entity().slot(),
                        slotState.entity().type(),
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.WAVE,
                        RoosterMotion.LIFT_WAVE_SFX));
                return false;
            }
        }
        return true;
    }
}
