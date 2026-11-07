package linksawakening.entity;

import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.state.PlayerState;
import static linksawakening.entity.Link.*;
import static linksawakening.entity.LinkRomConstants.*;

final class LinkSideView {
    private final Link link;
    boolean sideScrolling;
    int sideViewModifier;
    int sideViewSpeedX;
    int sideViewSpeedY;
    boolean sideViewGrounded;
    boolean sideViewFeatherPending;
    /** Mirrors wC147: Link was standing on a side-view moving entity this frame. */
    boolean standingOnSideViewEntity;
    /** One-frame hLinkSpeedY write supplied by the side-view platform handler. */
    int sideViewPlatformVerticalSpeedPending = -1;

    LinkSideView(Link link) { this.link = link; }

    /** Warp entry sets hLinkPhysicsModifier=1 at $00:18F0. */
    void setSideScrolling(boolean enabled) {
        if (sideScrolling == enabled) return;
        sideScrolling = enabled;
        sideViewModifier = enabled ? 1 : 0;
        sideViewSpeedX = sideViewSpeedY = 0;
        sideViewGrounded = false;
        sideViewFeatherPending = false;
        link.topDownFeatherStarted = false;
        link.linkAirborne.airborne = false;
        link.linkAirborne.zSubPixels = link.linkAirborne.zVelocity = 0;
    }

    /** $02:68B7–6C74: side-view speeds and single-point collision probes. */
    void updateSideViewMotion(int platformSpeedY) {
        int mask = link.buildJoypadMask();
        if (!link.linkAirborne.airborne && link.linkEquipment.itemsBlockMotion()) return;
        boolean recoiling = link.linkDamage.collisionIgnoreFramesRemaining > 0;
        boolean ladderFrame = sideViewModifier == 1;
        if (link.linkMotion.forcedSpeedPending) {
            sideViewSpeedX = link.linkMotion.forcedSpeedX;
            if (!link.linkMotion.forcedSpeedXOnly) sideViewSpeedY = link.linkMotion.forcedSpeedY;
            link.linkMotion.forcedSpeedPending = link.linkMotion.forcedSpeedXOnly = false;
        }
        if (recoiling) {
            link.linkDamage.collisionIgnoreFramesRemaining--;
        } else if (sideViewModifier == 1) {
            link.linkAirborne.airborne = false;
            sideViewSpeedX = (mask & 3) == 1 ? 8 : (mask & 3) == 2 ? -8 : 0;
            sideViewSpeedY = (mask & 12) == 4 ? -16 : (mask & 12) == 8 ? 16 : 0;
            link.direction = Link.DIRECTION_UP;
        } else {
            int targetX = (mask & 3) == 1 ? 16 : (mask & 3) == 2 ? -16 : 0;
            if (sideViewGrounded || standingOnSideViewEntity) {
                if (link.linkAirborne.airborne) {
                    link.soundSink.play(GameplaySoundEvent.LINK_FOOTSTEP);
                    if (link.playerState != null) link.playerState.setRunningWithPegasusBoots(false);
                }
                link.linkAirborne.airborne = false;
                sideViewSpeedX = targetX;
            } else {
                link.linkAirborne.airborne = true;
                if ((mask & 3) != 0) sideViewSpeedX += Integer.compare(targetX, sideViewSpeedX);
            }
            if ((mask & 3) != 0 && !link.linkEquipment.itemsLockFacing())
                link.direction = (mask & 1) != 0 ? Link.DIRECTION_RIGHT : Link.DIRECTION_LEFT;
            if ((mask & 15) == 4) link.direction = Link.DIRECTION_UP;
            if ((mask & 15) == 8) link.direction = Link.DIRECTION_DOWN;
        }
        if (platformSpeedY >= 0) sideViewSpeedY = (byte) platformSpeedY;
        int oldX = link.linkMotion.subX, oldY = link.linkMotion.subY;
        moveSideViewAndCollide(sideViewSpeedX, sideViewSpeedY, mask);
        int centerPhysics = link.collision == null ? 0
            : link.collision.objectPhysicsFlagAtPoint(link.linkMotion.romEntityX(), link.linkMotion.romEntityY() - 5);
        if (!recoiling) {
            if (sideViewModifier == 1 && centerPhysics != 0xB1) sideViewModifier = 0;
            else if (sideViewModifier == 0 && centerPhysics == 0xB1 && (mask & 12) != 0)
                sideViewModifier = 1;
        }
        if (!ladderFrame || recoiling) {
            if (recoiling) sideViewSpeedY = (byte) (sideViewSpeedY + 3);
            else if (sideViewSpeedY < 0x40) {
                sideViewSpeedY++;
                boolean featherHeld = link.playerState != null && link.inputState != null
                    && (link.playerState.itemB() == PlayerState.INVENTORY_ROCS_FEATHER
                        ? link.inputState.isDown(link.inputConfig.bKey())
                        : link.inputState.isDown(link.inputConfig.aKey()));
                if (sideViewSpeedY >= 0 || !featherHeld) sideViewSpeedY++;
            }
        }
        if (sideViewFeatherPending) {
            sideViewFeatherPending = false;
            link.linkAirborne.airborne = true;
            sideViewSpeedY = (mask & 3) == 0 ? -22 : -24;
            sideViewGrounded = false;
            moveSideViewAndCollide(sideViewSpeedX, sideViewSpeedY, mask);
            link.linkAnimator.jumpAnimationCounter = link.linkAnimator.jumpAnimationFrame = 0;
            link.soundSink.play(GameplaySoundEvent.ROC_FEATHER_JUMP);
        }
        link.linkAirborne.zSubPixels = link.linkAirborne.zVelocity = 0;
        link.linkMotion.lastRomSpeedX = sideViewSpeedX & 255;
        link.linkMotion.lastRomSpeedY = sideViewSpeedY & 255;
        link.movingThisFrame = link.linkMotion.subX != oldX || link.linkMotion.subY != oldY;
        if (link.movingThisFrame) link.tickAnimation();
        else link.walkTickCounter = link.walkFrame = 0;
        // Side-view falling uses the normal link.linkAirborne.airborne pose, not a Z-axis flip.
        link.linkAnimator.jumpAnimationFrame = 3;
    }

