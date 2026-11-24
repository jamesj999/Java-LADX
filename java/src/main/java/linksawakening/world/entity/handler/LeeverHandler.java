package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_LEEVER;
import static linksawakening.world.entity.EntitySnapshots.withVariant;

import linksawakening.world.entity.EntityFrameContext;
import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistry;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.motion.LeeverMotion;

/** ROM handler stages for Leever. */
final class LeeverHandler {
    static void install(EntityHandlerRegistry registry) {
        registry.registerInitializer(ENTITY_LEEVER, (entity, random) -> initialize(registry, entity));
        registry.registerAdvance(ENTITY_LEEVER, (entity, context) -> advance(registry, entity, context));
        registry.registerTick(ENTITY_LEEVER, EntityTickStage.PHASE_1_AFTER_STATUS_TIMERS, 0, LeeverHandler::tickTimer);
        registry.registerTick(
                ENTITY_LEEVER, EntityTickStage.PHASE_2_BEFORE_RECOIL_APPLICATION, 5, LeeverHandler::allowsHandler);
        registry.registerTick(
                ENTITY_LEEVER, EntityTickStage.PHASE_2_AFTER_RECOIL_APPLICATION, 5, LeeverHandler::publishInitVariant);
    }

    private static boolean tickTimer(EntityFrameExecution frame) {
        // The shared transition timer ($14:4DA8) has its own gates, including ocarina.
        if (frame.gates().projectileTimersInteractive()) {
            frame.motion(LeeverMotion.class, LeeverMotion::new)
                    .tickTransitionCountdown(frame.slotFrame().entity().slot());
        }
        return true;
    }

    private static boolean allowsHandler(EntityFrameExecution frame) {
        // The active handler checks $04:7FA3 before recoil or regular movement.
        return frame.slotFrame().wasInitializing()
                || (frame.gates().handlerInteractive() && !frame.inputs().creditsGameplay());
    }

    private static boolean publishInitVariant(EntityFrameExecution frame) {
        var slotState = frame.slotFrame();
        if (slotState.wasInitializing()) {
            // EntityInitLeever calls SetEntitySpriteVariant($FF) before
            // the first active handler dispatch.
            slotState.updated(withVariant(slotState.entity(), -1));
        }
        return true;
    }

    private static void initialize(EntityHandlerRegistry registry, RoomEntity entity) {
        registry.motion(LeeverMotion.class, LeeverMotion::new).initialize(entity.slot());
    }

    private static RoomEntity advance(EntityHandlerRegistry registry, RoomEntity entity, EntityFrameContext context) {
        return registry.motion(LeeverMotion.class, LeeverMotion::new)
                .advance(
                        entity,
                        context.frameCounter(),
                        context.linkEntityX(),
                        context.linkEntityY(),
                        context.randomByteSupplier(),
                        context.backgroundCollision());
    }
}
