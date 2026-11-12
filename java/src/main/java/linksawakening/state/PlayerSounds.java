package linksawakening.state;

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
}
