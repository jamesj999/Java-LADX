package linksawakening.state;

import java.util.function.Function;

/** Last ROM resource sound write awaiting presentation. */
public final class PlayerSounds {
    private ResourceSound pending;

    void write(ResourceSound sound) {
        pending = sound;
    }

    void clear() {
        pending = null;
    }

    public ResourceSound takeResourceSound() {
        ResourceSound sound = pending;
        pending = null;
        return sound;
    }

    /** Consumes the pending sound and maps it at the presentation boundary; no request yields null. */
    public <T> T takeResourceSound(Function<ResourceSound, T> presentation) {
        ResourceSound sound = takeResourceSound();
        return sound == null ? null : presentation.apply(sound);
    }
}
