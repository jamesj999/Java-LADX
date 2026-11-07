package linksawakening.world.entity.handler;

import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.EnemyProjectileCollision;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityLinkCollisionRules;

import linksawakening.world.entity.motion.ArmosMotion;

/** ROM handler stages for ArmosStatue. */
final class ArmosStatueHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_ARMOS_STATUE,
        EntityTickStage.PHASE_3_ARMOS_STATUE,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            RoomEntityBackgroundInteraction armosBackgroundInteraction =
                state.background().interaction();
            if (armosBackgroundInteraction == null && state.backgroundCollision != null) {
              armosBackgroundInteraction =
                  RoomEntityBackgroundInteraction.fromBoolean(state.backgroundCollision);
            }
            ArmosMotion.Update armosUpdate =
                state.armosMotion().advance(
                    state.entity,
                    state.frame,
                    state.linkEntityX,
                    state.linkEntityY,
                    state.randomByteSupplier,
                    state.handlerLinkCollisionEnabled,
                    armosBackgroundInteraction,
                    state.slot().ignoreHits(state.entity.slot()));
            state.updated = armosUpdate.entity();
            if (armosUpdate.linkFinalPositionCopyRequested()
                && state.projectileLinkState.motionState()
                    < EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE) {
              state.background().requestLinkPush(
                  state.entity, EntityLinkCollisionRules.RESTORE_ONLY);
            }
            if (armosUpdate.woke()) {
              // ArmosStatueEntityHandler's wake branch starts the same
              // $18 visible flash used by the ROM's enemy hit handlers.
              state.slot().setFlashCountdown(state.entity.slot(), 0x18);
            }
            if (armosUpdate.activated()) {
              // State 1 clears the harmless physics bit and enables the
              // normal hitbox/sword path for state 2.
              state.slot().andPhysicsFlags(state.entity.slot(), 0x7F);
              state.slot().setIgnoreHits(state.entity.slot(), 0);
            }
          }
          return true;
        });
  }
}
