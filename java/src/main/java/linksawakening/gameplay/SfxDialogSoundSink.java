package linksawakening.gameplay;

import linksawakening.audio.apu.GameBoyApu;
import linksawakening.audio.sfx.SoundEffect;
import linksawakening.audio.sfx.SoundEffectPcmRenderer;
import linksawakening.audio.sfx.SoundEffectPlayer;
import linksawakening.dialog.DialogController;

import java.util.Objects;

public final class SfxDialogSoundSink implements DialogSoundSink {
    private static final int SAMPLE_RATE = 44_100;

    private final GameBoyApu apu;
    private final SoundEffectPlayer player;
    private final DialogSoundEffectMap soundEffects;
    private final PcmSoundOutput output;

    public SfxDialogSoundSink(PcmSoundOutput output) {
        this(new GameBoyApu(SAMPLE_RATE), output);
    }

    public SfxDialogSoundSink(GameBoyApu apu, PcmSoundOutput output) {
        this(apu, new SoundEffectPlayer(apu), output);
    }

    private SfxDialogSoundSink(GameBoyApu apu, SoundEffectPlayer player, PcmSoundOutput output) {
        this(apu, player, DialogSoundEffectMap.fromCatalog(player.catalog()), output);
    }

    public SfxDialogSoundSink(
            GameBoyApu apu,
            SoundEffectPlayer player,
            DialogSoundEffectMap soundEffects,
            PcmSoundOutput output) {
        this.apu = Objects.requireNonNull(apu, "apu");
        this.player = Objects.requireNonNull(player, "player");
        this.soundEffects = Objects.requireNonNull(soundEffects, "soundEffects");
        this.output = Objects.requireNonNull(output, "output");
    }

    @Override
    public void play(DialogController.SoundEvent event) {
        SoundEffect effect = soundEffects.resolve(event).orElse(null);
        if (effect == null) {
            return;
        }

        short[] pcm = SoundEffectPcmRenderer.renderOneShot(apu, player, effect);
        if (pcm.length > 0) {
            output.play(pcm, apu.sampleRate());
        }
    }
}
