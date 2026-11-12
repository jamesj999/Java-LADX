package linksawakening.ui;

import java.util.function.Function;
import linksawakening.scene.BackgroundScene;

/**
 * Input/state model for ROM {@code FileSelectionHandler} and the New Game screens.
 */
public final class FileMenuController extends FileMenuEditing {
    /** Named mode values. */
    public enum Mode {
        SELECT,
        CREATE,
        ERASE_PICK,
        ERASE_CONFIRM,
        COPY_SOURCE,
        COPY_TARGET,
        COPY_CONFIRM
    }

    public FileMenuController(
            FileMenuRomData romData,
            Function<String, BackgroundScene> sceneProvider,
            int saveFilesMask,
            int[][] savedNames) {
        super(romData, sceneProvider, saveFilesMask, savedNames);
    }

    public FileMenuController(
            FileMenuRomData romData,
            Function<String, BackgroundScene> sceneProvider,
            int saveFilesMask,
            int[][] savedNames,
            int[] maxHearts,
            int[] deathCountsBcd) {
        super(romData, sceneProvider, saveFilesMask, savedNames, maxHearts, deathCountsBcd);
    }
}
