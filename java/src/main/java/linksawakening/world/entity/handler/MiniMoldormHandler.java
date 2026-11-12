package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_MINI_MOLDORM;
import static linksawakening.world.entity.EntitySnapshots.withDefinition;
import static linksawakening.world.entity.EntitySnapshots.withVariant;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;
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
        if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            int slot = state.entity.slot();
            // RenderMiniMoldorm records the history before the bank-$04
            // recoil helper runs, so keep this ordering explicit.
            state.miniMoldormMotion().beginFrame(state.entity, state.slot().ignoreHits(slot) != 0);
            EnemyRecoilMotion.Update recoil =
                    state.background().applyEnemyRecoilIfNeeded(state.entity, state.backgroundCollision);
            state.entity = recoil.entity();
            state.updated = state.entity;
            RoomEntityBackgroundInteraction miniBackgroundInteraction =
                    state.background().interaction();
            if (miniBackgroundInteraction == null && state.backgroundCollision != null) {
                miniBackgroundInteraction = RoomEntityBackgroundInteraction.fromBoolean(state.backgroundCollision);
            }
            int transitionForMotion = state.slot().transitionCountdown(slot);
            if (transitionForMotion != 0) {
                // The common runtime timer has already performed the ROM
                // pre-handler decrement; MiniMoldormMotion mirrors that
                // decrement for its direct source-derived API as well.
                transitionForMotion = (transitionForMotion + 1) & 0xFF;
            }
            MiniMoldormMotion.Update miniUpdate = state.miniMoldormMotion()
                    .advanceAfterHistory(
                            state.entity,
                            transitionForMotion,
                            state.slot().ignoreHits(slot),
                            state.slot().flashCountdown(slot),
                            miniBackgroundInteraction,
                            state.randomByteSupplier,
                            state.frame);
            state.updated = miniUpdate.entity();
            state.slot().setTransitionCountdown(slot, miniUpdate.transitionCountdown());
            if (state.sprites() != null) {
                state.updated = withDefinition(
                        state.updated,
                        state.sprites()
                                .forMiniMoldormState(
                                        miniUpdate.headSpriteVariant(),
                                        state.updated.x(),
                                        (state.updated.y() - state.updated.z()) & 0xFF,
                                        miniUpdate.segment1X(),
                                        miniUpdate.segment1Y(),
                                        miniUpdate.segment2X(),
                                        miniUpdate.segment2Y()),
                        0);
            } else if (state.updated.spriteDefinition().supported()
                    && miniUpdate.headSpriteVariant()
                            < state.updated.spriteDefinition().variantCount()) {
                state.updated = withVariant(state.updated, miniUpdate.headSpriteVariant());
            }
            state.preserveMiniMoldormPresentation = true;
        }
        return true;
    }

    private static boolean phase2MiniMoldorm2(EntityFrameExecution state) {
        state.miniMoldormMotion().initialize(state.entity);

        return true;
    }
}
