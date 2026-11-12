package linksawakening.app.screen;

import static linksawakening.world.room.RoomConstants.*;
import static org.lwjgl.glfw.GLFW.*;

import linksawakening.cutscene.CutsceneManager;
import linksawakening.input.InputState;
import linksawakening.render.RenderScreen;

public final class TitleScreen implements Screen {
    public record Dependencies(CutsceneManager cutsceneManager, InputState inputState, ScreenNavigator navigator) {}

    private final Dependencies dependencies;

    public TitleScreen(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    @Override
    public void tick() {
        if (dependencies.cutsceneManager() != null)
            dependencies.cutsceneManager().tick();
        dependencies.inputState().tickEdges();
        return;
    }

    @Override
    public void onKey(int key, int action) {
        if (key == GLFW_KEY_ENTER && action == GLFW_PRESS)
            dependencies.navigator().startFileSelection();
    }

    @Override
    public RenderScreen renderScreen() {
        return RenderScreen.TITLE;
    }
}
