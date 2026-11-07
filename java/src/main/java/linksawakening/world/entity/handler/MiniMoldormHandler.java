package linksawakening.world.entity.handler;

import linksawakening.world.RoomEntityBackgroundInteraction;
import linksawakening.world.EntityTickStage;
import linksawakening.world.RoomEntityRuntime;
import linksawakening.world.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;







import linksawakening.world.entity.motion.EnemyRecoilMotion;
import linksawakening.world.entity.motion.MiniMoldormMotion;

import static linksawakening.world.EntitySnapshots.withDefinition;
import static linksawakening.world.EntitySnapshots.withVariant;

/** ROM handler stages for MiniMoldorm. */
final class MiniMoldormHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_MINI_MOLDORM,
        EntityTickStage.PHASE_2_MINI_MOLDORM,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            int slot = state.entity.slot();
            // RenderMiniMoldorm records the history before the bank-$04
            // recoil helper runs, so keep this ordering explicit.
            state.runtime.miniMoldormMotion.beginFrame(
                state.entity, state.runtime.enemyIgnoreHitsCountdown[slot] != 0);
            EnemyRecoilMotion.Update recoil =
                state.runtime.backgroundMechanics.applyEnemyRecoilIfNeeded(
                    state.entity, state.backgroundCollision);
            state.entity = recoil.entity();
            state.updated = state.entity;
            RoomEntityBackgroundInteraction miniBackgroundInteraction =
                state.runtime.backgroundInteraction;
            if (miniBackgroundInteraction == null && state.backgroundCollision != null) {
              miniBackgroundInteraction =
                  RoomEntityBackgroundInteraction.fromBoolean(state.backgroundCollision);
            }
            int transitionForMotion = state.runtime.enemyTransitionCountdown[slot];
            if (transitionForMotion != 0) {
              // The common runtime timer has already performed the ROM
              // pre-handler decrement; MiniMoldormMotion mirrors that
              // decrement for its direct source-derived API as well.
              transitionForMotion = (transitionForMotion + 1) & 0xFF;
            }
            MiniMoldormMotion.Update miniUpdate =
                state.runtime.miniMoldormMotion.advanceAfterHistory(
                    state.entity,
                    transitionForMotion,
                    state.runtime.enemyIgnoreHitsCountdown[slot],
                    state.runtime.enemyFlashCountdown[slot],
                    miniBackgroundInteraction,
                    state.randomByteSupplier,
                    state.frame);
            state.updated = miniUpdate.entity();
            state.runtime.enemyTransitionCountdown[slot] = miniUpdate.transitionCountdown();
            if (state.runtime.spriteHandlers != null) {
              state.updated =
                  withDefinition(
                      state.updated,
                      state.runtime.spriteHandlers.forMiniMoldormState(
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
        });
    for (int type = 0; type < 256; type++) {
      if (((type == RoomEntityRuntime.ENTITY_MINI_MOLDORM))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_MINI_MOLDORM_2,
            state -> {
              {
                state.runtime.miniMoldormMotion.initialize(state.entity);
              }
              return true;
            });
      }
    }
  }
}
