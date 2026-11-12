package linksawakening.ui;

import java.util.Objects;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.GPU;
import linksawakening.state.InventoryItem;
import linksawakening.state.PlayerState;

/** DX inventory UI; InventoryTransition owns the ROM fade/load/interactive states. */
public final class InventoryMenu {

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

    private final InventoryTransition transition = new InventoryTransition();
    private int cursorSlot = 0;
    // Mirrors wInventoryCursorFrameCounter (bank20.asm:62A2). Bit 4 drives
    // the cursor blink (on for 16 frames, off for 16). Reset on any dpad
    // press so the bracket is guaranteed visible the frame the cursor moves.
    private int cursorFrameCounter = 0;

    public InventoryMenu(InventoryTilemapLoader tilemapLoader, PlayerState playerState) {
        this(tilemapLoader, playerState, GameplaySoundSink.none(), null);
    }

    public InventoryMenu(InventoryTilemapLoader tilemapLoader, PlayerState playerState, GameplaySoundSink soundSink) {
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
     * is fading/loading. DX accepts menu input only in subtype $08 ($20:63C4).
     */
    public void requestToggle() {
        if (isTransitioning()) {
            return;
        }
        if (ocarinaSongMenu.isVisible()) {
            // The ROM consumes Start by closing the popup first. Once its
            // 16-frame closing animation ends, the inventory bar itself is
            // closed on the following gameplay tick.
            ocarinaSongMenu.requestClose(true);
            return;
        }
        toggleInventory();
    }

    private void toggleInventory() {
        if (isFullyClosed()) {
            instrumentPaletteFrame = 0;
            transition.open(!showInstruments);
            soundSink.play(GameplaySoundEvent.INVENTORY_OPEN); // $02:6176.
        } else if (isFullyOpen()) {
            transition.close();
            soundSink.play(GameplaySoundEvent.INVENTORY_CLOSE); // CloseInventory $20:6441.
        }
    }

    public void tick() {
        if (isFullyOpen() && ocarinaSongMenu.tick() && ocarinaSongMenu.consumeCloseInventoryRequest()) {
            toggleInventory();
        }
        boolean wasInteractive = isFullyOpen();
        transition.tick();
        if (wasInteractive) {
            instrumentPaletteFrame = (instrumentPaletteFrame + 1) & 127;
            cursorFrameCounter = (cursorFrameCounter + 1) & 0xFF;
        }
    }

    public boolean isFullyClosed() {
        return !transition.blocksWorld();
    }

    public boolean isFullyOpen() {
        return transition.interactive();
    }

    public boolean shouldBlockOverworldInput() {
        return transition.blocksWorld();
    }

    public int windowY() {
        return transition.windowY();
    }

    public int displayedWindowY() {
        return transition.displayedWindowY();
    }

    public boolean isTransitioning() {
        return shouldBlockOverworldInput() && !isFullyOpen();
    }

    public boolean inventoryAppearing() {
        return transition.appearing();
    }

    public InventoryTransition transition() {
        return transition;
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
     * 0, or +1. The ROM applies horizontal then vertical clamping and emits
     * one cursor jingle only if the final slot differs from the initial slot.
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
        // moveInventoryCursor, $20:5F1B-5F56 applies each axis before
        // comparing the final selection once at $20:5F85-5F91.
        int next = Math.max(0, Math.min(PlayerState.SUBSCREEN_SLOT_COUNT - 1, cursorSlot + dx));
        next = Math.max(0, Math.min(PlayerState.SUBSCREEN_SLOT_COUNT - 1, next + dy * SUBSCREEN_COLS));
        if (next != cursorSlot) {
            cursorSlot = next;
            soundSink.play(GameplaySoundEvent.MENU_MOVE);
            if (playerState.inventory().subscreenItem(cursorSlot) == InventoryItem.OCARINA.romValue()
                    && playerState.progression().ocarinaSongFlags() != 0) {
                ocarinaSongMenu.requestOpen();
            }
        }
        cursorFrameCounter = 0;
    }

    public void pressA() {
        if (!isFullyOpen()) {
            return;
        }
        if (ocarinaSongMenu.isVisible() && !ocarinaSongMenu.isReady()) return;
        int selectedItem = playerState.inventory().subscreenItem(cursorSlot);
        playerState.inventory().swapItemAWithSubscreen(cursorSlot);
        soundSink.play(GameplaySoundEvent.MENU_VALIDATE);
        openOcarinaIfSelected(selectedItem);
    }

    public void pressB() {
        if (!isFullyOpen()) {
            return;
        }
        if (ocarinaSongMenu.isVisible() && !ocarinaSongMenu.isReady()) return;
        int selectedItem = playerState.inventory().subscreenItem(cursorSlot);
        playerState.inventory().swapItemBWithSubscreen(cursorSlot);
        soundSink.play(GameplaySoundEvent.MENU_VALIDATE);
        openOcarinaIfSelected(selectedItem);
    }

    private void openOcarinaIfSelected(int selectedItem) {
        if (selectedItem == InventoryItem.OCARINA.romValue()
                && playerState.progression().ocarinaSongFlags() != 0) {
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
        if (romData != null)
            gpu.updatePowderInventoryTiles(romData, playerState.ammunition().hasToadstool());
        int[] tilemap = tilemapLoader.copyBaseTilemap();
        int[] attrmap = tilemapLoader.copyAttrmap();
        HudOverlay.apply(tilemap, attrmap, playerState);
        boolean cursorVisible = isFullyOpen() && (cursorFrameCounter & 0x10) == 0;
        InventorySubscreenOverlay.apply(tilemap, attrmap, playerState, cursorSlot, cursorVisible);

        if (dungeonPanel != null && !showInstruments && windowY() == 0)
            dungeonPanel.apply(tilemap, attrmap, dungeonMapId, dungeonCollapsed, dungeonItems, dungeonRooms);
        int offsetY = displayedWindowY();
        if (offsetY >= Framebuffer.HEIGHT) {
            return;
        }

        int[][] inventoryPalettes = tilemapLoader.palettes();
        if (instrumentPanel != null && showInstruments && windowY() == 0) {
            inventoryPalettes = instrumentPanel.palettes(inventoryPalettes, instrumentPaletteFrame);
        }
        TilemapRenderer.render(
                displayBuffer,
                gpu,
                tilemap,
                attrmap,
                windowY() == 128 && roomPalettes != null ? roomPalettes : inventoryPalettes,
                InventoryTilemapLoader.WIDTH,
                InventoryTilemapLoader.HEIGHT,
                0,
                offsetY);

        if (instrumentPanel != null && showInstruments && windowY() == 0) {
            instrumentPanel.render(displayBuffer, attrmap, inventoryPalettes, instrumentFlags, offsetY);
        }

        if (dungeonPanel != null && !showInstruments && windowY() == 0)
            dungeonPanel.render(
                    displayBuffer,
                    tilemap,
                    attrmap,
                    inventoryPalettes,
                    offsetY,
                    dungeonMapId,
                    dungeonPosition,
                    dungeonFrame,
                    tilemapLoader.objectPalettes());

        if (instrumentPanel != null && isFullyOpen()) {
            instrumentPanel.renderNavigation(displayBuffer, tilemapLoader.objectPalettes(), instrumentPaletteFrame);
        }

        if (ocarinaSongMenu.isVisible()) {
            if (romData != null && !ocarinaTilesLoaded) {
                gpu.loadOcarinaSymbolsTiles(romData);
                ocarinaTilesLoaded = true;
            }
            ocarinaPopupRenderer.render(displayBuffer, gpu, tilemapLoader.objectPalettes(), ocarinaSongMenu);
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
        return new ClosedWindow(
                tiles, attributes, roomPalettes == null ? tilemapLoader.palettes() : roomPalettes, displayedWindowY());
    }

    public void submit(linksawakening.render.HardwareFrame frameModel, GPU gpu, int[][] roomPalettes) {
        if (romData != null)
            gpu.updatePowderInventoryTiles(romData, playerState.ammunition().hasToadstool());
        int[] tilemap = tilemapLoader.copyBaseTilemap();
        int[] attrmap = tilemapLoader.copyAttrmap();
        HudOverlay.apply(tilemap, attrmap, playerState);
        boolean cursorVisible = isFullyOpen() && (cursorFrameCounter & 0x10) == 0;
        InventorySubscreenOverlay.apply(tilemap, attrmap, playerState, cursorSlot, cursorVisible);

        if (dungeonPanel != null && !showInstruments && windowY() == 0)
            dungeonPanel.apply(tilemap, attrmap, dungeonMapId, dungeonCollapsed, dungeonItems, dungeonRooms);
        int offsetY = displayedWindowY();
        if (offsetY >= Framebuffer.HEIGHT) {
            return;
        }

        int[][] inventoryPalettes = tilemapLoader.palettes();
        if (instrumentPanel != null && showInstruments && windowY() == 0) {
            inventoryPalettes = instrumentPanel.palettes(inventoryPalettes, instrumentPaletteFrame);
        }
        TilemapRenderer.submit(
                frameModel,
                gpu,
                tilemap,
                attrmap,
                windowY() == 128 && roomPalettes != null ? roomPalettes : inventoryPalettes,
                InventoryTilemapLoader.WIDTH,
                InventoryTilemapLoader.HEIGHT,
                0,
                offsetY);

        if (instrumentPanel != null && showInstruments && windowY() == 0) {
            instrumentPanel.submit(frameModel, attrmap, inventoryPalettes, instrumentFlags, offsetY);
        }

        if (dungeonPanel != null && !showInstruments && windowY() == 0)
            dungeonPanel.submit(
                    frameModel,
                    tilemap,
                    attrmap,
                    inventoryPalettes,
                    offsetY,
                    dungeonMapId,
                    dungeonPosition,
                    dungeonFrame,
                    tilemapLoader.objectPalettes());

        if (instrumentPanel != null && isFullyOpen()) {
            instrumentPanel.submitNavigation(frameModel, tilemapLoader.objectPalettes(), instrumentPaletteFrame);
        }

        if (ocarinaSongMenu.isVisible()) {
            if (romData != null && !ocarinaTilesLoaded) {
                gpu.loadOcarinaSymbolsTiles(romData);
                ocarinaTilesLoaded = true;
            }
            ocarinaPopupRenderer.submit(frameModel, gpu, tilemapLoader.objectPalettes(), ocarinaSongMenu);
        } else if (ocarinaTilesLoaded && romData != null) {
            gpu.loadSharedVfxTiles(romData);
            ocarinaTilesLoaded = false;
        }
    }
}
