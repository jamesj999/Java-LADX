package linksawakening.world.entity.handler;

import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;

import linksawakening.world.entity.motion.ColorShellMotion;

import static linksawakening.world.entity.EntitySnapshots.withStatus;
import static linksawakening.world.entity.EntityTypeRules.persistentClearMask;

/** ROM handler stages for ColorShellType. */
final class ColorShellTypeHandler {
  static void install(EntityHandlerRegistration registry) {
    for (int type = 0; type < 256; type++) {
      if ((ColorShellMotion.isColorShellType(type))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_COLOR_SHELL_TYPE,
            state -> {
              if (state.runtime.entityMapId == state.runtime.MAP_COLOR_DUNGEON) {
                state.runtime.colorShellMotion.enterState6(state.entity);
                RoomEntity activeShell =
                    state.runtime.slotProperties.refreshColorShellDisplay(
                        withStatus(state.entity, EntityStatus.ACTIVE), EntityStatus.ACTIVE);
                state.replaceEntity(activeShell);
                return false;
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if (((ColorShellMotion.isColorShellType(type)))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_COLOR_SHELL_TYPE_2,
            state -> {
              {
                state.runtime.colorShellMotion.initialize(state.entity.slot());
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if ((ColorShellMotion.isColorShellType(type))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_5_COLOR_SHELL_TYPE,
            state -> {
              if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
                ColorShellMotion.Update shellUpdate =
                    state.runtime.colorShellMotion.advance(
                        state.entity,
                        state.frame,
                        state.linkEntityX,
                        state.linkEntityY,
                        state.randomByteSupplier,
                        state.backgroundCollision,
                        state.slot().ignoreHits(state.entity.slot()),
                        state.runtime.colorShellWorld,
                        state.entities());
                state.updated = shellUpdate.entity();
                state.slot().setIgnoreHits(state.entity.slot(), shellUpdate.nextIgnoreHitsCountdown());
                if (shellUpdate.unloadRequested()) {
                  state.runtime.pendingClearedEntityMask |= persistentClearMask(state.entity);
                  state.lifecycle().disableEntityWithoutPersistence(state.entity.slot());
                  return false;
                }
              }
              return true;
            });
      }
    }
  }
}
