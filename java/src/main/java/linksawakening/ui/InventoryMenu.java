package linksawakening.ui;

import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.GPU;
import linksawakening.state.PlayerState;
import linksawakening.rom.RomUtil;
import linksawakening.render.IndexedRenderer;

import java.util.Objects;

/**
 * The DX inventory changes the Game Boy window position while the screen is
 * white. Gameplay and menu input resume after the destination fades in.
 *
 * <p>While the menu is fully open, the cursor navigates the 10-slot subscreen
 * (bank20.asm:moveInventoryCursor): left/right step by 1, up/down step by 2,
 * giving a 2-column x 5-row grid. A/B press swaps the cursor's subscreen slot
 * with the corresponding button slot.
 */
public final class InventoryMenu {

    private static final int WINDOW_Y_OPEN = 0x00;
    private static final int WINDOW_Y_CLOSED = 0x80;
    // CGB inventory entry waits for the opening jingle before fading the room.
    private static final int ENTRY_FADE_START = 10;
    private static final int ENTRY_WHITE = 26;
    private static final int ENTRY_SHOW_MENU = 36;
    private static final int ENTRY_INTERACTIVE = 52;
    private static final int EXIT_WHITE = 17;
    private static final int EXIT_SHOW_ROOM = 35;
    private static final int EXIT_INTERACTIVE = 51;
    private enum Transition { NONE, OPENING, CLOSING }
    private static final int STATUS_PANEL_HIDDEN_Y = Framebuffer.HEIGHT;
    private static final int STATUS_PANEL_SHOWN_Y = 0x78;
    private static final int STATUS_PANEL_STEP = 4;

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
    private byte[] instrumentProgress;
    private GPU instrumentWindowTiles;
    private int instrumentColorFrame;
    private GPU dungeonWindowTiles;
    private DungeonMinimap dungeonMinimap;
    private InventoryQuestItems questItems;
    private InventoryStatusRenderer statusRenderer;

    public void setDungeonMinimap(int map, boolean sideView, int position, byte[] statuses,
                                 byte[] items, boolean collapsed) {
        dungeonMinimap = new DungeonMinimap(romData, map, sideView, position, statuses, items, collapsed);
        instrumentProgress = null;
        instrumentColorFrame = 0;
        questItems = romData == null ? null : new InventoryQuestItems(romData, playerState, tilemapLoader, items);
    }

    public void setInstrumentProgress(byte[] flags) {
        setInstrumentProgress(flags, null);
    }

    public void setInstrumentProgress(byte[] flags, byte[] items) {
        dungeonMinimap = null;
        instrumentProgress = flags == null ? null : flags.clone();
        instrumentColorFrame = 0;
        questItems = romData == null ? null : new InventoryQuestItems(romData, playerState, tilemapLoader, items);
    }

    private int windowY = WINDOW_Y_CLOSED;
    private Transition transition = Transition.NONE;
    private int transitionFrame;
    private int statusPanelY = STATUS_PANEL_HIDDEN_Y;
    private StatusPanelMotion statusPanelMotion = StatusPanelMotion.HIDDEN;
    private int cursorSlot = 0;
    // Mirrors wInventoryCursorFrameCounter (bank20.asm:62A2). Bit 4 drives
    // the cursor blink (on for 16 frames, off for 16). Reset on any dpad
    // press so the bracket is guaranteed visible the frame the cursor moves.
    private int cursorFrameCounter = 0;

