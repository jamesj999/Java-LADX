package linksawakening.entity;

import static linksawakening.entity.Link.*;
import static linksawakening.entity.LinkRomConstants.*;

import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.state.Direction;
import linksawakening.state.InventoryItem;

final class LinkSideView {
    private final LinkComponentRef<LinkAirborne> linkAirborne;
    private final LinkComponentRef<LinkAnimationState> linkAnimationState;
    private final LinkComponentRef<LinkAnimator> linkAnimator;
    private final LinkComponentRef<LinkControls> linkControls;
    private final LinkComponentRef<LinkDamage> linkDamage;
    private final LinkComponentRef<LinkEquipment> linkEquipment;
    private final LinkComponentRef<LinkMotion> linkMotion;
    private final LinkComponentRef<LinkServices> linkServices;
    private final LinkComponentRef<LinkStateMachine> linkStateMachine;

    LinkSideView(Dependencies dependencies) {
        linkAirborne = dependencies.linkAirborne;
        linkAnimationState = dependencies.linkAnimationState;
        linkAnimator = dependencies.linkAnimator;
        linkControls = dependencies.linkControls;
        linkDamage = dependencies.linkDamage;
        linkEquipment = dependencies.linkEquipment;
        linkMotion = dependencies.linkMotion;
        linkServices = dependencies.linkServices;
        linkStateMachine = dependencies.linkStateMachine;
    }

    static final class Dependencies {
        LinkComponentRef<LinkAirborne> linkAirborne;
        LinkComponentRef<LinkAnimationState> linkAnimationState;
        LinkComponentRef<LinkAnimator> linkAnimator;
        LinkComponentRef<LinkControls> linkControls;
        LinkComponentRef<LinkDamage> linkDamage;
        LinkComponentRef<LinkEquipment> linkEquipment;
        LinkComponentRef<LinkMotion> linkMotion;
        LinkComponentRef<LinkServices> linkServices;
        LinkComponentRef<LinkStateMachine> linkStateMachine;
    }

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

    /** Warp entry sets hLinkPhysicsModifier=1 at $00:18F0. */
    void setSideScrolling(boolean enabled) {
        if (sideScrolling == enabled) return;
        sideScrolling = enabled;
        sideViewModifier = enabled ? 1 : 0;
        sideViewSpeedX = sideViewSpeedY = 0;
        sideViewGrounded = false;
        sideViewFeatherPending = false;
        linkAnimationState.get().topDownFeatherStarted = false;
        linkAirborne.get().airborne = false;
        linkAirborne.get().zSubPixels = linkAirborne.get().zVelocity = 0;
    }

    /** $02:68B7–6C74: side-view speeds and single-point collision probes. */
    void updateSideViewMotion(int platformSpeedY) {
        int mask = linkControls.get().accessBuildJoypadMask();
        if (!linkAirborne.get().airborne && linkEquipment.get().itemsBlockMotion()) return;
        boolean recoiling = linkDamage.get().collisionIgnoreFramesRemaining > 0;
        boolean ladderFrame = sideViewModifier == 1;
        if (linkMotion.get().forcedSpeedPending) {
            sideViewSpeedX = linkMotion.get().forcedSpeedX;
            if (!linkMotion.get().forcedSpeedXOnly) sideViewSpeedY = linkMotion.get().forcedSpeedY;
            linkMotion.get().forcedSpeedPending = linkMotion.get().forcedSpeedXOnly = false;
        }
        if (recoiling) {
            linkDamage.get().collisionIgnoreFramesRemaining--;
        } else if (sideViewModifier == 1) {
            linkAirborne.get().airborne = false;
            sideViewSpeedX = (mask & 3) == 1 ? 8 : (mask & 3) == 2 ? -8 : 0;
            sideViewSpeedY = (mask & 12) == 4 ? -16 : (mask & 12) == 8 ? 16 : 0;
            linkAnimationState.get().direction = Direction.UP;
        } else {
            int targetX = (mask & 3) == 1 ? 16 : (mask & 3) == 2 ? -16 : 0;
            if (sideViewGrounded || standingOnSideViewEntity) {
                if (linkAirborne.get().airborne) {
                    linkServices.get().soundSink.play(GameplaySoundEvent.LINK_FOOTSTEP);
                    if (linkServices.get().playerState != null)
                        linkServices.get().playerState.equipmentLevels().setRunningWithPegasusBoots(false);
                }
                linkAirborne.get().airborne = false;
                sideViewSpeedX = targetX;
            } else {
                linkAirborne.get().airborne = true;
                if ((mask & 3) != 0) sideViewSpeedX += Integer.compare(targetX, sideViewSpeedX);
            }
            if ((mask & 3) != 0 && !linkEquipment.get().itemsLockFacing())
                linkAnimationState.get().direction = ((mask & 1) != 0 ? Direction.RIGHT : Direction.LEFT);
            if ((mask & 15) == 4) linkAnimationState.get().direction = Direction.UP;
            if ((mask & 15) == 8) linkAnimationState.get().direction = Direction.DOWN;
        }
        finishSideViewMotion(platformSpeedY, mask, recoiling, ladderFrame);
    }

