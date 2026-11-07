package linksawakening.world.entity.handler;

import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;

import linksawakening.world.entity.motion.ZolGelMotion;

import static linksawakening.world.entity.EntityTypeRules.isZolGelType;

/** ROM handler stages for ZolGelType. */
final class ZolGelTypeHandler {
  static void install(EntityHandlerRegistration registry) {
    for (int type = 0; type < 256; type++) {
      if (((isZolGelType(type)))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_ZOL_GEL_TYPE,
            state -> {
              {
                state.runtime.zolGelMotion.initialize(
                    state.entity.slot(), state.entity.type(), state.randomByteSupplier);
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if ((isZolGelType(type))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_3_ZOL_GEL_TYPE,
            state -> {
              if (state.status == EntityStatus.ACTIVE
                  && !state.wasInitializing
                  && state.handlerInteractive
                 ) {
                RoomEntityBackgroundInteraction zolGelBackgroundInteraction =
                    state.runtime.backgroundInteraction;
                if (zolGelBackgroundInteraction == null && state.backgroundCollision != null) {
                  zolGelBackgroundInteraction =
                      RoomEntityBackgroundInteraction.fromBoolean(state.backgroundCollision);
                }
                ZolGelMotion.Update zolGelUpdate =
                    state.runtime.zolGelMotion.advance(
                        state.entity,
                        state.linkEntityX,
                        state.linkEntityY,
                        state.linkZ,
                        state.randomByteSupplier,
                        zolGelBackgroundInteraction,
                        state.frame,
                        state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()],
                        state.runtime.joypadHeld || state.runtime.actionButtonsHeld);
                state.updated = zolGelUpdate.entity();
                state.runtime.gelClingingToLink |= zolGelUpdate.clinging();
                if (zolGelUpdate.split() != null) {
                  state.updated =
                      state.lifecycle().applyZolSplit(
                          state.entity, state.updated, zolGelUpdate.split());
                }
              }
              return true;
            });
      }
    }
  }
}
