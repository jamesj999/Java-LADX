package linksawakening.app.input;

import static linksawakening.world.room.RoomConstants.*;
import static org.lwjgl.glfw.GLFW.*;

import java.util.Objects;
import linksawakening.app.debug.DebugLog;
import linksawakening.app.gameplay.DebugScroll;
import linksawakening.app.screen.FileSaveScreen;
import linksawakening.app.screen.GameplayScreen;
import linksawakening.app.screen.Screen;
import linksawakening.app.screen.TitleScreen;
import linksawakening.app.services.InputServices;
import linksawakening.app.services.PlayerServices;
import linksawakening.app.services.UiServices;
import linksawakening.app.services.WorldServices;
import linksawakening.app.state.SessionState;
import linksawakening.config.AppConfig;
import linksawakening.cutscene.CutsceneManager;
import linksawakening.gameplay.GameplayDialogInput;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.rom.RomTables;
import linksawakening.world.room.ActiveRoom;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.Warp;

/** Routes host keys and queued gameplay edges to their screen and interaction owners. */
public final class InputRouter implements DebugScroll {
    private final InputServices input;
    private final WorldServices world;
    private final PlayerServices player;
    private final UiServices ui;
    private final SessionState state;
    private final InputActions actions;

    public InputRouter(
            InputServices input,
            WorldServices world,
            PlayerServices player,
            UiServices ui,
            SessionState state,
            InputActions actions) {
        this.input = Objects.requireNonNull(input);
        this.world = Objects.requireNonNull(world);
        this.player = Objects.requireNonNull(player);
        this.ui = Objects.requireNonNull(ui);
        this.state = Objects.requireNonNull(state);
        this.actions = Objects.requireNonNull(actions);
    }

    public int ROOM_PIXEL_WIDTH() {
        return 160;
    }

    public int ROOM_PIXEL_HEIGHT() {
        return 128;
    }

    public void onKeyEvent(int key, int action) {
        // Desktop shortcut only: Escape never enters the Game Boy button state.
        if (key == GLFW_KEY_ESCAPE) {
            if (action == GLFW_PRESS) {
                Screen current = actions.navigator().current();
                if (current instanceof GameplayScreen gameplay) {
                    gameplay.requestFileSave();
                } else if (!(current instanceof FileSaveScreen)) {
                    actions.lifecycle().requestStop();
                }
            }
            return;
        }
        {
            input.inputState().onKeyEvent(key, action);
        }

        if (action == GLFW_PRESS && state.options().config().debugEnabled()) {
            if (key == GLFW_KEY_F2) {
                actions.navigator().startDebugGameplay();
                return;
            }
            if (key == GLFW_KEY_F3) {
                actions.debug().toggleNoClip();
                return;
            }
        }

        actions.navigator().current().onKey(key, action);
    }

