package linksawakening.app.state;

import java.util.Objects;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.ui.FileMenuAction;
import linksawakening.ui.FileMenuController;
import linksawakening.ui.FileMenuFrameSnapshot;

/** One screen operation's lifetime; inactive screens have no controller to query. */
public final class FileMenuState {
    private Operation operation = Inactive.INSTANCE;

    public void open(FileMenuController controller) {
        operation = new Active(controller);
    }

    public void clear() {
        operation = Inactive.INSTANCE;
    }

    public FileMenuAction tick(InputState input, InputConfig config) {
        return operation.tick(input, config);
    }

    public FileMenuFrameSnapshot snapshotOrNull() {
        return operation.snapshotOrNull();
    }

    public FileMenuController activeController() {
        return operation.controller();
    }

    private sealed interface Operation permits Active, Inactive {
        FileMenuAction tick(InputState input, InputConfig config);

        FileMenuFrameSnapshot snapshotOrNull();

        FileMenuController controller();
    }

    private record Active(FileMenuController controller) implements Operation {
        Active {
            Objects.requireNonNull(controller);
        }

        public FileMenuAction tick(InputState input, InputConfig config) {
            return controller.tick(input, config);
        }

        public FileMenuFrameSnapshot snapshotOrNull() {
            return controller.snapshot();
        }
    }

    private enum Inactive implements Operation {
        INSTANCE;

        public FileMenuController controller() {
            throw new IllegalStateException("File menu controller is not initialized");
        }

        public FileMenuAction tick(InputState input, InputConfig config) {
            return controller().tick(input, config);
        }

        public FileMenuFrameSnapshot snapshotOrNull() {
            return null;
        }
    }
}
