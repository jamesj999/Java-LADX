package linksawakening.ui;

import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.GPU;
import linksawakening.state.PlayerState;

import java.util.Objects;

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
    private final GameplaySoundSink soundSink;
    private final OcarinaSongMenu ocarinaSongMenu;
    private final OcarinaPopupRenderer ocarinaPopupRenderer;
    private final byte[] romData;
    private boolean ocarinaTilesLoaded;
    private final InventoryInstrumentPanel instrumentPanel;
    private final InventoryDungeonPanel dungeonPanel;
    private int dungeonMapId, dungeonPosition = -1, dungeonFrame;
    private boolean dungeonCollapsed;
    private byte[] dungeonItems = new byte[5], dungeonRooms = new byte[64];
    private byte[] instrumentFlags = new byte[8];
    private boolean showInstruments = true;
    private int instrumentPaletteFrame;

    private int windowY = WINDOW_Y_CLOSED;
    private int subscreenScrollIncrement = CLOSED_INCREMENT;
    private boolean inventoryAppearing = false;
    private int cursorSlot = 0;
    // Mirrors wInventoryCursorFrameCounter (bank20.asm:62A2). Bit 4 drives
    // the cursor blink (on for 16 frames, off for 16). Reset on any dpad
    // press so the bracket is guaranteed visible the frame the cursor moves.
    private int cursorFrameCounter = 0;

    public InventoryMenu(InventoryTilemapLoader tilemapLoader, PlayerState playerState) {
        this(tilemapLoader, playerState, GameplaySoundSink.none(), null);
    }

    public InventoryMenu(
            InventoryTilemapLoader tilemapLoader,
            PlayerState playerState,
            GameplaySoundSink soundSink) {
        this(tilemapLoader, playerState, soundSink, null);
    }

    public InventoryMenu(
            InventoryTilemapLoader tilemapLoader,
            PlayerState playerState,
            GameplaySoundSink soundSink,
            byte[] romData) {
        this.tilemapLoader = tilemapLoader;
        this.playerState = playerState;
        this.soundSink = Objects.requireNonNull(soundSink, "soundSink");
        this.ocarinaSongMenu = new OcarinaSongMenu(playerState, this.soundSink);
        this.ocarinaPopupRenderer = new OcarinaPopupRenderer(tilemapLoader.ocarinaPopupOam());
        this.romData = romData;
        instrumentPanel = romData == null ? null : new InventoryInstrumentPanel(romData);
        dungeonPanel = romData == null ? null : new InventoryDungeonPanel(romData);
    }

    /**
     * Called when the player presses the menu button. Ignored while the menu
     * is mid-slide, matching the disassembly guard on wInventoryAppearing.
     */
    public void requestToggle() {
        if (inventoryAppearing) {
            return;
        }
        if (ocarinaSongMenu.isVisible()) {
            // The ROM consumes Start by closing the popup first. Once its
            // 16-frame closing animation ends, the inventory bar itself is
            // closed on the following gameplay tick.
            ocarinaSongMenu.requestClose(true);
            return;
        }
        toggleInventorySlide();
    }

    private void toggleInventorySlide() {
        if (inventoryAppearing) {
            return;
        }
        boolean opening = isFullyClosed();
        if (opening) instrumentPaletteFrame = 0;
        subscreenScrollIncrement = -subscreenScrollIncrement;
        inventoryAppearing = true;
        soundSink.play(opening ? GameplaySoundEvent.INVENTORY_OPEN : GameplaySoundEvent.INVENTORY_CLOSE);
    }

    /**
     * Advances the slide by one frame. Called every tick before rendering.
     */
    public void tick() {
        if (ocarinaSongMenu.tick()
                && ocarinaSongMenu.consumeCloseInventoryRequest()
                && isFullyOpen()) {
            toggleInventorySlide();
        }
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
            instrumentPaletteFrame = (instrumentPaletteFrame + 1) & 127;
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

    public boolean isTransitioning() {
        return inventoryAppearing;
    }

    public int cursorSlot() {
        return cursorSlot;
    }

    public boolean isOcarinaMenuVisible() {
        return ocarinaSongMenu.isVisible();
    }

    public boolean isOcarinaMenuReady() {
        return ocarinaSongMenu.isReady();
    }

    public int ocarinaMenuAnimationFrame() {
        return ocarinaSongMenu.animationFrame();
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
        if (ocarinaSongMenu.isVisible()) {
            if (dx != 0 && dy == 0) {
                ocarinaSongMenu.moveSelection(dx);
            }
            return;
        }
        int next = cursorSlot;
        if (dx != 0 && dy == 0) {
            next += dx;
        } else if (dy != 0 && dx == 0) {
            next += dy * SUBSCREEN_COLS;
        }
        if (next >= 0 && next < PlayerState.SUBSCREEN_SLOT_COUNT && next != cursorSlot) {
            cursorSlot = next;
            soundSink.play(GameplaySoundEvent.MENU_MOVE);
            if (playerState.subscreenItem(cursorSlot) == PlayerState.INVENTORY_OCARINA
                    && playerState.ocarinaSongFlags() != 0) {
                ocarinaSongMenu.requestOpen();
            }
        }
        cursorFrameCounter = 0;
    }

    public void pressA() {
        if (!isFullyOpen()) {
            return;
        }
        int selectedItem = playerState.subscreenItem(cursorSlot);
        playerState.swapItemAWithSubscreen(cursorSlot);
        soundSink.play(GameplaySoundEvent.MENU_VALIDATE);
        openOcarinaIfSelected(selectedItem);
    }

    public void pressB() {
        if (!isFullyOpen()) {
            return;
        }
        int selectedItem = playerState.subscreenItem(cursorSlot);
        playerState.swapItemBWithSubscreen(cursorSlot);
        soundSink.play(GameplaySoundEvent.MENU_VALIDATE);
        openOcarinaIfSelected(selectedItem);
    }

    private void openOcarinaIfSelected(int selectedItem) {
        if (selectedItem == PlayerState.INVENTORY_OCARINA
                && playerState.ocarinaSongFlags() != 0) {
            ocarinaSongMenu.requestOpen();
        }
    }

    public int instrumentPaletteTick() {
        return instrumentPaletteFrame;
    }

    public void setWorldContext(boolean indoor, int mapId, byte[] dungeonProgress) {
        if (dungeonProgress.length != 8) throw new IllegalArgumentException("Eight instrument flags required");
        // $02:6183-$619E: dungeons 0..7 and Color Dungeon use a minimap.
        showInstruments = !indoor || (mapId >= 8 && mapId != 0xFF);
        instrumentFlags = dungeonProgress.clone();
        dungeonMapId = mapId;
        dungeonCollapsed = (dungeonProgress[6] & 4) != 0;
    }

    public void setDungeonContext(byte[] items, byte[] rooms, int position, int frame) {
        dungeonItems = items.clone();
        dungeonRooms = rooms.clone();
        dungeonPosition = position;
        dungeonFrame = frame;
    }

    public void render(byte[] displayBuffer, GPU gpu) {
        render(displayBuffer, gpu, null);
    }

    public void render(byte[] displayBuffer, GPU gpu, int[][] roomPalettes) {
        if (romData != null) gpu.updatePowderInventoryTiles(romData, playerState.hasToadstool());
        int[] tilemap = tilemapLoader.copyBaseTilemap();
        int[] attrmap = tilemapLoader.copyAttrmap();
        HudOverlay.apply(tilemap, attrmap, playerState);
        boolean cursorVisible = isFullyOpen() && (cursorFrameCounter & 0x10) == 0;
        InventorySubscreenOverlay.apply(tilemap, attrmap, playerState, cursorSlot, cursorVisible);

        if (dungeonPanel != null && !showInstruments && !isFullyClosed())
            dungeonPanel.apply(tilemap, attrmap, dungeonMapId, dungeonCollapsed, dungeonItems, dungeonRooms);
        int offsetY = windowY;
        if (offsetY >= Framebuffer.HEIGHT) {
            return;
        }

        int[][] inventoryPalettes = tilemapLoader.palettes();
        if (instrumentPanel != null && showInstruments && !isFullyClosed()) {
            inventoryPalettes = instrumentPanel.palettes(inventoryPalettes, instrumentPaletteFrame);
        }
        TilemapRenderer.render(
            displayBuffer,
            gpu,
            tilemap,
            attrmap,
            isFullyClosed() && roomPalettes != null ? roomPalettes : inventoryPalettes,
            InventoryTilemapLoader.WIDTH,
            InventoryTilemapLoader.HEIGHT,
            0,
            offsetY
        );

        if (instrumentPanel != null && showInstruments && !isFullyClosed()) {
            instrumentPanel.render(displayBuffer, attrmap, inventoryPalettes, instrumentFlags, offsetY);
        }

        if (dungeonPanel != null && !showInstruments && !isFullyClosed())
            dungeonPanel.render(displayBuffer, tilemap, attrmap, inventoryPalettes, offsetY,
                dungeonMapId, dungeonPosition, dungeonFrame, tilemapLoader.objectPalettes());

        if (instrumentPanel != null && isFullyOpen()) {
            instrumentPanel.renderNavigation(displayBuffer, tilemapLoader.objectPalettes(), instrumentPaletteFrame);
        }

        if (ocarinaSongMenu.isVisible()) {
            if (romData != null && !ocarinaTilesLoaded) {
                gpu.loadOcarinaSymbolsTiles(romData);
                ocarinaTilesLoaded = true;
            }
            ocarinaPopupRenderer.render(displayBuffer, gpu, tilemapLoader.objectPalettes(),
                ocarinaSongMenu);
        } else if (ocarinaTilesLoaded && romData != null) {
            gpu.loadSharedVfxTiles(romData);
            ocarinaTilesLoaded = false;
        }
    }
    public record ClosedWindow(int[] tiles, int[] attributes, int[][] palettes, int y) {}

    public boolean canSubmitClosedWindow() {
        return isFullyClosed() && !ocarinaSongMenu.isVisible();
    }

    /** Read-only snapshot of the closed inventory's HUD window. */
    public ClosedWindow closedWindow(int[][] roomPalettes) {
        if (!canSubmitClosedWindow()) throw new IllegalStateException("Inventory window is open");
        int[] tiles = tilemapLoader.copyBaseTilemap();
        int[] attributes = tilemapLoader.copyAttrmap();
        HudOverlay.apply(tiles, attributes, playerState);
        InventorySubscreenOverlay.apply(tiles, attributes, playerState, cursorSlot, false);
        return new ClosedWindow(tiles, attributes,
            roomPalettes == null ? tilemapLoader.palettes() : roomPalettes, windowY);
    }
    public void submit(linksawakening.render.HardwareFrame frameModel, GPU gpu, int[][] roomPalettes) {
        if (romData != null) gpu.updatePowderInventoryTiles(romData, playerState.hasToadstool());
        int[] tilemap = tilemapLoader.copyBaseTilemap();
        int[] attrmap = tilemapLoader.copyAttrmap();
        HudOverlay.apply(tilemap, attrmap, playerState);
        boolean cursorVisible = isFullyOpen() && (cursorFrameCounter & 0x10) == 0;
        InventorySubscreenOverlay.apply(tilemap, attrmap, playerState, cursorSlot, cursorVisible);

        if (dungeonPanel != null && !showInstruments && !isFullyClosed())
            dungeonPanel.apply(tilemap, attrmap, dungeonMapId, dungeonCollapsed, dungeonItems, dungeonRooms);
        int offsetY = windowY;
        if (offsetY >= Framebuffer.HEIGHT) {
            return;
        }

        int[][] inventoryPalettes = tilemapLoader.palettes();
        if (instrumentPanel != null && showInstruments && !isFullyClosed()) {
            inventoryPalettes = instrumentPanel.palettes(inventoryPalettes, instrumentPaletteFrame);
        }
        TilemapRenderer.submit(
            frameModel,
            gpu,
            tilemap,
            attrmap,
            isFullyClosed() && roomPalettes != null ? roomPalettes : inventoryPalettes,
            InventoryTilemapLoader.WIDTH,
            InventoryTilemapLoader.HEIGHT,
            0,
            offsetY
        );

        if (instrumentPanel != null && showInstruments && !isFullyClosed()) {
            instrumentPanel.submit(frameModel, attrmap, inventoryPalettes, instrumentFlags, offsetY);
        }

        if (dungeonPanel != null && !showInstruments && !isFullyClosed())
            dungeonPanel.submit(frameModel, tilemap, attrmap, inventoryPalettes, offsetY,
                dungeonMapId, dungeonPosition, dungeonFrame, tilemapLoader.objectPalettes());

        if (instrumentPanel != null && isFullyOpen()) {
            instrumentPanel.submitNavigation(frameModel, tilemapLoader.objectPalettes(), instrumentPaletteFrame);
        }

        if (ocarinaSongMenu.isVisible()) {
            if (romData != null && !ocarinaTilesLoaded) {
                gpu.loadOcarinaSymbolsTiles(romData);
                ocarinaTilesLoaded = true;
            }
            ocarinaPopupRenderer.submit(frameModel, gpu, tilemapLoader.objectPalettes(),
                ocarinaSongMenu);
        } else if (ocarinaTilesLoaded && romData != null) {
            gpu.loadSharedVfxTiles(romData);
            ocarinaTilesLoaded = false;
        }
    }
}
