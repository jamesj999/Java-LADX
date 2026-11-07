package linksawakening.render;

import java.util.Arrays;
import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.Tile;

/** CGB pixel composition before RGB conversion. Window replaces BG, including color zero. */
public final class HardwareFrame implements RenderLayer, OamSink {
    private static final int WIDTH = Framebuffer.WIDTH;
    private static final int HEIGHT = Framebuffer.HEIGHT;
    private final int[][] backgroundPalettes = new int[WIDTH * HEIGHT][];
    private final byte[] backgroundIndex = new byte[WIDTH * HEIGHT];
    private final boolean[] backgroundPriority = new boolean[WIDTH * HEIGHT];
    private final int[] objects = new int[WIDTH * HEIGHT];
    private final int[] objectOrder = new int[WIDTH * HEIGHT];
    private final boolean[] objectBehind = new boolean[WIDTH * HEIGHT];
    private final boolean[] approvedHudEvidence = new boolean[WIDTH * HEIGHT];
    private final int[] hudTiles = new int[WIDTH * HEIGHT], hudAttributes = new int[WIDTH * HEIGHT];
    private String submittingLayer = "unknown";
    private final String[] bgSources = new String[WIDTH * HEIGHT], objSources = new String[WIDTH * HEIGHT];
    private final int[] objFlags = new int[WIDTH * HEIGHT];
    private int closedWindowY = Integer.MAX_VALUE;
    private final boolean backgroundPriorityEnabled;

    public HardwareFrame(boolean backgroundPriorityEnabled) {
        this.backgroundPriorityEnabled = backgroundPriorityEnabled;
        Arrays.fill(objectOrder, Integer.MAX_VALUE);
    }

    void submittingLayer(String name) { submittingLayer = name; }

    public String mismatchGroup(int pixel, byte[] legacy, byte[] hardware) {
        int y = pixel / WIDTH, at = pixel * 4;
        String region = y >= 128 ? "HUD" : y < 32 ? "TOP" : "PLAYFIELD";
        int oldRgb = (legacy[at] & 255) << 16 | (legacy[at+1] & 255) << 8 | (legacy[at+2] & 255);
        int newRgb = (hardware[at] & 255) << 16 | (hardware[at+1] & 255) << 8 | (hardware[at+2] & 255);
        return String.format("OBJ=%s BG=%s region=%s BGattr=$%02X BGindex=%d OBJflags=$%02X legacy=$%06X hardware=$%06X windowY=%s",
            objSources[pixel], bgSources[pixel], region, hudAttributes[pixel], backgroundIndex[pixel], objFlags[pixel],
            oldRgb, newRgb, closedWindowY == Integer.MAX_VALUE ? "none" : closedWindowY);
    }

    /** Call BG first, then window tiles in screen coordinates. */
    public void backgroundTile(Tile tile, int x, int y, boolean flipX, boolean flipY,
                               int[] palette, boolean priority) {
        stamp(tile, x, y, flipX, flipY, (pixel, index) -> {
            approvedHudEvidence[pixel] = false;
            bgSources[pixel] = submittingLayer;
            backgroundPalettes[pixel] = palette;
            backgroundIndex[pixel] = (byte) index;
            backgroundPriority[pixel] = priority;
        });
    }

    public void backgroundPixel(int x, int y, int index, int[] palette, boolean priority) {
        if (x < 0 || y < 0 || x >= WIDTH || y >= HEIGHT) return;
        int pixel = y * WIDTH + x;
        approvedHudEvidence[pixel] = false;
            bgSources[pixel] = submittingLayer;
        backgroundIndex[pixel] = (byte) index;
        backgroundPalettes[pixel] = palette;
        backgroundPriority[pixel] = priority;
    }

    public void backgroundPixel(int x, int y, int index, int[] palette, int tile, int attr) {
        backgroundPixel(x, y, index, palette, (attr & 128) != 0);
        if (x < 0 || y < 0 || x >= WIDTH || y >= HEIGHT) return;
        hudTiles[y * WIDTH + x] = tile;
        hudAttributes[y * WIDTH + x] = attr;
    }

    /** Retains provenance for the empirically verified closed-HUD fidelity correction. */
    public void closedHudTile(Tile tile, int tileId, int attributes, int x, int y, int[] palette) {
        closedWindowY = Math.min(closedWindowY, y);
        backgroundTile(tile, x, y, (attributes & 32) != 0, (attributes & 64) != 0,
            palette, (attributes & 128) != 0);
        if (tile == null) return;
        stamp(tile, x, y, (attributes & 32) != 0, (attributes & 64) != 0, (pixel, index) -> {
            hudTiles[pixel] = tileId;
            hudAttributes[pixel] = attributes;
            approvedHudEvidence[pixel] = pixel / WIDTH >= 128;
        });
    }

