package linksawakening.render;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.IntUnaryOperator;
import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.Tile;

/** CGB pixel composition before RGB conversion. Window replaces BG, including color zero. */
public final class HardwareFrame implements RenderLayer, OamSink {
    private static final int WIDTH = Framebuffer.WIDTH;
    private static final int HEIGHT = Framebuffer.HEIGHT;
    final int[][] backgroundPalettes = new int[WIDTH * HEIGHT][];
    final byte[] backgroundIndex = new byte[WIDTH * HEIGHT];
    final boolean[] backgroundPriority = new boolean[WIDTH * HEIGHT];
    final int[] objects = new int[WIDTH * HEIGHT];
    final int[] objectOrder = new int[WIDTH * HEIGHT];
    final boolean[] objectBehind = new boolean[WIDTH * HEIGHT];
    private static final IntUnaryOperator IDENTITY = color -> color;
    private int paletteTransform;
    private final int[] bgTransforms = new int[WIDTH * HEIGHT];
    private final ArrayList<IntUnaryOperator> transforms = new ArrayList<>();
    private int[] copiedTransforms = new int[16];
    private IntUnaryOperator objectPaletteTransform = IDENTITY;
    private String submittingLayer = "unknown";
    int closedWindowY = Integer.MAX_VALUE;
    final boolean backgroundPriorityEnabled;

    private final HardwareFrameObserver observer;
    private HardwareFrame backgroundScratch;

    HardwareFrame backgroundScratch() {
        if (backgroundScratch == null) backgroundScratch = new HardwareFrame(true);
        backgroundScratch.reset();
        return backgroundScratch;
    }

    public HardwareFrame(boolean backgroundPriorityEnabled) {
        this(backgroundPriorityEnabled, null);
    }

    HardwareFrame(boolean backgroundPriorityEnabled, HardwareFrameObserver observer) {
        this.observer = observer;
        if (observer != null) observer.attach(this);
        this.backgroundPriorityEnabled = backgroundPriorityEnabled;
        transforms.add(IDENTITY);
        Arrays.fill(objectOrder, Integer.MAX_VALUE);
    }

    /** Clears submitted planes and entries while retaining all scratch storage. */
    public void reset() {
        Arrays.fill(backgroundPalettes, null);
        Arrays.fill(backgroundIndex, (byte) 0);
        Arrays.fill(backgroundPriority, false);
        Arrays.fill(bgTransforms, 0);
        Arrays.fill(objectOrder, Integer.MAX_VALUE);
        Arrays.fill(objectBehind, false);
        Arrays.fill(objects, 0);
        transforms.clear();
        transforms.add(IDENTITY);
        paletteTransform = 0;
        objectPaletteTransform = IDENTITY;
        submittingLayer = "unknown";
        closedWindowY = Integer.MAX_VALUE;
        inventoryCutoffY = -1;
        oam.clear();
        nextEntry = 0;
        if (observer != null) observer.reset();
    }

    HardwareFrameObserver observer() {
        return observer;
    }

    void submittingLayer(String name) {
        submittingLayer = name;
    }

    /** Retains only the reserved OBJ identity required by the ROM dialog hide rule. */
    void objectVisibilitySource(RenderLayer layer) {
        submittingLayer = layer instanceof LinkRenderLayer
                ? "LinkRenderLayer"
                : layer instanceof DialogRenderLayer ? "DialogRenderLayer" : "unknown";
    }

    /** Call BG first, then window tiles in screen coordinates. */
    public void backgroundTile(Tile tile, int x, int y, boolean flipX, boolean flipY, int[] palette, boolean priority) {
        if (tile == null) return;
        for (int ty = 0; ty < 8; ty++)
            for (int tx = 0; tx < 8; tx++)
                backgroundPixel(
                        x + tx, y + ty, tile.getPixel(flipX ? 7 - tx : tx, flipY ? 7 - ty : ty), palette, priority);
    }

    public void backgroundPixel(int x, int y, int index, int[] palette, boolean priority) {
        if (x < 0 || y < 0 || x >= WIDTH || y >= HEIGHT) return;
        int pixel = y * WIDTH + x;
        if (observer != null) observer.backgroundPixel(pixel, submittingLayer);
        backgroundIndex[pixel] = (byte) index;
        backgroundPalettes[pixel] = palette;
        bgTransforms[pixel] = paletteTransform;
        backgroundPriority[pixel] = priority;
    }

