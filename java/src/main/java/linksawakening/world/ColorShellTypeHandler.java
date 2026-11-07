package linksawakening.world;

import java.util.Arrays;
import static linksawakening.world.EntitySnapshots.withStatus;
import static linksawakening.world.EntityTypeRules.persistentClearMask;

/** ROM handler stages for ColorShellType. */
final class ColorShellTypeHandler {
  static void installPhase2(EntityHandlerRegistry registry) {
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
                state.runtime.slots[state.index] = activeShell;
                return false;
              }
              return true;
            });
      }
    }
  }

  static void installPhase2Part2(EntityHandlerRegistry registry) {
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
  }

  static void installPhase5(EntityHandlerRegistry registry) {
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
                        state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()],
                        state.runtime.colorShellWorld,
                        Arrays.asList(state.runtime.slots));
                state.updated = shellUpdate.entity();
                state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()] =
                    shellUpdate.nextIgnoreHitsCountdown();
                if (shellUpdate.unloadRequested()) {
                  state.runtime.pendingClearedEntityMask |= persistentClearMask(state.entity);
                  state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
                  return false;
                }
              }
              return true;
            });
      }
    }
  }
}
