package linksawakening.app;

import java.util.Objects;
import linksawakening.app.screen.InputCompletion;
import linksawakening.input.InputState;

/** Advances hardware-style edges before clearing the frame's dialog-consumed flag. */
public final class FrameInputCompletion implements InputCompletion {
    private final InputState input;
    private final GameplayInputState gameplay;

    public FrameInputCompletion(InputState input, GameplayInputState gameplay) {
        this.input = Objects.requireNonNull(input);
        this.gameplay = Objects.requireNonNull(gameplay);
    }

    public void complete() {
        input.tickEdges();
        gameplay.complete();
    }
}
