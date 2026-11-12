package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_MINI_MOLDORM;
import static linksawakening.world.entity.EntitySnapshots.withDefinition;
import static linksawakening.world.entity.EntitySnapshots.withVariant;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.EnemyRecoilMotion;
import linksawakening.world.entity.motion.MiniMoldormMotion;

/** ROM handler stages for MiniMoldorm. */
final class MiniMoldormHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_MINI_MOLDORM,
                EntityTickStage.PHASE_2_AFTER_RECOIL_APPLICATION,
                0,
                MiniMoldormHandler::phase2MiniMoldorm);
        registry.registerTick(
                ENTITY_MINI_MOLDORM,
                EntityTickStage.PHASE_2_AFTER_INITIALIZE,
                4,
                MiniMoldormHandler::phase2MiniMoldorm2);
    }

    private static boolean phase2MiniMoldorm(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var presentationFlags = state.presentation();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            int slot = slotState.entity().slot();
            // RenderMiniMoldorm records the history before the bank-$04
            // recoil helper runs, so keep this ordering explicit.
            state.motion(MiniMoldormMotion.class, MiniMoldormMotion::new)
                    .beginFrame(slotState.entity(), state.slot().ignoreHits(slot) != 0);
            EnemyRecoilMotion.Update recoil =
                    state.background().applyEnemyRecoilIfNeeded(slotState.entity(), frameInput.backgroundCollision());
            slotState.entity(recoil.entity());
            slotState.updated(slotState.entity());
            RoomEntityBackgroundInteraction miniBackgroundInteraction =
                    state.background().interaction();
            if (miniBackgroundInteraction == null && frameInput.backgroundCollision() != null) {
                miniBackgroundInteraction = frameInput.hinoxBackgroundInteraction();
            }
            int transitionForMotion = state.slot().transitionCountdown(slot);
            if (transitionForMotion != 0) {
                // The common runtime timer has already performed the ROM
                // pre-handler decrement; MiniMoldormMotion mirrors that
                // decrement for its direct source-derived API as well.
                transitionForMotion = (transitionForMotion + 1) & 0xFF;
            }
            MiniMoldormMotion.Update miniUpdate = state.motion(MiniMoldormMotion.class, MiniMoldormMotion::new)
                    .advanceAfterHistory(
                            slotState.entity(),
                            transitionForMotion,
                            state.slot().ignoreHits(slot),
                            state.slot().flashCountdown(slot),
                            miniBackgroundInteraction,
                            frameInput.randomByteSupplier(),
                            frameInput.frame());
            slotState.updated(miniUpdate.entity());
            state.slot().setTransitionCountdown(slot, miniUpdate.transitionCountdown());
            if (state.sprites() != null) {
                slotState.updated(withDefinition(
                        slotState.updated(),
                        state.sprites()
                                .forMiniMoldormState(
                                        miniUpdate.headSpriteVariant(),
                                        slotState.updated().x(),
                                        (slotState.updated().y()
                                                        - slotState.updated().z())
                                                & 0xFF,
                                        miniUpdate.segment1X(),
                                        miniUpdate.segment1Y(),
                                        miniUpdate.segment2X(),
                                        miniUpdate.segment2Y()),
                        0));
            } else if (slotState.updated().spriteDefinition().supported()
                    && miniUpdate.headSpriteVariant()
                            < slotState.updated().spriteDefinition().variantCount()) {
                slotState.updated(withVariant(slotState.updated(), miniUpdate.headSpriteVariant()));
            }
            presentationFlags.preserveMiniMoldormPresentation(true);
        }
        return true;
    }

    private static boolean phase2MiniMoldorm2(EntityFrameExecution state) {
        var slotState = state.slotFrame();

        state.motion(MiniMoldormMotion.class, MiniMoldormMotion::new).initialize(slotState.entity());

        return true;
    }
}
