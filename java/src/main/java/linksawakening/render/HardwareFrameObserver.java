package linksawakening.render;

import java.util.List;
import linksawakening.gpu.Tile;

/** Optional submission observer; absent in production composition. Implementations own their storage. */
interface HardwareFrameObserver {
    HardwareFrameObserver NONE = new HardwareFrameObserver() {};

    default void attach(HardwareFrame frame) {}

    default void reset() {}

    default void backgroundPixel(int pixel, String source) {}

    default void hudPixel(int pixel, int tile, int attributes) {}

    default void copiedBackgroundPixel(int pixel, HardwareFrameObserver source, String layer) {}

    default void closedHudTile(Tile tile, int tileId, int attributes, int x, int y) {}

    default void beforeInventoryHiding(List<OamEntry> entries, int windowY) {}

    default void beginComposition() {}

    default void objectPixel(int pixel, String source, int flags) {}
}
