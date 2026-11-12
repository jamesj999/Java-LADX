package linksawakening.app.host;

import java.util.Objects;
import java.util.function.BiConsumer;
import linksawakening.gameplay.PcmSoundOutput;

/** Window-free host used by replay sessions. One instance is retained for the owning session. */
public final class HeadlessHost implements Host {
    private final PcmSoundOutput output;

    public HeadlessHost() {
        this((pcm, rate) -> {});
    }

    public HeadlessHost(PcmSoundOutput output) {
        this.output = Objects.requireNonNull(output);
    }

    @Override
    public PcmSoundOutput createPcmOutput() {
        return output;
    }

    @Override
    public void open(BiConsumer<Integer, Integer> keys) {}

    @Override
    public boolean shouldClose() {
        return false;
    }

    @Override
    public void pollEvents() {}

    @Override
    public void present(byte[] pixels) {}

    @Override
    public void close() {}
}