    void moveSideViewAndCollide(int speedX, int speedY, int mask) {
        int oldX = link.linkMotion.subX, oldY = link.linkMotion.subY;
        link.linkMotion.subX += speedX;
        link.linkMotion.subY += speedY;
        sideViewGrounded = standingOnSideViewEntity;
        if (link.collision == null || link.linkMotion.romEntityX() < 4 || link.linkMotion.romEntityX() >= 156
            || link.linkMotion.romEntityY() < 12 || link.linkMotion.romEntityY() >= 128) return;
        int x = link.linkMotion.romEntityX(), y = link.linkMotion.romEntityY();
        if (speedX != 0 && (sideViewSolidAt(x + (speedX > 0 ? 3 : -3), y - 8)
            || link.collision.objectIdAtPoint(x + (speedX > 0 ? 3 : -3), y - 8) == 0xFF)) {
            link.linkMotion.subX = oldX;
            link.linkMotion.romCollisionType |= speedX > 0 ? 1 : 2;
        }
        int sampleY = y + (speedY < 0 ? -12 : 0);
        int object = link.collision.objectIdAtPoint(x, sampleY);
        boolean ladderTop = speedY >= 0 && (mask & 8) == 0 && (sampleY & 15) < 3
            && (object == 0x62 || object == 0x64 || object == 0x66);
        if (sideViewSolidAt(x, sampleY) || ladderTop) {
            if (speedY >= 0) {
                link.linkMotion.subY = (((y & ~15) - 16) << Link.SUB_PIXEL_SHIFT) | (link.linkMotion.subY & 15);
                sideViewSpeedY = 0;
                sideViewGrounded = true;
                link.linkMotion.romCollisionType |= 8;
            } else {
                link.linkMotion.subY = oldY;
                link.linkMotion.romCollisionType |= 4;
            }
        }
    }

    boolean sideViewSolidAt(int x, int y) {
        int physics = link.collision.objectPhysicsFlagAtPoint(x, y);
        return physics == 0x60 || physics == 0x01;
    }

    /** Applies the side-view platform handler's Link position and speed writes. */
    /** Applies a standing platform contact (the common side-view case). */
    void applySideViewPlatformContact(int horizontalDelta, int romPositionY,
                                              int romSpeedY, boolean standing) {
        if (horizontalDelta < -0x80 || horizontalDelta > 0x7F) {
            throw new IllegalArgumentException("Side-view platform X delta must be a signed byte");
        }
        link.validateRomByte(romPositionY, "Side-view platform Link Y");
        link.validateRomByte(romSpeedY, "Side-view platform Link Y speed");

        int currentRomX = link.linkMotion.romEntityX() & 0xFF;
        int nextRomX = (currentRomX + horizontalDelta) & 0xFF;
        int nextTopLeftX = (nextRomX - 0x08) & 0xFF;
        int nextTopLeftY = (romPositionY - 0x10) & 0xFF;
        link.linkMotion.subX = (nextTopLeftX << Link.SUB_PIXEL_SHIFT) | (link.linkMotion.subX & 0x0F);
        link.linkMotion.subY = (nextTopLeftY << Link.SUB_PIXEL_SHIFT) | (link.linkMotion.subY & 0x0F);
        standingOnSideViewEntity = standing;
        // The platform handler writes hLinkSpeedY as an HRAM observation for
        // this frame and for one subsequent vertical movement attempt. It
        // never queues horizontal movement.
        link.linkMotion.lastRomSpeedY = romSpeedY & 0xFF;
        sideViewPlatformVerticalSpeedPending = standing ? romSpeedY & 0xFF : -1;
    }

    void applySideViewPlatformContact(int horizontalDelta, int romPositionY,
                                              int romSpeedY) {
        applySideViewPlatformContact(horizontalDelta, romPositionY, romSpeedY, true);
    }

    /** Clears all transient side-view platform state during a room transition. */
    void clearSideViewPlatformState() {
        standingOnSideViewEntity = false;
        sideViewPlatformVerticalSpeedPending = -1;
    }

    /** Clears only the per-frame side-view standing flag before entity processing. */
    void clearStandingOnSideViewEntity() {
        standingOnSideViewEntity = false;
    }

    /** Applies state-0 pot's top snap and hLinkSpeedY/wC147 writes. */
    void applySideViewPotContact(int romPositionY, int romSpeedY) {
        link.validateRomByte(romPositionY, "Side-view pot Link Y");
        link.validateRomByte(romSpeedY, "Side-view pot Link Y speed");
        int nextTopLeftY = (romPositionY - 0x10) & 0xFF;
        link.linkMotion.subY = (nextTopLeftY << Link.SUB_PIXEL_SHIFT) | (link.linkMotion.subY & 0x0F);
        standingOnSideViewEntity = true;
        link.linkMotion.lastRomSpeedY = romSpeedY;
        sideViewPlatformVerticalSpeedPending = romSpeedY;
    }
}