    private enum StatusPanelMotion {
        HIDDEN,
        HELD,
        RETURNING
    }

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
    }

    /**
     * Called when the player presses the menu button. Ignored while the menu
     * is in a transition.
     */
    public void requestToggle() {
        if (isTransitioning() || isStatusPanelActive()) {
            return;
        }
        if (ocarinaSongMenu.isVisible()) {
            // The ROM consumes Start by closing the popup first. Once its
            // 16-frame closing animation ends, the inventory bar itself is
            // closed on the following gameplay tick.
            ocarinaSongMenu.requestClose(true);
            return;
        }
        beginInventoryTransition();
    }

    private void beginInventoryTransition() {
        boolean opening = isFullyClosed();
        transition = opening ? Transition.OPENING : Transition.CLOSING;
        transitionFrame = -1;
        soundSink.play(opening ? GameplaySoundEvent.INVENTORY_OPEN : GameplaySoundEvent.INVENTORY_CLOSE);
    }

    /**
     * Advances the inventory by one frame. Called every tick before rendering.
     */
    public void tick() {
        tick(false);
    }

    public void tick(boolean selectHeld) {
        if (ocarinaSongMenu.tick()
                && ocarinaSongMenu.consumeCloseInventoryRequest()
                && isFullyOpen()) {
            beginInventoryTransition();
        }
        if (isTransitioning()) {
            transitionFrame++;
            if (transition == Transition.OPENING) {
                if (transitionFrame == ENTRY_SHOW_MENU) windowY = WINDOW_Y_OPEN;
                if (transitionFrame == ENTRY_INTERACTIVE) transition = Transition.NONE;
            } else {
                if (transitionFrame == EXIT_WHITE) windowY = WINDOW_Y_CLOSED;
                if (transitionFrame == EXIT_INTERACTIVE) transition = Transition.NONE;
            }
        }
        if (isFullyOpen()) {
            instrumentColorFrame = (instrumentColorFrame + 1) & 0x7F;
            if (!isStatusPanelActive()) {
                cursorFrameCounter = (cursorFrameCounter + 1) & 0xFF;
            }
        }
        tickStatusPanel(selectHeld);
    }

    public void requestStatusPanel() {
        if (isFullyOpen() && !isStatusPanelActive() && !ocarinaSongMenu.isVisible()) {
            statusPanelMotion = StatusPanelMotion.HELD;
        }
    }

    public int statusPanelY() {
        return statusPanelY;
    }

    public boolean isStatusPanelActive() {
        return statusPanelMotion != StatusPanelMotion.HIDDEN;
    }

    private void tickStatusPanel(boolean selectHeld) {
        switch (statusPanelMotion) {
            case HIDDEN -> { }
            case HELD -> {
                if (selectHeld) {
                    statusPanelY = Math.max(STATUS_PANEL_SHOWN_Y, statusPanelY - STATUS_PANEL_STEP);
                } else {
                    statusPanelMotion = StatusPanelMotion.RETURNING;
                }
            }
            case RETURNING -> {
                statusPanelY = Math.min(STATUS_PANEL_HIDDEN_Y, statusPanelY + STATUS_PANEL_STEP);
                if (statusPanelY == STATUS_PANEL_HIDDEN_Y) {
                    statusPanelMotion = StatusPanelMotion.HIDDEN;
                }
            }
        }
    }

    public boolean isFullyClosed() {
        return windowY == WINDOW_Y_CLOSED && !isTransitioning();
    }

    public boolean isFullyOpen() {
        return windowY == WINDOW_Y_OPEN && !isTransitioning();
    }

    public boolean shouldBlockOverworldInput() {
        return isTransitioning() || windowY != WINDOW_Y_CLOSED;
    }

    public boolean audioAttenuated() {
        return switch (transition) {
            case OPENING -> transitionFrame >= ENTRY_WHITE - 1;
            case CLOSING -> transitionFrame < EXIT_WHITE + 2;
            case NONE -> isFullyOpen();
        };
    }

    public int windowY() {
        return windowY;
    }

    public boolean isTransitioning() {
        return transition != Transition.NONE;
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
        if (!isFullyOpen() || isStatusPanelActive()) {
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
        if (!isFullyOpen() || isStatusPanelActive()) {
            return;
        }
        int selectedItem = playerState.subscreenItem(cursorSlot);
        playerState.swapItemAWithSubscreen(cursorSlot);
        soundSink.play(GameplaySoundEvent.MENU_VALIDATE);
        openOcarinaIfSelected(selectedItem);
    }

    public void pressB() {
        if (!isFullyOpen() || isStatusPanelActive()) {
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

    public void render(byte[] displayBuffer, GPU gpu) {
        render(displayBuffer, gpu, null);
    }

    public void render(byte[] displayBuffer, GPU gpu, int[][] gameplayPalettes) {
        render(displayBuffer, gpu, gameplayPalettes, (cursorFrameCounter & 8) == 0);
    }

    public void render(byte[] displayBuffer, GPU gpu, int[][] gameplayPalettes, boolean swordFlashVisible) {
        int[] tilemap = tilemapLoader.copyBaseTilemap();
        int[] attrmap = tilemapLoader.copyAttrmap();
        if (romData != null) {
            gpu.updatePowderTiles(romData, playerState.hasToadstool());
        }
        HudOverlay.apply(tilemap, attrmap, playerState, tilemapLoader);
        boolean cursorVisible = isFullyOpen() && !isStatusPanelActive() && (cursorFrameCounter & 0x10) == 0;
        InventorySubscreenOverlay.apply(tilemap, attrmap, playerState, cursorSlot, cursorVisible, tilemapLoader);

        int offsetY = windowY;
        if (offsetY >= Framebuffer.HEIGHT) {
            return;
        }

        TilemapRenderer.render(
            displayBuffer,
            gpu,
            tilemap,
            attrmap,
            windowY == WINDOW_Y_CLOSED && gameplayPalettes != null ? gameplayPalettes : tilemapLoader.palettes(),
            InventoryTilemapLoader.WIDTH,
            InventoryTilemapLoader.HEIGHT,
            0,
            offsetY
        );

        if (questItems != null && windowY < WINDOW_Y_CLOSED) {
            questItems.render(displayBuffer, windowY);
        }
        if (dungeonMinimap != null && windowY < WINDOW_Y_CLOSED) {
            if (dungeonWindowTiles == null) {
                dungeonWindowTiles = new GPU();
                dungeonWindowTiles.loadDungeonMinimapTiles(romData);
            }
            dungeonMinimap.render(displayBuffer, dungeonWindowTiles, tilemapLoader, windowY,
                instrumentColorFrame, !isStatusPanelActive());
        }
        if (instrumentProgress != null && romData != null && windowY < WINDOW_Y_CLOSED) {
            renderInstruments(displayBuffer);
        }

        if (swordFlashVisible) drawPoweredSword(displayBuffer, gpu);
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
        if (romData != null && isFullyOpen() && !ocarinaSongMenu.isVisible()) {
            if (statusRenderer == null) {
                statusRenderer = new InventoryStatusRenderer(romData, tilemapLoader.objectPalettes());
            }
            if (isStatusPanelActive()) {
                statusRenderer.drawPanel(displayBuffer, statusPanelY - 16, playerState);
            } else {
                statusRenderer.drawPrompt(displayBuffer, instrumentColorFrame);
            }
        }
        if (isTransitioning()) fadeInventoryTransition(displayBuffer);
    }

    private void fadeInventoryTransition(byte[] buffer) {
        boolean opening = transition == Transition.OPENING;
        int white = opening ? ENTRY_WHITE : EXIT_WHITE;
        int reveal = opening ? ENTRY_SHOW_MENU : EXIT_SHOW_ROOM;
        if (transitionFrame < white) {
            int start = opening ? ENTRY_FADE_START : 1;
            MenuTransition.applyPaletteFade(buffer, Math.max(0, transitionFrame - start) / 2, true);
        } else if (transitionFrame < reveal) {
            java.util.Arrays.fill(buffer, (byte) 255);
        } else {
            MenuTransition.applyPaletteFade(buffer, (transitionFrame - reveal) / 2, false);
        }
    }

    private void drawPoweredSword(byte[] buffer, GPU gpu) {
        if (romData == null || playerState.activePowerUp() != PlayerState.ACTIVE_POWER_UP_PIECE_OF_POWER) return;
        int slots = isFullyClosed() ? 2 : 2 + PlayerState.SUBSCREEN_SLOT_COUNT;
        for (int slot = 0; slot < slots; slot++) {
            int item = switch (slot) {
                case 0 -> playerState.itemB();
                case 1 -> playerState.itemA();
                default -> playerState.subscreenItem(slot - 2);
            };
            if (item == PlayerState.INVENTORY_SWORD) {
                int x = Byte.toUnsignedInt(romData[RomUtil.romOffset(0x20, 0x633A) + slot]) - 8;
                int y = windowY + Byte.toUnsignedInt(romData[RomUtil.romOffset(0x20, 0x6346) + slot]) - 16;
                IndexedRenderer.drawSpriteTile8x16(buffer, gpu, 0x04, x, y, false, true,
                    tilemapLoader.objectPalettes()[4]);
                return;
            }
        }
    }

    /** Mirrors LoadInstrumentsBG. The ROM positions are relative to $9D00 in the window map. */
    private void renderInstruments(byte[] displayBuffer) {
        if (instrumentWindowTiles == null) {
            // The window temporarily owns these slots. The room underneath
            // the sliding window must keep its own tile graphics.
            instrumentWindowTiles = new GPU();
            instrumentWindowTiles.loadInventoryInstrumentTiles(romData);
        }
        int[] palette = tilemapLoader.palettes()[7].clone();
        int colors = RomUtil.romOffset(0x20, 0x5E75) + (instrumentColorFrame / 8) * 4;
        for (int i = 1; i <= 2; i++, colors += 2) {
            palette[i] = RomUtil.decodeRgb555(Byte.toUnsignedInt(romData[colors])
                | Byte.toUnsignedInt(romData[colors + 1]) << 8);
        }
        int[][] palettes = {palette};
        int table = RomUtil.romOffset(1, 0x6BCF);
        for (int i = 0; i < 8; i++) {
            int position = 0x100 + Byte.toUnsignedInt(romData[table + i]);
            int instrument = Byte.toUnsignedInt(romData[table + 16 + i]);
            int number = Byte.toUnsignedInt(romData[table + 8 + i]);
            int[] tiles = (instrumentProgress[i] & 2) != 0
                ? new int[]{instrument, instrument + 1, instrument + 16, instrument + 17}
                : new int[]{0x7C, 0x7C, 0x7C, number};
            TilemapRenderer.render(displayBuffer, instrumentWindowTiles, tiles, null, palettes,
                2, 2, (position % 32) * 8, windowY + (position / 32) * 8);
        }
    }
}
