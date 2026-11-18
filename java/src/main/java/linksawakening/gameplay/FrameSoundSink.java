package linksawakening.gameplay;

import linksawakening.audio.apu.GameBoyApu;
import linksawakening.audio.music.GameplayMusicController;
import linksawakening.audio.music.MusicDriver;
import linksawakening.audio.music.MusicTrack;
import linksawakening.audio.music.MusicTrackPlayer;
import linksawakening.audio.sfx.LiveSoundEffects;
import linksawakening.audio.sfx.SoundEffectCatalog;
import linksawakening.audio.sfx.SoundEffectNamespace;
import linksawakening.dialog.DialogController;

/** Runs the ROM sound effects and music in VBlank order on the four channels of one APU. */
public final class FrameSoundSink implements GameplaySoundSink, DialogSoundSink, MusicTrackPlayer {
    private static final int SAMPLE_RATE = 48_000;

    private final GameBoyApu apu = new GameBoyApu(SAMPLE_RATE);
    private final MusicDriver music;
    private int musicBank = 0x1B;
    private MusicTrack pendingTrack;
    private boolean changeTrack;
    private final LiveSoundEffects effects;
    private final SoundEffectCatalog catalog;
    private final DialogSoundEffectMap dialogEffects;
    private final PcmSoundOutput output;

    public FrameSoundSink(byte[] romData, PcmSoundOutput output) {
        music = new MusicDriver(romData, apu);
        effects = new LiveSoundEffects(apu, romData);
        catalog = SoundEffectCatalog.fromRom(romData);
        dialogEffects = DialogSoundEffectMap.fromCatalog(catalog);
        this.output = output;
    }

    @Override
    public void play(GameplaySoundEvent event) {
        if (event != null) effects.request(event.namespace(), event.id());
    }

    @Override
    public void play(DialogController.SoundEvent event) {
        dialogEffects.resolve(event).ifPresent(effect -> effects.request(effect.namespace(), effect.id()));
    }

    @Override
    public void playRomSound(SoundEffectNamespace namespace, int id) {
        if (id > 0 && id <= 0xFF && catalog.find(namespace, id).isPresent()) {
            effects.request(namespace, id);
        }
    }

    public void setMenuAttenuated(boolean attenuated) {
        setMasterVolume(attenuated ? 3 : 7);
    }

    public void setMasterVolume(int volume) {
        apu.writeRegister(GameBoyApu.NR50, volume * 0x11);
    }

    @Override
    public void play(MusicTrack track) {
        pendingTrack = java.util.Objects.requireNonNull(track);
        changeTrack = true;
    }

    @Override
    public void stop() {
        pendingTrack = null;
        changeTrack = true;
    }

    public boolean isMusicPlaying() {
        return changeTrack ? pendingTrack != null : music.isPlaying();
    }

    public void finishFrame(GameplayMusicController controller, boolean advanceDriver) {
        if (advanceDriver) tickDriver(controller);
        output.play(apu.render(SAMPLE_RATE / 60), SAMPLE_RATE);
    }

    private void tickDriver(GameplayMusicController controller) {
        int requestedTrack = effects.tick();
        if (effects.silencedMusic()) {
            music.clearScore();
            if (!changeTrack) controller.effectSilencedMusic();
        }
        if (requestedTrack != 0) controller.playRequestedTrack(requestedTrack);
        if (changeTrack) {
            if (pendingTrack != null) musicBank = pendingTrack.bank();
            effects.changeMusicTrack(musicBank, pendingTrack == null);
            int occupied = effects.musicSuppressionMask();
            if (pendingTrack == null) music.stopShared(occupied);
            else music.startShared(pendingTrack, occupied);
            changeTrack = false;
        }
        music.tick60Hz(effects.musicSuppressionMask());
    }
}
