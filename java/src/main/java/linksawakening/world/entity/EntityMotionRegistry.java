package linksawakening.world.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/** One motion instance per type and room, created on its first use. */
final class EntityMotionRegistry {
    private final Map<Class<?>, Object> motions = new HashMap<>();

    <T> T get(Class<T> type, Supplier<? extends T> factory) {
        Object motion = motions.get(type);
        if (motion == null) {
            motion = Objects.requireNonNull(factory.get(), "motion");
            motions.put(type, type.cast(motion));
        }
        return type.cast(motion);
    }
}
