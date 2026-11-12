package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.*;

import java.util.function.IntSupplier;
import linksawakening.world.entity.*;
import linksawakening.world.entity.motion.*;

/** ROM initialization and motion dispatch. */
final class GibdoHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerInitializer(ENTITY_GIBDO, (entity, random) -> initialize(registry, entity, random));
        registry.registerAdvance(ENTITY_GIBDO, (entity, context) -> advance(registry, entity, context));
    }

    private static void initialize(EntityHandlerRegistration registry, RoomEntity entity, IntSupplier random) {
        registry.motion(GibdoMotion.class, GibdoMotion::new).initialize(entity.slot());
    }

    private static RoomEntity advance(
            EntityHandlerRegistration registry, RoomEntity entity, EntityFrameContext context) {
        return registry.motion(GibdoMotion.class, GibdoMotion::new)
                .advance(entity, context.randomByteSupplier(), context.backgroundCollision());
    }
}
