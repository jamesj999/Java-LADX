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
        registry.registerTick(ENTITY_ROOSTER, EntityTickStage.PHASE_2_ROOSTER, RoosterHandler::phase2Rooster);
        registry.registerTick(ENTITY_ROOSTER, EntityTickStage.PHASE_5_ROOSTER, RoosterHandler::phase5Rooster);
    }

    private static boolean phase2Rooster(EntityFrameExecution state) {
        RoosterMotion.Update roosterUpdate = RoosterMotion.advanceLifted(
                state.frame,
                state.linkZ & 0xFF,
                state.link().linkPressedButtonsMask(),
                state.romCollisionType,
                state.link().currentLinkSpeedX(),
                state.link().currentLinkSpeedY(),
                state.romLinkDirection);
        state.lifted = withVariant(state.lifted, roosterUpdate.spriteVariant());
        state.liftThrow().setLiftedSourceDirection(state.entity.slot(), roosterUpdate.romDirection());
        state.liftedRomDirection = roosterUpdate.romDirection();
        state.emit(new RoosterLinkStateRequest(
                state.entity.slot(),
                roosterUpdate.positionZ(),
                roosterUpdate.velocityZ(),
                roosterUpdate.speedX(),
                roosterUpdate.speedY(),
                roosterUpdate.romDirection()));
        state.projectiles().playBoomerangSoundIfDue(state.entity);

        return true;
    }

    private static boolean phase5Rooster(EntityFrameExecution state) {
        if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            state.preserveRoosterPresentation = true;
            if (state.projectileLinkState.motionState() == 0
                    && (state.linkZ & 0xFF) == 0
                    && state.link().linkAttackStepAnimationCountdown() == 0
                    && state.liftThrow().cuccoPowerBraceletHeld()
                    && RoomEntityCombatRules.overlapsLink(state.entity, state.linkEntityX, state.linkEntityY)
                    && state.liftThrow().beginLift(state.entity.slot(), state.romLinkDirection)) {
                state.emit(new RoosterLinkStateRequest(
                        state.entity.slot(),
                        0x02,
                        0x00,
                        state.link().currentLinkSpeedX(),
                        state.link().currentLinkSpeedY(),
                        state.romLinkDirection));
                state.emit(new EntityCombatEvent(
                        state.entity.slot(),
                        state.entity.type(),
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
