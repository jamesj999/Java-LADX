package linksawakening.audio.browser;

import linksawakening.audio.sfx.SoundEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class SoundEffectBrowserModel {
    private final List<SoundEffect> effects;

    public SoundEffectBrowserModel(List<SoundEffect> effects) {
        this.effects = List.copyOf(Objects.requireNonNull(effects, "effects"));
    }

    public List<SoundEffect> filteredEffects(String filter) {
        String normalizedFilter = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
        if (normalizedFilter.isEmpty()) {
            return effects;
        }

        List<SoundEffect> filtered = new ArrayList<>();
        for (SoundEffect effect : effects) {
            String id = hexByte(effect.id()).toLowerCase(Locale.ROOT);
            String name = effect.name().toLowerCase(Locale.ROOT);
            String namespace = effect.namespace().name().toLowerCase(Locale.ROOT);
            if (id.contains(normalizedFilter)
                    || name.contains(normalizedFilter)
                    || namespace.contains(normalizedFilter)) {
                filtered.add(effect);
            }
        }
        return filtered;
    }

    public String metadata(SoundEffect effect) {
        Objects.requireNonNull(effect, "effect");
        return effect.namespace().name() + " " + hexByte(effect.id())
                + System.lineSeparator()
                + effect.name()
                + System.lineSeparator()
                + effect.namespace().ladxRegisterName()
                + System.lineSeparator()
                + effect.source();
    }

    static String hexByte(int value) {
        return String.format(Locale.ROOT, "0x%02X", value & 0xFF);
    }
}
