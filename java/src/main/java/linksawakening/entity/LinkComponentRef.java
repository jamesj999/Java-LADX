package linksawakening.entity;

/** Assembly-time cell for mutually dependent components; bound once before a Link escapes. */
final class LinkComponentRef<T> {
    private T value;

    void bind(T value) {
        if (this.value != null) throw new IllegalStateException("Component already bound");
        this.value = java.util.Objects.requireNonNull(value);
    }

    T get() {
        return value;
    }
}
