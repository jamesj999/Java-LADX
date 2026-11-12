package linksawakening.audio.music;

/** Track selection and stop contract for music transports. */
public interface MusicTrackPlayer {
    void play(MusicTrack track);

    void stop();
}
