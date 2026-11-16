package linksawakening.state;

import java.util.function.Function;

/** Last ROM resource sound write awaiting presentation. */
public final class PlayerSounds {
    // The presence bit is independent of the last ROM sound byte; no new enum value changes switches.
    private ResourceSound pending = ResourceSound.RUPEE;
    private boolean pendingSound;

    public boolean hasPendingSound() {
        return pendingSound;
    }

    public ResourceSound takeResourceSound() {
        ResourceSound sound = pendingSound ? pending : null;
        pendingSound = false;
        return sound;
    }

    /** Consumes the pending sound and maps it at the presentation boundary; no request yields null. */
    public <T> T takeResourceSound(Function<ResourceSound, T> presentation) {
        if (!pendingSound) return null;
        ResourceSound sound = pending;
        pendingSound = false;
        return presentation.apply(sound);
    }

    void write(ResourceSound sound) {
        pending = sound;
        pendingSound = true;
    }

    void clear() {
        pendingSound = false;
    }
}
