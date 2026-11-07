package linksawakening.app.screen;

import linksawakening.render.RenderScreen;

/** A session-local screen owns frame and key dispatch. */
public interface Screen {
    default linksawakening.render.GameFrameState renderState(linksawakening.render.GameFrameState state) { return state; }
    void tick();
    void onKey(int key, int action);
    RenderScreen renderScreen();
}
