package linksawakening.entity;

import static linksawakening.entity.Link.*;
import static linksawakening.entity.LinkMotion.*;

/** LinkPositionAccess behavior on the shared LinkMotion state. */
abstract class LinkPositionAccess extends LinkPositionUpdates {
    protected LinkPositionAccess(Dependencies dependencies) {
        super(dependencies);
    }

    protected int pixelY() {
        return subY >> Link.SUB_PIXEL_SHIFT;
    }

    /** ROM hLinkPositionX: the first 8x16 OAM column's X origin. */
    protected int romEntityX() {
        return pixelX() + 0x08;
    }

    /** ROM hLinkPositionY before the separate vertical Z offset is applied. */
    protected int romEntityY() {
        return pixelY() + 0x10;
    }

    /** ROM hLinkFinalPositionX captured before the current Link update. */
    protected int romFinalPositionX() {
        return hasRomFinalPosition ? (romFinalSubX >> Link.SUB_PIXEL_SHIFT) + 0x08 : romEntityX();
    }

    /** Mirrors ClearLinkPositionIncrement for handlers that clear HRAM speed bytes. */
    protected void clearRomPositionIncrement() {
        forcedSpeedX = 0;
        forcedSpeedY = 0;
        forcedSpeedPending = false;
        forcedSpeedXOnly = false;
        linkSideView.get().sideViewPlatformVerticalSpeedPending = -1;
        lastRomSpeedX = 0;
        lastRomSpeedY = 0;
    }

    protected boolean accessConsumeDoorWarp() {
        boolean pending = this.doorWarpPending;
        this.doorWarpPending = false;
        return pending;
    }

    protected int accessRomCollisionType() {
        return this.romCollisionType & 0xFF;
    }

    protected int accessRomCollisionProbePixelX() {
        return linkAnimationState.get().accessHasFacingCollision()
                ? this.collisionProbePixelX[linkAnimationState.get().direction]
                : this.pixelX();
    }

    protected int accessRomCollisionProbePixelY() {
        return linkAnimationState.get().accessHasFacingCollision()
                ? this.collisionProbePixelY[linkAnimationState.get().direction]
                : this.pixelY();
    }

    protected int accessRomSpeedX() {
        return this.lastRomSpeedX & 0xFF;
    }

    protected int accessRomSpeedY() {
        return this.lastRomSpeedY & 0xFF;
    }

    protected int accessRomSwordCollisionY() {
        return this.romEntityY() - linkAirborne.get().accessZPixels();
    }

    protected void accessSetPixelPosition(int pixelX, int pixelY) {
        this.setPixelPosition(pixelX, pixelY);
    }

    protected void accessSetRoomScrollEntryPixelPosition(int pixelX, int pixelY) {
        this.setRoomScrollEntryPixelPosition(pixelX, pixelY);
    }

    protected void accessSetRoomEntryPixelPosition(int pixelX, int pixelY) {
        this.setRoomEntryPixelPosition(pixelX, pixelY);
    }

    protected void accessMarkRoomEntryPosition() {
        this.markRoomEntryPosition();
    }

    protected int accessRoomEntryPixelX() {
        return this.roomEntryPixelX();
    }

    protected int accessRoomEntryPixelY() {
        return this.roomEntryPixelY();
    }

    protected int accessRoomEntryRomPositionX() {
        return this.roomEntryRomPositionX();
    }

    protected int accessRoomEntryRomPositionY() {
        return this.roomEntryRomPositionY();
    }

    protected void accessSetRoomEntryRomPosition(int romPositionX, int romPositionY) {
        this.setRoomEntryRomPosition(romPositionX, romPositionY);
    }

    protected void accessApplyRomSpeed(int speedX, int speedY) {
        this.applyRomSpeed(speedX, speedY);
    }

    protected void accessApplyRomSpeedX(int speedX) {
        this.applyRomSpeedX(speedX);
    }

    protected void accessApplyRomFinalPosition(int speedX, int speedY) {
        this.applyRomFinalPosition(speedX, speedY);
    }

    protected void accessCaptureRomFinalPosition() {
        this.captureRomFinalPosition();
    }

    protected void accessRestoreRomFinalPosition() {
        this.restoreRomFinalPosition();
    }

    protected void accessRestoreRomFinalPositionX() {
        this.restoreRomFinalPositionX();
    }

    protected void accessClearRomPositionIncrement() {
        this.clearRomPositionIncrement();
    }

    protected int accessPixelX() {
        return this.pixelX();
    }

    protected int accessPixelY() {
        return this.pixelY();
    }

    protected int accessRomEntityX() {
        return this.romEntityX();
    }

    protected int accessRomFinalPositionX() {
        return this.romFinalPositionX();
    }

    protected int accessRomEntityY() {
        return this.romEntityY();
    }
}