    private void finishSideViewMotion(int platformSpeedY, int mask, boolean recoiling, boolean ladderFrame) {
        if (platformSpeedY >= 0) sideViewSpeedY = (byte) platformSpeedY;
        int oldX = linkMotion.get().subX, oldY = linkMotion.get().subY;
        moveSideViewAndCollide(sideViewSpeedX, sideViewSpeedY, mask);
        int centerPhysics = linkServices.get().collision == null
                ? 0
                : linkServices
                        .get()
                        .collision
                        .objectPhysicsFlagAtPoint(
                                linkMotion.get().romEntityX(), linkMotion.get().romEntityY() - 5);
        if (!recoiling) {
            if (sideViewModifier == 1 && centerPhysics != 0xB1) sideViewModifier = 0;
            else if (sideViewModifier == 0 && centerPhysics == 0xB1 && (mask & 12) != 0) sideViewModifier = 1;
        }
        if (!ladderFrame || recoiling) {
            if (recoiling) sideViewSpeedY = (byte) (sideViewSpeedY + 3);
            else if (sideViewSpeedY < 0x40) {
                sideViewSpeedY++;
                boolean featherHeld = linkServices.get().playerState != null
                        && linkServices.get().inputState != null
                        && (linkServices.get().playerState.inventory().itemB() == InventoryItem.ROCS_FEATHER.romValue()
                                ? linkServices
                                        .get()
                                        .inputState
                                        .isDown(linkServices.get().inputConfig.bKey())
                                : linkServices
                                        .get()
                                        .inputState
                                        .isDown(linkServices.get().inputConfig.aKey()));
                if (sideViewSpeedY >= 0 || !featherHeld) sideViewSpeedY++;
            }
        }
        if (sideViewFeatherPending) {
            sideViewFeatherPending = false;
            linkAirborne.get().airborne = true;
            sideViewSpeedY = (mask & 3) == 0 ? -22 : -24;
            sideViewGrounded = false;
            moveSideViewAndCollide(sideViewSpeedX, sideViewSpeedY, mask);
            linkAnimator.get().jumpAnimationCounter = linkAnimator.get().jumpAnimationFrame = 0;
            linkServices.get().soundSink.play(GameplaySoundEvent.ROC_FEATHER_JUMP);
        }
        linkAirborne.get().zSubPixels = linkAirborne.get().zVelocity = 0;
        linkMotion.get().lastRomSpeedX = sideViewSpeedX & 255;
        linkMotion.get().lastRomSpeedY = sideViewSpeedY & 255;
        linkAnimationState.get().movingThisFrame = linkMotion.get().subX != oldX || linkMotion.get().subY != oldY;
        if (linkAnimationState.get().movingThisFrame) linkAnimationState.get().accessTickAnimation();
        else linkAnimationState.get().walkTickCounter = linkAnimationState.get().walkFrame = 0;
        // Side-view falling uses the normal airborne pose, not a Z-axis flip.
        linkAnimator.get().jumpAnimationFrame = 3;
    }

