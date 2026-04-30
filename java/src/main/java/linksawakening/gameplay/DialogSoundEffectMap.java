package linksawakening.gameplay;

import linksawakening.audio.sfx.SoundEffect;
import linksawakening.audio.sfx.SoundEffectCatalog;
import linksawakening.audio.sfx.SoundEffectNamespace;
import linksawakening.dialog.DialogController;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class DialogSoundEffectMap {
    private final Map<DialogController.SoundEvent, SoundEffect> effects;

    private DialogSoundEffectMap(Map<DialogController.SoundEvent, SoundEffect> effects) {
        this.effects = new EnumMap<>(Objects.requireNonNull(effects, "effects"));
    }

    public static DialogSoundEffectMap fromCatalog(SoundEffectCatalog catalog) {
        Objects.requireNonNull(catalog, "catalog");
        Map<DialogController.SoundEvent, SoundEffect> effects =
                new EnumMap<>(DialogController.SoundEvent.class);
        put(effects, catalog, DialogController.SoundEvent.TEXT_PRINT, SoundEffectNamespace.WAVE, 0x0F);
        put(effects, catalog, DialogController.SoundEvent.DIALOG_BREAK, SoundEffectNamespace.JINGLE, 0x15);
        put(effects, catalog, DialogController.SoundEvent.MOVE_SELECTION, SoundEffectNamespace.JINGLE, 0x0A);
        return new DialogSoundEffectMap(effects);
    }

    public Optional<SoundEffect> resolve(DialogController.SoundEvent event) {
        if (event == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(effects.get(event));
    }

    private static void put(
            Map<DialogController.SoundEvent, SoundEffect> effects,
            SoundEffectCatalog catalog,
            DialogController.SoundEvent event,
            SoundEffectNamespace namespace,
            int id) {
        effects.put(event, catalog.find(namespace, id).orElseThrow(
                () -> new IllegalStateException("Missing dialog sound effect: " + namespace + " 0x"
                        + Integer.toHexString(id).toUpperCase())));
    }
}
