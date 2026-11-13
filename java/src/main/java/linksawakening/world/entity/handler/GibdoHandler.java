package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_GIBDO;

import java.util.function.IntSupplier;
import linksawakening.world.entity.EntityFrameContext;
import linksawakening.world.entity.EntityHandlerRegistry;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.motion.GibdoMotion;

/** ROM initialization and motion dispatch. */
final class GibdoHandler {
    static void install(EntityHandlerRegistry registry) {
        registry.registerInitializer(ENTITY_GIBDO, (entity, random) -> initialize(registry, entity, random));
        registry.registerAdvance(ENTITY_GIBDO, (entity, context) -> advance(registry, entity, context));
    }

    private static void initialize(EntityHandlerRegistry registry, RoomEntity entity, IntSupplier random) {
        registry.motion(GibdoMotion.class, GibdoMotion::new).initialize(entity.slot());
    }

    private static RoomEntity advance(EntityHandlerRegistry registry, RoomEntity entity, EntityFrameContext context) {
        return registry.motion(GibdoMotion.class, GibdoMotion::new)
                .advance(entity, context.randomByteSupplier(), context.backgroundCollision());
    }
}