    public void backgroundPixel(int x, int y, int index, int[] palette, int tile, int attr) {
        backgroundPixel(x, y, index, palette, (attr & 128) != 0);
        if (x < 0 || y < 0 || x >= WIDTH || y >= HEIGHT) return;
        if (observer != null) observer.hudPixel(y * WIDTH + x, tile, attr);
    }

    /** Retains provenance for the empirically verified closed-HUD fidelity correction. */
    public void closedHudTile(Tile tile, int tileId, int attributes, int x, int y, int[] palette) {
        closedWindowY = Math.min(closedWindowY, y);
        backgroundTile(tile, x, y, (attributes & 32) != 0, (attributes & 64) != 0, palette, (attributes & 128) != 0);
        if (tile == null || observer == null) return;
        observer.closedHudTile(tile, tileId, attributes, x, y);
    }

    /** HideSprites ($01:5F4B-$5F69): at WY=0 every visible gameplay OAM entry is hidden. */
    public void hideGameplayObjectsForOpenInventory() {
        hideGameplayObjectsUnderWindow(0);
    }

    int inventoryCutoffY = -1;

    /** bank1.asm $5F51-$5F67: hide a whole OAM entry when raw Y >= WY+8. */
    public void hideGameplayObjectsUnderWindow(int windowY) {
        inventoryCutoffY = windowY;
        if (observer != null) observer.beforeInventoryHiding(oam, windowY);
        for (int i = oam.size() - 1; i >= 0; i--) {
            if (((oam.get(i).entryY() + 16) & 255) >= windowY + 8) oam.remove(i);
        }
    }

    public void sprite8x16(linksawakening.gpu.GPU gpu, int tile, int x, int y, boolean fx, boolean fy, int[] palette) {
        int base = tile & 254;
        append8x16(gpu.getTile(fy ? base + 1 : base), gpu.getTile(fy ? base : base + 1), x, y, fx, fy, palette, false);
    }

    int backgroundRgb(int pixel) {
        if (backgroundPalettes[pixel] == null) return 0;
        return transforms.get(bgTransforms[pixel]).applyAsInt(backgroundPalettes[pixel][backgroundIndex[pixel]]);
    }

    public void withLayerSource(String name, Runnable submission) {
        String previous = submittingLayer;
        submittingLayer = name;
        try {
            submission.run();
        } finally {
            submittingLayer = previous;
        }
    }

    public void withPaletteTransform(IntUnaryOperator transform, Runnable submission) {
        withPaletteTransforms(transform, transform, submission);
    }

    public void withPaletteTransforms(IntUnaryOperator background, IntUnaryOperator object, Runnable submission) {
        var previousBg = paletteTransform;
        var previousObj = objectPaletteTransform;
        paletteTransform = transforms.size();
        transforms.add(background.andThen(transforms.get(previousBg)));
        objectPaletteTransform = object.andThen(previousObj);
        try {
            submission.run();
        } finally {
            paletteTransform = previousBg;
            objectPaletteTransform = previousObj;
        }
    }

    public void copyBackgroundRows(HardwareFrame source, int rows) {
        if (rows < 0 || rows > HEIGHT) throw new IllegalArgumentException("Background row count");
        if (copiedTransforms.length < source.transforms.size()) copiedTransforms = new int[source.transforms.size()];
        int sourceTransformCount = source.transforms.size();
        for (int i = 0; i < sourceTransformCount; i++) {
            copiedTransforms[i] = transforms.size();
            transforms.add(source.transforms.get(i).andThen(transforms.get(paletteTransform)));
        }
        for (int pixel = 0; pixel < rows * WIDTH; pixel++) {
            backgroundPalettes[pixel] = source.backgroundPalettes[pixel];
            backgroundIndex[pixel] = source.backgroundIndex[pixel];
            backgroundPriority[pixel] = source.backgroundPriority[pixel];
            if (observer != null) observer.copiedBackgroundPixel(pixel, source.observer, submittingLayer);
            bgTransforms[pixel] = copiedTransforms[source.bgTransforms[pixel]];
        }
    }

    private final ArrayList<OamEntry> oam = new ArrayList<>();
    private final ArrayList<OamEntry> entryPool = new ArrayList<>();
    private int nextEntry;

