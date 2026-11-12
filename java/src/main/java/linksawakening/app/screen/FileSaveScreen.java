package linksawakening.app.screen;

import static linksawakening.world.room.RoomConstants.*;

import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.render.GameFrameState;
import linksawakening.render.RenderScreen;
import linksawakening.ui.FileSaveAction;

public final class FileSaveScreen implements Screen {
    public record Dependencies(
            linksawakening.app.ScreenState screens,
            InputState inputState,
            InputConfig inputConfig,
            SaveActions actions,
            ScreenNavigator navigator,
            InputCompletion completeInput) {}

    private final Dependencies dependencies;

    public FileSaveScreen(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    @Override
    public void tick() {
        if (dependencies.screens().fileSaveController == null) {
            throw new IllegalStateException("File save controller is not initialized");
        }
        FileSaveAction action =
                dependencies.screens().fileSaveController.tick(dependencies.inputState(), dependencies.inputConfig());
        if (action.type() == FileSaveAction.Type.RETURN_TO_GAME) {
            dependencies.screens().fileSaveController = null;
            dependencies.navigator().show(ScreenKind.GAMEPLAY);
        } else if (action.type() == FileSaveAction.Type.SAVE_AND_QUIT) {
            dependencies.actions().saveCurrentPlayerState();
            dependencies.navigator().startFileSelection();
        }
        dependencies.completeInput().complete();
        return;
    }

    @Override
    public void onKey(int key, int action) {}

    @Override
    public RenderScreen renderScreen() {
        return RenderScreen.FILE_SAVE;
    }

    @Override
    public GameFrameState renderState(GameFrameState state) {
        var controller = dependencies.screens().fileSaveController;
        return state.withFileMenuFrame(controller == null ? null : controller.snapshot());
    }
}
