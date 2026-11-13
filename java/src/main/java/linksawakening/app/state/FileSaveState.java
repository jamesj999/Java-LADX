package linksawakening.app.state;

import java.util.Objects;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.ui.FileMenuFrameSnapshot;
import linksawakening.ui.FileSaveAction;
import linksawakening.ui.FileSaveController;

/** One screen operation's lifetime; inactive screens have no controller to query. */
public final class FileSaveState {
    private Operation operation = Inactive.INSTANCE;

    public void open(FileSaveController controller) {
        operation = new Active(controller);
    }

    public void clear() {
        operation = Inactive.INSTANCE;
    }

    public FileSaveAction tick(InputState input, InputConfig config) {
        return operation.tick(input, config);
    }

    public FileMenuFrameSnapshot snapshotOrNull() {
        return operation.snapshotOrNull();
    }

    public FileSaveController activeController() {
        return operation.controller();
    }

    private sealed interface Operation permits Active, Inactive {
        FileSaveAction tick(InputState input, InputConfig config);

        FileMenuFrameSnapshot snapshotOrNull();

        FileSaveController controller();
    }

    private record Active(FileSaveController controller) implements Operation {
        Active {
            Objects.requireNonNull(controller);
        }

        public FileSaveAction tick(InputState input, InputConfig config) {
            return controller.tick(input, config);
        }

        public FileMenuFrameSnapshot snapshotOrNull() {
            return controller.snapshot();
        }
    }

    private enum Inactive implements Operation {
        INSTANCE;

        public FileSaveController controller() {
            throw new IllegalStateException("File save controller is not initialized");
        }

        public FileSaveAction tick(InputState input, InputConfig config) {
            return controller().tick(input, config);
        }

        public FileMenuFrameSnapshot snapshotOrNull() {
            return null;
        }
    }
}
