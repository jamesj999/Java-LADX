package linksawakening.gpu;

/** Stores one decoded eight-by-eight indexed hardware tile. */
public final class Tile {
    private final int[] colorIndex;
    private final boolean readOnly;

    public Tile() {
        this(new int[64]);
    }

    public Tile(int[] pixels) {
        this(pixels, false);
    }

    private Tile(int[] pixels, boolean readOnly) {
        this.colorIndex = pixels;
        this.readOnly = readOnly;
    }

    public int getPixel(int x, int y) {
        return colorIndex[y * 8 + x];
    }

    public void setPixel(int x, int y, int color) {
        if (readOnly) throw new UnsupportedOperationException("Snapshot tiles are read-only");
        colorIndex[y * 8 + x] = color;
    }

    Tile frozenCopy() {
        return new Tile(colorIndex.clone(), true);
    }

    public Tile copy() {
        return new Tile(colorIndex.clone());
    }
}
