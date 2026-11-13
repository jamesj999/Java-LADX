package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_BUTTERFLY;

import linksawakening.world.entity.EntityFrameContext;
import linksawakening.world.entity.EntityHandlerRegistry;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.motion.ButterflyMotion;

/** ROM initialization and motion dispatch. */
final class ButterflyHandler {
    static void install(EntityHandlerRegistry registry) {
        registry.registerAdvance(ENTITY_BUTTERFLY, (entity, context) -> advance(registry, entity, context));
    }

    private static RoomEntity advance(EntityHandlerRegistry registry, RoomEntity entity, EntityFrameContext context) {
        return registry.motion(ButterflyMotion.class, ButterflyMotion::new)
                .advance(
                        entity,
                        context.frameCounter(),
                        context.linkEntityX(),
                        context.linkEntityY(),
                        context.randomByteSupplier());
    }
}
