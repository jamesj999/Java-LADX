package linksawakening.app.screen;

import linksawakening.render.GameFrameState;

/**
 * Gameplay lifecycle operations consumed by the input-queue screen. Implementations are connected when the
 * owning session is wired.
 */
public interface GameplayScreenDriver {
    boolean interceptFrameInput();

    void onKey(int key, int action);

    void tick();

    GameFrameState renderState(GameFrameState state);
}
