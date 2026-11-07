package linksawakening.world.entity.handler;

import linksawakening.world.RoomEntityBackgroundInteraction;
import linksawakening.world.EntityTickStage;
import linksawakening.world.RoomEntityRuntime;
import linksawakening.world.EntityStatus;
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
                    state.runtime.linkPressedButtonsMask,
                    state.romCollisionType,
                    mimicBackgroundInteraction,
                    state.runtime.enemyIgnoreHitsCountdown[slot],
                    state.frame);
            state.updated = mimicUpdate.entity();
            state.preserveMimicPresentation = true;
          }
          return true;
        });
  }
}
