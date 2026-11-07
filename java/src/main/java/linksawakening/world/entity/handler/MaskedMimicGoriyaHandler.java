package linksawakening.world.entity.handler;

import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EntityStatus;
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
                state.background().interaction();
            if (maskedMimicBackgroundInteraction == null && state.backgroundCollision != null) {
              maskedMimicBackgroundInteraction =
                  RoomEntityBackgroundInteraction.fromBoolean(state.backgroundCollision);
            }
            MaskedMimicMotion.Update maskedMimicUpdate =
                state.maskedMimicMotion().advance(
                    state.entity,
                    state.linkEntityX,
                    state.linkEntityY,
                    state.romLinkDirection,
                    state.link().linkPressedButtonsMask(),
                    state.romCollisionType,
                    maskedMimicBackgroundInteraction,
                    state.slot().ignoreHits(slot),
                    state.frame);
            state.updated = maskedMimicUpdate.entity();
            state.slot().setOptions1Override(slot, maskedMimicUpdate.options1());
            state.preserveMaskedMimicPresentation = true;
          }
          return true;
        });
  }
}
