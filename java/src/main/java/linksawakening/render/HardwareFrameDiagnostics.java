package linksawakening.render;

import static linksawakening.gpu.Framebuffer.HEIGHT;
import static linksawakening.gpu.Framebuffer.WIDTH;

import java.util.Arrays;
import java.util.List;

/** Optional pixel provenance and narrowly evidenced legacy comparison for capture tools. */
final class HardwareFrameDiagnostics {
    private HardwareFrame frame;
    final boolean[] approvedHudEvidence = new boolean[WIDTH * HEIGHT];
    final int[] hudTiles = new int[WIDTH * HEIGHT], hudAttributes = new int[WIDTH * HEIGHT];
    final String[] bgSources = new String[WIDTH * HEIGHT], objSources = new String[WIDTH * HEIGHT];
    final int[] objFlags = new int[WIDTH * HEIGHT];
    int[] inventoryOriginalColors;
    boolean[] inventoryOriginalHidden;
    String[] inventoryOriginalSources;

    void reset() {
        Arrays.fill(approvedHudEvidence, false);
        Arrays.fill(hudTiles, 0);
        Arrays.fill(hudAttributes, 0);
        Arrays.fill(bgSources, null);
        Arrays.fill(objSources, null);
        Arrays.fill(objFlags, 0);
        inventoryOriginalColors = null;
        inventoryOriginalHidden = null;
        inventoryOriginalSources = null;
    }

    void attach(HardwareFrame frame) {
        this.frame = frame;
    }

    void beforeInventoryHiding(List<OamEntry> before, int windowY) {
        boolean anyHidden = false;
        for (var entry : before) if (((entry.entryY() + 16) & 255) >= windowY + 8) anyHidden = true;
        if (windowY <= 0 || windowY >= 128 || !anyHidden) return;
        inventoryOriginalColors = new int[WIDTH * HEIGHT];
        inventoryOriginalHidden = new boolean[WIDTH * HEIGHT];
        inventoryOriginalSources = new String[WIDTH * HEIGHT];
        for (OamEntry entry : before) {
            stamp(entry.tile(), entry.x(), entry.y(), entry.flipX(), entry.flipY(), (pixel, index) -> {
                if (index == 0) return;
                inventoryOriginalColors[pixel] = entry.transform().applyAsInt(entry.palette()[index]);
                inventoryOriginalHidden[pixel] = ((entry.entryY() + 16) & 255) >= windowY + 8;
                inventoryOriginalSources[pixel] = entry.source();
            });
        }
    }

    public String fidelitySource(int pixel) {
        String source = objSources[pixel];
        if (source == null && inventoryOriginalHidden != null && inventoryOriginalHidden[pixel])
            source = inventoryOriginalSources[pixel];
        return "OBJ=" + source + " BG=" + bgSources[pixel];
    }

    public String mismatchGroup(int pixel, byte[] legacy, byte[] hardware) {
        int y = pixel / WIDTH, at = pixel * 4;
        String region = y >= 128 ? "HUD" : y < 32 ? "TOP" : "PLAYFIELD";
        int oldRgb = (legacy[at] & 255) << 16 | (legacy[at + 1] & 255) << 8 | (legacy[at + 2] & 255);
        int newRgb = (hardware[at] & 255) << 16 | (hardware[at + 1] & 255) << 8 | (hardware[at + 2] & 255);
        return String.format(
                "OBJ=%s BG=%s region=%s BGattr=$%02X BGindex=%d OBJflags=$%02X legacy=$%06X hardware=$%06X windowY=%s",
                objSources[pixel],
                bgSources[pixel],
                region,
                hudAttributes[pixel],
                frame.backgroundIndex[pixel],
                objFlags[pixel],
                oldRgb,
                newRgb,
                frame.closedWindowY == Integer.MAX_VALUE ? "none" : frame.closedWindowY);
    }

    public String hudEvidence(int pixel) {
        return String.format(
                "tile=$%02X attr=$%02X index=%d objSubmission=%d bgRGB=$%06X objRGB=$%06X",
                hudTiles[pixel],
                hudAttributes[pixel],
                frame.backgroundIndex[pixel],
                frame.objectOrder[pixel],
                frame.backgroundRgb(pixel),
                frame.objects[pixel]);
    }

    public boolean explainsHudPriority(byte[] legacy, byte[] hardware, int pixel) {
        boolean evidencedWindow = approvedHudEvidence[pixel]
                || ("DialogRenderLayer".equals(bgSources[pixel])
                        && ("LinkRenderLayer".equals(objSources[pixel])
                                || "DialogRenderLayer".equals(objSources[pixel])));
        if (!evidencedWindow
                || frame.objectOrder[pixel] == Integer.MAX_VALUE
                || frame.backgroundPalettes[pixel] == null) return false;
        int offset = pixel * 4;
        if (frame.backgroundPriorityEnabled
                && frame.backgroundIndex[pixel] != 0
                && (frame.backgroundPriority[pixel] || frame.objectBehind[pixel])) return false;
        int bg = frame.backgroundRgb(pixel);
        int obj = frame.objects[pixel];
        for (int channel = 0; channel < 3; channel++) {
            int shift = 16 - channel * 8;
            if ((legacy[offset + channel] & 255) != ((bg >> shift) & 255)
                    || (hardware[offset + channel] & 255) != ((obj >> shift) & 255)) return false;
        }
        return legacy[offset + 3] == (byte) 255 && hardware[offset + 3] == (byte) 255;
    }

    public boolean explainsInventoryHiding(byte[] legacy, byte[] hardware, int pixel) {
        if (inventoryOriginalHidden == null
                || !inventoryOriginalHidden[pixel]
                || pixel / WIDTH >= frame.inventoryCutoffY
                || frame.objectOrder[pixel] != Integer.MAX_VALUE
                || frame.backgroundPalettes[pixel] == null
                || "DialogRenderLayer".equals(bgSources[pixel])) return false;
        int at = pixel * 4;
        int oldRgb = inventoryOriginalColors[pixel];
        int newRgb = frame.backgroundRgb(pixel);
        for (int channel = 0; channel < 3; channel++) {
            int shift = 16 - channel * 8;
            if ((legacy[at + channel] & 255) != ((oldRgb >> shift) & 255)
                    || (hardware[at + channel] & 255) != ((newRgb >> shift) & 255)) return false;
        }
        return legacy[at + 3] == (byte) 255 && hardware[at + 3] == (byte) 255;
    }

    void closedHudTile(linksawakening.gpu.Tile tile, int tileId, int attributes, int x, int y) {
        stamp(tile, x, y, (attributes & 32) != 0, (attributes & 64) != 0, (pixel, index) -> {
            hudTiles[pixel] = tileId;
            hudAttributes[pixel] = attributes;
            approvedHudEvidence[pixel] = true;
        });
    }

    interface PixelWriter {
        void write(int pixel, int index);
    }

    static void stamp(linksawakening.gpu.Tile tile, int x, int y, boolean flipX, boolean flipY, PixelWriter writer) {
        if (tile == null) return;
        for (int ty = 0; ty < 8; ty++)
            for (int tx = 0; tx < 8; tx++) {
                int px = x + tx, py = y + ty;
                if (px < 0 || py < 0 || px >= WIDTH || py >= HEIGHT) continue;
                writer.write(py * WIDTH + px, tile.getPixel(flipX ? 7 - tx : tx, flipY ? 7 - ty : ty));
            }
    }
}
