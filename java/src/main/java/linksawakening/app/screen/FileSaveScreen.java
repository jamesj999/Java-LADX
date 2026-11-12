package linksawakening.app.screen;

import static linksawakening.world.room.RoomConstants.*;

import java.util.function.Supplier;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.render.GameFrameState;
import linksawakening.render.RenderScreen;
import linksawakening.ui.FileSaveAction;
import linksawakening.ui.FileSaveController;

public final class FileSaveScreen implements Screen {
    public record Dependencies(
            Supplier<FileSaveController> fileSaveController,
            Supplier<InputState> inputState,
            Supplier<InputConfig> inputConfig,
            Runnable returnToGame,
            Runnable saveCurrentPlayerState,
            Runnable startFileSelection,
            InputCompletion completeInput) {}

    private final Dependencies dependencies;

    public FileSaveScreen(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    @Override
    public void tick() {
        if (dependencies.fileSaveController().get() == null) {
            throw new IllegalStateException("File save controller is not initialized");
        }
        FileSaveAction action = dependencies
                .fileSaveController()
                .get()
                .tick(
                        dependencies.inputState().get(),
                        dependencies.inputConfig().get());
        if (action.type() == FileSaveAction.Type.RETURN_TO_GAME) {
            dependencies.returnToGame().run();
        } else if (action.type() == FileSaveAction.Type.SAVE_AND_QUIT) {
            dependencies.saveCurrentPlayerState().run();
            dependencies.startFileSelection().run();
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
        var controller = dependencies.fileSaveController().get();
        return state.withFileMenuFrame(controller == null ? null : controller.snapshot());
    }
}
