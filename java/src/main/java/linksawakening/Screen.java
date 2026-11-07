package linksawakening;

import linksawakening.render.RenderScreen;

/** A session-local screen owns frame and key dispatch. */
public interface Screen {
    void tick();
    void onKey(int key, int action);
    RenderScreen renderScreen();
}
