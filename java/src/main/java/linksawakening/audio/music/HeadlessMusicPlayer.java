package linksawakening.audio.music;

import linksawakening.audio.apu.GameBoyApu;

/** Runs ROM music commands on the gameplay clock without an audio device. */
public final class HeadlessMusicPlayer implements FrameMusicPlayer {
    private final GameBoyApu apu = new GameBoyApu(48_000);
    private final MusicDriver driver;

    public HeadlessMusicPlayer(byte[] rom) { driver = new MusicDriver(rom, apu); }
    @Override public void play(MusicTrack track) {
        apu.reset();
        driver.start(track);
        driver.tick60Hz();
    }
    @Override public void stop() { driver.stop(); }
    @Override public void update() { driver.tick60Hz(); }
    @Override public boolean isMusicPlaying() { return driver.isPlaying(); }
    @Override public void close() { stop(); }
}
