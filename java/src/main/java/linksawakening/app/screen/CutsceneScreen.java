package linksawakening.app.screen;

import static org.lwjgl.glfw.GLFW.*;

import java.util.Objects;
import linksawakening.app.MusicDirector;
import linksawakening.app.gameplay.DialogSoundRouting;
import linksawakening.app.story.MusicRequests;
import linksawakening.cutscene.CutsceneManager;
import linksawakening.dialog.DialogController;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.render.RenderScreen;

/** Ticks intro dialogs and cutscenes before completing the input frame. */
public final class CutsceneScreen implements Screen {
    /** Required screen collaborators, validated before the first tick. */
    public record Dependencies(
            CutsceneManager cutsceneManager,
            InputState inputState,
            DialogController dialogController,
            InputConfig inputConfig,
            DialogSoundRouting sounds,
            ScreenNavigator navigator,
            MusicRequests music) {
        public Dependencies {
            Objects.requireNonNull(cutsceneManager);
            Objects.requireNonNull(inputState);
            Objects.requireNonNull(dialogController);
            Objects.requireNonNull(inputConfig);
            Objects.requireNonNull(sounds);
            Objects.requireNonNull(navigator);
            Objects.requireNonNull(music);
        }
    }

    private final Dependencies dependencies;

    public CutsceneScreen(Dependencies dependencies) {
        this.dependencies = Objects.requireNonNull(dependencies);
    }

    @Override
    public void tick() {

        dependencies.dialogController().tick();
        dependencies.sounds().route();

        if (dependencies.cutsceneManager().isActive()) {
            dependencies.cutsceneManager().tick();
        }
        if (!dependencies.cutsceneManager().isActive()
                && dependencies.cutsceneManager().isShowingTitleScene()) {
            dependencies.navigator().show(ScreenKind.TITLE);
        }
        dependencies.inputState().tickEdges();
        return;
    }

    @Override
    public void onKey(int key, int action) {
        if (key == GLFW_KEY_ENTER
                && action == GLFW_PRESS
                && dependencies.cutsceneManager().skipIntroToTitle()) {
            dependencies.navigator().show(ScreenKind.TITLE);
            dependencies.music().playDirectMusic(MusicDirector.directTitleScreenMusicTrack());
            return;
        }
        if (action == GLFW_PRESS
                && (key == dependencies.inputConfig().aKey()
                        || key == dependencies.inputConfig().bKey()
                        || key == GLFW_KEY_ENTER)) {
            dependencies.dialogController().advance();
        }
    }

    @Override
    public RenderScreen renderScreen() {
        return RenderScreen.CUTSCENE;
    }
}
