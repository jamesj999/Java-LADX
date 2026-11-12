package linksawakening.app.flow;

import java.util.Objects;
import linksawakening.app.host.Host;
import linksawakening.app.services.AudioServices;

/**
 * Releases presentation, PCM, music, then the host in source order. One instance is retained for the owning
 * session.
 */
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
