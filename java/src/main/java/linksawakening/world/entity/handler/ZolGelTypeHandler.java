package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.*;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.entity.motion.ZolGelMotion;

/** ROM handler stages for ZolGelType. */
final class ZolGelTypeHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(ENTITY_ZOL, EntityTickStage.PHASE_2_ZOL_GEL_TYPE, ZolGelTypeHandler::phase2ZolGelType);
        registry.registerTick(ENTITY_GEL, EntityTickStage.PHASE_2_ZOL_GEL_TYPE, ZolGelTypeHandler::phase2ZolGelType);
        registry.registerTick(ENTITY_ZOL, EntityTickStage.PHASE_3_ZOL_GEL_TYPE, ZolGelTypeHandler::phase3ZolGelType);
        registry.registerTick(ENTITY_GEL, EntityTickStage.PHASE_3_ZOL_GEL_TYPE, ZolGelTypeHandler::phase3ZolGelType);
    }

    private static boolean phase2ZolGelType(EntityFrameExecution state) {
        state.zolGelMotion().initialize(state.entity.slot(), state.entity.type(), state.randomByteSupplier);
        if (state.entity.type() == 0x1B) {
            // EntityInitZol ($03:4953) overrides health-group zero's
            // default one HP. The first ordinary sword hit must survive
            // long enough for AnimateZolGel's flash-$08 split.
            state.slot().setHealth(state.entity.slot(), 2);
        }

        return true;
    }

    private static boolean phase3ZolGelType(EntityFrameExecution state) {
        if (state.status == EntityStatus.ACTIVE && !state.wasInitializing && state.handlerInteractive) {
            RoomEntityBackgroundInteraction zolGelBackgroundInteraction =
                    state.background().interaction();
            if (zolGelBackgroundInteraction == null && state.backgroundCollision != null) {
                zolGelBackgroundInteraction = RoomEntityBackgroundInteraction.fromBoolean(state.backgroundCollision);
            }
            ZolGelMotion.Update zolGelUpdate = state.zolGelMotion()
                    .advance(
                            state.entity,
                            state.linkEntityX,
                            state.linkEntityY,
                            state.linkZ,
                            state.randomByteSupplier,
                            zolGelBackgroundInteraction,
                            state.frame,
                            state.slot().ignoreHits(state.entity.slot()),
                            state.link().joypadHeld() || state.link().actionButtonsHeld());
            state.updated = zolGelUpdate.entity();
            state.lifecycle().recordGelClinging(zolGelUpdate.clinging());
            if (zolGelUpdate.split() != null) {
                state.updated = state.lifecycle().applyZolSplit(state.entity, state.updated, zolGelUpdate.split());
            }
        }
        return true;
    }
}
