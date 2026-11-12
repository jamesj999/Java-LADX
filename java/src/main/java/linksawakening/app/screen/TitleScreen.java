package linksawakening.app.screen;

import static linksawakening.world.room.RoomConstants.*;
import static org.lwjgl.glfw.GLFW.*;

import java.util.function.Supplier;
import linksawakening.cutscene.CutsceneManager;
import linksawakening.input.InputState;
import linksawakening.render.RenderScreen;

public final class TitleScreen implements Screen {
    public record Dependencies(
            Supplier<CutsceneManager> cutsceneManager, Supplier<InputState> inputState, Runnable startFileSelection) {}

    private final Dependencies dependencies;

    public TitleScreen(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    @Override
    public void tick() {
        if (dependencies.cutsceneManager().get() != null)
            dependencies.cutsceneManager().get().tick();
        dependencies.inputState().get().tickEdges();
        return;
    }

    @Override
    public void onKey(int key, int action) {
        if (key == GLFW_KEY_ENTER && action == GLFW_PRESS)
            dependencies.startFileSelection().run();
    }

    @Override
    public RenderScreen renderScreen() {
        return RenderScreen.TITLE;
    }
}
