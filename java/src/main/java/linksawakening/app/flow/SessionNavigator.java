package linksawakening.app.flow;

import java.util.Objects;
import linksawakening.app.audio.RoomMusicSelection;
import linksawakening.app.screen.Screen;
import linksawakening.app.screen.ScreenKind;
import linksawakening.app.screen.ScreenNavigator;
import linksawakening.app.screen.ScreenSet;
import linksawakening.app.state.ScreenState;
import linksawakening.scene.BackgroundScene;
import linksawakening.world.room.ActiveRoom;
import linksawakening.world.room.RoomLoadListener;

/** The sole two-phase cycle boundary. Construct before ROM/UI/world wiring; attach once
 * after screens and routes exist, before publishing the session or loading its first room. */
public final class SessionNavigator implements ScreenNavigator, RoomLoadListener {
    private final ScreenState state;
    private Targets targets;

    private record Targets(ScreenSet screens, ScreenRouter router, RoomMusicSelection music) {}

    public SessionNavigator(ScreenState state) {
        this.state = Objects.requireNonNull(state);
    }

    public void attach(ScreenSet screens, ScreenRouter router, RoomMusicSelection music) {
        if (targets != null) throw new IllegalStateException("Navigator already attached");
        targets = new Targets(
                Objects.requireNonNull(screens), Objects.requireNonNull(router), Objects.requireNonNull(music));
    }

    private Targets targets() {
        return Objects.requireNonNull(targets, "Navigator not attached");
    }

    public Screen current() {
        return state.currentScreen;
    }

    public Screen screen(ScreenKind kind) {
        return targets().screens().screen(kind);
    }

    public void show(ScreenKind kind) {
        state.currentScreen = screen(kind);
    }

    public void startFileSelection() {
        targets().router().startFileSelection();
    }

    public void startDebugGameplay() {
        targets().router().startDebugGameplay();
    }

    public BackgroundScene loadFileMenuBackgroundScene(String sceneId) {
        return targets().router().loadFileMenuBackgroundScene(sceneId);
    }

    public void setCutsceneScene(String sceneId) {
        targets().router().setCutsceneScene(sceneId);
    }

    public void roomLoaded(ActiveRoom room) {
        targets().music().roomLoaded(room);
    }
}
