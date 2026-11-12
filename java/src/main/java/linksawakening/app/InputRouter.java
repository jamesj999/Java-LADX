package linksawakening.app;

import static linksawakening.world.room.RoomConstants.*;
import static org.lwjgl.glfw.GLFW.*;

import java.util.Objects;
import linksawakening.app.gameplay.DialogInteractions;
import linksawakening.app.screen.FileSaveScreen;
import linksawakening.app.screen.GameplayScreen;
import linksawakening.app.screen.Screen;
import linksawakening.app.screen.TitleScreen;
import linksawakening.app.story.OpeningStory;
import linksawakening.config.AppConfig;
import linksawakening.cutscene.CutsceneManager;
import linksawakening.dialog.DialogController;
import linksawakening.entity.Link;
import linksawakening.gameplay.GameplayDialogInput;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.rom.RomTables;
import linksawakening.ui.InventoryController;
import linksawakening.world.room.ActiveRoom;
import linksawakening.world.room.RoomSession;
import linksawakening.world.room.RoomTransitionCoordinator;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;
import linksawakening.world.transition.Warp;

public final class InputRouter implements linksawakening.app.gameplay.DebugScroll {
    private final SessionContext context;

    public InputRouter(SessionContext context) {
        this.context = Objects.requireNonNull(context);
    }

    public RoomSession roomSession() {
        return context.roomSession();
    }

    public TransitionController transitionController() {
        return context.transitionController();
    }

    public RoomTransitionCoordinator roomTransitionCoordinator() {
        return context.roomTransitionCoordinator();
    }

    public int ROOM_PIXEL_WIDTH() {
        return context.roomPixelWidth();
    }

    public int ROOM_PIXEL_HEIGHT() {
        return context.roomPixelHeight();
    }

    public ScrollController scrollController() {
        return context.scrollController();
    }

    public InputConfig inputConfig() {
        return context.inputConfig();
    }

    public InputState inputState() {
        return context.inputState();
    }

    public InventoryController inventoryController() {
        return context.inventoryController();
    }

    public DialogController dialogController() {
        return context.dialogController();
    }

    public RomTables romTables() {
        return context.romTables();
    }

    public Link link() {
        return context.link();
    }

    public AppConfig config() {
        return context.config();
    }

    public linksawakening.app.screen.SessionLifecycle stop() {
        return context.stop();
    }

    public DebugActions toggleDebug() {
        return context.inputRouterToggleDebug();
    }

    public Screen screen() {
        return context.navigator.current();
    }

    public OpeningStory story() {
        return context.story();
    }

    public DialogInteractions dialogs() {
        return context.dialogInteractions();
    }

    public void onKeyEvent(int key, int action) {
        // Desktop shortcut only: Escape never enters the Game Boy button state.
        if (key == GLFW_KEY_ESCAPE) {
            if (action == GLFW_PRESS) {
                Screen current = screen();
                if (current instanceof GameplayScreen gameplay) {
                    gameplay.requestFileSave();
                } else if (!(current instanceof FileSaveScreen)) {
                    stop().requestStop();
                }
            }
            return;
        }
        {
            inputState().onKeyEvent(key, action);
        }

        if (action == GLFW_PRESS && config().debugEnabled()) {
            if (key == GLFW_KEY_F2) {
                context.navigator.startDebugGameplay();
                return;
            }
            if (key == GLFW_KEY_F3) {
                toggleDebug().toggleNoClip();
                return;
            }
        }

        screen().onKey(key, action);
    }

