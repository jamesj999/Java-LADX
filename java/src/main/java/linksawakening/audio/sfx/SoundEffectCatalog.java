package linksawakening.audio.sfx;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class SoundEffectCatalog {
    private static final String CONSTANTS_SOURCE = "LADX-Disassembly/src/constants/sfx.asm";

    private final List<SoundEffect> effects;

    public SoundEffectCatalog(List<SoundEffect> effects) {
        this.effects = List.copyOf(Objects.requireNonNull(effects, "effects"));
    }

    public static SoundEffectCatalog firstPass() {
        return new SoundEffectCatalog(List.of(
                effect(SoundEffectNamespace.NOISE, 0x02, "NOISE_SFX_SWORD_SWING_A"),
                effect(SoundEffectNamespace.NOISE, 0x05, "NOISE_SFX_CUT_GRASS"),
                effect(SoundEffectNamespace.NOISE, 0x09, "NOISE_SFX_POT_SMASHED"),
                effect(SoundEffectNamespace.NOISE, 0x0C, "NOISE_SFX_EXPLOSION"),
                effect(SoundEffectNamespace.WAVE, 0x03, "WAVE_SFX_LINK_HURT"),
                effect(SoundEffectNamespace.WAVE, 0x04, "WAVE_SFX_LOW_HEARTS"),
                effect(SoundEffectNamespace.WAVE, 0x05, "WAVE_SFX_RUPEE"),
                effect(SoundEffectNamespace.WAVE, 0x0F, "WAVE_SFX_TEXT_PRINT")));
    }

    public List<SoundEffect> effects() {
        return effects;
    }

    public Optional<SoundEffect> find(SoundEffectNamespace namespace, int id) {
        Objects.requireNonNull(namespace, "namespace");
        return effects.stream()
                .filter(effect -> effect.namespace() == namespace && effect.id() == (id & 0xFF))
                .findFirst();
    }

    private static SoundEffect effect(SoundEffectNamespace namespace, int id, String name) {
        return new SoundEffect(namespace, id, name, CONSTANTS_SOURCE);
    }
}
