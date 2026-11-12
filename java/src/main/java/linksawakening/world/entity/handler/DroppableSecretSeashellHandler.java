package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_DROPPABLE_SECRET_SEASHELL;
import static linksawakening.world.entity.EntityRomConstants.SECRET_SEASHELL_HIDDEN_OPTIONS1;
import static linksawakening.world.entity.EntityRomConstants.SECRET_SEASHELL_REVEALED_OPTIONS1;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityObjectSample;
import linksawakening.world.entity.motion.SecretSeashellMotion;

/** ROM handler stages for DroppableSecretSeashell. */
final class DroppableSecretSeashellHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_DROPPABLE_SECRET_SEASHELL,
                EntityTickStage.PHASE_2_DROPPABLE_SECRET_SEASHELL,
                DroppableSecretSeashellHandler::phase2DroppableSecretSeashell);
    }

    private static boolean phase2DroppableSecretSeashell(EntityFrameExecution state) {
        if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            if (state.roomInspection().chestSwordLevel() >= 0x02
                    || (state.roomInspection().entityRoomStatus() & 0x10) != 0
                    || (state.roomInspection().entityRoomId() == 0xE3
                            && (state.roomInspection().entityRoomStatus() & 0x40) == 0)) {
                // DroppableSeashellEntityHandler uses UnloadEntityAndReturn
                // for these gates, which deliberately does not mark the
                // source load-order bit as completed.
                state.lifecycle().disableEntityWithoutPersistence(state.entity.slot());
                return false;
            }
            RoomEntityObjectSample objectUnderEntity = state.background().objectQuery() == null
                    ? null
                    : state.background().objectQuery().sample(state.entity);
            SecretSeashellMotion.Update shellUpdate = state.secretSeashellMotion()
                    .advance(
                            state.entity,
                            state.roomInspection().entityRoomId(),
                            state.frame,
                            false,
                            state.rewards().secretSeashellScreenShakeActive(),
                            state.rewards().secretSeashellPegasusCollisionActive(),
                            state.rewards().secretSeashellPegasusCollisionCoordinatesKnown(),
                            state.rewards().secretSeashellPegasusCollisionX(),
                            state.rewards().secretSeashellPegasusCollisionY(),
                            state.linkEntityX,
                            state.linkEntityY,
                            objectUnderEntity);
            state.updated = shellUpdate.entity();
            state.slot()
                    .setOptions1Override(
                            state.entity.slot(),
                            state.secretSeashellMotion().privateState3(state.entity.slot()) != 0
                                    ? SECRET_SEASHELL_HIDDEN_OPTIONS1
                                    : SECRET_SEASHELL_REVEALED_OPTIONS1);
            state.preserveSecretSeashellPresentation = true;
        }
        return true;
    }
}
