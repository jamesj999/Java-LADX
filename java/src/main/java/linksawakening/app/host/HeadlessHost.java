package linksawakening.app.host;

import java.util.function.BiConsumer;

/** Window-free host used by replay sessions. */
public final class HeadlessHost implements Host {
    @Override public linksawakening.gameplay.PcmSoundOutput createPcmOutput() {
        return (pcm, rate) -> {};
    }
    @Override public void open(BiConsumer<Integer, Integer> keys) {}
    @Override public boolean shouldClose() { return false; }
    @Override public void pollEvents() {}
    @Override public void present(byte[] pixels) {}
    @Override public void close() {}
}
