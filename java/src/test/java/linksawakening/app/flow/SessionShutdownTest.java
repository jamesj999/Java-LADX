package linksawakening.app.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import linksawakening.app.host.Host;
import linksawakening.audio.music.FrameMusicPlayer;
import linksawakening.audio.music.MusicTrack;
import linksawakening.gameplay.PcmSoundOutput;
import org.junit.jupiter.api.Test;

final class SessionShutdownTest {
    @Test
    void pcmFailurePreservesReleaseOrderAndLaterFailures() {
        var calls = new ArrayList<String>();
        var first = new IllegalStateException("pcm");
        var last = new AssertionError("encoder");
        var pcm = new PcmSoundOutput() {
            @Override
            public void play(short[] samples, int rate) {}

            @Override
            public void close() {
                calls.add("pcm");
                throw first;
            }
        };
        var music = new FrameMusicPlayer() {
            @Override
            public void update() {}

            @Override
            public boolean isMusicPlaying() {
                return false;
            }

            @Override
            public void play(MusicTrack track) {}

            @Override
            public void stop() {}

            @Override
            public void close() {
                calls.add("music");
            }
        };
        var host = new Host() {
            @Override
            public PcmSoundOutput createPcmOutput() {
                return pcm;
            }

            @Override
            public void open(BiConsumer<Integer, Integer> keys) {}

            @Override
            public void pollEvents() {}

            @Override
            public boolean shouldClose() {
                return false;
            }

            @Override
            public void present(byte[] pixels) {}

            @Override
            public void releasePresentation() {
                calls.add("presentation");
            }

            @Override
            public void close() {
                calls.add("host");
                throw first;
            }
        };
        var shutdown = new SessionShutdown(host, pcm, music);
        var thrown = assertThrows(
                IllegalStateException.class,
                () -> shutdown.close(() -> {
                    calls.add("recording");
                    throw last;
                }));
        assertSame(first, thrown);
        assertEquals(List.of(last), List.of(thrown.getSuppressed()));
        assertEquals(List.of("presentation", "pcm", "music", "host", "recording"), calls);
    }
}
