package linksawakening.app.screen;

import linksawakening.ui.FileMenuFrameSnapshot;

/** Gameplay lifecycle operations consumed by the input-queue screen. */
public interface GameplayScreenDriver {
    boolean interceptFrameInput();

    void onKey(int key, int action);

    void tick();

    FileMenuFrameSnapshot gameOver();
}
