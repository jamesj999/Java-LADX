package linksawakening.entity;

import static linksawakening.entity.Link.*;

/** Owns subpixel coordinates, speeds and shadows for ComputeLinkPosition / UpdateFinalLinkPosition. */
final class LinkPosition {

    private int subX;

    int subX() {
        return subX;
    }

    int subX(int value) {
        return subX = value;
    }

    private int subY;

    int subY() {
        return subY;
    }

    int subY(int value) {
        return subY = value;
    }

    private int lastRomSpeedX;

    int lastRomSpeedX() {
        return lastRomSpeedX;
    }

    int lastRomSpeedX(int value) {
        return lastRomSpeedX = value;
    }

    private int lastRomSpeedY;

    int lastRomSpeedY() {
        return lastRomSpeedY;
    }

    int lastRomSpeedY(int value) {
        return lastRomSpeedY = value;
    }

    private boolean forcedSpeedPending;

    boolean forcedSpeedPending() {
        return forcedSpeedPending;
    }

    boolean forcedSpeedPending(boolean value) {
        return forcedSpeedPending = value;
    }

    private boolean forcedSpeedXOnly;

    boolean forcedSpeedXOnly() {
        return forcedSpeedXOnly;
    }

    boolean forcedSpeedXOnly(boolean value) {
        return forcedSpeedXOnly = value;
    }

    private int forcedSpeedX;

    int forcedSpeedX() {
        return forcedSpeedX;
    }

    int forcedSpeedX(int value) {
        return forcedSpeedX = value;
    }

    private int forcedSpeedY;

    int forcedSpeedY() {
        return forcedSpeedY;
    }

    int forcedSpeedY(int value) {
        return forcedSpeedY = value;
    }

    private int lastSafeSubX;

    int lastSafeSubX() {
        return lastSafeSubX;
    }

    int lastSafeSubX(int value) {
        return lastSafeSubX = value;
    }

    private int lastSafeSubY;

    int lastSafeSubY() {
        return lastSafeSubY;
    }

    int lastSafeSubY(int value) {
        return lastSafeSubY = value;
    }

    private boolean hasLastSafePosition;

    boolean hasLastSafePosition() {
        return hasLastSafePosition;
    }

    boolean hasLastSafePosition(boolean value) {
        return hasLastSafePosition = value;
    }

    private int romFinalSubX;

    private int romFinalSubY;

    private boolean hasRomFinalPosition;

    private int roomEntrySubX;

    int roomEntrySubX() {
        return roomEntrySubX;
    }

    int roomEntrySubX(int value) {
        return roomEntrySubX = value;
    }

    private int roomEntrySubY;

    int roomEntrySubY() {
        return roomEntrySubY;
    }

    int roomEntrySubY(int value) {
        return roomEntrySubY = value;
    }

    private boolean hasRoomEntryPosition;

    boolean hasRoomEntryPosition() {
        return hasRoomEntryPosition;
    }

    boolean hasRoomEntryPosition(boolean value) {
        return hasRoomEntryPosition = value;
    }

    private final LinkComponentRef<LinkSideView> linkSideView;
    private final LinkCollisionState collisionState;

    LinkPosition(LinkComponentRef<LinkSideView> linkSideView, LinkCollisionState collisionState) {
        this.linkSideView = linkSideView;
        this.collisionState = collisionState;
    }

    void setPixelPosition(int pixelX, int pixelY) {
        subX = pixelX << Link.SUB_PIXEL_SHIFT;
        subY = pixelY << Link.SUB_PIXEL_SHIFT;
    }

    void setRoomEntryPixelPosition(int pixelX, int pixelY) {
        linkSideView.get().clearSideViewPlatformState();
        setPixelPosition(pixelX, pixelY);
        markRoomEntryPosition();
    }

    void setRoomScrollEntryPixelPosition(int pixelX, int pixelY) {
        linkSideView.get().clearSideViewPlatformState();
        subX = subX + ((pixelX - pixelX()) << Link.SUB_PIXEL_SHIFT);
        subY = subY + ((pixelY - pixelY()) << Link.SUB_PIXEL_SHIFT);
        markRoomEntryPosition();
    }

    void markRoomEntryPosition() {
        collisionState.doorWarpPending(false);
        roomEntrySubX = subX;
        roomEntrySubY = subY;
        hasRoomEntryPosition = true;
    }

    /** Returns the Java top-left X coordinate recorded for the current room entry. */
    int roomEntryPixelX() {
        return hasRoomEntryPosition ? roomEntrySubX >> Link.SUB_PIXEL_SHIFT : pixelX();
    }

    /** Returns the Java top-left Y coordinate recorded for the current room entry. */
    int roomEntryPixelY() {
        return hasRoomEntryPosition ? roomEntrySubY >> Link.SUB_PIXEL_SHIFT : pixelY();
    }

