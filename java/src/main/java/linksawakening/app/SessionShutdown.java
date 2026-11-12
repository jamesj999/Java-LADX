package linksawakening.app;

import java.util.Objects;
import linksawakening.app.debug.*;
import linksawakening.app.gameplay.*;
import linksawakening.app.host.*;
import linksawakening.app.screen.*;
import linksawakening.app.story.*;
import linksawakening.world.transition.*;

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
