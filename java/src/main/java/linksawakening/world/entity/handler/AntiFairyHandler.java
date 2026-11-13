package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_ANTI_FAIRY;

import java.util.function.IntSupplier;
import linksawakening.world.entity.EntityFrameContext;
import linksawakening.world.entity.EntityHandlerRegistry;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.motion.AntiFairyMotion;

/** ROM initialization and motion dispatch. */
final class AntiFairyHandler {
    static void install(EntityHandlerRegistry registry) {
        registry.registerInitializer(ENTITY_ANTI_FAIRY, (entity, random) -> initialize(registry, entity, random));
        registry.registerAdvance(ENTITY_ANTI_FAIRY, (entity, context) -> advance(registry, entity, context));
    }

    private static void initialize(EntityHandlerRegistry registry, RoomEntity entity, IntSupplier random) {
        registry.motion(AntiFairyMotion.class, AntiFairyMotion::new).initialize(entity.slot(), random);
    }

    private static RoomEntity advance(EntityHandlerRegistry registry, RoomEntity entity, EntityFrameContext context) {
        return registry.motion(AntiFairyMotion.class, AntiFairyMotion::new)
                .advance(entity, context.frameCounter(), context.backgroundCollision(), context.randomByteSupplier());
    }
}