    /**
     * Returns the source {@code hLinkPositionX} value for the recorded entry.
     * The Game Boy stores Link's OAM origin, which is eight pixels to the right
     * of this host model's top-left sprite coordinate.
     */
    int roomEntryRomPositionX() {
        return roomEntryPixelX() + 0x08;
    }

    /**
     * Returns the source {@code hLinkPositionY} value for the recorded entry.
     * The Game Boy stores the lower OAM origin, sixteen pixels below this host
     * model's top-left sprite coordinate.
     */
    int roomEntryRomPositionY() {
        return roomEntryPixelY() + 0x10;
    }

    /** Applies a source warp/save position (OAM origin) to the host model. */
    void setRoomEntryRomPosition(int romPositionX, int romPositionY) {
        setRoomEntryPixelPosition(romPositionX - 0x08, romPositionY - 0x10);
    }

    /**
     * Applies the next-frame hLinkSpeedX/hLinkSpeedY values written by an
     * entity handler such as the mirror-shield laser reflection path.
     *
     * <p>The ROM stores these as signed bytes in HRAM.  They are consumed by
     * the following Link motion update, after AnimateEntities has finished
     * writing them for the current frame.</p>
     */
    void applyRomSpeed(int speedX, int speedY) {
        if ((speedX & ~0xFF) != 0 || (speedY & ~0xFF) != 0) {
            throw new IllegalArgumentException("Link response speeds must be unsigned bytes");
        }
        forcedSpeedX = (byte) speedX;
        forcedSpeedY = (byte) speedY;
        forcedSpeedXOnly = false;
        forcedSpeedPending = true;
    }

    /** Applies only the hLinkSpeedX write emitted by a side-view pot push. */
    void applyRomSpeedX(int speedX) {
        Link.validateRomByte(speedX, "Link X speed");
        forcedSpeedX = (byte) speedX;
        // The pot handler leaves hLinkSpeedY untouched; Link computes its
        // ordinary Y input on the next motion accessUpdate.
        forcedSpeedXOnly = true;
        forcedSpeedPending = true;
    }

    /**
     * Applies the speed bytes written by a handler that immediately calls
     * {@code UpdateFinalLinkPosition}, bypassing input and collision probes.
     */
    void applyRomFinalPosition(int speedX, int speedY) {
        if ((speedX & ~0xFF) != 0 || (speedY & ~0xFF) != 0) {
            throw new IllegalArgumentException("Link final-position speeds must be unsigned bytes");
        }
        subY = subY + ((byte) speedY);
        subX = subX + ((byte) speedX);
    }

    /** Captures hLinkPositionX/Y into the ROM's per-frame final-position shadow. */
    void captureRomFinalPosition() {
        romFinalSubX = subX;
        romFinalSubY = subY;
        hasRomFinalPosition = true;
    }

    /** Copies the captured ROM final position back over an entity push. */
    void restoreRomFinalPosition() {
        if (!hasRomFinalPosition) {
            return;
        }
        subX = romFinalSubX;
        subY = romFinalSubY;
    }

    /** Copies only hLinkFinalPositionX back over a side-view pot push. */
    void restoreRomFinalPositionX() {
        if (hasRomFinalPosition) {
            subX = romFinalSubX;
        }
    }

    int pixelX() {
        return subX >> Link.SUB_PIXEL_SHIFT;
    }

    int pixelY() {
        return subY >> Link.SUB_PIXEL_SHIFT;
    }

    /** ROM hLinkPositionX: the first 8x16 OAM column's X origin. */
    int romEntityX() {
        return pixelX() + 0x08;
    }

    /** ROM hLinkPositionY before the separate vertical Z offset is applied. */
    int romEntityY() {
        return pixelY() + 0x10;
    }

    /** ROM hLinkFinalPositionX captured before the current Link update. */
    int romFinalPositionX() {
        return hasRomFinalPosition ? (romFinalSubX >> Link.SUB_PIXEL_SHIFT) + 0x08 : romEntityX();
    }

    /** Mirrors ClearLinkPositionIncrement for handlers that clear HRAM speed bytes. */
    void clearRomPositionIncrement() {
        forcedSpeedX = 0;
        forcedSpeedY = 0;
        forcedSpeedPending = false;
        forcedSpeedXOnly = false;
        linkSideView.get().sideViewPlatformVerticalSpeedPending = -1;
        lastRomSpeedX = 0;
        lastRomSpeedY = 0;
    }

    int accessRomSpeedX() {
        return lastRomSpeedX & 0xFF;
    }

    int accessRomSpeedY() {
        return lastRomSpeedY & 0xFF;
    }
}
