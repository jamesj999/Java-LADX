package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_MAGIC_ROD_FIREBALL;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;

/** ROM handler stages for MagicRodFireball. */
final class MagicRodFireballHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_MAGIC_ROD_FIREBALL,
                EntityTickStage.PHASE_1_BEFORE_PICKUP_SEQUENCE,
                8,
                MagicRodFireballHandler::phase1MagicRodFireball);
    }

    private static boolean phase1MagicRodFireball(EntityFrameExecution state) {
        if (state.status == EntityStatus.ACTIVE) {
            state.projectiles()
                    .advanceMagicRodFireballEntity(
                            state.index,
                            state.entity,
                            state.frame,
                            state.handlerInteractive,
                            state.projectileTimersInteractive);
            return false;
        }
        return true;
    }
}
