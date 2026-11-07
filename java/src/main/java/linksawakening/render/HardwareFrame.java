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
    private java.util.function.IntUnaryOperator paletteTransform = color -> color;
    private final java.util.function.IntUnaryOperator[] bgTransforms = new java.util.function.IntUnaryOperator[WIDTH * HEIGHT];
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

    public String fidelitySource(int pixel) {
        String source = objSources[pixel];
        if (source == null && inventoryOriginalHidden != null && inventoryOriginalHidden[pixel])
            source = inventoryOriginalSources[pixel];
        return "OBJ=" + source + " BG=" + bgSources[pixel];
    }

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
            bgTransforms[pixel] = paletteTransform;
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
            bgTransforms[pixel] = paletteTransform;
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
            approvedHudEvidence[pixel] = true;
        });
    }

    public String hudEvidence(int pixel) {
        return String.format("tile=$%02X attr=$%02X index=%d objSubmission=%d bgRGB=$%06X objRGB=$%06X",
            hudTiles[pixel], hudAttributes[pixel], backgroundIndex[pixel], objectOrder[pixel],
            backgroundRgb(pixel), objects[pixel]);
    }

    /** Call after compose. Requires the legacy RGB to equal the resolved HUD color and a hardware-visible OBJ. */
    public boolean explainsHudPriority(byte[] legacy, byte[] hardware, int pixel) {
        boolean evidencedWindow = approvedHudEvidence[pixel]
            || ("DialogRenderLayer".equals(bgSources[pixel])
                && ("LinkRenderLayer".equals(objSources[pixel]) || "DialogRenderLayer".equals(objSources[pixel])));
        if (!evidencedWindow
            || objectOrder[pixel] == Integer.MAX_VALUE || backgroundPalettes[pixel] == null) return false;
        int offset = pixel * 4;
        if (backgroundPriorityEnabled && backgroundIndex[pixel] != 0
            && (backgroundPriority[pixel] || objectBehind[pixel])) return false;
        int bg = backgroundRgb(pixel);
        int obj = objects[pixel];
        for (int channel = 0; channel < 3; channel++) {
            int shift = 16 - channel * 8;
            if ((legacy[offset + channel] & 255) != ((bg >> shift) & 255)
                || (hardware[offset + channel] & 255) != ((obj >> shift) & 255)) return false;
        }
        return legacy[offset + 3] == (byte) 255 && hardware[offset + 3] == (byte) 255;
    }

    /** HideSprites ($01:5F4B-$5F69): at WY=0 every visible gameplay OAM entry is hidden. */
    public void hideGameplayObjectsForOpenInventory() { hideGameplayObjectsUnderWindow(0); }

    private int inventoryCutoffY = -1;
    private int[] inventoryOriginalColors;
    private boolean[] inventoryOriginalHidden;
    private String[] inventoryOriginalSources;

    /** bank1.asm $5F51-$5F67: hide a whole OAM entry when raw Y >= WY+8. */
    public void hideGameplayObjectsUnderWindow(int windowY) {
        inventoryCutoffY = windowY;
        var before = java.util.List.copyOf(oam);
        var hidden = new java.util.HashSet<ObjectTile>();
        for (ObjectTile entry : before) {
            if (((entry.entryY() + 16) & 255) >= windowY + 8) hidden.add(entry);
        }
        oam.removeIf(hidden::contains);
        if (windowY <= 0 || windowY >= 128 || hidden.isEmpty()) return;
        inventoryOriginalColors = new int[WIDTH * HEIGHT];
        inventoryOriginalHidden = new boolean[WIDTH * HEIGHT];
        inventoryOriginalSources = new String[WIDTH * HEIGHT];
        for (ObjectTile entry : before) {
            stamp(entry.tile(), entry.x(), entry.y(), entry.flipX(), entry.flipY(), (pixel, index) -> {
                if (index == 0) return;
                inventoryOriginalColors[pixel] = entry.transform().applyAsInt(entry.palette()[index]);
                inventoryOriginalHidden[pixel] = hidden.contains(entry);
                inventoryOriginalSources[pixel] = entry.source();
            });
        }
    }

    /** A narrow, ROM-proven whole-entry hiding difference above the moving window. */
    public boolean explainsInventoryHiding(byte[] legacy, byte[] hardware, int pixel) {
        if (inventoryOriginalHidden == null || !inventoryOriginalHidden[pixel]
            || pixel / WIDTH >= inventoryCutoffY || objectOrder[pixel] != Integer.MAX_VALUE
            || backgroundPalettes[pixel] == null || "DialogRenderLayer".equals(bgSources[pixel])) return false;
        int at = pixel * 4;
        int oldRgb = inventoryOriginalColors[pixel];
        int newRgb = backgroundRgb(pixel);
        for (int channel = 0; channel < 3; channel++) {
            int shift = 16 - channel * 8;
            if ((legacy[at + channel] & 255) != ((oldRgb >> shift) & 255)
                || (hardware[at + channel] & 255) != ((newRgb >> shift) & 255)) return false;
        }
        return legacy[at + 3] == (byte)255 && hardware[at + 3] == (byte)255;
    }

    public void sprite8x16(linksawakening.gpu.GPU gpu, int tile, int x, int y,
                           boolean fx, boolean fy, int[] palette) {
        int base = tile & 254;
        append8x16(gpu.getTile(fy ? base + 1 : base), gpu.getTile(fy ? base : base + 1),
            x, y, fx, fy, palette, false);
    }

    private int backgroundRgb(int pixel) {
        if (backgroundPalettes[pixel] == null) return 0;
        return bgTransforms[pixel].applyAsInt(backgroundPalettes[pixel][backgroundIndex[pixel]]);
    }

    public void withLayerSource(String name, Runnable submission) {
        String previous = submittingLayer;
        submittingLayer = name;
        try { submission.run(); } finally { submittingLayer = previous; }
    }

    public void withPaletteTransform(java.util.function.IntUnaryOperator transform, Runnable submission) {
        var previous = paletteTransform;
        paletteTransform = transform.andThen(previous);
        try { submission.run(); } finally { paletteTransform = previous; }
    }

    public void copyBackgroundRows(HardwareFrame source, int rows) {
        if (rows < 0 || rows > HEIGHT) throw new IllegalArgumentException("Background row count");
        for (int pixel = 0; pixel < rows * WIDTH; pixel++) {
            backgroundPalettes[pixel] = source.backgroundPalettes[pixel];
            backgroundIndex[pixel] = source.backgroundIndex[pixel];
            backgroundPriority[pixel] = source.backgroundPriority[pixel];
            hudTiles[pixel] = source.hudTiles[pixel]; hudAttributes[pixel] = source.hudAttributes[pixel];
            bgSources[pixel] = submittingLayer;
            approvedHudEvidence[pixel] = false;
            bgTransforms[pixel] = source.bgTransforms[pixel] == null ? paletteTransform
                : source.bgTransforms[pixel].andThen(paletteTransform);
        }
    }

    private record ObjectTile(Tile tile, int x, int y, boolean flipX, boolean flipY,
                              int[] palette, int order, boolean behind, String source, int entryY, java.util.function.IntUnaryOperator transform) {}
    private final java.util.List<ObjectTile> oam = new java.util.ArrayList<>();
    @Override public void append(Tile tile, int x, int y, boolean flipX, boolean flipY,
                                 int[] palette, boolean behindBackground) {
        oam.add(new ObjectTile(tile, x, y, flipX, flipY, palette, oam.size(), behindBackground, submittingLayer, y, paletteTransform));
    }
    @Override public void appendFromEntry(int entryY, Tile tile, int x, int y, boolean fx, boolean fy,
                                          int[] palette, boolean behind) {
        oam.add(new ObjectTile(tile, x, y, fx, fy, palette, oam.size(), behind, submittingLayer, entryY, paletteTransform));
    }

    /** bank1.asm $5F6A-$5FB2: skip reserved Link/prompt entries 0-8, hide whole other entries. */
    public void hideSpritesUnderDialog(boolean bottom) {
        if (closedWindowY == 0 || inventoryCutoffY >= 0) return; // $5F6A-$5F6E: fully open inventory has no dialog filter.
        oam.removeIf(entry -> {
            if (entry.source().equals("LinkRenderLayer") || entry.source().equals("LinkShadowRenderLayer")
                || entry.source().equals("DialogRenderLayer")) return false;
            int rawY = (entry.entryY() + 16) & 255;
            return bottom ? rawY >= 0x58 : rawY < 0x3E;
        });
    }

    @Override public void appendInSlot(int slot, Tile tile, int x, int y, boolean fx, boolean fy,
                                       int[] palette, boolean behind) {
        append(tile, x, y, fx, fy, palette, behind);
    }
    @Override public void render(RenderContext context) {
        compose(context.buffer());
    }

    public void compose(byte[] buffer) { compose(buffer, false); }

    void composeOverlay(byte[] buffer) { compose(buffer, true); }

    private void compose(byte[] buffer, boolean overlay) {
        Arrays.fill(objectOrder, Integer.MAX_VALUE);
        Arrays.fill(objSources, null);
        for (ObjectTile entry : oam) {
            stamp(entry.tile(), entry.x(), entry.y(), entry.flipX(), entry.flipY(), (pixel, index) -> {
                if (index == 0) return;
                objects[pixel] = entry.transform().applyAsInt(entry.palette()[index]);
                objSources[pixel] = entry.source();
                objFlags[pixel] = (entry.flipX() ? 32 : 0) | (entry.flipY() ? 64 : 0) | (entry.behind() ? 128 : 0);
                objectOrder[pixel] = entry.order();
                objectBehind[pixel] = entry.behind();
            });
        }
        int pixels = overlay ? Math.min(backgroundIndex.length, buffer.length / 4) : backgroundIndex.length;
        for (int pixel = 0; pixel < pixels; pixel++) {
            if (overlay && backgroundPalettes[pixel] == null && objectOrder[pixel] == Integer.MAX_VALUE) continue;
            boolean hidden = backgroundPriorityEnabled && backgroundIndex[pixel] != 0
                && (backgroundPriority[pixel] || objectBehind[pixel]);
            int rgb = objectOrder[pixel] != Integer.MAX_VALUE && !hidden
                ? objects[pixel] : backgroundRgb(pixel);
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
