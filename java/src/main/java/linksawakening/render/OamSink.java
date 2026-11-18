package linksawakening.render;

import linksawakening.gpu.Tile;

/** Adapter target for sprite producers, including world-owned renderers. */
@FunctionalInterface
public interface OamSink {
    void append(Tile tile, int x, int y, boolean flipX, boolean flipY, int[] palette, boolean behindBackground);
    /** Compatibility metadata only; production uses painter order until RomOamAllocator exists. */
    default void appendInSlot(
            int slot, Tile tile, int x, int y, boolean flipX, boolean flipY, int[] palette, boolean behindBackground) {
        append(tile, x, y, flipX, flipY, palette, behindBackground);
    }
    /** One ROM 8x16 OAM entry; both decoded halves retain its original screen Y. */
    default void append8x16(
            Tile top, Tile bottom, int x, int y, boolean fx, boolean fy, int[] palette, boolean behind) {
        appendFromEntry(y, top, x, y, fx, fy, palette, behind);
        appendFromEntry(y, bottom, x, y + 8, fx, fy, palette, behind);
    }

    default void appendFromEntry(
            int entryY, Tile tile, int x, int y, boolean fx, boolean fy, int[] palette, boolean behind) {
        append(tile, x, y, fx, fy, palette, behind);
    }
}
