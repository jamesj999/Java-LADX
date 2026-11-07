package linksawakening.world;



/** ROM handler stages for Bomb. */
final class BombHandler {
  static void installPhase1(EntityHandlerRegistry registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_BOMB,
        EntityTickStage.PHASE_1_BOMB,
        state -> {
          if (state.status == EntityStatus.ACTIVE) {
            BombMotion.Decision bombDecision =
                state.runtime.bombInteractions.advanceBombEntity(
                    state.index,
                    state.entity,
                    state.projectileEvents,
                    state.linkEntityX,
                    state.linkEntityY,
                    state.projectileLinkState);
            state.entity = state.entityAt(state.index);
            if (bombDecision.phase() == BombMotion.Phase.NORMAL) {
              state.replaceEntity(state.runtime.bombPrivateState4[state.entity.slot()] != 0
                      ? state.runtime.liftThrow.advanceEnemyBombEntity(
                          state.entity, state.backgroundCollision, state.frame)
                      : state.runtime.liftThrow.advancePlacedBombEntity(
                          state.entity, state.backgroundCollision, state.frame));
            }
            state.runtime.liftThrow.tryLiftBombIfRequested(
                state.index,
                bombDecision,
                state.linkEntityX,
                state.linkEntityY,
                state.linkZ,
                state.romLinkDirection);
            return false;
          }
          return true;
        });
  }

  static void installPhase2(EntityHandlerRegistry registry) {
    for (int type = 0; type < 256; type++) {
      if (((type == RoomEntityRuntime.ENTITY_BOMB))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_BOMB,
            state -> {
              {
                BombMotion.Decision bombDecision =
                    state.runtime.bombInteractions.advanceBombEntity(
                        state.index,
                        state.entity,
                        state.projectileEvents,
                        state.linkEntityX,
                        state.linkEntityY,
                        state.projectileLinkState);
                state.entity = state.entityAt(state.index);
                if (bombDecision.unloadAfterPresentation()) {
                  return false;
                }
              }
              return true;
            });
      }
    }
  }

  static void installPhase2Part2(EntityHandlerRegistry registry) {
    for (int type = 0; type < 256; type++) {
      if (((type == RoomEntityRuntime.ENTITY_BOMB))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_BOMB_2,
            state -> {
              {
                BombMotion.Decision bombDecision =
                    state.runtime.bombInteractions.advanceBombEntity(
                        state.index,
                        state.entity,
                        state.projectileEvents,
                        state.linkEntityX,
                        state.linkEntityY,
                        state.projectileLinkState);
                state.entity = state.entityAt(state.index);
                if (bombDecision.unloadAfterPresentation()) {
                  return false;
                }
                state.entity =
                    state.runtime.bombInteractions.applyBombStunnedPostActiveWork(
                        state.entity, state.backgroundCollision, state.frame);
                state.replaceEntity(state.entity);
              }
              return true;
            });
      }
    }
  }
}
