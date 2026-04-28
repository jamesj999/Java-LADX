package linksawakening.ui;

import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.GPU;
import linksawakening.state.PlayerState;

/**
 * Port of the DMG inventory-slide mechanic from the LADX disassembly
 * (see bank1.asm:InitializeInventoryBar and bank2.asm:jr_002_61C6).
 *
 * The GB Window layer holds a single 20x18 inventory tilemap. Its Y origin
 * (wWindowY) slides between $00 (fully open, covering the screen) and $80
 * (fully closed, leaving only the top 2 rows visible as the status bar).
 * Each slide step advances wWindowY by wSubscreenScrollIncrement (+/-8).
 *
 * <p>While the menu is fully open, the cursor navigates the 10-slot subscreen
 * (bank20.asm:moveInventoryCursor): left/right step by 1, up/down step by 2,
 * giving a 2-column x 5-row grid. A/B press swaps the cursor's subscreen slot
 * with the corresponding button slot.
 */
public final class InventoryMenu {

    private static final int WINDOW_Y_OPEN = 0x00;
    private static final int WINDOW_Y_CLOSED = 0x80;
    private static final int SCROLL_STEP = 0x08;
    private static final int CLOSED_INCREMENT = +SCROLL_STEP;

    // Subscreen grid: 2 cols x 5 rows. Even slot index = left column,
    // odd = right column. Up/down moves by 2 slots (next row, same col);
    // left/right moves by 1 (other column, same row).
    private static final int SUBSCREEN_COLS = 2;

    private final InventoryTilemapLoader tilemapLoader;
    private final PlayerState playerState;

    private int windowY = WINDOW_Y_CLOSED;
    private int subscreenScrollIncrement = CLOSED_INCREMENT;
    private boolean inventoryAppearing = false;
    private int cursorSlot = 0;
    // Mirrors wInventoryCursorFrameCounter (bank20.asm:62A2). Bit 4 drives
    // the cursor blink (on for 16 frames, off for 16). Reset on any dpad
    // press so the bracket is guaranteed visible the frame the cursor moves.
    private int cursorFrameCounter = 0;

    public InventoryMenu(InventoryTilemapLoader tilemapLoader, PlayerState playerState) {
        this.tilemapLoader = tilemapLoader;
        this.playerState = playerState;
    }

    /**
     * Called when the player presses the menu button. Ignored while the menu
     * is mid-slide, matching the disassembly guard on wInventoryAppearing.
     */
    public void requestToggle() {
        if (inventoryAppearing) {
            return;
        }
        subscreenScrollIncrement = -subscreenScrollIncrement;
        inventoryAppearing = true;
    }

    /**
     * Advances the slide by one frame. Called every tick before rendering.
     */
    public void tick() {
        if (inventoryAppearing) {
            windowY += subscreenScrollIncrement;
            if (windowY <= WINDOW_Y_OPEN) {
                windowY = WINDOW_Y_OPEN;
                inventoryAppearing = false;
            } else if (windowY >= WINDOW_Y_CLOSED) {
                windowY = WINDOW_Y_CLOSED;
                inventoryAppearing = false;
            }
        }
        if (isFullyOpen()) {
            cursorFrameCounter = (cursorFrameCounter + 1) & 0xFF;
        }
    }

    public boolean isFullyClosed() {
        return windowY == WINDOW_Y_CLOSED && !inventoryAppearing;
    }

    public boolean isFullyOpen() {
        return windowY == WINDOW_Y_OPEN && !inventoryAppearing;
    }

    public boolean shouldBlockOverworldInput() {
        return inventoryAppearing || windowY != WINDOW_Y_CLOSED;
    }

    public int windowY() {
        return windowY;
    }

    public int cursorSlot() {
        return cursorSlot;
    }

    /**
     * Move the cursor one step. {@code dx} and {@code dy} should each be -1,
     * 0, or +1 (pass only one non-zero value per call — diagonals keep the
     * current slot, matching the original's OR-reducing table lookup).
     */
    public void moveCursor(int dx, int dy) {
        if (!isFullyOpen()) {
            return;
        }
        int next = cursorSlot;
        if (dx != 0 && dy == 0) {
            next += dx;
        } else if (dy != 0 && dx == 0) {
            next += dy * SUBSCREEN_COLS;
        }
        if (next >= 0 && next < PlayerState.SUBSCREEN_SLOT_COUNT) {
            cursorSlot = next;
        }
        cursorFrameCounter = 0;
    }

    public void pressA() {
        if (!isFullyOpen()) {
            return;
        }
        playerState.swapItemAWithSubscreen(cursorSlot);
    }

    public void pressB() {
        if (!isFullyOpen()) {
            return;
        }
        playerState.swapItemBWithSubscreen(cursorSlot);
    }

    public void render(byte[] displayBuffer, GPU gpu) {
        int[] tilemap = tilemapLoader.copyBaseTilemap();
        int[] attrmap = tilemapLoader.copyAttrmap();
        HudOverlay.apply(tilemap, playerState);
        boolean cursorVisible = isFullyOpen() && (cursorFrameCounter & 0x10) == 0;
        InventorySubscreenOverlay.apply(tilemap, attrmap, playerState, cursorSlot, cursorVisible);

        int offsetY = windowY;
        if (offsetY >= Framebuffer.HEIGHT) {
            return;
        }

        TilemapRenderer.render(
            displayBuffer,
            gpu,
            tilemap,
            attrmap,
            tilemapLoader.palettes(),
            InventoryTilemapLoader.WIDTH,
            InventoryTilemapLoader.HEIGHT,
            0,
            offsetY
        );
    }
}
