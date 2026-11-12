package linksawakening.entity;

import static linksawakening.entity.Link.*;
import static linksawakening.entity.LinkMotion.*;

import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.state.Direction;
import linksawakening.state.LinkMotionState;

/** ComputeLinkPosition, UpdateFinalLinkPosition and CopyLinkFinalPositionToPosition state and command implementation. */
final class LinkMotion extends LinkCollisionMotion {
    public LinkMotion(Dependencies dependencies) {
        super(dependencies);
    }

    protected boolean applyOverworldLedgeCollision(int pointY) {
        linkAirborne.get().directionalLedgeCollisionCounter = 0;
        if (linkEntityEffects.get().roosterCarryActive || linkAirborne.get().overworldLedgeFallActive) {
            return true;
        }
        if ((pointY & 0x0F) < 8) {
            return false;
        }

        linkStateMachine.get().resetPegasusBoots();
        linkAnimator.get().ledgeJumpRenderY = pixelY() - linkAirborne.get().accessZPixels();
        subX = (accessRomEntityX() & 0xF0) << Link.SUB_PIXEL_SHIFT;
        subY -= 8 << Link.SUB_PIXEL_SHIFT;
        linkStateMachine.get().motionState = LinkMotionState.UNSTUCKING;
        linkStateMachine.get().physicsModifier = 0;
        linkAirborne.get().overworldLedgeFallActive = true;
        // The collision changes wLinkMotionState only. The dispatcher runs
        // LinkMotionUnstuckingHandler on the following frame ($02:4960).
        linkServices.get().soundSink.play(GameplaySoundEvent.LEDGE_FALL);
        return true;
    }

    /** Ports LinkMotionUnstuckingHandler's initial downward search. */
    protected void beginOverworldLedgeUnstuckingMotion() {
        lastRomSpeedX = 0;
        lastRomSpeedY = 0xFF;
        subY += 16 << Link.SUB_PIXEL_SHIFT;
        linkAirborne.get().zSubPixels += 16 << Link.SUB_PIXEL_SHIFT;
        for (int step = 0; step < 16; step++) {
            subY += 8 << Link.SUB_PIXEL_SHIFT;
            linkAirborne.get().zSubPixels += 8 << Link.SUB_PIXEL_SHIFT;
            if (!ledgeUnstuckingPositionBlocked()) {
                break;
            }
        }
        linkStateMachine.get().physicsModifier = 1;
        subY -= 3 << Link.SUB_PIXEL_SHIFT;
        linkAirborne.get().airborne = true;
        linkAirborne.get().zVelocity = 0;
        linkAnimator.get().jumpAnimationCounter = 0;
        linkAnimator.get().jumpAnimationFrame = 0;
    }

    protected boolean ledgeUnstuckingPositionBlocked() {
        int spriteX = accessPixelX();
        int spriteY = accessPixelY();
        boolean blocked = false;
        int lastObject = 0xFF;
        for (int index = 0; index < 2; index++) {
            int pointX = spriteX + LinkRomConstants.COLLISION_POINTS_X[Direction.UP.javaIndex()][index];
            int pointY = spriteY + LinkRomConstants.COLLISION_POINTS_Y[Direction.UP.javaIndex()][index];
            blocked |= linkServices
                    .get()
                    .collision
                    .pointBlockedForLink(pointX, pointY, linkControls.get().accessPlayerHasFlippers());
            lastObject = linkServices.get().collision.objectIdAtPoint(pointX, pointY);
        }
        // LinkMotionUnstuckingHandler ($02:4990-499E) continues over $E1,
        // but explicitly stops over OBJECT_WELL despite its solid physics.
        if (lastObject == 0xE1) return true;
        if (lastObject == 0x61) return false;
        return blocked;
    }

    protected void setPixelPosition(int pixelX, int pixelY) {
        subX = pixelX << Link.SUB_PIXEL_SHIFT;
        subY = pixelY << Link.SUB_PIXEL_SHIFT;
    }

    protected void setRoomEntryPixelPosition(int pixelX, int pixelY) {
        linkSideView.get().clearSideViewPlatformState();
        setPixelPosition(pixelX, pixelY);
        markRoomEntryPosition();
    }

    protected void setRoomScrollEntryPixelPosition(int pixelX, int pixelY) {
        linkSideView.get().clearSideViewPlatformState();
        subX += (pixelX - pixelX()) << Link.SUB_PIXEL_SHIFT;
        subY += (pixelY - pixelY()) << Link.SUB_PIXEL_SHIFT;
        markRoomEntryPosition();
    }

    protected void markRoomEntryPosition() {
        doorWarpPending = false;
        roomEntrySubX = subX;
        roomEntrySubY = subY;
        hasRoomEntryPosition = true;
    }

    /** Returns the Java top-left X coordinate recorded for the current room entry. */
    protected int roomEntryPixelX() {
        return hasRoomEntryPosition ? roomEntrySubX >> Link.SUB_PIXEL_SHIFT : pixelX();
    }

    /** Returns the Java top-left Y coordinate recorded for the current room entry. */
    protected int roomEntryPixelY() {
        return hasRoomEntryPosition ? roomEntrySubY >> Link.SUB_PIXEL_SHIFT : pixelY();
    }

    /**
     * Returns the source {@code hLinkPositionX} value for the recorded entry.
     * The Game Boy stores Link's OAM origin, which is eight pixels to the right
     * of this host model's top-left sprite coordinate.
     */
    protected int roomEntryRomPositionX() {
        return roomEntryPixelX() + 0x08;
    }

    /**
     * Returns the source {@code hLinkPositionY} value for the recorded entry.
     * The Game Boy stores the lower OAM origin, sixteen pixels below this host
     * model's top-left sprite coordinate.
     */
    protected int roomEntryRomPositionY() {
        return roomEntryPixelY() + 0x10;
    }

    /** Applies a source warp/save position (OAM origin) to the host model. */
    protected void setRoomEntryRomPosition(int romPositionX, int romPositionY) {
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
    protected void applyRomSpeed(int speedX, int speedY) {
        if ((speedX & ~0xFF) != 0 || (speedY & ~0xFF) != 0) {
            throw new IllegalArgumentException("Link response speeds must be unsigned bytes");
        }
        forcedSpeedX = (byte) speedX;
        forcedSpeedY = (byte) speedY;
        forcedSpeedXOnly = false;
        forcedSpeedPending = true;
    }

    /** Applies only the hLinkSpeedX write emitted by a side-view pot push. */
    protected void applyRomSpeedX(int speedX) {
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
    protected void applyRomFinalPosition(int speedX, int speedY) {
        if ((speedX & ~0xFF) != 0 || (speedY & ~0xFF) != 0) {
            throw new IllegalArgumentException("Link final-position speeds must be unsigned bytes");
        }
        subY += (byte) speedY;
        subX += (byte) speedX;
    }

    /** Captures hLinkPositionX/Y into the ROM's per-frame final-position shadow. */
    protected void captureRomFinalPosition() {
        romFinalSubX = subX;
        romFinalSubY = subY;
        hasRomFinalPosition = true;
    }

    /** Copies the captured ROM final position back over an entity push. */
    protected void restoreRomFinalPosition() {
        if (!hasRomFinalPosition) {
            return;
        }
        subX = romFinalSubX;
        subY = romFinalSubY;
    }

    /** Copies only hLinkFinalPositionX back over a side-view pot push. */
    protected void restoreRomFinalPositionX() {
        if (hasRomFinalPosition) {
            subX = romFinalSubX;
        }
    }

    protected int pixelX() {
        return subX >> Link.SUB_PIXEL_SHIFT;
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
