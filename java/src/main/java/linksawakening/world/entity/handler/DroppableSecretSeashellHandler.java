package linksawakening.world.entity.handler;

import linksawakening.world.EntityTickStage;
import linksawakening.world.RoomEntityRuntime;
import linksawakening.world.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.RoomEntityObjectSample;







import linksawakening.world.entity.motion.SecretSeashellMotion;



/** ROM handler stages for DroppableSecretSeashell. */
final class DroppableSecretSeashellHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_DROPPABLE_SECRET_SEASHELL,
        EntityTickStage.PHASE_2_DROPPABLE_SECRET_SEASHELL,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            if (state.runtime.chestSwordLevel >= 0x02
                || (state.runtime.entityRoomStatus & 0x10) != 0
                || (state.runtime.entityRoomId == 0xE3
                    && (state.runtime.entityRoomStatus & 0x40) == 0)) {
              // DroppableSeashellEntityHandler uses UnloadEntityAndReturn
              // for these gates, which deliberately does not mark the
              // source load-order bit as completed.
              state.lifecycle().disableEntityWithoutPersistence(state.entity.slot());
              return false;
            }
            RoomEntityObjectSample objectUnderEntity =
                state.runtime.objectQuery == null
                    ? null
                    : state.runtime.objectQuery.sample(state.entity);
            SecretSeashellMotion.Update shellUpdate =
                state.runtime.secretSeashellMotion.advance(
                    state.entity,
                    state.runtime.entityRoomId,
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
            state.runtime.entityOptions1Override[state.entity.slot()] =
                state.runtime.secretSeashellMotion.privateState3(state.entity.slot()) != 0
                    ? state.runtime.SECRET_SEASHELL_HIDDEN_OPTIONS1
                    : state.runtime.SECRET_SEASHELL_REVEALED_OPTIONS1;
            state.preserveSecretSeashellPresentation = true;
          }
          return true;
        });
  }
}
