package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_WIZROBE_PROJECTILE;
import static linksawakening.world.entity.EntitySnapshots.withFlipAttribute;

import java.util.List;
import linksawakening.world.entity.EnemyProjectileCollision;
import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistry;
import linksawakening.world.entity.EntityPresentationFlags;
import linksawakening.world.entity.EntitySlotExecution;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityObjectCollision;
import linksawakening.world.entity.event.EntityProjectileEvent;
import linksawakening.world.entity.motion.WizrobeProjectileMotion;

/** ROM handler stages for WizrobeProjectile. */
final class WizrobeProjectileHandler {
    static void install(EntityHandlerRegistry registry) {
        registry.registerTick(
                ENTITY_WIZROBE_PROJECTILE,
                EntityTickStage.PHASE_2_AFTER_INITIALIZE,
                7,
                WizrobeProjectileHandler::phase2WizrobeProjectile);
        registry.registerTick(
                ENTITY_WIZROBE_PROJECTILE,
                EntityTickStage.PHASE_3_BEFORE_LASER,
                15,
                WizrobeProjectileHandler::phase3WizrobeProjectile);
    }

    private static boolean phase2WizrobeProjectile(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var presentationFlags = state.presentation();

        state.motion(WizrobeProjectileMotion.class, WizrobeProjectileMotion::new)
                .initialize(slotState.entity().slot(), frameInput.randomByteSupplier());
        presentationFlags.preserveWizrobeProjectilePresentation(true);

        return true;
    }

    private static boolean phase3WizrobeProjectile(EntityFrameExecution state) {
        var motion = state.motion(WizrobeProjectileMotion.class, WizrobeProjectileMotion::new);
        var slotState = state.slotFrame();
        if (slotState.status() != EntityStatus.ACTIVE || slotState.wasInitializing()) {
            return true;
        }

        var inputs = state.inputs();
        if (advanceProjectile(
                slotState,
                motion,
                inputs.frame(),
                inputs.projectileLinkState(),
                state.slotProperties().contactDamage(slotState.entity().slot()),
                inputs.projectileEvents(),
                inputs.objectCollision(),
                state.presentation())) {
            state.lifecycle().disableEntityWithoutPersistence(slotState.entity().slot());
            return false;
        }

        return true;
    }

    private static boolean advanceProjectile(
            EntitySlotExecution slotState,
            WizrobeProjectileMotion motion,
            int frame,
            EnemyProjectileCollision.LinkState link,
            int contactDamage,
            List<EntityProjectileEvent> events,
            RoomEntityObjectCollision objectCollision,
            EntityPresentationFlags presentation) {
        RoomEntity projectile = slotState.entity();
        var collision =
                EnemyProjectileCollision.check(projectile, motion.direction(projectile.slot()), link, contactDamage);
        if (collision.isPresent()) {
            events.add(collision.orElseThrow());
        }
        slotState.updated(motion.advance(projectile));
        slotState.updated(withFlipAttribute(slotState.updated(), motion.flipAttribute(frame)));
        presentation.preserveWizrobeProjectilePresentation(true);
        return objectCollision.collides(slotState.updated())
                || collision.map(EntityProjectileEvent::remove).orElse(false);
    }
}
