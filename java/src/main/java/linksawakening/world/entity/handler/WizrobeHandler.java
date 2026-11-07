package linksawakening.world.entity.handler;

import linksawakening.world.EntityTickStage;
import linksawakening.world.RoomEntityRuntime;
import linksawakening.world.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;







import linksawakening.world.entity.motion.WizrobeMotion;

import static linksawakening.world.EntitySnapshots.withVariant;

/** ROM handler stages for Wizrobe. */
final class WizrobeHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_WIZROBE,
        EntityTickStage.PHASE_2_WIZROBE,
        state -> {
          if (state.wasInitializing) {
            // EntityInitWizrobe decrements the initial display-list variant
            // before the first active handler dispatch.
            state.updated = withVariant(state.entity, -1);
          }
          return true;
        });
    for (int type = 0; type < 256; type++) {
      if (((type == RoomEntityRuntime.ENTITY_WIZROBE))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_WIZROBE_2,
            state -> {
              {
                state.runtime.wizrobeMotion.initializeFromRoom(
                    state.entity.slot(), state.entity.spriteVariant());
                state.runtime.enemyTransitionCountdown[state.entity.slot()] = 0x80;
                state.preserveWizrobePresentation = true;
              }
              return true;
            });
      }
    }
    registry.registerTick(
        RoomEntityRuntime.ENTITY_WIZROBE,
        EntityTickStage.PHASE_3_WIZROBE,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            WizrobeMotion.Update wizrobeUpdate =
                state.runtime.wizrobeMotion.advance(
                    state.entity,
                    state.linkEntityX,
                    state.linkEntityY,
                    state.randomByteSupplier,
                    state.runtime.enemyTransitionCountdown[state.entity.slot()],
                    state.runtime.enemyPhysicsFlags[state.entity.slot()]);
            state.updated = wizrobeUpdate.entity();
            state.runtime.enemyTransitionCountdown[state.entity.slot()] =
                wizrobeUpdate.transitionCountdown();
            state.runtime.enemyPhysicsFlags[state.entity.slot()] = wizrobeUpdate.physicsFlags();
            state.preserveWizrobePresentation = true;
            if (wizrobeUpdate.projectileSpawn() != null) {
              state.enemySpawns().spawnWizrobeProjectile(
                  state.updated, wizrobeUpdate.projectileSpawn());
            }
          }
          return true;
        });
  }
}