    public void gameplayKey(int key, int action) {
        if (link().isPassingOut()) return;
        if (action != GLFW_PRESS) return;

        if (roomSession().doors().tailCaveKeyholeSequenceActive()) {
            return;
        }

        if (story().marinWakeUpBlocksKeyInput()) {
            if (GameplayDialogInput.handleOverworldKeyPress(key, action, inputConfig(), dialogController())) {
                context.gameplayInput().consume();
            }
            return;
        }

        if (GameplayDialogInput.handleOverworldKeyPress(key, action, inputConfig(), dialogController())) {
            context.gameplayInput().consume();
            return;
        }
        // ApplyGotItem bypasses InitGotItemSequence ($00:1040-1077),
        // including its inventory/map and normal interaction input.
        if (link().dialogGotItemActive()) return;

        if (story().tryOpenTarinShieldDialog(key)) {
            return;
        }
        if (story().tryOpenMarinFollowUpDialog(key)) {
            return;
        }
        if (dialogs().tryOpenChest(key)) {
            return;
        }
        if (dialogs().tryOpenOverworldDialog(key)) {
            return;
        }

        if (!link().recoveryBlocksInput()) inventoryController().dispatchToggleInput();

        // Debug: F1 dumps Link's surrounding cells with object IDs and
        // physics flags, to diagnose walk-through-wall/tree reports.
        if (shouldRunDebugDump(config(), key, action)) {
            dumpLinkSurroundings();
        }
    }

    public static boolean skipIntroCutsceneIfRequested(int key, int action, CutsceneManager manager) {
        return key == GLFW_KEY_ENTER && action == GLFW_PRESS && manager != null && manager.skipIntroToTitle();
    }

    public static boolean shouldRunDebugDump(AppConfig config, int key, int action) {
        return config.debugEnabled() && key == GLFW_KEY_F1 && action == GLFW_PRESS;
    }

    public void dumpLinkSurroundings() {
        if (!roomSession().rooms().hasActiveRoom()) return;
        ActiveRoom room = roomSession().rooms().activeRoom();
        int[] roomObjectsArea = room.roomObjectsArea();
        int lx = link().pixelX();
        int ly = link().pixelY();
        DebugLog.log("=== Link debug dump ===");
        DebugLog.log("roomId=" + String.format("%02X", room.roomId()) + " mapCat=" + room.mapCategory() + " mapId="
                + String.format("%02X", room.mapId()));
        DebugLog.log("Link pixel (" + lx + "," + ly + ") tile=" + String.format("%02X", Warp.packTileLocation(lx, ly)));
        int centerCol = (lx + 8) >> 4;
        int centerRow = (ly + 8) >> 4;
        int tableIdx = (room.mapCategory() == Warp.CATEGORY_OVERWORLD)
                ? RomTables.PHYSICS_TABLE_OVERWORLD
                : RomTables.PHYSICS_TABLE_INDOORS1;
        for (int dy = -1; dy <= 2; dy++) {
            StringBuilder sb = new StringBuilder(" row " + (centerRow + dy) + ":");
            for (int dx = -1; dx <= 2; dx++) {
                int col = centerCol + dx;
                int row = centerRow + dy;
                if (col < 0 || col > 9 || row < 0 || row > 7) {
                    sb.append(" --");
                    continue;
                }
                int areaIndex = ROOM_OBJECTS_BASE + row * ROOM_OBJECT_ROW_STRIDE + col;
                int id = roomObjectsArea[areaIndex];
                int flag = romTables().physicsTables().objectPhysicsFlag(tableIdx, id);
                sb.append(" id=")
                        .append(String.format("%02X", id))
                        .append("(fl=")
                        .append(String.format("%02X", flag))
                        .append(")");
            }
            DebugLog.log(sb.toString());
        }
    }

