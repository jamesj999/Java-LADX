package linksawakening.audio.music;

/** Music transport used by the game loop, independent of its audio device. */
public interface FrameMusicPlayer extends MusicTrackPlayer, AutoCloseable {
    default void setMenuAttenuated(boolean attenuated) {}

    default void setWarpVolume(int level) {}

    void update();

    default void update(boolean advanceDriver) {
        if (advanceDriver) update();
    }

    boolean isMusicPlaying();

    @Override
    void close();
}
