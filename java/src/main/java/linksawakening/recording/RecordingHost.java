package linksawakening.recording;

import java.util.function.BiConsumer;
import java.util.function.Supplier;
import linksawakening.app.host.Host;
import linksawakening.audio.music.ReusableFramePcmOutput;
import linksawakening.gameplay.PcmSoundOutput;

/** Taps engine audio before device buffering, including when no device exists. */
public final class RecordingHost implements Host {
    private final Host delegate;
    private final Supplier<GameplayRecorder> recorder;

    public RecordingHost(Host delegate, Supplier<GameplayRecorder> recorder) {
        this.delegate = delegate;
        this.recorder = recorder;
    }

    @Override
    public PcmSoundOutput createPcmOutput() {
        PcmSoundOutput output = delegate.createPcmOutput();
        return new ReusableFramePcmOutput() {
            @Override
            public void play(short[] pcm, int rate) {
                output.play(pcm, rate);
            }

            @Override
            public void playFrame(short[] pcm, int rate) {
                GameplayRecorder active = recorder.get();
                if (active != null) active.captureAudio(pcm, rate);
                output.playFrame(output instanceof ReusableFramePcmOutput ? pcm : pcm.clone(), rate);
            }

            @Override
            public void playReplacing(short[] pcm, int rate) {
                output.playReplacing(pcm, rate);
            }

            @Override
            public void close() {
                output.close();
            }
        };
    }

    @Override
    public void open(BiConsumer<Integer, Integer> keys) {
        delegate.open(keys);
    }

    @Override
    public boolean shouldClose() {
        return delegate.shouldClose();
    }

    @Override
    public void pollEvents() {
        delegate.pollEvents();
    }

    @Override
    public void present(byte[] pixels) {
        delegate.present(pixels);
    }

    @Override
    public void releasePresentation() {
        delegate.releasePresentation();
    }

    @Override
    public void recordingStatus(boolean active) {
        delegate.recordingStatus(active);
    }

    @Override
    public void close() {
        delegate.close();
    }
}
