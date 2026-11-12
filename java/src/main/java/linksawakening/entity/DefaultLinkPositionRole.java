package linksawakening.entity;

import static linksawakening.entity.Link.*;

/** Delegates position operations to state owners used by ROM {@code ComputeLinkPosition}. */
final class DefaultLinkPositionRole implements LinkPositionRole {
    private final LinkMotion linkMotion;

    DefaultLinkPositionRole(LinkAssembly assembly) {
        linkMotion = assembly.linkMotion.get();
    }

    public boolean consumeDoorWarp() {
        return linkMotion.accessConsumeDoorWarp();
    }

    public int romCollisionType() {
        return linkMotion.accessRomCollisionType();
    }

    public int romCollisionProbePixelX() {
        return linkMotion.accessRomCollisionProbePixelX();
    }

    public int romCollisionProbePixelY() {
        return linkMotion.accessRomCollisionProbePixelY();
    }

    public int romSpeedX() {
        return linkMotion.accessRomSpeedX();
    }

    public int romSpeedY() {
        return linkMotion.accessRomSpeedY();
    }

    public int romSwordCollisionY() {
        return linkMotion.accessRomSwordCollisionY();
    }

    public void setPixelPosition(int pixelX, int pixelY) {
        linkMotion.accessSetPixelPosition(pixelX, pixelY);
    }

    public void setRoomScrollEntryPixelPosition(int pixelX, int pixelY) {
        linkMotion.accessSetRoomScrollEntryPixelPosition(pixelX, pixelY);
    }

    public void setRoomEntryPixelPosition(int pixelX, int pixelY) {
        linkMotion.accessSetRoomEntryPixelPosition(pixelX, pixelY);
    }

    public void markRoomEntryPosition() {
        linkMotion.accessMarkRoomEntryPosition();
    }

    public int roomEntryPixelX() {
        return linkMotion.accessRoomEntryPixelX();
    }

    public int roomEntryPixelY() {
        return linkMotion.accessRoomEntryPixelY();
    }

    public int roomEntryRomPositionX() {
        return linkMotion.accessRoomEntryRomPositionX();
    }

    public int roomEntryRomPositionY() {
        return linkMotion.accessRoomEntryRomPositionY();
    }

    public void setRoomEntryRomPosition(int romPositionX, int romPositionY) {
        linkMotion.accessSetRoomEntryRomPosition(romPositionX, romPositionY);
    }

    public void applyRomSpeed(int speedX, int speedY) {
        linkMotion.accessApplyRomSpeed(speedX, speedY);
    }

    public void applyRomSpeedX(int speedX) {
        linkMotion.accessApplyRomSpeedX(speedX);
    }

    public void applyRomFinalPosition(int speedX, int speedY) {
        linkMotion.accessApplyRomFinalPosition(speedX, speedY);
    }

    public void captureRomFinalPosition() {
        linkMotion.accessCaptureRomFinalPosition();
    }

    public void restoreRomFinalPosition() {
        linkMotion.accessRestoreRomFinalPosition();
    }

    public void restoreRomFinalPositionX() {
        linkMotion.accessRestoreRomFinalPositionX();
    }

    public void clearRomPositionIncrement() {
        linkMotion.accessClearRomPositionIncrement();
    }

    public int pixelX() {
        return linkMotion.accessPixelX();
    }

    public int pixelY() {
        return linkMotion.accessPixelY();
    }

    public int romEntityX() {
        return linkMotion.accessRomEntityX();
    }

    public int romFinalPositionX() {
        return linkMotion.accessRomFinalPositionX();
    }

    public int romEntityY() {
        return linkMotion.accessRomEntityY();
    }
}
