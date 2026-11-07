package linksawakening.world.entity.handler;

import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;

import linksawakening.world.entity.motion.ZombieMotion;

import static linksawakening.world.entity.EntitySnapshots.withVariant;

/** ROM handler stages for Zombie. */
final class ZombieHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_ZOMBIE,
        EntityTickStage.PHASE_2_ZOMBIE,
        state -> {
          if (state.wasInitializing) {
            // The room-loaded Zombie is only a hidden spawn marker; its
            // bank-$18 handler never renders the parent entity.
            state.updated = withVariant(state.entity, -1);
            state.preserveZombiePresentation = true;
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_ZOMBIE,
        EntityTickStage.PHASE_4_ZOMBIE,
        state -> {
          if (state.status == EntityStatus.ACTIVE
              && !state.wasInitializing

              && !state.creditsGameplay
              && state.handlerLinkCollisionEnabled) {
            int slot = state.entity.slot();
            RoomEntityBackgroundInteraction zombieBackgroundInteraction =
                state.background().interaction();
            if (zombieBackgroundInteraction == null && state.backgroundCollision != null) {
              zombieBackgroundInteraction =
                  RoomEntityBackgroundInteraction.fromBoolean(state.backgroundCollision);
            }
            ZombieMotion.Update zombieUpdate =
                state.zombieMotion().advance(
                    state.updated,
                    state.frame,
                    state.slot().transitionCountdown(slot),
                    state.slot().physicsFlags(slot),
                    state.linkEntityX,
                    state.linkEntityY,
                    state.randomByteSupplier,
                    state.background().objectQuery(),
                    zombieBackgroundInteraction,
                    state.slot().ignoreHits(slot));
            state.updated = zombieUpdate.entity();
            state.slot().setTransitionCountdown(slot, zombieUpdate.transitionCountdown());
            state.slot().setPhysicsFlags(slot, zombieUpdate.physicsFlags());
            state.applyGenericGroundInteraction = zombieUpdate.appliesBackgroundInteraction();
            if (zombieUpdate.spawnRequest() != null) {
              state.enemySpawns().spawnZombieChild(zombieUpdate.spawnRequest());
            }
            if (zombieUpdate.unloadRequested()) {
              state.runtime.clearEntity(slot);
              return false;
            }
            state.preserveZombiePresentation = true;
          }
          return true;
        });
  }
}