    public void gameplayKey(int key, int action) {
        if (player.link().status().isPassingOut()) return;
        if (action != GLFW_PRESS) return;

        if (world.roomSession().doors().tailCaveKeyholeSequenceActive()) {
            return;
        }

        if (actions.story().marinWakeUpBlocksKeyInput()) {
            if (GameplayDialogInput.handleOverworldKeyPress(key, action, input.inputConfig(), ui.dialogController())) {
                state.input().consume();
            }
            return;
        }

        if (GameplayDialogInput.handleOverworldKeyPress(key, action, input.inputConfig(), ui.dialogController())) {
            state.input().consume();
            return;
        }
        // ApplyGotItem bypasses InitGotItemSequence ($00:1040-1077),
        // including its inventory/map and normal interaction input.
        if (player.link().animation().dialogGotItemActive()) return;

        if (actions.story().tryOpenTarinShieldDialog(key)) {
            return;
        }
        if (actions.story().tryOpenMarinFollowUpDialog(key)) {
            return;
        }
        if (actions.dialogs().tryOpenChest(key)) {
            return;
        }
        if (actions.dialogs().tryOpenOverworldDialog(key)) {
            return;
        }

        if (!player.link().status().recoveryBlocksInput())
            ui.inventoryController().dispatchToggleInput();

        // Debug: F1 dumps Link's surrounding cells with object IDs and
        // physics flags, to diagnose walk-through-wall/tree reports.
        if (shouldRunDebugDump(state.options().config(), key, action)) {
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
        if (!world.roomSession().rooms().hasActiveRoom()) return;
        ActiveRoom room = world.roomSession().rooms().activeRoom();
        int[] roomObjectsArea = room.roomObjectsArea();
        int lx = player.link().position().pixelX();
        int ly = player.link().position().pixelY();
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
                int flag = actions.tables().physicsTables().objectPhysicsFlag(tableIdx, id);
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
        if (!input.inputState().isDown(GLFW_KEY_LEFT_SHIFT)
                && !input.inputState().isDown(GLFW_KEY_RIGHT_SHIFT)) {
            return false;
        }
        int direction = input.inputState().isDown(input.inputConfig().leftKey())
                ? ScrollController.LEFT
                : input.inputState().isDown(input.inputConfig().rightKey())
                        ? ScrollController.RIGHT
                        : input.inputState().isDown(input.inputConfig().upKey())
                                ? ScrollController.UP
                                : input.inputState().isDown(input.inputConfig().downKey())
                                        ? ScrollController.DOWN
                                        : ScrollController.NONE;
        if (direction == ScrollController.NONE
                || world.scrollController().isActive()
                || world.transitionController().isInputBlocked()
                || !world.roomSession().rooms().hasActiveRoom()) {
            return false;
        }
        switch (direction) {
            case ScrollController.LEFT -> player.link()
                    .position()
                    .setPixelPosition(-1, player.link().position().pixelY());
            case ScrollController.RIGHT -> player.link()
                    .position()
                    .setPixelPosition(
                            ROOM_PIXEL_WIDTH(), player.link().position().pixelY());
            case ScrollController.UP -> player.link()
                    .position()
                    .setPixelPosition(player.link().position().pixelX(), -1);
            case ScrollController.DOWN -> player.link()
                    .position()
                    .setPixelPosition(player.link().position().pixelX(), ROOM_PIXEL_HEIGHT());
            default -> {
                return false;
            }
        }
        if (world.roomSession().rooms().activeRoom().mapCategory() == Warp.CATEGORY_OVERWORLD) {
            world.roomTransitionCoordinator().handleOverworldBoundary(player.link());
        } else {
            world.roomTransitionCoordinator().handleWarpAndIndoorBoundaries(player.link());
        }
        return world.scrollController().isActive();
    }

    public static boolean shouldEnterFileSelection(Screen screen, int key, int action) {
        return screen instanceof TitleScreen && key == GLFW_KEY_ENTER && action == GLFW_PRESS;
    }
    /** presentSaveScreenIfNeeded ($00:0E46), before ordinary frame input. */
    public boolean tryPresentFileSave() {
        // Consume before gating: a blocked desktop request must not fire later.
        boolean requested =
                actions.navigator().current() instanceof GameplayScreen gameplay && gameplay.consumeFileSaveRequest();
        if (player.link().status().isPassingOut()
                || world.roomSession().doors().tailCaveKeyholeSequenceActive()
                || actions.story().marinWakeUpBlocksKeyInput()) return false;
        if ((requested
                        || shouldEnterFileSave(
                                input.inputConfig(),
                                input.inputState(),
                                input.inputConfig().selectKey(),
                                GLFW_PRESS))
                && canPresentFileSave()) {
            actions.saves().startFileSave();
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
        return (!player.link().status().recoveryBlocksInput())
                && !world.scrollController().isActive()
                && !world.transitionController().isInputBlocked()
                && (!ui.inventoryController().shouldBlockOverworldInput())
                && (!GameplayDialogInput.blocksGameplay(ui.dialogController()));
    }

    public void headlessButton(String button, boolean pressed) {
        if (button.equals("none")) return;
        if (button.contains("+")) {
            for (String part : button.split("\\+")) headlessButton(part, pressed);
            return;
        }
        int key =
                switch (button) {
                    case "up" -> input.inputConfig().upKey();
                    case "down" -> input.inputConfig().downKey();
                    case "left" -> input.inputConfig().leftKey();
                    case "right" -> input.inputConfig().rightKey();
                    case "a" -> input.inputConfig().aKey();
                    case "b" -> input.inputConfig().bKey();
                    case "start" -> input.inputConfig().menuOpenKey();
                    case "select" -> input.inputConfig().selectKey();
                    case "escape" -> GLFW_KEY_ESCAPE;
                    default -> throw new IllegalArgumentException("Unknown button: " + button);
                };
        onKeyEvent(key, pressed ? GLFW_PRESS : GLFW_RELEASE);
    }
}
