package linksawakening.audio.sfx;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import linksawakening.audio.apu.GameBoyApu;
import linksawakening.rom.Rom;

/** Frame-driven facade over ROM-backed sound effect execution. */
public final class SoundEffectPlayer implements SoundEffectPlayback {
    @Override
    public boolean available() {
        return true;
    }

    private static final String ROM_RESOURCE = "rom/azle.gbc";

    private final SoundEffectCatalog catalog;
    private final RomSoundEffectEngine engine;

    // Last requested identity; only observable while effectSelected and the engine is playing.
    private SoundEffect currentEffect;
    private boolean effectSelected;

    public SoundEffectPlayer(GameBoyApu apu) {
        this(apu, loadRomResource());
    }

    public SoundEffectPlayer(GameBoyApu apu, Rom romData) {
        this(apu, romData, SoundEffectCatalog.fromRom(romData));
    }

    public SoundEffectPlayer(GameBoyApu apu, Rom romData, SoundEffectCatalog catalog) {
        Objects.requireNonNull(apu, "apu");
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

    @Override
    public void play(SoundEffect effect) {
        Objects.requireNonNull(effect, "effect");
        if (!supports(effect)) {
            throw new IllegalArgumentException("Unknown sound effect: " + effect.name());
        }

        currentEffect = effect;
        effectSelected = true;
        engine.start(effect);
        if (!engine.isPlaying()) {
            effectSelected = false;
        }
    }

    @Override
    public void tick60Hz() {
        if (!isPlaying()) {
            return;
        }
        engine.tick60Hz();
        if (!engine.isPlaying()) {
            effectSelected = false;
        }
    }

    @Override
    public boolean isPlaying() {
        return effectSelected && engine.isPlaying();
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

    @Override
    public void stop() {
        effectSelected = false;
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
