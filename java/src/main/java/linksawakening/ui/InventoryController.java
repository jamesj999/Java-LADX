package linksawakening.ui;

import linksawakening.gpu.GPU;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;

public final class InventoryController {

    private final InputState inputState;
    private final InputConfig inputConfig;
    private final InventoryMenu menu;

    public InventoryController(InputState inputState, InputConfig inputConfig, InventoryMenu menu) {
        this.inputState = inputState;
        this.inputConfig = inputConfig;
        this.menu = menu;
    }

    public void dispatchToggleInput() {
        if (inputState.wasPressed(inputConfig.menuOpenKey())) {
            menu.requestToggle();
        }
    }

    public void tick() {
        menu.tick();
    }

    public void dispatchMenuInput() {
        if (!menu.isFullyOpen()) {
            return;
        }
        if (inputState.wasPressed(inputConfig.leftKey())) {
            menu.moveCursor(-1, 0);
        }
        if (inputState.wasPressed(inputConfig.rightKey())) {
            menu.moveCursor(+1, 0);
        }
        if (inputState.wasPressed(inputConfig.upKey())) {
            menu.moveCursor(0, -1);
        }
        if (inputState.wasPressed(inputConfig.downKey())) {
            menu.moveCursor(0, +1);
        }
        if (inputState.wasPressed(inputConfig.aKey())) {
            menu.pressA();
        }
        if (inputState.wasPressed(inputConfig.bKey())) {
            menu.pressB();
        }
    }

    public boolean shouldBlockOverworldInput() {
        return menu.shouldBlockOverworldInput();
    }

    public void render(byte[] displayBuffer, GPU gpu, int[][] roomPalettes) {
        menu.render(displayBuffer, gpu, roomPalettes);
    }

    public void render(byte[] displayBuffer, GPU gpu) {
        menu.render(displayBuffer, gpu);
    }
    public boolean canSubmitClosedWindow() { return menu.canSubmitClosedWindow(); }
    public InventoryMenu.ClosedWindow closedWindow(int[][] roomPalettes) {
        return menu.closedWindow(roomPalettes);
    }
    public void submit(linksawakening.render.HardwareFrame frame, GPU gpu, int[][] palettes) {
        menu.submit(frame, gpu, palettes);
    }
    public boolean canSubmitFullyOpenWindow() { return menu.isFullyOpen(); }
    public int windowY() { return menu.windowY(); }
}