    void moveSideViewAndCollide(int speedX, int speedY, int mask) {
        int oldX = linkMotion.get().subX, oldY = linkMotion.get().subY;
        linkMotion.get().subX += speedX;
        linkMotion.get().subY += speedY;
        sideViewGrounded = standingOnSideViewEntity;
        if (linkServices.get().collision == null
                || linkMotion.get().romEntityX() < 4
                || linkMotion.get().romEntityX() >= 156
                || linkMotion.get().romEntityY() < 12
                || linkMotion.get().romEntityY() >= 128) return;
        int x = linkMotion.get().romEntityX(), y = linkMotion.get().romEntityY();
        if (speedX != 0
                && (sideViewSolidAt(x + (speedX > 0 ? 3 : -3), y - 8)
                        || linkServices.get().collision.objectIdAtPoint(x + (speedX > 0 ? 3 : -3), y - 8) == 0xFF)) {
            linkMotion.get().subX = oldX;
            linkMotion.get().romCollisionType |= speedX > 0 ? 1 : 2;
        }
        int sampleY = y + (speedY < 0 ? -12 : 0);
        int object = linkServices.get().collision.objectIdAtPoint(x, sampleY);
        boolean ladderTop = speedY >= 0
                && (mask & 8) == 0
                && (sampleY & 15) < 3
                && (object == 0x62 || object == 0x64 || object == 0x66);
        if (sideViewSolidAt(x, sampleY) || ladderTop) {
            if (speedY >= 0) {
                linkMotion.get().subY = (((y & ~15) - 16) << Link.SUB_PIXEL_SHIFT) | (linkMotion.get().subY & 15);
                sideViewSpeedY = 0;
                sideViewGrounded = true;
                linkMotion.get().romCollisionType |= 8;
            } else {
                linkMotion.get().subY = oldY;
                linkMotion.get().romCollisionType |= 4;
            }
        }
    }

    boolean sideViewSolidAt(int x, int y) {
        int physics = linkServices.get().collision.objectPhysicsFlagAtPoint(x, y);
        return physics == 0x60 || physics == 0x01;
    }

    /** Applies a standing platform contact (the common side-view case). */
    void applySideViewPlatformContact(int horizontalDelta, int romPositionY, int romSpeedY, boolean standing) {
        if (horizontalDelta < -0x80 || horizontalDelta > 0x7F) {
            throw new IllegalArgumentException("Side-view platform X delta must be a signed byte");
        }
        Link.validateRomByte(romPositionY, "Side-view platform Link Y");
        Link.validateRomByte(romSpeedY, "Side-view platform Link Y speed");

        int currentRomX = linkMotion.get().romEntityX() & 0xFF;
        int nextRomX = (currentRomX + horizontalDelta) & 0xFF;
        int nextTopLeftX = (nextRomX - 0x08) & 0xFF;
        int nextTopLeftY = (romPositionY - 0x10) & 0xFF;
        linkMotion.get().subX = (nextTopLeftX << Link.SUB_PIXEL_SHIFT) | (linkMotion.get().subX & 0x0F);
        linkMotion.get().subY = (nextTopLeftY << Link.SUB_PIXEL_SHIFT) | (linkMotion.get().subY & 0x0F);
        standingOnSideViewEntity = standing;
        // The platform handler writes hLinkSpeedY as an HRAM observation for
        // this frame and for one subsequent vertical movement attempt. It
        // never queues horizontal movement.
        linkMotion.get().lastRomSpeedY = romSpeedY & 0xFF;
        sideViewPlatformVerticalSpeedPending = standing ? romSpeedY & 0xFF : -1;
    }

    void applySideViewPlatformContact(int horizontalDelta, int romPositionY, int romSpeedY) {
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
        Link.validateRomByte(romPositionY, "Side-view pot Link Y");
        Link.validateRomByte(romSpeedY, "Side-view pot Link Y speed");
        int nextTopLeftY = (romPositionY - 0x10) & 0xFF;
        linkMotion.get().subY = (nextTopLeftY << Link.SUB_PIXEL_SHIFT) | (linkMotion.get().subY & 0x0F);
        standingOnSideViewEntity = true;
        linkMotion.get().lastRomSpeedY = romSpeedY;
        sideViewPlatformVerticalSpeedPending = romSpeedY;
    }

    void accessSetSideScrolling(boolean enabled) {
        this.setSideScrolling(enabled);
    }

    boolean accessStandingOnSideViewEntity() {
        return this.standingOnSideViewEntity;
    }

    boolean accessRomItemAnimationFrameBlocked() {
        return !this.sideScrolling && linkStateMachine.get().accessRomInteractiveMotionBlockValue() == 2;
    }

    void accessApplySideViewPlatformContact(int horizontalDelta, int romPositionY, int romSpeedY, boolean standing) {
        this.applySideViewPlatformContact(horizontalDelta, romPositionY, romSpeedY, standing);
    }

    void accessApplySideViewPlatformContact(int horizontalDelta, int romPositionY, int romSpeedY) {
        this.applySideViewPlatformContact(horizontalDelta, romPositionY, romSpeedY);
    }

    void accessClearSideViewPlatformState() {
        this.clearSideViewPlatformState();
    }

    void accessClearStandingOnSideViewEntity() {
        this.clearStandingOnSideViewEntity();
    }

    void accessApplySideViewPotContact(int romPositionY, int romSpeedY) {
        this.applySideViewPotContact(romPositionY, romSpeedY);
    }
}
