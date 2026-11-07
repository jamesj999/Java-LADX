package linksawakening.app.screen;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.render.GameFrameState;
import linksawakening.render.RenderScreen;
import linksawakening.save.SaveRamStore;
import linksawakening.save.SaveSlotState;
import linksawakening.ui.FileMenuAction;
import linksawakening.ui.FileMenuController;

import java.io.IOException;

import static linksawakening.world.RoomConstants.*;
import static org.lwjgl.glfw.GLFW.*;

import java.util.function.*;

public final class FileMenuScreen implements Screen {
    public record Dependencies(Supplier<FileMenuController> fileMenuController, Supplier<InputState> inputState, Supplier<InputConfig> inputConfig, Supplier<SaveRamStore> saveRamStore, IntConsumer selectSlot, Runnable startNewGame, Consumer<SaveSlotState> startSavedGame, Consumer<Runnable> persistFileMenuMutation, Runnable completeInput) {}
    private final Dependencies dependencies;
    public FileMenuScreen(Dependencies dependencies) { this.dependencies = dependencies; }
    @Override public void tick() {
            if (dependencies.fileMenuController().get() == null) {
                throw new IllegalStateException("File menu controller is not initialized");
            }
            FileMenuAction action = dependencies.fileMenuController().get().tick(dependencies.inputState().get(), dependencies.inputConfig().get());
            if (action.type() == FileMenuAction.Type.START_NEW_GAME) {
                try {
                    dependencies.saveRamStore().get().createNewGame(action.selectedSlot(), action.nameBytes());
                    dependencies.saveRamStore().get().flush();
                } catch (IOException exception) {
                    throw new IllegalStateException("Failed to persist new save file", exception);
                }
                dependencies.selectSlot().accept(action.selectedSlot());
                dependencies.startNewGame().run();
            } else if (action.type() == FileMenuAction.Type.LOAD_GAME) {
                SaveSlotState saved = dependencies.saveRamStore().get().readSlot(action.selectedSlot());
                dependencies.selectSlot().accept(action.selectedSlot());
                if (saved.spawnPositionX() == 0) {
                    dependencies.startNewGame().run();
                } else {
                    dependencies.startSavedGame().accept(saved);
                }
            } else if (action.type() == FileMenuAction.Type.ERASE_SLOT) {
                dependencies.persistFileMenuMutation().accept(() -> dependencies.saveRamStore().get().eraseSlot(action.selectedSlot()));
            } else if (action.type() == FileMenuAction.Type.COPY_SLOT) {
                dependencies.persistFileMenuMutation().accept(() ->
                    dependencies.saveRamStore().get().copySlot(action.selectedSlot(), action.targetSlot()));
            }
            dependencies.completeInput().run();
            return;
        
    
    }
    @Override public void onKey(int key, int action) {
    }
    @Override public RenderScreen renderScreen() { return RenderScreen.FILE_MENU; }

    @Override public GameFrameState renderState(GameFrameState state) {
        var controller = dependencies.fileMenuController().get();
        return state.withFileMenuFrame(controller == null ? null : controller.snapshot());
    }

}
