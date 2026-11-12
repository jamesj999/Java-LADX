package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_PAIRODD;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.PairoddMotion;

/** ROM handler stages for Pairodd. */
final class PairoddHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(ENTITY_PAIRODD, EntityTickStage.PHASE_3_PAIRODD, PairoddHandler::phase3Pairodd);
    }

    private static boolean phase3Pairodd(EntityFrameExecution state) {
        if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            PairoddMotion.Update pairoddUpdate = state.pairoddMotion()
                    .advance(
                            state.entity,
                            state.frame,
                            state.linkEntityX,
                            state.linkEntityY,
                            state.randomByteSupplier,
                            state.slot().flashCountdown(state.entity.slot()) > 0);
            state.updated = pairoddUpdate.entity();
            if (pairoddUpdate.spawnProjectile()) {
                state.enemySpawns().spawnPairoddProjectile(state.entity, state.linkEntityX, state.linkEntityY);
            }
            if (pairoddUpdate.teleportJingle()) {
                state.emit(new EntityCombatEvent(
                        state.entity.slot(),
                        state.entity.type(),
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        0x3C));
            }
        }
        return true;
    }
}
