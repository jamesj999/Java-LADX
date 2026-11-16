package linksawakening.entity;

/** Probe pixels, collision mask and pending door warp from BackgroundCollisionHandler. */
final class LinkCollisionState {

    private int romCollisionType;

    int romCollisionType() {
        return romCollisionType;
    }

    int romCollisionType(int value) {
        romCollisionType = value;
        return romCollisionType;
    }

    private boolean doorWarpPending;

    boolean doorWarpPending() {
        return doorWarpPending;
    }

    boolean doorWarpPending(boolean value) {
        doorWarpPending = value;
        return doorWarpPending;
    }

    private final int[] collisionProbePixelX = new int[4];

    int[] collisionProbePixelX() {
        return collisionProbePixelX;
    }

    private final int[] collisionProbePixelY = new int[4];

    int[] collisionProbePixelY() {
        return collisionProbePixelY;
    }
}