    @Override
    public void append(Tile tile, int x, int y, boolean flipX, boolean flipY, int[] palette, boolean behindBackground) {
        appendFromEntry(y, tile, x, y, flipX, flipY, palette, behindBackground);
    }

    @Override
    public void appendFromEntry(
            int entryY, Tile tile, int x, int y, boolean fx, boolean fy, int[] palette, boolean behind) {
        if (nextEntry == entryPool.size()) entryPool.add(new OamEntry());
        OamEntry entry = entryPool.get(nextEntry++);
        entry.tile = tile;
        entry.x = x;
        entry.y = y;
        entry.flipX = fx;
        entry.flipY = fy;
        entry.palette = palette;
        entry.order = oam.size();
        entry.behind = behind;
        entry.source = submittingLayer;
        entry.entryY = entryY;
        entry.transform = objectPaletteTransform;
        oam.add(entry);
    }

    /** bank1.asm $5F6A-$5FB2: skip reserved Link/prompt entries 0-8, hide whole other entries. */
    public void hideSpritesUnderDialog(boolean bottom) {
        if (closedWindowY == 0 || inventoryCutoffY >= 0)
            return; // $5F6A-$5F6E: fully open inventory has no dialog filter.
        oam.removeIf(entry -> {
            if (entry.source().equals("LinkRenderLayer")
                    || entry.source().equals("LinkShadowRenderLayer")
                    || entry.source().equals("LinkGrassRenderLayer")
                    || entry.source().equals("DialogRenderLayer")) return false;
            int rawY = (entry.entryY() + 16) & 255;
            return bottom ? rawY >= 0x58 : rawY < 0x3E;
        });
    }

    @Override
    public void appendInSlot(int slot, Tile tile, int x, int y, boolean fx, boolean fy, int[] palette, boolean behind) {
        append(tile, x, y, fx, fy, palette, behind);
    }

    @Override
    public void render(RenderContext context) {
        compose(context.buffer());
    }

    public void compose(byte[] buffer) {
        compose(buffer, false);
    }

    void composeOverlay(byte[] buffer) {
        compose(buffer, true);
    }

    private void compose(byte[] buffer, boolean overlay) {
        Arrays.fill(objectOrder, Integer.MAX_VALUE);
        if (observer != null) observer.beginComposition();
        for (int i = 0; i < oam.size(); i++) rasterizeObject(oam.get(i));
        int pixels = overlay ? Math.min(backgroundIndex.length, buffer.length / 4) : backgroundIndex.length;
        for (int pixel = 0; pixel < pixels; pixel++) {
            if (overlay && backgroundPalettes[pixel] == null && objectOrder[pixel] == Integer.MAX_VALUE) continue;
            boolean hidden = backgroundPriorityEnabled
                    && backgroundIndex[pixel] != 0
                    && (backgroundPriority[pixel] || objectBehind[pixel]);
            int rgb = objectOrder[pixel] != Integer.MAX_VALUE && !hidden ? objects[pixel] : backgroundRgb(pixel);
            int offset = pixel * 4;
            buffer[offset] = (byte) (rgb >> 16);
            buffer[offset + 1] = (byte) (rgb >> 8);
            buffer[offset + 2] = (byte) rgb;
            buffer[offset + 3] = (byte) 255;
        }
    }

    private void rasterizeObject(OamEntry entry) {
        if (entry.tile == null) return;
        for (int ty = 0; ty < 8; ty++)
            for (int tx = 0; tx < 8; tx++) {
                int x = entry.x + tx, y = entry.y + ty;
                if (x < 0 || y < 0 || x >= WIDTH || y >= HEIGHT) continue;
                int index = entry.tile.getPixel(entry.flipX ? 7 - tx : tx, entry.flipY ? 7 - ty : ty);
                if (index == 0) continue;
                int pixel = y * WIDTH + x;
                objects[pixel] = entry.transform.applyAsInt(entry.palette[index]);
                objectOrder[pixel] = entry.order;
                objectBehind[pixel] = entry.behind;
                if (observer != null) {
                    observer.objectPixel(
                            pixel,
                            entry.source,
                            (entry.flipX ? 32 : 0) | (entry.flipY ? 64 : 0) | (entry.behind ? 128 : 0));
                }
            }
    }
}
