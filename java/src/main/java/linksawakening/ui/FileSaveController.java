package linksawakening.ui;

import linksawakening.cutscene.IntroSprite;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.scene.BackgroundScene;
import linksawakening.scene.BackgroundSceneCatalog;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * State and ROM-backed presentation for the in-game save choice.
 *
 * <p>This mirrors {@code FileSaveInteractive}: the two options are selected
 * with the d-pad and A, B, or Start confirms the current option.</p>
 */
public final class FileSaveController {

    private static final int OPTION_COUNT = 2;
    private static final int SAVE_ARROW_TILE = 0xBE;
    private static final int SAVE_ARROW_OAM_X = 0x24;
    private static final int[] SAVE_OPTION_OAM_Y = {0x48, 0x58};

    private final Function<String, BackgroundScene> sceneProvider;
    private BackgroundScene scene;
    private int selectedOption;
    private FileMenuFrameSnapshot snapshot;

    public FileSaveController(Function<String, BackgroundScene> sceneProvider) {
        this.sceneProvider = Objects.requireNonNull(sceneProvider, "sceneProvider");
        refreshSnapshot();
    }

    public FileMenuFrameSnapshot snapshot() {
        return snapshot;
    }

    public FileSaveAction tick(InputState inputState, InputConfig inputConfig) {
        Objects.requireNonNull(inputState, "inputState");
        Objects.requireNonNull(inputConfig, "inputConfig");

        if (inputState.wasPressed(inputConfig.upKey())
            || inputState.wasPressed(inputConfig.downKey())) {
            selectedOption = (selectedOption + 1) % OPTION_COUNT;
        }

        FileSaveAction action = FileSaveAction.none();
        if (inputState.wasPressed(inputConfig.aKey())
            || inputState.wasPressed(inputConfig.bKey())
            || inputState.wasPressed(inputConfig.menuOpenKey())) {
            action = new FileSaveAction(selectedOption == 0
                ? FileSaveAction.Type.RETURN_TO_GAME
                : FileSaveAction.Type.SAVE_AND_QUIT);
        }
        refreshSnapshot();
        return action;
    }

    private void refreshSnapshot() {
        if (scene == null) {
            scene = Objects.requireNonNull(
                sceneProvider.apply(BackgroundSceneCatalog.FILE_SAVE_SCENE),
                "sceneProvider returned null for " + BackgroundSceneCatalog.FILE_SAVE_SCENE);
        }
        int oamY = SAVE_OPTION_OAM_Y[selectedOption];
        snapshot = new FileMenuFrameSnapshot(
            BackgroundSceneCatalog.FILE_SAVE_SCENE,
            selectedOption,
            false,
            false,
            0,
            0,
            new int[5],
            scene.tilemap(),
            scene.attrmap(),
            scene.palettes(),
            scene.objectPalettes(),
            List.of(new IntroSprite(SAVE_ARROW_TILE,
                SAVE_ARROW_OAM_X - 8, oamY - 8, 0, false, false)));
    }
}
