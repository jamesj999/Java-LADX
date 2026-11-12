package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.*;

import linksawakening.world.entity.*;
import linksawakening.world.entity.motion.*;

/** ROM initialization and motion dispatch. */
final class ButterflyHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerAdvance(ENTITY_BUTTERFLY, (entity, context) -> advance(registry, entity, context));
    }

    private static RoomEntity advance(
            EntityHandlerRegistration registry, RoomEntity entity, EntityFrameContext context) {
        return registry.motion(ButterflyMotion.class, ButterflyMotion::new)
                .advance(
                        entity,
                        context.frameCounter(),
                        context.linkEntityX(),
                        context.linkEntityY(),
                        context.randomByteSupplier());
    }
}
