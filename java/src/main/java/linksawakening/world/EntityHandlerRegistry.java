package linksawakening.world;

import java.util.Objects;
import java.util.function.IntSupplier;

/** ROM type-byte dispatch; unregistered types remain explicitly unsupported. */
final class EntityHandlerRegistry {
    private final boolean[] advancing = new boolean[256];
    private final EntityHandler[] handlers = new EntityHandler[256];

    void register(int type, EntityHandler handler) {
        Objects.checkIndex(type, handlers.length);
        if (handlers[type] != null) throw new IllegalArgumentException("Duplicate entity type: " + type);
        handlers[type] = Objects.requireNonNull(handler);
    }

    boolean supports(int type) {
        return type >= 0 && type < handlers.length && handlers[type] != null;
    }

    void initialize(RoomEntity entity, IntSupplier randomByteSupplier) {
        if (supports(entity.type())) handlers[entity.type()].initialize(entity, randomByteSupplier);
    }
    void registerAdvance(int type, java.util.function.BiFunction<RoomEntity, EntityFrameContext, RoomEntity> advance) {
        Objects.checkIndex(type, handlers.length);
        EntityHandler initializer = handlers[type];
        advancing[type] = true;
        Objects.requireNonNull(advance);
        handlers[type] = new EntityHandler() {
            @Override public void initialize(RoomEntity entity, IntSupplier random) {
                if (initializer != null) initializer.initialize(entity, random);
            }
            @Override public RoomEntity advance(RoomEntity entity, EntityFrameContext context) {
                return advance.apply(entity, context);
            }
        };
    }

    boolean hasAdvance(int type) { return type >= 0 && type < advancing.length && advancing[type]; }

    RoomEntity advance(RoomEntity entity, EntityFrameContext context) {
        return supports(entity.type()) ? handlers[entity.type()].advance(entity, context) : entity;
    }

}
