package linksawakening.entity;

import java.util.Objects;

/** Assembly-time cell for mutually dependent components; bound once before a Link escapes. */
final class LinkComponentRef<T> {
    private T value;

    void bind(T value) {
        if (this.value != null) throw new IllegalStateException("Component already bound");
        this.value = Objects.requireNonNull(value);
    }

    T get() {
        return value;
    }
}