    public String hudEvidence(int pixel) {
        return String.format("tile=$%02X attr=$%02X index=%d objSubmission=%d bgRGB=$%06X objRGB=$%06X",
            hudTiles[pixel], hudAttributes[pixel], backgroundIndex[pixel], objectOrder[pixel],
            backgroundPalettes[pixel] == null ? 0 : backgroundPalettes[pixel][backgroundIndex[pixel]], objects[pixel]);
    }

    /** Call after compose. Requires the legacy RGB to equal the resolved HUD color and a hardware-visible OBJ. */
    public boolean explainsHudPriority(byte[] legacy, byte[] hardware, int pixel) {
        if (!approvedHudEvidence[pixel]
            || objectOrder[pixel] == Integer.MAX_VALUE || backgroundPalettes[pixel] == null) return false;
        int offset = pixel * 4;
        if (backgroundPriorityEnabled && backgroundIndex[pixel] != 0
            && (backgroundPriority[pixel] || objectBehind[pixel])) return false;
        int bg = backgroundPalettes[pixel][backgroundIndex[pixel]];
        int obj = objects[pixel];
        for (int channel = 0; channel < 3; channel++) {
            int shift = 16 - channel * 8;
            if ((legacy[offset + channel] & 255) != ((bg >> shift) & 255)
                || (hardware[offset + channel] & 255) != ((obj >> shift) & 255)) return false;
        }
        return legacy[offset + 3] == (byte) 255 && hardware[offset + 3] == (byte) 255;
    }

    private record ObjectTile(Tile tile, int x, int y, boolean flipX, boolean flipY,
                              int[] palette, int order, boolean behind, String source) {}
    private final java.util.List<ObjectTile> oam = new java.util.ArrayList<>();
    @Override public void append(Tile tile, int x, int y, boolean flipX, boolean flipY,
                                 int[] palette, boolean behindBackground) {
        oam.add(new ObjectTile(tile, x, y, flipX, flipY, palette, oam.size(), behindBackground, submittingLayer));
    }
    @Override public void appendInSlot(int slot, Tile tile, int x, int y, boolean fx, boolean fy,
                                       int[] palette, boolean behind) {
        append(tile, x, y, fx, fy, palette, behind);
    }
    /** Compatibility adapter: slot metadata is unused until RomOamAllocator exists. */
    @Deprecated
    public void objectTile(Tile tile, int x, int y, boolean flipX, boolean flipY,
                           int[] palette, int oamIndex, boolean behindBackground) {
        if (oamIndex < 0 || oamIndex == Integer.MAX_VALUE) throw new IllegalArgumentException("OAM index");
        oam.add(new ObjectTile(tile, x, y, flipX, flipY, palette, oam.size(), behindBackground, submittingLayer));
    }

    @Override public void render(RenderContext context) {
        compose(context.buffer());
    }

    public void compose(byte[] buffer) {
        Arrays.fill(objectOrder, Integer.MAX_VALUE);
        for (ObjectTile entry : oam) {
            stamp(entry.tile(), entry.x(), entry.y(), entry.flipX(), entry.flipY(), (pixel, index) -> {
                if (index == 0) return;
                objects[pixel] = entry.palette()[index];
                objSources[pixel] = entry.source();
                objFlags[pixel] = (entry.flipX() ? 32 : 0) | (entry.flipY() ? 64 : 0) | (entry.behind() ? 128 : 0);
                objectOrder[pixel] = entry.order();
                objectBehind[pixel] = entry.behind();
            });
        }
        for (int pixel = 0; pixel < backgroundIndex.length; pixel++) {
            boolean hidden = backgroundPriorityEnabled && backgroundIndex[pixel] != 0
                && (backgroundPriority[pixel] || objectBehind[pixel]);
            int rgb = objectOrder[pixel] != Integer.MAX_VALUE && !hidden
                ? objects[pixel] : backgroundPalettes[pixel] == null ? 0 : backgroundPalettes[pixel][backgroundIndex[pixel]];
            int offset = pixel * 4;
            buffer[offset] = (byte) (rgb >> 16);
            buffer[offset + 1] = (byte) (rgb >> 8);
            buffer[offset + 2] = (byte) rgb;
            buffer[offset + 3] = (byte) 255;
        }
    }

    private interface PixelWriter { void write(int pixel, int index); }
    private static void stamp(Tile tile, int x, int y, boolean flipX, boolean flipY, PixelWriter writer) {
        if (tile == null) return;
        for (int ty = 0; ty < 8; ty++) for (int tx = 0; tx < 8; tx++) {
            int px = x + tx, py = y + ty;
            if (px < 0 || py < 0 || px >= WIDTH || py >= HEIGHT) continue;
            writer.write(py * WIDTH + px, tile.getPixel(flipX ? 7 - tx : tx, flipY ? 7 - ty : ty));
        }
    }
}
