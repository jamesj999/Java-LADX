package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_DROPPABLE_SECRET_SEASHELL;
import static linksawakening.world.entity.EntityRomConstants.SECRET_SEASHELL_HIDDEN_OPTIONS1;
import static linksawakening.world.entity.EntityRomConstants.SECRET_SEASHELL_REVEALED_OPTIONS1;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityObjectSample;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.SecretSeashellMotion;

/** ROM handler stages for DroppableSecretSeashell. */
final class DroppableSecretSeashellHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_DROPPABLE_SECRET_SEASHELL,
                EntityTickStage.PHASE_2_BEFORE_RECOIL_APPLICATION,
                2,
                DroppableSecretSeashellHandler::phase2DroppableSecretSeashell);
    }

    private static boolean phase2DroppableSecretSeashell(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var presentationFlags = state.presentation();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            if (state.roomInspection().chestSwordLevel() >= 0x02
                    || (state.roomInspection().entityRoomStatus() & 0x10) != 0
                    || (state.roomInspection().entityRoomId() == 0xE3
                            && (state.roomInspection().entityRoomStatus() & 0x40) == 0)) {
                // DroppableSeashellEntityHandler uses UnloadEntityAndReturn
                // for these gates, which deliberately does not mark the
                // source load-order bit as completed.
                state.lifecycle()
                        .disableEntityWithoutPersistence(slotState.entity().slot());
                return false;
            }
            RoomEntityObjectSample objectUnderEntity = state.background().objectQuery() == null
                    ? null
                    : state.background().objectQuery().sample(slotState.entity());
            SecretSeashellMotion.Update shellUpdate = state.motion(
                            SecretSeashellMotion.class, SecretSeashellMotion::new)
                    .advance(
                            slotState.entity(),
                            state.roomInspection().entityRoomId(),
                            frameInput.frame(),
                            false,
                            state.rewards().secretSeashellScreenShakeActive(),
                            state.rewards().secretSeashellPegasusCollisionActive(),
                            state.rewards().secretSeashellPegasusCollisionCoordinatesKnown(),
                            state.rewards().secretSeashellPegasusCollisionX(),
                            state.rewards().secretSeashellPegasusCollisionY(),
                            frameInput.linkEntityX(),
                            frameInput.linkEntityY(),
                            objectUnderEntity);
            slotState.updated(shellUpdate.entity());
            state.slot()
                    .setOptions1Override(
                            slotState.entity().slot(),
                            state.motion(SecretSeashellMotion.class, SecretSeashellMotion::new)
                                                    .privateState3(
                                                            slotState.entity().slot())
                                            != 0
                                    ? SECRET_SEASHELL_HIDDEN_OPTIONS1
                                    : SECRET_SEASHELL_REVEALED_OPTIONS1);
            presentationFlags.preserveSecretSeashellPresentation(true);
        }
        return true;
    }
}
