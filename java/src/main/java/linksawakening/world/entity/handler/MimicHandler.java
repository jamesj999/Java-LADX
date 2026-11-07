package linksawakening.world.entity.handler;

import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;

import linksawakening.world.entity.motion.MimicMotion;

/** ROM handler stages for Mimic. */
final class MimicHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_MIMIC,
        EntityTickStage.PHASE_2_MIMIC,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            int slot = state.entity.slot();
            RoomEntityBackgroundInteraction mimicBackgroundInteraction =
                state.runtime.backgroundInteraction;
            if (mimicBackgroundInteraction == null && state.backgroundCollision != null) {
              mimicBackgroundInteraction =
                  RoomEntityBackgroundInteraction.fromBoolean(state.backgroundCollision);
            }
            MimicMotion.Update mimicUpdate =
                state.runtime.mimicMotion.advance(
                    state.entity,
                    state.frame,
                    state.link().linkPressedButtonsMask(),
                    state.romCollisionType,
                    mimicBackgroundInteraction,
                    state.slot().ignoreHits(slot),
                    state.frame);
            state.updated = mimicUpdate.entity();
            state.preserveMimicPresentation = true;
          }
          return true;
        });
  }
}
