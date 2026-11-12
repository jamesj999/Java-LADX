package linksawakening.app.screen;

import static linksawakening.input.KeyCodes.*;

import java.util.Objects;
import linksawakening.cutscene.CutsceneManager;
import linksawakening.input.InputState;
import linksawakening.render.RenderScreen;

/** Owns title-screen input edges and advances title presentation. */
public final class TitleScreen implements Screen {
    /** Required screen collaborators, validated before the first tick. */
    public record Dependencies(CutsceneManager cutsceneManager, InputState inputState, ScreenNavigator navigator) {
        public Dependencies {
            Objects.requireNonNull(cutsceneManager);
            Objects.requireNonNull(inputState);
            Objects.requireNonNull(navigator);
        }
    }

    private final Dependencies dependencies;

    public TitleScreen(Dependencies dependencies) {
        this.dependencies = Objects.requireNonNull(dependencies);
    }

    @Override
    public void tick() {
        dependencies.cutsceneManager().tick();
        dependencies.inputState().tickEdges();
        return;
    }

    @Override
    public void onKey(int key, int action) {
        if (key == KEY_ENTER && action == PRESS) dependencies.navigator().startFileSelection();
    }

    @Override
    public RenderScreen renderScreen() {
        return RenderScreen.TITLE;
    }
}
