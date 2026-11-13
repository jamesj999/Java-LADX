package linksawakening.entity;

import java.util.Objects;

/** Explicit unbound/bound assembly phase; a component is bound once before Link escapes. */
final class LinkComponentRef<T> {
    private static final Object UNBOUND = new Object();
    private Object binding = UNBOUND;

    void bind(T value) {
        if (binding != UNBOUND) throw new IllegalStateException("Component already bound");
        binding = Objects.requireNonNull(value, "value");
    }

    @SuppressWarnings("unchecked")
    T get() {
        if (binding == UNBOUND) throw new IllegalStateException("Component has not been bound");
        return (T) binding;
    }
}
