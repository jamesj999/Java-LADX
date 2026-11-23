package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_GOPONGA_FLOWER_PROJECTILE;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistry;
import linksawakening.world.entity.EntitySlotView;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.motion.GopongaProjectileMotion;

/** ROM handler stages for GopongaFlowerProjectile. */
final class GopongaFlowerProjectileHandler {
    static void install(EntityHandlerRegistry registry) {
        registry.registerTick(
                ENTITY_GOPONGA_FLOWER_PROJECTILE,
                EntityTickStage.PHASE_3_AFTER_LASER,
                1,
                GopongaFlowerProjectileHandler::phase3GopongaFlowerProjectile);
    }

    private static boolean phase3GopongaFlowerProjectile(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        if (slotState.status() != EntityStatus.ACTIVE || slotState.wasInitializing()) {
            return true;
        }

        RoomEntity projectile = slotState.entity();
        GopongaProjectileMotion.Update update = advanceProjectile(
                projectile,
                state.motion(GopongaProjectileMotion.class, GopongaProjectileMotion::new),
                state.inputs().frame(),
                state.gates().handlerLinkCollisionEnabled(),
                state.slot());
        if (update.unloadRequested()) {
            state.lifecycle().disableEntityWithoutPersistence(projectile.slot());
            return false;
        }

        slotState.updated(update.entity());
        state.presentation().preserveGopongaProjectilePresentation(true);
        return true;
    }

    private static GopongaProjectileMotion.Update advanceProjectile(
            RoomEntity projectile,
            GopongaProjectileMotion motion,
            int frame,
            boolean linkCollisionEnabled,
            EntitySlotView slots) {
        int slot = projectile.slot();
        GopongaProjectileMotion.Update update = motion.advance(
                projectile, frame, slots.transitionCountdown(slot), slots.ignoreHits(slot), linkCollisionEnabled);
        slots.setHealth(slot, GopongaProjectileMotion.HEALTH_OVERRIDE);
        slots.setTransitionCountdown(slot, update.transitionCountdown());
        slots.setIgnoreHits(slot, update.ignoreHitsCountdown());
        return update;
    }
}
