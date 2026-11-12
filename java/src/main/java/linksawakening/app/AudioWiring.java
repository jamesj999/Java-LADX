package linksawakening.app;

import linksawakening.app.host.Host;
import linksawakening.audio.apu.GameBoyApu;
import linksawakening.audio.music.AreaMusicResolver;
import linksawakening.audio.music.FrameAudioPlayer;
import linksawakening.audio.music.FrameMusicPlayer;
import linksawakening.audio.music.GameplayMusicController;
import linksawakening.audio.music.MusicCatalog;
import linksawakening.audio.sfx.SoundEffectPlayer;
import linksawakening.gameplay.DialogSoundEffectMap;
import linksawakening.gameplay.DialogSoundSink;
import linksawakening.gameplay.GameplaySoundEffectMap;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.gameplay.PcmSoundOutput;
import linksawakening.rom.Rom;

/** Connects ROM music and effect catalogs to the host PCM output. */
final class AudioWiring {
    private AudioWiring() {}

    static AudioServices build(Rom romData, Host host) {
        PcmSoundOutput dialogSoundOutput = host.createPcmOutput();
        MusicCatalog musicCatalog = MusicCatalog.fromRom(romData);
        FrameMusicPlayer musicPlayer = new FrameAudioPlayer(romData, 48_000, dialogSoundOutput);
        // ROM background tracks own their loop commands; finite fanfares must finish.
        GameplayMusicController gameplayMusicController =
                new GameplayMusicController(AreaMusicResolver.fromRom(romData), musicCatalog, musicPlayer);
        var audio = (FrameAudioPlayer) musicPlayer;
        var catalog = new SoundEffectPlayer(new GameBoyApu(48_000), romData).catalog();
        var dialogEffects = DialogSoundEffectMap.fromCatalog(catalog);
        var gameplayEffects = GameplaySoundEffectMap.fromCatalog(catalog);
        DialogSoundSink dialogSoundSink = event -> dialogEffects.resolve(event).ifPresent(audio::playSoundEffect);
        GameplaySoundSink gameplaySoundSink =
                event -> gameplayEffects.resolve(event).ifPresent(audio::playSoundEffect);
        return new AudioServices(
                musicPlayer, gameplayMusicController, dialogSoundSink, gameplaySoundSink, dialogSoundOutput);
    }
}
