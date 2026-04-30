package linksawakening.gameplay;

import linksawakening.dialog.DialogController;
import linksawakening.input.InputConfig;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

public final class GameplayDialogInput {

    private GameplayDialogInput() {
    }

    public static boolean handleOverworldKeyPress(int key,
                                                  int action,
                                                  InputConfig inputConfig,
                                                  DialogController dialogController) {
        if (action != GLFW_PRESS || !blocksGameplay(dialogController)) {
            return false;
        }
        if (isAdvanceKey(key, inputConfig)) {
            dialogController.advance();
            return true;
        }
        return false;
    }

    public static boolean blocksGameplay(DialogController dialogController) {
        return dialogController != null && dialogController.isActive();
    }

    public static boolean isAdvanceKey(int key, InputConfig inputConfig) {
        if (key == GLFW_KEY_ENTER) {
            return true;
        }
        return isActionButtonKey(key, inputConfig);
    }

    public static boolean isActionButtonKey(int key, InputConfig inputConfig) {
        return inputConfig != null && (key == inputConfig.aKey() || key == inputConfig.bKey());
    }
}
