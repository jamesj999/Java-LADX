package linksawakening.app;

import java.util.Objects;
import linksawakening.app.host.Host;

/** Releases presentation, PCM, music, then the host in source order. */
public final class SessionShutdown {
    private final Host host;
    private final AudioServices audio;

    public SessionShutdown(Host host, AudioServices audio) {
        this.host = Objects.requireNonNull(host);
        this.audio = Objects.requireNonNull(audio);
    }

    public void close() {
        host.releasePresentation();
        audio.dialogSoundOutput().close();
        audio.musicPlayer().close();
        host.close();
    }
}
