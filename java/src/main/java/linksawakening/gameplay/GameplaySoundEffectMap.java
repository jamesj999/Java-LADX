package linksawakening.gameplay;

import linksawakening.audio.sfx.SoundEffect;
import linksawakening.audio.sfx.SoundEffectCatalog;
import linksawakening.audio.sfx.SoundEffectNamespace;

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
        put(effects, catalog, GameplaySoundEvent.ROC_FEATHER_JUMP, SoundEffectNamespace.JINGLE, 0x0D);
        put(effects, catalog, GameplaySoundEvent.PIT_FALL, SoundEffectNamespace.WAVE, 0x0C);
        put(effects, catalog, GameplaySoundEvent.SWORD_SWING_A, SoundEffectNamespace.NOISE, 0x02);
        put(effects, catalog, GameplaySoundEvent.SWORD_SWING_B, SoundEffectNamespace.NOISE, 0x14);
        put(effects, catalog, GameplaySoundEvent.SWORD_SWING_C, SoundEffectNamespace.NOISE, 0x15);
        put(effects, catalog, GameplaySoundEvent.SWORD_SWING_D, SoundEffectNamespace.NOISE, 0x18);
        put(effects, catalog, GameplaySoundEvent.SWORD_FULLY_CHARGED, SoundEffectNamespace.JINGLE, 0x04);
        put(effects, catalog, GameplaySoundEvent.SPIN_ATTACK, SoundEffectNamespace.NOISE, 0x03);
        put(effects, catalog, GameplaySoundEvent.CUT_GRASS, SoundEffectNamespace.NOISE, 0x05);
        put(effects, catalog, GameplaySoundEvent.INVENTORY_OPEN, SoundEffectNamespace.JINGLE, 0x11);
        put(effects, catalog, GameplaySoundEvent.INVENTORY_CLOSE, SoundEffectNamespace.JINGLE, 0x12);
        put(effects, catalog, GameplaySoundEvent.MENU_MOVE, SoundEffectNamespace.JINGLE, 0x0A);
        put(effects, catalog, GameplaySoundEvent.MENU_VALIDATE, SoundEffectNamespace.JINGLE, 0x13);
        return new GameplaySoundEffectMap(effects);
    }

    public Optional<SoundEffect> resolve(GameplaySoundEvent event) {
        if (event == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(effects.get(event));
    }

    private static void put(
            Map<GameplaySoundEvent, SoundEffect> effects,
            SoundEffectCatalog catalog,
            GameplaySoundEvent event,
            SoundEffectNamespace namespace,
            int id) {
        effects.put(event, catalog.find(namespace, id).orElseThrow(
                () -> new IllegalStateException("Missing gameplay sound effect: " + namespace + " 0x"
                        + Integer.toHexString(id).toUpperCase())));
    }
}
