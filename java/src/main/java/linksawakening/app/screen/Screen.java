package linksawakening.app.screen;

import linksawakening.render.GameFrameState;
import linksawakening.render.RenderScreen;

/** A session-local screen owns frame and key dispatch. */
public interface Screen {
    default GameFrameState renderState(GameFrameState state) {
        return state;
    }

    void tick();

    void onKey(int key, int action);

    RenderScreen renderScreen();
}
