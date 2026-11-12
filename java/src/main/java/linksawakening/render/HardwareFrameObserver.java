package linksawakening.render;

import java.util.List;
import linksawakening.gpu.Tile;

/** Optional submission observer; absent in production composition. Implementations own their storage. */
interface HardwareFrameObserver {
    void attach(HardwareFrame frame);

    void reset();

    void backgroundPixel(int pixel, String source);

    void hudPixel(int pixel, int tile, int attributes);

    void copiedBackgroundPixel(int pixel, HardwareFrameObserver source, String layer);

    void closedHudTile(Tile tile, int tileId, int attributes, int x, int y);

    void beforeInventoryHiding(List<OamEntry> entries, int windowY);

    void beginComposition();

    void objectPixel(int pixel, String source, int flags);
}
