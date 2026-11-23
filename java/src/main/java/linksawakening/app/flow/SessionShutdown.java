package linksawakening.app.flow;

import java.util.Objects;
import linksawakening.app.host.Host;
import linksawakening.app.services.AudioServices;
import linksawakening.audio.music.FrameMusicPlayer;
import linksawakening.gameplay.PcmSoundOutput;

/**
 * Releases presentation, PCM, music, then the host in source order. One instance is retained for the owning
 * session.
 */
public final class SessionShutdown {
    private final Host host;
    private final PcmSoundOutput pcm;
    private final FrameMusicPlayer music;

    public SessionShutdown(Host host, AudioServices audio) {
        this(host, audio.dialogSoundOutput(), audio.musicPlayer());
    }

    SessionShutdown(Host host, PcmSoundOutput pcm, FrameMusicPlayer music) {
        this.host = Objects.requireNonNull(host);
        this.pcm = Objects.requireNonNull(pcm);
        this.music = Objects.requireNonNull(music);
    }

    public void close() {
        close(() -> {});
    }

    /** The encoder must receive its final audio and video before it is closed. */
    public void close(Runnable closeRecording) {
        closeInOrder(host::releasePresentation, pcm::close, music::close, host::close, closeRecording);
    }

    private static void closeInOrder(Runnable... releases) {
        Throwable first = null;
        for (Runnable release : releases) {
            try {
                release.run();
            } catch (RuntimeException | Error failure) {
                if (first == null) first = failure;
                else if (first != failure) first.addSuppressed(failure);
            }
        }
        if (first instanceof Error error) throw error;
        if (first != null) throw (RuntimeException) first;
    }
}
