package linksawakening.entity;

import static linksawakening.entity.Link.*;

/** Probe pixels, collision mask and pending door warp from BackgroundCollisionHandler. */
final class LinkCollisionState {
    private int romCollisionType;

    int romCollisionType() {
        return romCollisionType;
    }

    int romCollisionType(int value) {
        return romCollisionType = value;
    }

    private boolean doorWarpPending;

    boolean doorWarpPending() {
        return doorWarpPending;
    }

    boolean doorWarpPending(boolean value) {
        return doorWarpPending = value;
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
