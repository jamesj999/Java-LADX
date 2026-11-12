package linksawakening.cutscene;

import static linksawakening.cutscene.IntroSequence.*;

/** Explicit shared intro map/scroll writes consumed at the original VBlank boundary. */
final class IntroVideoMemory {

    private int scrollY;
    private int beachScrollX;
    private boolean drawCommandQueued;
    private boolean beachScrollStoppedThisFrame;
    private int[] tilemap;
    private int[] attrmap;

    int scrollY() {
        return scrollY;
    }

    int scrollY(int value) {
        scrollY = value;
        return value;
    }

    int beachScrollX() {
        return beachScrollX;
    }

    int beachScrollX(int value) {
        beachScrollX = value;
        return value;
    }

    boolean drawCommandQueued() {
        return drawCommandQueued;
    }

    boolean drawCommandQueued(boolean value) {
        drawCommandQueued = value;
        return value;
    }

    boolean beachScrollStoppedThisFrame() {
        return beachScrollStoppedThisFrame;
    }

    boolean beachScrollStoppedThisFrame(boolean value) {
        beachScrollStoppedThisFrame = value;
        return value;
    }

    int[] tilemap() {
        return tilemap;
    }

    int[] tilemap(int[] value) {
        tilemap = value;
        return value;
    }

    int[] attrmap() {
        return attrmap;
    }

    int[] attrmap(int[] value) {
        attrmap = value;
        return value;
    }

    private int scrollX;

    int scrollX() {
        return scrollX;
    }

    int scrollX(int value) {
        scrollX = value;
        return value;
    }
}
