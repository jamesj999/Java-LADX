package linksawakening.app;

import static linksawakening.world.room.RoomConstants.*;
import static org.lwjgl.glfw.GLFW.*;

import linksawakening.app.screen.Screen;
import linksawakening.config.AppConfig;
import linksawakening.cutscene.CutsceneManager;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;

/** Compatibility names for tests outside the app ownership boundary. */
public final class InputRouter {
    private InputRouter() {}

    public static boolean skipIntroCutsceneIfRequested(int key, int action, CutsceneManager manager) {
        return linksawakening.app.input.InputRouter.skipIntroCutsceneIfRequested(key, action, manager);
    }

    public static boolean shouldRunDebugDump(AppConfig config, int key, int action) {
        return linksawakening.app.input.InputRouter.shouldRunDebugDump(config, key, action);
    }

    public static boolean shouldEnterFileSelection(Screen screen, int key, int action) {
        return linksawakening.app.input.InputRouter.shouldEnterFileSelection(screen, key, action);
    }

    public static boolean shouldEnterFileSave(InputConfig inputConfig, InputState inputState, int key, int action) {
        return linksawakening.app.input.InputRouter.shouldEnterFileSave(inputConfig, inputState, key, action);
    }
}
