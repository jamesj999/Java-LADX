package linksawakening.gameplay;

import linksawakening.audio.sfx.SoundEffect;
import linksawakening.audio.sfx.SoundEffectCatalog;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class GameplaySoundEffectMap {
    private final Map<GameplaySoundEvent, SoundEffect> effects;

    private GameplaySoundEffectMap(Map<GameplaySoundEvent, SoundEffect> effects) {
        this.effects = new EnumMap<>(Objects.requireNonNull(effects, "effects"));
    }

    public static GameplaySoundEffectMap fromCatalog(SoundEffectCatalog catalog) {
        Objects.requireNonNull(catalog, "catalog");
        Map<GameplaySoundEvent, SoundEffect> effects = new EnumMap<>(GameplaySoundEvent.class);
        for (GameplaySoundEvent event : GameplaySoundEvent.values()) {
            effects.put(event, catalog.find(event.namespace(), event.id()).orElseThrow(
                () -> new IllegalStateException("Missing gameplay sound effect: " + event)));
        }
        return new GameplaySoundEffectMap(effects);
    }

    public Optional<SoundEffect> resolve(GameplaySoundEvent event) {
        if (event == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(effects.get(event));
    }
}
