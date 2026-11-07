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
    private final boolean backgroundPriorityEnabled;

    public HardwareFrame(boolean backgroundPriorityEnabled) {
        this.backgroundPriorityEnabled = backgroundPriorityEnabled;
        Arrays.fill(objectOrder, Integer.MAX_VALUE);
    }

    /** Call BG first, then window tiles in screen coordinates. */
    public void backgroundTile(Tile tile, int x, int y, boolean flipX, boolean flipY,
                               int[] palette, boolean priority) {
        stamp(tile, x, y, flipX, flipY, (pixel, index) -> {
            backgroundPalettes[pixel] = palette;
            backgroundIndex[pixel] = (byte) index;
            backgroundPriority[pixel] = priority;
        });
    }

    public void backgroundPixel(int x, int y, int index, int[] palette, boolean priority) {
        if (x < 0 || y < 0 || x >= WIDTH || y >= HEIGHT) return;
        int pixel = y * WIDTH + x;
        backgroundIndex[pixel] = (byte) index;
        backgroundPalettes[pixel] = palette;
        backgroundPriority[pixel] = priority;
    }

    private record ObjectTile(Tile tile, int x, int y, boolean flipX, boolean flipY,
                              int[] palette, int order, boolean behind) {}
    private final java.util.List<ObjectTile> oam = new java.util.ArrayList<>();
    @Override public void append(Tile tile, int x, int y, boolean flipX, boolean flipY,
                                 int[] palette, boolean behindBackground) {
        objectTile(tile, x, y, flipX, flipY, palette, oam.size(), behindBackground);
    }
    public void objectTile(Tile tile, int x, int y, boolean flipX, boolean flipY,
                           int[] palette, int oamIndex, boolean behindBackground) {
        if (oamIndex < 0 || oamIndex == Integer.MAX_VALUE) throw new IllegalArgumentException("OAM index");
        oam.add(new ObjectTile(tile, x, y, flipX, flipY, palette, oamIndex, behindBackground));
    }

    @Override public void render(RenderContext context) {
        compose(context.buffer());
    }

    public void compose(byte[] buffer) {
        Arrays.fill(objectOrder, Integer.MAX_VALUE);
        for (ObjectTile entry : oam) {
            stamp(entry.tile(), entry.x(), entry.y(), entry.flipX(), entry.flipY(), (pixel, index) -> {
                if (index == 0 || entry.order() >= objectOrder[pixel]) return;
                objects[pixel] = entry.palette()[index];
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
