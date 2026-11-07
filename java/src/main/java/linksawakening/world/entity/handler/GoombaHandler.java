package linksawakening.world.entity.handler;

import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;

import linksawakening.world.entity.motion.GoombaMotion;

/** ROM handler stages for Goomba. */
final class GoombaHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_GOOMBA,
        EntityTickStage.PHASE_3_GOOMBA,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            GoombaMotion.Update goombaUpdate =
                state.goombaMotion().advance(
                    state.entity,
                    state.frame,
                    state.linkEntityX,
                    state.linkEntityY,
                    state.randomByteSupplier,
                    state.backgroundCollision,
                    state.slot().transitionCountdown(state.entity.slot()),
                    state.background().groundInteractionSideScrolling());
            state.updated = goombaUpdate.entity();
            state.slot().setTransitionCountdown(state.entity.slot(), goombaUpdate.transitionCountdown());
            if (goombaUpdate.startDying()) {
              // func_007_666B writes ENTITY_DROPPABLE_HEART and the
              // private-countdown-3 death window before setting status
              // DYING and physics flags to 4. Keep both the source
              // table and the runtime death countdown synchronized;
              // the latter drives the shared Java death presentation.
              state.runtime.droppedItemBySlot[state.entity.slot()] = 0x2D;
              state.runtime.dropPrivateCountdown3[state.entity.slot()] = 0x0C;
              state.runtime.dyingCountdown[state.entity.slot()] = 0x0C;
              state.slot().setPhysicsFlags(state.entity.slot(), 0x04);
              state.status = EntityStatus.DYING;
            }
          }
          return true;
        });
  }
}
