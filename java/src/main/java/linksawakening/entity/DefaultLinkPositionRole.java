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
        return linkMotion.position().accessRomSpeedX();
    }

    public int romSpeedY() {
        return linkMotion.position().accessRomSpeedY();
    }

    public int romSwordCollisionY() {
        return linkMotion.romSwordCollisionY();
    }

    public void setPixelPosition(int pixelX, int pixelY) {
        linkMotion.position().setPixelPosition(pixelX, pixelY);
    }

    public void setRoomScrollEntryPixelPosition(int pixelX, int pixelY) {
        linkMotion.position().setRoomScrollEntryPixelPosition(pixelX, pixelY);
    }

    public void setRoomEntryPixelPosition(int pixelX, int pixelY) {
        linkMotion.position().setRoomEntryPixelPosition(pixelX, pixelY);
    }

    public void markRoomEntryPosition() {
        linkMotion.position().markRoomEntryPosition();
    }

    public int roomEntryPixelX() {
        return linkMotion.position().roomEntryPixelX();
    }

    public int roomEntryPixelY() {
        return linkMotion.position().roomEntryPixelY();
    }

    public int roomEntryRomPositionX() {
        return linkMotion.position().roomEntryRomPositionX();
    }

    public int roomEntryRomPositionY() {
        return linkMotion.position().roomEntryRomPositionY();
    }

    public void setRoomEntryRomPosition(int romPositionX, int romPositionY) {
        linkMotion.position().setRoomEntryRomPosition(romPositionX, romPositionY);
    }

    public void applyRomSpeed(int speedX, int speedY) {
        linkMotion.position().applyRomSpeed(speedX, speedY);
    }

    public void applyRomSpeedX(int speedX) {
        linkMotion.position().applyRomSpeedX(speedX);
    }

    public void applyRomFinalPosition(int speedX, int speedY) {
        linkMotion.position().applyRomFinalPosition(speedX, speedY);
    }

    public void captureRomFinalPosition() {
        linkMotion.position().captureRomFinalPosition();
    }

    public void restoreRomFinalPosition() {
        linkMotion.position().restoreRomFinalPosition();
    }

    public void restoreRomFinalPositionX() {
        linkMotion.position().restoreRomFinalPositionX();
    }

    public void clearRomPositionIncrement() {
        linkMotion.position().clearRomPositionIncrement();
    }

    public int pixelX() {
        return linkMotion.position().pixelX();
    }

    public int pixelY() {
        return linkMotion.position().pixelY();
    }

    public int romEntityX() {
        return linkMotion.position().romEntityX();
    }

    public int romFinalPositionX() {
        return linkMotion.position().romFinalPositionX();
    }

    public int romEntityY() {
        return linkMotion.position().romEntityY();
    }
}
