package linksawakening.world.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/** One motion instance per type and room, created on its first use. */
final class EntityMotionRegistry {
    private enum Entry {
        UNCREATED
    }

    private final Map<Class<?>, Object> motions = new HashMap<>();

    <T> T get(Class<T> type, Supplier<? extends T> factory) {
        return get(type, factory, Supplier::get);
    }

    <T, C> T get(Class<T> type, C context, Function<? super C, ? extends T> factory) {
        Object motion = motions.getOrDefault(type, Entry.UNCREATED);
        if (motion == Entry.UNCREATED) {
            motion = Objects.requireNonNull(factory.apply(context), "motion");
            motions.put(type, type.cast(motion));
        }
        return type.cast(motion);
    }
}
