package linksawakening.world.entity.handler;

import linksawakening.world.EntityTickStage;
import linksawakening.world.EnemyProjectileCollision;
import linksawakening.world.RoomEntityRuntime;
import linksawakening.world.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.EntityProjectileEvent;







import static linksawakening.world.EntitySnapshots.withFlipAttribute;

/** ROM handler stages for WizrobeProjectile. */
final class WizrobeProjectileHandler {
  static void install(EntityHandlerRegistration registry) {
    for (int type = 0; type < 256; type++) {
      if (((type == RoomEntityRuntime.ENTITY_WIZROBE_PROJECTILE))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_WIZROBE_PROJECTILE,
            state -> {
              {
                state.runtime.wizrobeProjectileMotion.initialize(
                    state.entity.slot(), state.randomByteSupplier);
                state.preserveWizrobeProjectilePresentation = true;
              }
              return true;
            });
      }
    }
    registry.registerTick(
        RoomEntityRuntime.ENTITY_WIZROBE_PROJECTILE,
        EntityTickStage.PHASE_3_WIZROBE_PROJECTILE,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            var collisionEvent =
                EnemyProjectileCollision.check(
                    state.entity,
                    state.runtime.wizrobeProjectileMotion.direction(state.entity.slot()),
                    state.projectileLinkState);
            if (collisionEvent.isPresent()) {
              state.projectileEvents.add(collisionEvent.orElseThrow());
            }
            state.updated = state.runtime.wizrobeProjectileMotion.advance(state.entity);
            state.updated =
                withFlipAttribute(
                    state.updated,
                    state.runtime.wizrobeProjectileMotion.flipAttribute(state.frame));
            state.preserveWizrobeProjectilePresentation = true;
            boolean objectCollisionHit =
                state.objectCollision != null && state.objectCollision.collides(state.updated);
            if (objectCollisionHit
                || collisionEvent.map(EntityProjectileEvent::remove).orElse(false)) {
              state.lifecycle().disableEntityWithoutPersistence(state.entity.slot());
              return false;
            }
          }
          return true;
        });
  }
}