    public boolean tryDebugScreenScroll() {
        if (!inputState().isDown(GLFW_KEY_LEFT_SHIFT) && !inputState().isDown(GLFW_KEY_RIGHT_SHIFT)) {
            return false;
        }
        int direction = inputState().isDown(inputConfig().leftKey())
                ? ScrollController.LEFT
                : inputState().isDown(inputConfig().rightKey())
                        ? ScrollController.RIGHT
                        : inputState().isDown(inputConfig().upKey())
                                ? ScrollController.UP
                                : inputState().isDown(inputConfig().downKey())
                                        ? ScrollController.DOWN
                                        : ScrollController.NONE;
        if (direction == ScrollController.NONE
                || scrollController().isActive()
                || transitionController().isInputBlocked()
                || !roomSession().rooms().hasActiveRoom()) {
            return false;
        }
        switch (direction) {
            case ScrollController.LEFT -> link().setPixelPosition(-1, link().pixelY());
            case ScrollController.RIGHT -> link().setPixelPosition(ROOM_PIXEL_WIDTH(), link().pixelY());
            case ScrollController.UP -> link().setPixelPosition(link().pixelX(), -1);
            case ScrollController.DOWN -> link().setPixelPosition(link().pixelX(), ROOM_PIXEL_HEIGHT());
            default -> {
                return false;
            }
        }
        if (roomSession().rooms().activeRoom().mapCategory() == Warp.CATEGORY_OVERWORLD) {
            roomTransitionCoordinator().handleOverworldBoundary(link());
        } else {
            roomTransitionCoordinator().handleWarpAndIndoorBoundaries(link());
        }
        return scrollController().isActive();
    }

    public static boolean shouldEnterFileSelection(Screen screen, int key, int action) {
        return screen instanceof TitleScreen && key == GLFW_KEY_ENTER && action == GLFW_PRESS;
    }
    /** presentSaveScreenIfNeeded ($00:0E46), before ordinary frame input. */
    public boolean tryPresentFileSave() {
        // Consume before gating: a blocked desktop request must not fire later.
        boolean requested = screen() instanceof GameplayScreen gameplay && gameplay.consumeFileSaveRequest();
        if (link().isPassingOut()
                || roomSession().doors().tailCaveKeyholeSequenceActive()
                || story().marinWakeUpBlocksKeyInput()) return false;
        if ((requested
                        || shouldEnterFileSave(
                                inputConfig(), inputState(), inputConfig().selectKey(), GLFW_PRESS))
                && canPresentFileSave()) {
            context.saves().startFileSave();
            return true;
        }
        return false;
    }

    public static boolean shouldEnterFileSave(InputConfig inputConfig, InputState inputState, int key, int action) {
        if (inputConfig == null || inputState == null || action != GLFW_PRESS) {
            return false;
        }
        return inputState.isDown(inputConfig.aKey())
                && inputState.isDown(inputConfig.bKey())
                && inputState.isDown(inputConfig.menuOpenKey())
                && inputState.isDown(inputConfig.selectKey())
                // The ROM compares the whole mask with $F0: all directions must be released.
                && !inputState.isDown(inputConfig.upKey())
                && !inputState.isDown(inputConfig.downKey())
                && !inputState.isDown(inputConfig.leftKey())
                && !inputState.isDown(inputConfig.rightKey());
    }

    public boolean canPresentFileSave() {
        return (!link().recoveryBlocksInput())
                && !scrollController().isActive()
                && !transitionController().isInputBlocked()
                && (!inventoryController().shouldBlockOverworldInput())
                && (!GameplayDialogInput.blocksGameplay(dialogController()));
    }

    public void headlessButton(String button, boolean pressed) {
        if (button.equals("none")) return;
        if (button.contains("+")) {
            for (String part : button.split("\\+")) headlessButton(part, pressed);
            return;
        }
        int key =
                switch (button) {
                    case "up" -> inputConfig().upKey();
                    case "down" -> inputConfig().downKey();
                    case "left" -> inputConfig().leftKey();
                    case "right" -> inputConfig().rightKey();
                    case "a" -> inputConfig().aKey();
                    case "b" -> inputConfig().bKey();
                    case "start" -> inputConfig().menuOpenKey();
                    case "select" -> inputConfig().selectKey();
                    case "escape" -> GLFW_KEY_ESCAPE;
                    default -> throw new IllegalArgumentException("Unknown button: " + button);
                };
        onKeyEvent(key, pressed ? GLFW_PRESS : GLFW_RELEASE);
    }
}
