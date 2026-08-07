package linksawakening.ui;

import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.GPU;
import linksawakening.render.IndexedRenderer;

import java.util.Objects;

/** Renders the ROM OAM block used by the Ocarina song-selection popup. */
public final class OcarinaPopupRenderer {

    private static final int FRAME_COUNT = 4;
    private static final int ENTRIES_PER_FRAME = 12;
    private static final int OAM_ENTRY_SIZE = 4;
    private static final int FRAME_SIZE = ENTRIES_PER_FRAME * OAM_ENTRY_SIZE;

    private static final int OAM_Y_OFFSET = 0x30;
    private static final int OAM_X_OFFSET = 0x60;
    private static final int OAM_Y_SCREEN_OFFSET = 0x10;
    private static final int OAM_X_SCREEN_OFFSET = 0x08;

    private static final int SELECTION_Y = 0x38;
    private static final int[] SELECTION_X = { 0x50, 0x60, 0x70 };
    private static final int SELECTION_TILE = 0x28;
    private static final int SELECTION_PALETTE = 0x06;
    private static final int SELECTION_FLIPPED_PALETTE = 0x26;

    private final int[] oamData;

    public OcarinaPopupRenderer(int[] oamData) {
        Objects.requireNonNull(oamData, "oamData");
        if (oamData.length != FRAME_COUNT * FRAME_SIZE) {
            throw new IllegalArgumentException("Expected "
                + (FRAME_COUNT * FRAME_SIZE) + " Ocarina popup bytes, got " + oamData.length);
        }
        this.oamData = oamData.clone();
    }

    public void render(byte[] displayBuffer, GPU gpu, int[][] objectPalettes,
                       OcarinaSongMenu menu) {
        Objects.requireNonNull(displayBuffer, "displayBuffer");
        Objects.requireNonNull(gpu, "gpu");
        Objects.requireNonNull(objectPalettes, "objectPalettes");
        Objects.requireNonNull(menu, "menu");
        if (!menu.isVisible() || objectPalettes.length == 0) {
            return;
        }

        int frame = Math.max(0, Math.min(FRAME_COUNT - 1, menu.animationFrame()));
        int base = frame * FRAME_SIZE;
        for (int entry = 0; entry < ENTRIES_PER_FRAME; entry++) {
            int offset = base + entry * OAM_ENTRY_SIZE;
            int y = (oamData[offset] + OAM_Y_OFFSET) & 0xFF;
            int x = (oamData[offset + 1] + OAM_X_OFFSET) & 0xFF;
            int tile = oamData[offset + 2] & 0xFF;
            int attributes = oamData[offset + 3] & 0xFF;

            int songIndex = songIndexForTile(tile);
            if (songIndex >= 0 && !menu.isSongAvailable(songIndex)) {
                // func_020_6111 replaces the attribute byte with $20 for an
                // unavailable icon, preserving the tile while making it read
                // as the blank/flipped popup variant.
                attributes = 0x20;
            }
            drawOamEntry(displayBuffer, gpu, objectPalettes, y, x, tile, attributes);
        }

        int selectedSong = Math.max(0, Math.min(2, menuPlayerSelectedSong(menu)));
        int markerX = SELECTION_X[selectedSong];
        drawOamEntry(displayBuffer, gpu, objectPalettes, SELECTION_Y, markerX,
            SELECTION_TILE, SELECTION_PALETTE);
        drawOamEntry(displayBuffer, gpu, objectPalettes, SELECTION_Y, markerX + 8,
            SELECTION_TILE, SELECTION_FLIPPED_PALETTE);
    }

    private static int menuPlayerSelectedSong(OcarinaSongMenu menu) {
        // OcarinaSongMenu intentionally exposes the persistent selection
        // through its song-selection behavior; this helper keeps rendering
        // independent of the menu's transition fields.
        return menu.selectedSongIndex();
    }

    private static void drawOamEntry(byte[] displayBuffer, GPU gpu, int[][] objectPalettes,
                                     int oamY, int oamX, int tile, int attributes) {
        int paletteIndex = attributes & 0x07;
        int[] palette = objectPalettes[Math.min(paletteIndex, objectPalettes.length - 1)];
        IndexedRenderer.drawSpriteTile(displayBuffer, gpu.getTile(tile),
            oamX - OAM_X_SCREEN_OFFSET, oamY - OAM_Y_SCREEN_OFFSET,
            (attributes & 0x20) != 0, (attributes & 0x40) != 0, palette);
    }

    private static int songIndexForTile(int tile) {
        return switch (tile) {
            case 0x22 -> 0;
            case 0x24 -> 1;
            case 0x26 -> 2;
            default -> -1;
        };
    }
}
