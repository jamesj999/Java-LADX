package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.withStatus;
import static linksawakening.world.entity.EntitySnapshots.withVariant;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityRuntime.CommonDroppableUpdate;

/** ROM handler stages for CommonDroppableType. */
final class CommonDroppableTypeHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_DROPPABLE_HEART,
                EntityTickStage.PHASE_2_BEFORE_RECOIL,
                2,
                CommonDroppableTypeHandler::phase2CommonDroppableType);
        registry.registerTick(
                ENTITY_DROPPABLE_RUPEE,
                EntityTickStage.PHASE_2_BEFORE_RECOIL,
                2,
                CommonDroppableTypeHandler::phase2CommonDroppableType);
        registry.registerTick(
                ENTITY_DROPPABLE_FAIRY,
                EntityTickStage.PHASE_2_BEFORE_RECOIL,
                2,
                CommonDroppableTypeHandler::phase2CommonDroppableType);
        registry.registerTick(
                ENTITY_DROPPABLE_ARROWS,
                EntityTickStage.PHASE_2_BEFORE_RECOIL,
                2,
                CommonDroppableTypeHandler::phase2CommonDroppableType);
        registry.registerTick(
                ENTITY_DROPPABLE_BOMBS,
                EntityTickStage.PHASE_2_BEFORE_RECOIL,
                2,
                CommonDroppableTypeHandler::phase2CommonDroppableType);
        registry.registerTick(
                ENTITY_DROPPABLE_MAGIC_POWDER,
                EntityTickStage.PHASE_2_BEFORE_RECOIL,
                2,
                CommonDroppableTypeHandler::phase2CommonDroppableType);
        registry.registerTick(
                ENTITY_HIDING_SLIME_KEY,
                EntityTickStage.PHASE_2_BEFORE_RECOIL,
                2,
                CommonDroppableTypeHandler::phase2CommonDroppableType);
        registry.registerTick(
                ENTITY_DROPPABLE_HEART,
                EntityTickStage.PHASE_2_BEFORE_RECOIL_APPLICATION,
                4,
                CommonDroppableTypeHandler::phase2CommonDroppableType2);
        registry.registerTick(
                ENTITY_DROPPABLE_RUPEE,
                EntityTickStage.PHASE_2_BEFORE_RECOIL_APPLICATION,
                4,
                CommonDroppableTypeHandler::phase2CommonDroppableType2);
        registry.registerTick(
                ENTITY_DROPPABLE_FAIRY,
                EntityTickStage.PHASE_2_BEFORE_RECOIL_APPLICATION,
                4,
                CommonDroppableTypeHandler::phase2CommonDroppableType2);
        registry.registerTick(
                ENTITY_DROPPABLE_ARROWS,
                EntityTickStage.PHASE_2_BEFORE_RECOIL_APPLICATION,
                4,
                CommonDroppableTypeHandler::phase2CommonDroppableType2);
        registry.registerTick(
                ENTITY_DROPPABLE_BOMBS,
                EntityTickStage.PHASE_2_BEFORE_RECOIL_APPLICATION,
                4,
                CommonDroppableTypeHandler::phase2CommonDroppableType2);
        registry.registerTick(
                ENTITY_DROPPABLE_MAGIC_POWDER,
                EntityTickStage.PHASE_2_BEFORE_RECOIL_APPLICATION,
                4,
                CommonDroppableTypeHandler::phase2CommonDroppableType2);
        registry.registerTick(
                ENTITY_HIDING_SLIME_KEY,
                EntityTickStage.PHASE_2_BEFORE_RECOIL_APPLICATION,
                4,
                CommonDroppableTypeHandler::phase2CommonDroppableType2);
    }

    private static boolean phase2CommonDroppableType(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var presentationFlags = state.presentation();

        if (slotState.wasInitializing()
                && state.lifecycle().droppablePrivateState3(slotState.entity().slot()) != 0) {
            // The init handler has already set the hidden options. The
            // corresponding active handler will be entered on the next
            // entity tick, so suppress this first display-list pass too.
            slotState.updated(withVariant(slotState.entity(), -1));
            presentationFlags.preserveDroppablePresentation(true);
        }
        return true;
    }

    private static boolean phase2CommonDroppableType2(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            CommonDroppableUpdate droppableUpdate = state.pickups()
                    .advanceCommonDroppable(
                            slotState.entity(),
                            frameInput.linkEntityX(),
                            frameInput.linkEntityY(),
                            state.background().objectQuery() == null
                                    ? null
                                    : state.background().objectQuery().sample(slotState.entity()));
            slotState.updated(droppableUpdate.entity());
            if (droppableUpdate.hidden()) {
                // DroppableRevealOrReturnIfNeeded returns past the caller's
                // remaining handler. In particular, no rendering, pickup,
                // movement, or background interaction may run while hidden.
                state.replaceEntity(withStatus(withVariant(slotState.updated(), -1), slotState.status()));
                return false;
            }
        }
        return true;
    }
}
