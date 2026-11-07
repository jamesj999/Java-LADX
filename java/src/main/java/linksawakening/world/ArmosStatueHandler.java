package linksawakening.world;



/** ROM handler stages for ArmosStatue. */
final class ArmosStatueHandler {
  static void install(EntityHandlerRegistry registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_ARMOS_STATUE,
        EntityTickStage.PHASE_3_ARMOS_STATUE,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            RoomEntityBackgroundInteraction armosBackgroundInteraction =
                state.runtime.backgroundInteraction;
            if (armosBackgroundInteraction == null && state.backgroundCollision != null) {
              armosBackgroundInteraction =
                  RoomEntityBackgroundInteraction.fromBoolean(state.backgroundCollision);
            }
            ArmosMotion.Update armosUpdate =
                state.runtime.armosMotion.advance(
                    state.entity,
                    state.frame,
                    state.linkEntityX,
                    state.linkEntityY,
                    state.randomByteSupplier,
                    state.handlerLinkCollisionEnabled,
                    armosBackgroundInteraction,
                    state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()]);
            state.updated = armosUpdate.entity();
            if (armosUpdate.linkFinalPositionCopyRequested()
                && state.projectileLinkState.motionState()
                    < EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE) {
              state.runtime.backgroundMechanics.requestLinkPush(
                  state.entity, EntityLinkCollisionRules.RESTORE_ONLY);
            }
            if (armosUpdate.woke()) {
              // ArmosStatueEntityHandler's wake branch starts the same
              // $18 visible flash used by the ROM's enemy hit handlers.
              state.runtime.enemyFlashCountdown[state.entity.slot()] = 0x18;
            }
            if (armosUpdate.activated()) {
              // State 1 clears the harmless physics bit and enables the
              // normal hitbox/sword path for state 2.
              state.runtime.enemyPhysicsFlags[state.entity.slot()] &= 0x7F;
              state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()] = 0;
            }
          }
          return true;
        });
  }
}
