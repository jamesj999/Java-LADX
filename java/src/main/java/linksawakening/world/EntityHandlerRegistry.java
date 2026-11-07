package linksawakening.world;

import java.util.Objects;
import java.util.function.IntSupplier;

/** ROM type-byte dispatch; unregistered types remain explicitly unsupported. */
final class EntityHandlerRegistry {
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
}
