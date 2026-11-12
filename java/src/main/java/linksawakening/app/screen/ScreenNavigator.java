package linksawakening.app.screen;

import linksawakening.scene.BackgroundScene;

/**
 * Navigation requests used by input, saves and screen lifecycle implementations. Implementations are
 * connected when the owning session is wired.
 */
public interface ScreenNavigator {
    Screen current();

    Screen screen(ScreenKind kind);

    void show(ScreenKind kind);

    void startFileSelection();

    void startDebugGameplay();

    BackgroundScene loadFileMenuBackgroundScene(String sceneId);

    void setCutsceneScene(String sceneId);
}
