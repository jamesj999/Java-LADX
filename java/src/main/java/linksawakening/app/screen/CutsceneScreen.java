package linksawakening.app.screen;

import static linksawakening.world.room.RoomConstants.*;
import static org.lwjgl.glfw.GLFW.*;

import linksawakening.cutscene.CutsceneManager;
import linksawakening.dialog.DialogController;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.render.RenderScreen;

public final class CutsceneScreen implements Screen {
    public record Dependencies(
            CutsceneManager cutsceneManager,
            InputState inputState,
            DialogController dialogController,
            InputConfig inputConfig,
            linksawakening.app.gameplay.DialogSoundRouting sounds,
            ScreenNavigator navigator,
            linksawakening.app.story.MusicRequests music) {}

    private final Dependencies dependencies;

    public CutsceneScreen(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    @Override
    public void tick() {
        if (dependencies.dialogController() != null) {
            dependencies.dialogController().tick();
            dependencies.sounds().route();
        }
        if (dependencies.cutsceneManager() != null
                && dependencies.cutsceneManager().isActive()) {
            dependencies.cutsceneManager().tick();
        }
        if (dependencies.cutsceneManager() != null
                && !dependencies.cutsceneManager().isActive()
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
                && dependencies.cutsceneManager() != null
                && dependencies.cutsceneManager().skipIntroToTitle()) {
            dependencies.navigator().show(ScreenKind.TITLE);
            dependencies.music().playDirectMusic(linksawakening.app.MusicDirector.directTitleScreenMusicTrack());
            return;
        }
        if (action == GLFW_PRESS
                && dependencies.dialogController() != null
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
