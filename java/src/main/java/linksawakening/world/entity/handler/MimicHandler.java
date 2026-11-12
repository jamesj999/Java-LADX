package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_MIMIC;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.MimicMotion;

/** ROM handler stages for Mimic. */
final class MimicHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_MIMIC, EntityTickStage.PHASE_2_AFTER_RECOIL_APPLICATION, 2, MimicHandler::phase2Mimic);
    }

    private static boolean phase2Mimic(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var presentationFlags = state.presentation();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            int slot = slotState.entity().slot();
            RoomEntityBackgroundInteraction mimicBackgroundInteraction =
                    state.background().interaction();
            if (mimicBackgroundInteraction == null && frameInput.backgroundCollision() != null) {
                mimicBackgroundInteraction =
                        RoomEntityBackgroundInteraction.fromBoolean(frameInput.backgroundCollision());
            }
            MimicMotion.Update mimicUpdate = state.motion(MimicMotion.class, MimicMotion::new)
                    .advance(
                            slotState.entity(),
                            frameInput.frame(),
                            state.link().linkPressedButtonsMask(),
                            frameInput.romCollisionType(),
                            mimicBackgroundInteraction,
                            state.slot().ignoreHits(slot),
                            frameInput.frame());
            slotState.updated(mimicUpdate.entity());
            presentationFlags.preserveMimicPresentation(true);
        }
        return true;
    }
}
