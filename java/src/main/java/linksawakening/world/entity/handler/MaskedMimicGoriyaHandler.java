package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_MASKED_MIMIC_GORIYA;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.MaskedMimicFrame;
import linksawakening.world.entity.motion.MaskedMimicMotion;

/** ROM handler stages for MaskedMimicGoriya. */
final class MaskedMimicGoriyaHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_MASKED_MIMIC_GORIYA,
                EntityTickStage.PHASE_2_AFTER_RECOIL_APPLICATION,
                3,
                MaskedMimicGoriyaHandler::phase2MaskedMimicGoriya);
    }

    private static boolean phase2MaskedMimicGoriya(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var presentationFlags = state.presentation();

        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && state.roomInspection().entityMapId() != 0x1F) {
            int slot = slotState.entity().slot();
            RoomEntityBackgroundInteraction maskedMimicBackgroundInteraction =
                    state.background().interaction();
            if (maskedMimicBackgroundInteraction == null && frameInput.backgroundCollision() != null) {
                maskedMimicBackgroundInteraction =
                        RoomEntityBackgroundInteraction.fromBoolean(frameInput.backgroundCollision());
            }
            MaskedMimicMotion.Update maskedMimicUpdate = state.motion(MaskedMimicMotion.class, MaskedMimicMotion::new)
                    .advance(new MaskedMimicFrame(
                            slotState.entity(),
                            frameInput.linkEntityX(),
                            frameInput.linkEntityY(),
                            frameInput.romLinkDirection(),
                            state.link().linkPressedButtonsMask(),
                            frameInput.romCollisionType(),
                            maskedMimicBackgroundInteraction,
                            state.slot().ignoreHits(slot),
                            frameInput.frame()));
            slotState.updated(maskedMimicUpdate.entity());
            state.slot().setOptions1Override(slot, maskedMimicUpdate.options1());
            presentationFlags.preserveMaskedMimicPresentation(true);
        }
        return true;
    }
}
