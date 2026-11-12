package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.*;

import java.util.function.IntSupplier;
import linksawakening.world.entity.*;
import linksawakening.world.entity.motion.*;

/** ROM initialization and motion dispatch. */
final class AntiFairyHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerInitializer(ENTITY_ANTI_FAIRY, (entity, random) -> initialize(registry, entity, random));
        registry.registerAdvance(ENTITY_ANTI_FAIRY, (entity, context) -> advance(registry, entity, context));
    }

    private static void initialize(EntityHandlerRegistration registry, RoomEntity entity, IntSupplier random) {
        registry.motion(AntiFairyMotion.class, AntiFairyMotion::new).initialize(entity.slot(), random);
    }

    private static RoomEntity advance(
            EntityHandlerRegistration registry, RoomEntity entity, EntityFrameContext context) {
        return registry.motion(AntiFairyMotion.class, AntiFairyMotion::new)
                .advance(entity, context.frameCounter(), context.backgroundCollision(), context.randomByteSupplier());
    }
}
