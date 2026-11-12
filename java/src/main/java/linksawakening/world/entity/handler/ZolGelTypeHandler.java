package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.*;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.ZolGelFrame;
import linksawakening.world.entity.motion.ZolGelMotion;

/** ROM handler stages for ZolGelType. */
final class ZolGelTypeHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_ZOL, EntityTickStage.PHASE_2_AFTER_INITIALIZE, 3, ZolGelTypeHandler::phase2ZolGelType);
        registry.registerTick(
                ENTITY_GEL, EntityTickStage.PHASE_2_AFTER_INITIALIZE, 3, ZolGelTypeHandler::phase2ZolGelType);
        registry.registerTick(ENTITY_ZOL, EntityTickStage.PHASE_3_BEFORE_LASER, 5, ZolGelTypeHandler::phase3ZolGelType);
        registry.registerTick(ENTITY_GEL, EntityTickStage.PHASE_3_BEFORE_LASER, 5, ZolGelTypeHandler::phase3ZolGelType);
    }

    private static boolean phase2ZolGelType(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        state.motion(ZolGelMotion.class, ZolGelMotion::new)
                .initialize(slotState.entity().slot(), slotState.entity().type(), frameInput.randomByteSupplier());
        if (slotState.entity().type() == 0x1B) {
            // EntityInitZol ($03:4953) overrides health-group zero's
            // default one HP. The first ordinary sword hit must survive
            // long enough for AnimateZolGel's flash-$08 split.
            state.slot().setHealth(slotState.entity().slot(), 2);
        }

        return true;
    }

    private static boolean phase3ZolGelType(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();

        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && timerGates.handlerInteractive()) {
            RoomEntityBackgroundInteraction zolGelBackgroundInteraction =
                    state.background().interaction();
            if (zolGelBackgroundInteraction == null && frameInput.backgroundCollision() != null) {
                zolGelBackgroundInteraction =
                        RoomEntityBackgroundInteraction.fromBoolean(frameInput.backgroundCollision());
            }
            ZolGelMotion.Update zolGelUpdate = state.motion(ZolGelMotion.class, ZolGelMotion::new)
                    .advance(new ZolGelFrame(
                            slotState.entity(),
                            frameInput.linkEntityX(),
                            frameInput.linkEntityY(),
                            frameInput.linkZ(),
                            frameInput.randomByteSupplier(),
                            zolGelBackgroundInteraction,
                            frameInput.frame(),
                            state.slot().ignoreHits(slotState.entity().slot()),
                            state.link().joypadHeld() || state.link().actionButtonsHeld()));
            slotState.updated(zolGelUpdate.entity());
            state.lifecycle().recordGelClinging(zolGelUpdate.clinging());
            if (zolGelUpdate.split() != null) {
                slotState.updated(
                        state.lifecycle().applyZolSplit(slotState.entity(), slotState.updated(), zolGelUpdate.split()));
            }
        }
        return true;
    }
}
