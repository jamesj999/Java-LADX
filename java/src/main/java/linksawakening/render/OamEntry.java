package linksawakening.render;

import java.util.function.IntUnaryOperator;
import linksawakening.gpu.Tile;

/** Reusable software OAM entry, retaining the original Y for both tile halves. */
final class OamEntry {
    Tile tile;

    Tile tile() {
        return tile;
    }

    int x;

    int x() {
        return x;
    }

    int y;

    int y() {
        return y;
    }

    boolean flipX;

    boolean flipX() {
        return flipX;
    }

    boolean flipY;

    boolean flipY() {
        return flipY;
    }

    int[] palette;

    int[] palette() {
        return palette;
    }

    int order;

    int order() {
        return order;
    }

    boolean behind;

    boolean behind() {
        return behind;
    }

    String source;

    String source() {
        return source;
    }

    int entryY;

    int entryY() {
        return entryY;
    }

    IntUnaryOperator transform;

    IntUnaryOperator transform() {
        return transform;
    }
}
