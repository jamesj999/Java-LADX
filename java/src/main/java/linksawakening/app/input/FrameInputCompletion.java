package linksawakening.app.input;

import java.util.Objects;
import linksawakening.app.screen.InputCompletion;
import linksawakening.app.state.GameplayInputState;
import linksawakening.input.InputState;

/**
 * Advances hardware-style edges before clearing the frame's dialog-consumed flag. One instance is retained
 * for the owning session.
 */
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
