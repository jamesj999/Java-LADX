package linksawakening.audio.sfx;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import linksawakening.audio.apu.GameBoyApu;
import linksawakening.rom.Rom;

/** Frame-driven facade over ROM-backed sound effect execution. */
public final class SoundEffectPlayer {
    private static final String ROM_RESOURCE = "rom/azle.gbc";

    private final GameBoyApu apu;
    private final SoundEffectCatalog catalog;
    private final RomSoundEffectEngine engine;

    private SoundEffect currentEffect;

    public SoundEffectPlayer(GameBoyApu apu) {
        this(apu, loadRomResource());
    }

    public SoundEffectPlayer(GameBoyApu apu, Rom romData) {
        this(apu, romData, SoundEffectCatalog.fromRom(romData));
    }

    public SoundEffectPlayer(GameBoyApu apu, Rom romData, SoundEffectCatalog catalog) {
        this.apu = Objects.requireNonNull(apu, "apu");
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        engine = new RomSoundEffectEngine(Objects.requireNonNull(romData, "romData"), apu);
    }

    public SoundEffectCatalog catalog() {
        return catalog;
    }

    public static List<SoundEffect> supportedEffects(SoundEffectCatalog catalog) {
        Objects.requireNonNull(catalog, "catalog");
        return catalog.effects();
    }

    public boolean supports(SoundEffect effect) {
        Objects.requireNonNull(effect, "effect");
        return catalog.find(effect.namespace(), effect.id()).isPresent();
    }

    public void play(SoundEffect effect) {
        Objects.requireNonNull(effect, "effect");
        if (!supports(effect)) {
            throw new IllegalArgumentException("Unknown sound effect: " + effect.name());
        }

        currentEffect = effect;
        engine.start(effect);
        if (!engine.isPlaying()) {
            currentEffect = null;
        }
    }

    public void tick60Hz() {
        if (!isPlaying()) {
            return;
        }
        engine.tick60Hz();
        if (!engine.isPlaying()) {
            currentEffect = null;
        }
    }

    public boolean isPlaying() {
        return currentEffect != null && engine.isPlaying();
    }

    public SoundEffect currentEffect() {
        return isPlaying() ? currentEffect : null;
    }

    public boolean ownsChannel(int channel) {
        return engine.ownsChannel(channel);
    }

    public boolean takeWaveRestoreRequest() {
        return engine.takeWaveRestoreRequest();
    }

    public void stop() {
        currentEffect = null;
        engine.stop();
    }

    private static Rom loadRomResource() {
        try (InputStream stream = SoundEffectPlayer.class.getClassLoader().getResourceAsStream(ROM_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing: " + ROM_RESOURCE);
            }
            return Rom.of(stream.readAllBytes());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load ROM: " + e.getMessage(), e);
        }
    }
}
