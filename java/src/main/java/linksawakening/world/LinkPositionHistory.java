package linksawakening.world;

import java.util.Arrays;

/**
 * The sixteen-byte Link position/direction history used by Marin's follower
 * handler (wLinkPositionXHistory through wLinkDirectionHistory).
 */
public final class LinkPositionHistory {
    public static final int LENGTH = 0x10;

    private final int[] x = new int[LENGTH];
    private final int[] y = new int[LENGTH];
    private final int[] z = new int[LENGTH];
    private final int[] direction = new int[LENGTH];

    /** Mirrors bank $01's decrementConsecutiveBytes helper for follower setup. */
    public void fill(int positionX, int positionY, int positionZ, int linkDirection) {
        Arrays.fill(x, byteValue(positionX));
        Arrays.fill(y, byteValue(positionY));
        Arrays.fill(z, byteValue(positionZ));
        Arrays.fill(direction, byteValue(linkDirection));
    }

    public void write(int index, int positionX, int positionY, int positionZ,
                      int linkDirection) {
        checkIndex(index);
        x[index] = byteValue(positionX);
        y[index] = byteValue(positionY);
        z[index] = byteValue(positionZ);
        direction[index] = byteValue(linkDirection);
    }

    public int xAt(int index) {
        checkIndex(index);
        return x[index];
    }

    public int yAt(int index) {
        checkIndex(index);
        return y[index];
    }

    public int zAt(int index) {
        checkIndex(index);
        return z[index];
    }

    public int directionAt(int index) {
        checkIndex(index);
        return direction[index];
    }

    private static int byteValue(int value) {
        return value & 0xFF;
    }

    private static void checkIndex(int index) {
        if (index < 0 || index >= LENGTH) {
            throw new IllegalArgumentException("Link history index out of range: " + index);
        }
    }
}
