package linksawakening.world.entity.handler;

import linksawakening.world.EntityTickStage;
import linksawakening.world.RoomEntityRuntime;
import linksawakening.world.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.RoomEntityCombatRules;
import linksawakening.world.EntityCombatEvent;







import linksawakening.world.entity.motion.RoosterMotion;

import linksawakening.world.entity.event.RoosterLinkStateRequest;
import static linksawakening.world.EntitySnapshots.withVariant;

/** ROM handler stages for Rooster. */
final class RoosterHandler {
  static void install(EntityHandlerRegistration registry) {
    for (int type = 0; type < 256; type++) {
      if (((type == RoomEntityRuntime.ENTITY_ROOSTER))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_ROOSTER,
            state -> {
              {
                RoosterMotion.Update roosterUpdate =
                    RoosterMotion.advanceLifted(
                        state.frame,
                        state.linkZ & 0xFF,
                        state.runtime.linkPressedButtonsMask,
                        state.romCollisionType,
                        state.runtime.currentLinkSpeedX,
                        state.runtime.currentLinkSpeedY,
                        state.romLinkDirection);
                state.lifted = withVariant(state.lifted, roosterUpdate.spriteVariant());
                state.runtime.liftedSourceDirection[state.entity.slot()] =
                    roosterUpdate.romDirection();
                state.liftedRomDirection = roosterUpdate.romDirection();
                state.emit(
                    new RoosterLinkStateRequest(
                        state.entity.slot(),
                        roosterUpdate.positionZ(),
                        roosterUpdate.velocityZ(),
                        roosterUpdate.speedX(),
                        roosterUpdate.speedY(),
                        roosterUpdate.romDirection()));
                state.runtime.projectiles.playBoomerangSoundIfDue(state.entity);
              }
              return true;
            });
      }
    }
    registry.registerTick(
        RoomEntityRuntime.ENTITY_ROOSTER,
        EntityTickStage.PHASE_5_ROOSTER,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            state.preserveRoosterPresentation = true;
            if (state.projectileLinkState.motionState() == 0
                && (state.linkZ & 0xFF) == 0
                && state.runtime.linkAttackStepAnimationCountdown == 0
                && state.runtime.liftThrow.cuccoPowerBraceletHeld()
                && RoomEntityCombatRules.overlapsLink(
                    state.entity, state.linkEntityX, state.linkEntityY)
                && state.runtime.beginLift(state.entity.slot(), state.romLinkDirection)) {
              state.emit(
                  new RoosterLinkStateRequest(
                      state.entity.slot(),
                      0x02,
                      0x00,
                      state.runtime.currentLinkSpeedX,
                      state.runtime.currentLinkSpeedY,
                      state.romLinkDirection));
              state.emit(
                  new EntityCombatEvent(
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
        });
  }
}
