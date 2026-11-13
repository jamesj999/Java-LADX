package linksawakening.entity;

/** Delegates position operations to state owners used by ROM {@code ComputeLinkPosition}. */
final class DefaultLinkPositionRole implements LinkPositionRole {
    private final LinkMotion linkMotion;

    DefaultLinkPositionRole(LinkAssembly assembly) {
        linkMotion = assembly.linkMotion.get();
    }

    @Override
    public boolean consumeDoorWarp() {
        return linkMotion.accessConsumeDoorWarp();
    }

    @Override
    public int romCollisionType() {
        return linkMotion.accessRomCollisionType();
    }

    @Override
    public int romCollisionProbePixelX() {
        return linkMotion.accessRomCollisionProbePixelX();
    }

    @Override
    public int romCollisionProbePixelY() {
        return linkMotion.accessRomCollisionProbePixelY();
    }

    @Override
    public int romSpeedX() {
        return linkMotion.position().accessRomSpeedX();
    }

    @Override
    public int romSpeedY() {
        return linkMotion.position().accessRomSpeedY();
    }

    @Override
    public int romSwordCollisionY() {
        return linkMotion.romSwordCollisionY();
    }

    @Override
    public void setPixelPosition(int pixelX, int pixelY) {
        linkMotion.position().setPixelPosition(pixelX, pixelY);
    }

    @Override
    public void setRoomScrollEntryPixelPosition(int pixelX, int pixelY) {
        linkMotion.position().setRoomScrollEntryPixelPosition(pixelX, pixelY);
    }

    @Override
    public void setRoomEntryPixelPosition(int pixelX, int pixelY) {
        linkMotion.position().setRoomEntryPixelPosition(pixelX, pixelY);
    }

    @Override
    public void markRoomEntryPosition() {
        linkMotion.position().markRoomEntryPosition();
    }

    @Override
    public int roomEntryPixelX() {
        return linkMotion.position().roomEntryPixelX();
    }

    @Override
    public int roomEntryPixelY() {
        return linkMotion.position().roomEntryPixelY();
    }

    @Override
    public int roomEntryRomPositionX() {
        return linkMotion.position().roomEntryRomPositionX();
    }

    @Override
    public int roomEntryRomPositionY() {
        return linkMotion.position().roomEntryRomPositionY();
    }

    @Override
    public void setRoomEntryRomPosition(int romPositionX, int romPositionY) {
        linkMotion.position().setRoomEntryRomPosition(romPositionX, romPositionY);
    }

    @Override
    public void applyRomSpeed(int speedX, int speedY) {
        linkMotion.position().applyRomSpeed(speedX, speedY);
    }

    @Override
    public void applyRomSpeedX(int speedX) {
        linkMotion.position().applyRomSpeedX(speedX);
    }

    @Override
    public void applyRomFinalPosition(int speedX, int speedY) {
        linkMotion.position().applyRomFinalPosition(speedX, speedY);
    }

    @Override
    public void captureRomFinalPosition() {
        linkMotion.position().captureRomFinalPosition();
    }

    @Override
    public void restoreRomFinalPosition() {
        linkMotion.position().restoreRomFinalPosition();
    }

    @Override
    public void restoreRomFinalPositionX() {
        linkMotion.position().restoreRomFinalPositionX();
    }

    @Override
    public void clearRomPositionIncrement() {
        linkMotion.position().clearRomPositionIncrement();
    }

    @Override
    public int pixelX() {
        return linkMotion.position().pixelX();
    }

    @Override
    public int pixelY() {
        return linkMotion.position().pixelY();
    }

    @Override
    public int romEntityX() {
        return linkMotion.position().romEntityX();
    }

    @Override
    public int romFinalPositionX() {
        return linkMotion.position().romFinalPositionX();
    }

    @Override
    public int romEntityY() {
        return linkMotion.position().romEntityY();
    }
}
