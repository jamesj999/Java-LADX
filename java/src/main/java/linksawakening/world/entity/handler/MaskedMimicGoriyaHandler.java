package linksawakening.world.entity.handler;

import linksawakening.world.RoomEntityBackgroundInteraction;
import linksawakening.world.EntityTickStage;
import linksawakening.world.RoomEntityRuntime;
import linksawakening.world.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;







import linksawakening.world.entity.motion.MaskedMimicMotion;



/** ROM handler stages for MaskedMimicGoriya. */
final class MaskedMimicGoriyaHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_MASKED_MIMIC_GORIYA,
        EntityTickStage.PHASE_2_MASKED_MIMIC_GORIYA,
        state -> {
          if (state.status == EntityStatus.ACTIVE
              && !state.wasInitializing

              && state.runtime.entityMapId != 0x1F) {
            int slot = state.entity.slot();
            RoomEntityBackgroundInteraction maskedMimicBackgroundInteraction =
                state.runtime.backgroundInteraction;
            if (maskedMimicBackgroundInteraction == null && state.backgroundCollision != null) {
              maskedMimicBackgroundInteraction =
                  RoomEntityBackgroundInteraction.fromBoolean(state.backgroundCollision);
            }
            MaskedMimicMotion.Update maskedMimicUpdate =
                state.runtime.maskedMimicMotion.advance(
                    state.entity,
                    state.linkEntityX,
                    state.linkEntityY,
                    state.romLinkDirection,
                    state.runtime.linkPressedButtonsMask,
                    state.romCollisionType,
                    maskedMimicBackgroundInteraction,
                    state.runtime.enemyIgnoreHitsCountdown[slot],
                    state.frame);
            state.updated = maskedMimicUpdate.entity();
            state.runtime.entityOptions1Override[slot] = maskedMimicUpdate.options1();
            state.preserveMaskedMimicPresentation = true;
          }
          return true;
        });
  }
}
