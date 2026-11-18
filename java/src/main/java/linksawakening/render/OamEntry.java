package linksawakening.render;

import java.util.function.IntUnaryOperator;
import linksawakening.gpu.Tile;

/** Reusable software OAM entry, retaining the original Y for both tile halves. */
final class OamEntry {
    Tile tile;

    int x;

    int y;

    boolean flipX;

    boolean flipY;

    int[] palette;

    int order;

    boolean behind;

    String source;

    int entryY;

    IntUnaryOperator transform;

    Tile tile() {
        return tile;
    }

    int x() {
        return x;
    }

    int y() {
        return y;
    }

    boolean flipX() {
        return flipX;
    }

    boolean flipY() {
        return flipY;
    }

    int[] palette() {
        return palette;
    }

    int order() {
        return order;
    }

    boolean behind() {
        return behind;
    }

    String source() {
        return source;
    }

    int entryY() {
        return entryY;
    }

    IntUnaryOperator transform() {
        return transform;
    }
}
