package linksawakening.entity;

/** Probe pixels, collision mask and pending door warp from BackgroundCollisionHandler. */
final class LinkCollisionState {

    private int romCollisionType;

    private boolean doorWarpPending;

    private final int[] collisionProbePixelX = new int[4];

    private final int[] collisionProbePixelY = new int[4];

    int romCollisionType() {
        return romCollisionType;
    }

    int romCollisionType(int value) {
        romCollisionType = value;
        return romCollisionType;
    }

    boolean doorWarpPending() {
        return doorWarpPending;
    }

    boolean doorWarpPending(boolean value) {
        doorWarpPending = value;
        return doorWarpPending;
    }

    int[] collisionProbePixelX() {
        return collisionProbePixelX;
    }

    int[] collisionProbePixelY() {
        return collisionProbePixelY;
    }
}
