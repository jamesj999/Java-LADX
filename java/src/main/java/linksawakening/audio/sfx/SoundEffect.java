package linksawakening.audio.sfx;

import java.util.Objects;

/** Named sound request with its ROM namespace, identifier and source. */
public record SoundEffect(SoundEffectNamespace namespace, int id, String name, String source) {
    public SoundEffect {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(source, "source");
        if (id < 0 || id > 0xFF) {
            throw new IllegalArgumentException("id out of range: " + id);
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }
}
