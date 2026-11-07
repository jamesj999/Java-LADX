package linksawakening.render;

import linksawakening.gpu.Tile;

/** Adapter target for sprite producers, including world-owned renderers. */
@FunctionalInterface
public interface OamSink {
    void append(Tile tile, int x, int y, boolean flipX, boolean flipY,
                int[] palette, boolean behindBackground);
    /** Explicit CGB OAM slot; adapters without slot metadata retain submission order. */
    default void appendInSlot(int slot, Tile tile, int x, int y, boolean flipX, boolean flipY,
                              int[] palette, boolean behindBackground) {
        append(tile, x, y, flipX, flipY, palette, behindBackground);
    }
}
