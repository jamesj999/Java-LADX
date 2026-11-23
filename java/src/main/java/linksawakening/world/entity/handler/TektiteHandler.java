package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_TEKTITE;

import java.util.function.IntSupplier;
import linksawakening.world.entity.EntityFrameContext;
import linksawakening.world.entity.EntityHandlerRegistry;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.motion.TektiteMotion;

/** ROM initialization and motion dispatch. */
final class TektiteHandler {
    static void install(EntityHandlerRegistry registry) {
        registry.registerInitializer(ENTITY_TEKTITE, (entity, random) -> initialize(registry, entity, random));
        registry.registerAdvance(ENTITY_TEKTITE, (entity, context) -> advance(registry, entity, context));
    }

    private static void initialize(EntityHandlerRegistry registry, RoomEntity entity, IntSupplier random) {
        registry.motion(TektiteMotion.class, TektiteMotion::new).initialize(entity.slot(), random);
    }

    private static RoomEntity advance(EntityHandlerRegistry registry, RoomEntity entity, EntityFrameContext context) {
        return registry.motion(TektiteMotion.class, TektiteMotion::new)
                .advance(
                        entity,
                        context.linkEntityX(),
                        context.linkEntityY(),
                        context.randomByteSupplier(),
                        context.backgroundCollision());
    }
}
