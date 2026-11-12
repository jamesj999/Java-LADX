package linksawakening.app;

import java.util.Objects;
import linksawakening.app.gameplay.GameplayFrame;
import linksawakening.app.gameplay.PassOutController;
import linksawakening.app.screen.GameplayScreenDriver;
import linksawakening.ui.FileMenuFrameSnapshot;

/** Connects the gameplay screen directly to its retained input, frame and recovery components. */
public final class GameplayScreenAdapter implements GameplayScreenDriver {
    private final InputRouter input;
    private final GameplayFrame frame;
    private final PassOutController passOut;

    public GameplayScreenAdapter(InputRouter input, GameplayFrame frame, PassOutController passOut) {
        this.input = Objects.requireNonNull(input);
        this.frame = Objects.requireNonNull(frame);
        this.passOut = Objects.requireNonNull(passOut);
    }

    public boolean interceptFrameInput() {
        return input.tryPresentFileSave();
    }

    public void onKey(int key, int action) {
        input.gameplayKey(key, action);
    }

    public void tick() {
        frame.tick();
    }

    public FileMenuFrameSnapshot gameOver() {
        return passOut.snapshot();
    }
}
