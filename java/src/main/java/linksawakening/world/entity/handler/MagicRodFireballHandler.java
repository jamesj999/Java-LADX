package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_MAGIC_ROD_FIREBALL;


import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;

/** ROM handler stages for MagicRodFireball. */
final class MagicRodFireballHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        ENTITY_MAGIC_ROD_FIREBALL,
        EntityTickStage.PHASE_1_MAGIC_ROD_FIREBALL,
        state -> {
          if (state.status == EntityStatus.ACTIVE) {
            state.projectiles().advanceMagicRodFireballEntity(
                state.index,
                state.entity,
                state.frame,
                state.handlerInteractive,
                state.projectileTimersInteractive);
            return false;
          }
          return true;
        });
  }
}
