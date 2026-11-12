package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.MAP_COLOR_DUNGEON;
import static linksawakening.world.entity.EntitySnapshots.withStatus;
import static linksawakening.world.entity.EntityTypeRules.persistentClearMask;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.motion.ColorShellMotion;

/** ROM handler stages for ColorShellType. */
final class ColorShellTypeHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(0xE9, EntityTickStage.PHASE_2_FALLING, 0, ColorShellTypeHandler::phase2ColorShellType);
        registry.registerTick(0xEA, EntityTickStage.PHASE_2_FALLING, 0, ColorShellTypeHandler::phase2ColorShellType);
        registry.registerTick(0xEB, EntityTickStage.PHASE_2_FALLING, 0, ColorShellTypeHandler::phase2ColorShellType);
        registry.registerTick(
                0xE9, EntityTickStage.PHASE_2_AFTER_INITIALIZE, 11, ColorShellTypeHandler::phase2ColorShellType2);
        registry.registerTick(
                0xEA, EntityTickStage.PHASE_2_AFTER_INITIALIZE, 11, ColorShellTypeHandler::phase2ColorShellType2);
        registry.registerTick(
                0xEB, EntityTickStage.PHASE_2_AFTER_INITIALIZE, 11, ColorShellTypeHandler::phase2ColorShellType2);
        registry.registerTick(
                0xE9, EntityTickStage.PHASE_5_BEFORE_COMMIT, 1, ColorShellTypeHandler::phase5ColorShellType);
        registry.registerTick(
                0xEA, EntityTickStage.PHASE_5_BEFORE_COMMIT, 1, ColorShellTypeHandler::phase5ColorShellType);
        registry.registerTick(
                0xEB, EntityTickStage.PHASE_5_BEFORE_COMMIT, 1, ColorShellTypeHandler::phase5ColorShellType);
    }

    private static boolean phase2ColorShellType(EntityFrameExecution state) {
        if (state.roomInspection().entityMapId() == MAP_COLOR_DUNGEON) {
            state.colorShellMotion().enterState6(state.entity);
            RoomEntity activeShell = state.slotProperties()
                    .refreshColorShellDisplay(withStatus(state.entity, EntityStatus.ACTIVE), EntityStatus.ACTIVE);
            state.replaceEntity(activeShell);
            return false;
        }
        return true;
    }

    private static boolean phase2ColorShellType2(EntityFrameExecution state) {
        state.colorShellMotion().initialize(state.entity.slot());

        return true;
    }

    private static boolean phase5ColorShellType(EntityFrameExecution state) {
        if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            ColorShellMotion.Update shellUpdate = state.colorShellMotion()
                    .advance(
                            state.entity,
                            state.frame,
                            state.linkEntityX,
                            state.linkEntityY,
                            state.randomByteSupplier,
                            state.backgroundCollision,
                            state.slot().ignoreHits(state.entity.slot()),
                            state.roomInspection().colorShellWorld(),
                            state.entities());
            state.updated = shellUpdate.entity();
            state.slot().setIgnoreHits(state.entity.slot(), shellUpdate.nextIgnoreHitsCountdown());
            if (shellUpdate.unloadRequested()) {
                state.lifecycle().requestPersistentClearMask(persistentClearMask(state.entity));
                state.lifecycle().disableEntityWithoutPersistence(state.entity.slot());
                return false;
            }
        }
        return true;
    }
}
