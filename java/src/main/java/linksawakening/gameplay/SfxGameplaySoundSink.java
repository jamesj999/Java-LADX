package linksawakening.gameplay;

import linksawakening.audio.apu.GameBoyApu;
import linksawakening.audio.sfx.SoundEffect;
import linksawakening.audio.sfx.SoundEffectPcmRenderer;
import linksawakening.audio.sfx.SoundEffectPlayer;

import java.util.Objects;

public final class SfxGameplaySoundSink implements GameplaySoundSink {
    private static final int SAMPLE_RATE = 44_100;

    private final GameBoyApu apu;
    private final SoundEffectPlayer player;
    private final GameplaySoundEffectMap soundEffects;
    private final PcmSoundOutput output;

    public SfxGameplaySoundSink(PcmSoundOutput output) {
        this(new GameBoyApu(SAMPLE_RATE), output);
    }

    public SfxGameplaySoundSink(byte[] romData, PcmSoundOutput output) {
        this(new GameBoyApu(SAMPLE_RATE), romData, output);
    }

    public SfxGameplaySoundSink(GameBoyApu apu, PcmSoundOutput output) {
        this(apu, new SoundEffectPlayer(apu), output);
    }

    public SfxGameplaySoundSink(GameBoyApu apu, byte[] romData, PcmSoundOutput output) {
        this(apu, new SoundEffectPlayer(apu, romData), output);
    }

    private SfxGameplaySoundSink(GameBoyApu apu, SoundEffectPlayer player, PcmSoundOutput output) {
        this(apu, player, GameplaySoundEffectMap.fromCatalog(player.catalog()), output);
    }

    public SfxGameplaySoundSink(
            GameBoyApu apu,
            SoundEffectPlayer player,
            GameplaySoundEffectMap soundEffects,
            PcmSoundOutput output) {
        this.apu = Objects.requireNonNull(apu, "apu");
        this.player = Objects.requireNonNull(player, "player");
        this.soundEffects = Objects.requireNonNull(soundEffects, "soundEffects");
        this.output = Objects.requireNonNull(output, "output");
    }

    @Override
    public void play(GameplaySoundEvent event) {
        SoundEffect effect = soundEffects.resolve(event).orElse(null);
        if (effect == null) {
            return;
        }

        short[] pcm = SoundEffectPcmRenderer.renderOneShot(apu, player, effect);
        if (pcm.length > 0) {
            output.playReplacing(pcm, apu.sampleRate());
        }
    }
}
