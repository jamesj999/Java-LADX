package linksawakening.app.screen;

import java.util.Objects;
import linksawakening.app.state.SaveState;
import linksawakening.app.state.ScreenState;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.render.GameFrameState;
import linksawakening.render.RenderScreen;
import linksawakening.save.SaveSlotState;
import linksawakening.ui.FileMenuAction;

/** Ticks the file-selection controller and dispatches its save actions. */
public final class FileMenuScreen implements Screen {
    /** Required screen collaborators, validated before the first tick. */
    public record Dependencies(
            ScreenState screens,
            InputState inputState,
            InputConfig inputConfig,
            SaveState saves,
            SaveActions actions,
            InputCompletion completeInput) {
        public Dependencies {
            Objects.requireNonNull(screens);
            Objects.requireNonNull(inputState);
            Objects.requireNonNull(inputConfig);
            Objects.requireNonNull(saves);
            Objects.requireNonNull(actions);
            Objects.requireNonNull(completeInput);
        }
    }

    private final Dependencies dependencies;

    public FileMenuScreen(Dependencies dependencies) {
        this.dependencies = Objects.requireNonNull(dependencies);
    }

    @Override
    public void tick() {
        if (dependencies.screens().fileMenuController == null) {
            throw new IllegalStateException("File menu controller is not initialized");
        }
        FileMenuAction action =
                dependencies.screens().fileMenuController.tick(dependencies.inputState(), dependencies.inputConfig());
        if (action.type() == FileMenuAction.Type.CREATE_FILE) {
            // $01:4B26 -> label_001_4555: creation returns to file selection.
            // Only selecting a stored nonzero name can start gameplay.
            dependencies
                    .actions()
                    .persistFileMenuMutation(() ->
                            dependencies.saves().saveRamStore.createNewGame(action.selectedSlot(), action.nameBytes()));
        } else if (action.type() == FileMenuAction.Type.LOAD_GAME) {
            SaveSlotState saved = dependencies.saves().saveRamStore.readSlot(action.selectedSlot());
            dependencies.screens().currentSaveSlot = action.selectedSlot();
            if (saved.spawnPositionX() == 0) {
                dependencies.actions().startNewGame();
            } else {
                dependencies.actions().startSavedGame(saved);
            }
        } else if (action.type() == FileMenuAction.Type.ERASE_SLOT) {
            dependencies
                    .actions()
                    .persistFileMenuMutation(
                            () -> dependencies.saves().saveRamStore.eraseSlot(action.selectedSlot()));
        } else if (action.type() == FileMenuAction.Type.COPY_SLOT) {
            dependencies
                    .actions()
                    .persistFileMenuMutation(() ->
                            dependencies.saves().saveRamStore.copySlot(action.selectedSlot(), action.targetSlot()));
        }
        dependencies.completeInput().complete();
        return;
    }

    @Override
    public void onKey(int key, int action) {}

    @Override
    public RenderScreen renderScreen() {
        return RenderScreen.FILE_MENU;
    }

    @Override
    public GameFrameState renderState(GameFrameState state) {
        var controller = dependencies.screens().fileMenuController;
        return state.withFileMenuFrame(controller == null ? null : controller.snapshot());
    }
}
