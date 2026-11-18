package linksawakening.app.input;

import java.util.Objects;
import linksawakening.app.gameplay.GameplayFrame;
import linksawakening.app.gameplay.recovery.PassOutController;
import linksawakening.app.screen.GameplayScreenDriver;
import linksawakening.render.GameFrameState;

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

    @Override
    public boolean interceptFrameInput() {
        return input.tryPresentFileSave() || input.tryPresentWorldMap();
    }

    @Override
    public void onKey(int key, int action) {
        input.gameplayKey(key, action);
    }

    @Override
    public void tick() {
        frame.tick();
    }

    @Override
    public GameFrameState renderState(GameFrameState state) {
        return passOut.renderState(state);
    }
}
