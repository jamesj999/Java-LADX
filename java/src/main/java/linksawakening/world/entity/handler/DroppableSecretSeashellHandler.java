package linksawakening.world.entity.handler;

import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.RoomEntityObjectSample;

import linksawakening.world.entity.motion.SecretSeashellMotion;

/** ROM handler stages for DroppableSecretSeashell. */
final class DroppableSecretSeashellHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_DROPPABLE_SECRET_SEASHELL,
        EntityTickStage.PHASE_2_DROPPABLE_SECRET_SEASHELL,
        state -> {
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
            RoomEntityObjectSample objectUnderEntity =
                state.background().objectQuery() == null
                    ? null
                    : state.background().objectQuery().sample(state.entity);
            SecretSeashellMotion.Update shellUpdate =
                state.secretSeashellMotion().advance(
                    state.entity,
                    state.roomInspection().entityRoomId(),
                    state.frame,
                    false,
                    state.runtime.secretSeashellScreenShakeActive,
                    state.runtime.secretSeashellPegasusCollisionActive,
                    state.runtime.secretSeashellPegasusCollisionCoordinatesKnown,
                    state.runtime.secretSeashellPegasusCollisionX,
                    state.runtime.secretSeashellPegasusCollisionY,
                    state.linkEntityX,
                    state.linkEntityY,
                    objectUnderEntity);
            state.updated = shellUpdate.entity();
            state.slot().setOptions1Override(state.entity.slot(), state.secretSeashellMotion().privateState3(state.entity.slot()) != 0
                    ? state.runtime.SECRET_SEASHELL_HIDDEN_OPTIONS1
                    : state.runtime.SECRET_SEASHELL_REVEALED_OPTIONS1);
            state.preserveSecretSeashellPresentation = true;
          }
          return true;
        });
  }
}
