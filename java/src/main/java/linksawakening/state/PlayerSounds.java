package linksawakening.state;

import static linksawakening.state.PlayerNumbers.*;
import static linksawakening.state.PlayerState.*;

/** PlayerSounds owns its player-state fields and their update rules. */
public final class PlayerSounds {
    final java.util.List<linksawakening.gameplay.GameplaySoundEvent> resourceSounds = new java.util.ArrayList<>();

    public linksawakening.gameplay.GameplaySoundEvent takeResourceSound() {
        var sound = resourceSounds.isEmpty() ? null : resourceSounds.get(resourceSounds.size() - 1);
        resourceSounds.clear();
        return sound;
    }
}
