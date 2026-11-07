package linksawakening.world;

import static linksawakening.world.EntitySnapshots.withVariant;

/** ROM handler stages for GopongaFlower. */
final class GopongaFlowerHandler {
  static void install(EntityHandlerRegistry registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_GOPONGA_FLOWER,
        EntityTickStage.PHASE_4_GOPONGA_FLOWER,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            if (GopongaFlowerMotion.overlapsInteractiveLink(
                state.entity,
                state.linkEntityX,
                state.linkEntityY,
                state.handlerLinkCollisionEnabled)) {
              state.runtime.backgroundMechanics.requestLinkPush(
                  state.entity, EntityLinkCollisionRules.STANDARD_PUSH);
            }
            state.updated =
                withVariant(state.updated, GopongaFlowerMotion.frameVariant(state.frame));
            state.preserveGopongaFlowerPresentation = true;
          }
          return true;
        });
  }
}
