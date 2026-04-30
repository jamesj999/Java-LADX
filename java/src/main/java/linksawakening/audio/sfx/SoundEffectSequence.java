package linksawakening.audio.sfx;

import java.util.List;
import java.util.Objects;

public final class SoundEffectSequence {
    private final SoundEffect effect;
    private final List<SoundEffectStep> steps;

    public SoundEffectSequence(SoundEffect effect, List<SoundEffectStep> steps) {
        this.effect = Objects.requireNonNull(effect, "effect");
        this.steps = List.copyOf(Objects.requireNonNull(steps, "steps"));
        if (this.steps.isEmpty()) {
            throw new IllegalArgumentException("steps must not be empty");
        }
    }

    public SoundEffect effect() {
        return effect;
    }

    public List<SoundEffectStep> steps() {
        return steps;
    }
}
