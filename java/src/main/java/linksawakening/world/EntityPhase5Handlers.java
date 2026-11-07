package linksawakening.world;

import linksawakening.world.entity.event.RoosterLinkStateRequest;

import static linksawakening.world.EntityMotionMath.*;
import static linksawakening.world.EntitySnapshots.*;
import static linksawakening.world.EntityTypeRules.*;
import static linksawakening.world.RoomEntityRuntime.*;

import java.util.Arrays;

/** Registered ROM type callbacks for ordered slot phase 5. */
final class EntityPhase5Handlers {
  static void install(EntityHandlerRegistry registry) {
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
              state.runtime.pendingRoosterLinkStateRequests.add(
                  new RoosterLinkStateRequest(
                      state.entity.slot(),
                      0x02,
                      0x00,
                      state.runtime.currentLinkSpeedX,
                      state.runtime.currentLinkSpeedY,
                      state.romLinkDirection));
              state.runtime.pendingEntityEvents.add(
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
    for (int type = 0; type < 256; type++) {
      if ((ColorShellMotion.isColorShellType(type))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_5_COLOR_SHELL_TYPE,
            state -> {
              if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
                ColorShellMotion.Update shellUpdate =
                    state.runtime.colorShellMotion.advance(
                        state.entity,
                        state.frame,
                        state.linkEntityX,
                        state.linkEntityY,
                        state.randomByteSupplier,
                        state.backgroundCollision,
                        state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()],
                        state.runtime.colorShellWorld,
                        Arrays.asList(state.runtime.slots));
                state.updated = shellUpdate.entity();
                state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()] =
                    shellUpdate.nextIgnoreHitsCountdown();
                if (shellUpdate.unloadRequested()) {
                  state.runtime.pendingClearedEntityMask |= persistentClearMask(state.entity);
                  state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
                  return false;
                }
              }
              return true;
            });
      }
    }
  }
}
