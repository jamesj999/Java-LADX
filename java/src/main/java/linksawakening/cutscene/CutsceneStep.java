package linksawakening.cutscene;

import java.util.Objects;
import java.util.function.Consumer;

public interface CutsceneStep {

    boolean tick(CutsceneContext context);

    CutsceneStep fresh();

    static CutsceneStep waitFrames(int frames) {
        return new WaitFramesStep(frames);
    }

    static CutsceneStep run(Consumer<CutsceneContext> action) {
        return new RunStep(action);
    }

    static CutsceneStep showDialog(String text) {
        return new ShowDialogStep(text);
    }

    final class WaitFramesStep implements CutsceneStep {
        private final int initialFrames;
        private int remainingFrames;

        private WaitFramesStep(int frames) {
            if (frames < 0) {
                throw new IllegalArgumentException("frames cannot be negative");
            }
            this.initialFrames = frames;
            this.remainingFrames = frames;
        }

        @Override
        public boolean tick(CutsceneContext context) {
            if (remainingFrames == 0) {
                return true;
            }
            remainingFrames--;
            return remainingFrames == 0;
        }

        @Override
        public CutsceneStep fresh() {
            return new WaitFramesStep(initialFrames);
        }
    }

    final class RunStep implements CutsceneStep {
        private final Consumer<CutsceneContext> action;
        private boolean ran;

        private RunStep(Consumer<CutsceneContext> action) {
            this.action = Objects.requireNonNull(action);
        }

        @Override
        public boolean tick(CutsceneContext context) {
            if (!ran) {
                action.accept(context);
                ran = true;
            }
            return true;
        }

        @Override
        public CutsceneStep fresh() {
            return new RunStep(action);
        }
    }

    final class ShowDialogStep implements CutsceneStep {
        private final String text;
        private boolean opened;

        private ShowDialogStep(String text) {
            this.text = Objects.requireNonNull(text);
        }

        @Override
        public boolean tick(CutsceneContext context) {
            if (!opened) {
                context.showDialog(text);
                opened = true;
            }
            return !context.isDialogActive();
        }

        @Override
        public CutsceneStep fresh() {
            return new ShowDialogStep(text);
        }
    }
}
