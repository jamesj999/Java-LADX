package linksawakening.audio.music;

/** Music transport used by the game loop, independent of its audio device. */
public interface FrameMusicPlayer extends MusicTrackPlayer, AutoCloseable {
    void update();

    boolean isMusicPlaying();

    @Override
    void close();
}
