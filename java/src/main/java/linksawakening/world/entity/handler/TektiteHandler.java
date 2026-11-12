package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.*;

import java.util.function.IntSupplier;
import linksawakening.world.entity.*;
import linksawakening.world.entity.motion.*;

/** ROM initialization and motion dispatch. */
final class TektiteHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerInitializer(ENTITY_TEKTITE, (entity, random) -> initialize(registry, entity, random));
        registry.registerAdvance(ENTITY_TEKTITE, (entity, context) -> advance(registry, entity, context));
    }

    private static void initialize(EntityHandlerRegistration registry, RoomEntity entity, IntSupplier random) {
        registry.motion(TektiteMotion.class, TektiteMotion::new).initialize(entity.slot(), random);
    }

    private static RoomEntity advance(
            EntityHandlerRegistration registry, RoomEntity entity, EntityFrameContext context) {
        return registry.motion(TektiteMotion.class, TektiteMotion::new)
                .advance(
                        entity,
                        context.linkEntityX(),
                        context.linkEntityY(),
                        context.randomByteSupplier(),
                        context.backgroundCollision());
    }
}
