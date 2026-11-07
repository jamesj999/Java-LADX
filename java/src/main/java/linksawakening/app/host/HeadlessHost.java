package linksawakening.app.host;

import java.util.function.BiConsumer;

/** Window-free host used by replay sessions. */
public final class HeadlessHost implements Host {
    private final linksawakening.gameplay.PcmSoundOutput output;
    public HeadlessHost() { this((pcm, rate) -> {}); }
    public HeadlessHost(linksawakening.gameplay.PcmSoundOutput output) {
        this.output = java.util.Objects.requireNonNull(output);
    }
    @Override public linksawakening.gameplay.PcmSoundOutput createPcmOutput() { return output; }
    @Override public void open(BiConsumer<Integer, Integer> keys) {}
    @Override public boolean shouldClose() { return false; }
    @Override public void pollEvents() {}
    @Override public void present(byte[] pixels) {}
    @Override public void close() {}
}
