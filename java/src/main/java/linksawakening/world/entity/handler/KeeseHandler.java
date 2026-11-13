package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.*;

import java.util.function.IntSupplier;
import linksawakening.world.entity.*;
import linksawakening.world.entity.motion.*;

/** ROM initialization and motion dispatch. */
final class KeeseHandler {
    static void install(EntityHandlerRegistry registry) {
        registry.registerInitializer(ENTITY_KEESE, (entity, random) -> initialize(registry, entity, random));
        registry.registerAdvance(ENTITY_KEESE, (entity, context) -> advance(registry, entity, context));
    }

    private static void initialize(EntityHandlerRegistry registry, RoomEntity entity, IntSupplier random) {
        registry.motion(KeeseMotion.class, KeeseMotion::new).initialize(entity.slot(), random);
    }

    private static RoomEntity advance(EntityHandlerRegistry registry, RoomEntity entity, EntityFrameContext context) {
        return registry.motion(KeeseMotion.class, KeeseMotion::new)
                .advance(
                        entity,
                        context.frameCounter(),
                        context.linkEntityX(),
                        context.linkEntityY(),
                        context.randomByteSupplier());
    }
}
