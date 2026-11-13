package linksawakening.cutscene;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/** IntroHandler's explicitly unloaded/running session and its last published frame. */
final class IntroPlayback {
    private Phase phase = Unloaded.INSTANCE;
    private final Consumer<String> sceneLoader;

    IntroPlayback(Consumer<String> sceneLoader) {
        this.sceneLoader = Objects.requireNonNull(sceneLoader, "sceneLoader");
    }

    void start(IntroSequence sequence, IntConsumer noiseSound) {
        sequence.setNoiseSound(noiseSound);
        phase = new Running(sequence);
    }

    void setNoiseSound(IntConsumer sink) {
        phase.setNoiseSound(sink);
    }

    boolean tick() {
        return phase.tick(sceneLoader);
    }

    boolean active() {
        return phase.active();
    }

    boolean skip() {
        return phase.skip(sceneLoader);
    }

    IntroFrameSnapshot frame() {
        return phase.frame();
    }

    /** Legacy nullable snapshot boundary; internal presentation always uses frame(). */
    IntroFrameSnapshot snapshotOrNull() {
        return phase == Unloaded.INSTANCE ? null : phase.frame();
    }

    private sealed interface Phase permits Unloaded, Running {
        default void setNoiseSound(IntConsumer sink) {}

        default boolean tick(Consumer<String> sceneLoader) {
            return false;
        }

        default boolean active() {
            return false;
        }

        default boolean skip(Consumer<String> sceneLoader) {
            return false;
        }

        default IntroFrameSnapshot frame() {
            return IntroFrameSnapshot.empty();
        }
    }

    private enum Unloaded implements Phase {
        INSTANCE
    }

    private static final class Running implements Phase {
        private final IntroSequence sequence;
        private IntroFrameSnapshot frame;

        Running(IntroSequence sequence) {
            this.sequence = Objects.requireNonNull(sequence, "sequence");
            frame = sequence.snapshot();
        }

        @Override
        public void setNoiseSound(IntConsumer sink) {
            sequence.setNoiseSound(sink);
        }

        @Override
        public boolean active() {
            return sequence.isActive();
        }

        @Override
        public IntroFrameSnapshot frame() {
            return frame;
        }

        @Override
        public boolean tick(Consumer<String> sceneLoader) {
            String previousScene = sequence.sceneId();
            sequence.tick();
            frame = sequence.snapshot();
            String nextScene = sequence.sceneId();
            if (!nextScene.equals(previousScene)) sceneLoader.accept(nextScene);
            return true;
        }

        @Override
        public boolean skip(Consumer<String> sceneLoader) {
            if (!sequence.isActive()) return false;
            sequence.skipToTitle();
            frame = sequence.snapshot();
            sceneLoader.accept(sequence.sceneId());
            return true;
        }
    }
}
