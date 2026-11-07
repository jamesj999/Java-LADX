package linksawakening.app.screen;
import linksawakening.cutscene.CutsceneManager;
import linksawakening.dialog.DialogController;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.render.RenderScreen;

import static linksawakening.world.room.RoomConstants.*;
import static org.lwjgl.glfw.GLFW.*;

import java.util.function.Supplier;

public final class CutsceneScreen implements Screen {
    public record Dependencies(
        Supplier<CutsceneManager> cutsceneManager,
        Supplier<InputState> inputState,
        Supplier<DialogController> dialogController,
        Supplier<InputConfig> inputConfig,
        Runnable routeDialogSounds,
        Runnable showTitle,
        Runnable skipToTitle) {}
    private final Dependencies dependencies;
    public CutsceneScreen(Dependencies dependencies) { this.dependencies = dependencies; }
    @Override public void tick() {
            if (dependencies.dialogController().get() != null) {
                dependencies.dialogController().get().tick();
                dependencies.routeDialogSounds().run();
            }
            if (dependencies.cutsceneManager().get() != null && dependencies.cutsceneManager().get().isActive()) {
                dependencies.cutsceneManager().get().tick();
            }
            if (dependencies.cutsceneManager().get() != null && !dependencies.cutsceneManager().get().isActive()
                && dependencies.cutsceneManager().get().isShowingTitleScene()) {
                dependencies.showTitle().run();
            }
            dependencies.inputState().get().tickEdges();
            return;
        
    
    }
    @Override public void onKey(int key, int action) {
        if (key == GLFW_KEY_ENTER && action == GLFW_PRESS && dependencies.cutsceneManager().get() != null && dependencies.cutsceneManager().get().skipIntroToTitle()) {
            dependencies.skipToTitle().run();
            return;
        }
        if (action == GLFW_PRESS && dependencies.dialogController().get() != null
            && (key == dependencies.inputConfig().get().aKey() || key == dependencies.inputConfig().get().bKey() || key == GLFW_KEY_ENTER)) {
            dependencies.dialogController().get().advance();
        }
    
    }
    @Override public RenderScreen renderScreen() { return RenderScreen.CUTSCENE; }

}
