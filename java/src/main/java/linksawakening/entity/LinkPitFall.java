package linksawakening.entity;

import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.physics.OverworldCollision;
import linksawakening.physics.PhysicsFlags;
import linksawakening.state.GroundStatus;

/** Owns pit probing, slipping and fall duration for ApplyLinkGroundPhysics_part2 ($02:7644). */
final class LinkPitFall {
    private boolean fallingIntoPit;

    boolean fallingIntoPit() {
        return fallingIntoPit;
    }

    boolean fallingIntoPit(boolean value) {
        return fallingIntoPit = value;
    }

    private GroundStatus groundStatus = GroundStatus.NORMAL;

    GroundStatus groundStatus() {
        return groundStatus;
    }

    GroundStatus groundStatus(GroundStatus value) {
        return groundStatus = value;
    }

    private int pitSlippingCounter;

    int pitSlippingCounter() {
        return pitSlippingCounter;
    }

    int pitSlippingCounter(int value) {
        return pitSlippingCounter = value;
    }

    private int fallingFrameCounter;

    int fallingFrameCounter() {
        return fallingFrameCounter;
    }

    int fallingFrameCounter(int value) {
        return fallingFrameCounter = value;
    }

    private int pitSlipTargetTopLeftX;

    int pitSlipTargetTopLeftX() {
        return pitSlipTargetTopLeftX;
    }

    int pitSlipTargetTopLeftX(int value) {
        return pitSlipTargetTopLeftX = value;
    }

    private int pitSlipTargetTopLeftY;

    int pitSlipTargetTopLeftY() {
        return pitSlipTargetTopLeftY;
    }

    int pitSlipTargetTopLeftY(int value) {
        return pitSlipTargetTopLeftY = value;
    }

    private int pitSlipPhysicsFlag = PhysicsFlags.NORMAL_PIT;

    int pitSlipPhysicsFlag() {
        return pitSlipPhysicsFlag;
    }

    int pitSlipPhysicsFlag(int value) {
        return pitSlipPhysicsFlag = value;
    }

    private boolean hasPitSlipTarget;

    boolean hasPitSlipTarget() {
        return hasPitSlipTarget;
    }

    boolean hasPitSlipTarget(boolean value) {
        return hasPitSlipTarget = value;
    }

    private final LinkComponentRef<LinkAnimationState> linkAnimationState;
    private final LinkComponentRef<LinkDamage> linkDamage;
    private final LinkComponentRef<LinkMotion> linkMotion;
    private final LinkComponentRef<LinkServices> linkServices;
    private final LinkComponentRef<LinkSignals> linkSignals;
    private final LinkElevation elevation;

    LinkPitFall(
            LinkComponentRef<LinkAnimationState> linkAnimationState,
            LinkComponentRef<LinkDamage> linkDamage,
            LinkComponentRef<LinkMotion> linkMotion,
            LinkComponentRef<LinkServices> linkServices,
            LinkComponentRef<LinkSignals> linkSignals,
            LinkElevation elevation) {
        this.linkAnimationState = linkAnimationState;
        this.linkDamage = linkDamage;
        this.linkMotion = linkMotion;
        this.linkServices = linkServices;
        this.linkSignals = linkSignals;
        this.elevation = elevation;
    }

    boolean linkOverPit() {
        return linkServices.get().collision != null
                && linkServices
                        .get()
                        .collision
                        .linkOnPit(
                                linkMotion.get().position().pixelX(),
                                linkMotion.get().position().pixelY());
    }

    void refreshGroundStatus() {
        if (linkServices.get().collision != null) {
            linkServices
                    .get()
                    .collision
                    .refreshLinkGroundInteraction(
                            linkMotion.get().position().pixelX(),
                            linkMotion.get().position().pixelY());
        }
        // ApplyLinkGroundPhysics ($02:75CA-75D4) bypasses the solid
        // physics byte for OBJECT_WELL and enters the fall without slipping.
        if (linkServices.get().collision != null
                && !linkServices.get().collision.usesIndoorPhysicsTable()
                && linkServices
                                .get()
                                .collision
                                .objectUnderLinkFeet(
                                        linkMotion.get().position().pixelX(),
                                        linkMotion.get().position().pixelY())
                        == 0x61) {
            if (!this.fallingIntoPit()) {
                this.pitSlipPhysicsFlag(PhysicsFlags.SOLID);
                startPitFall(false);
            }
        } else if (linkOverPit()) {
            enterPitGroundState();
        } else if (!this.fallingIntoPit()) {
            this.groundStatus(GroundStatus.NORMAL);
            this.hasPitSlipTarget(false);
        }
    }

    void enterPitGroundState() {
        if (this.groundStatus() != GroundStatus.PIT) {
            this.pitSlippingCounter(0);
            OverworldCollision.PitCell pit = linkServices.get().collision == null
                    ? null
                    : linkServices
                            .get()
                            .collision
                            .normalPitUnderLink(
                                    linkMotion.get().position().pixelX(),
                                    linkMotion.get().position().pixelY());
            if (pit != null) {
                this.pitSlipTargetTopLeftX(pit.targetTopLeftX());
                this.pitSlipTargetTopLeftY(pit.targetTopLeftY());
                this.pitSlipPhysicsFlag(pit.physicsFlag());
                this.hasPitSlipTarget(true);
            }
        }
        this.groundStatus(GroundStatus.PIT);
    }

    void slipTowardPitCenter() {
        if (!this.hasPitSlipTarget()) {
            this.groundStatus(GroundStatus.NORMAL);
            return;
        }

        this.pitSlippingCounter(this.pitSlippingCounter() + 1);
        if ((this.pitSlippingCounter() & 0x03) != 0) {
            return;
        }

        int x = linkMotion.get().position().pixelX();
        int y = linkMotion.get().position().pixelY();
        if (x < this.pitSlipTargetTopLeftX()) {
            linkMotion.get().position().subX(linkMotion.get().position().subX() + (1 << Link.SUB_PIXEL_SHIFT));
        } else if (x > this.pitSlipTargetTopLeftX()) {
            linkMotion.get().position().subX(linkMotion.get().position().subX() - (1 << Link.SUB_PIXEL_SHIFT));
        }
        if (y < this.pitSlipTargetTopLeftY()) {
            linkMotion.get().position().subY(linkMotion.get().position().subY() + (1 << Link.SUB_PIXEL_SHIFT));
        } else if (y > this.pitSlipTargetTopLeftY()) {
            linkMotion.get().position().subY(linkMotion.get().position().subY() - (1 << Link.SUB_PIXEL_SHIFT));
        }

        if (Math.abs(linkMotion.get().position().pixelX() - this.pitSlipTargetTopLeftX()) <= 1
                && Math.abs(linkMotion.get().position().pixelY() - this.pitSlipTargetTopLeftY()) <= 1) {
            startPitFall();
        }
    }

    void startPitFall() {
        startPitFall(true);
    }

    void startPitFall(boolean slipping) {
        // $02:7644 routes $50 and $51 through the same slip/fall path.
        this.groundStatus(GroundStatus.PIT);
        this.fallingIntoPit(true);
        elevation.airborne(false);
        elevation.zSubPixels(0);
        elevation.zVelocity(0);
        this.fallingFrameCounter(0);
        linkAnimationState.get().walkTickCounter = 0;
        linkAnimationState.get().walkFrame = 0;
        linkAnimationState.get().movingThisFrame = false;
        if (slipping)
            linkMotion.get().position().subY(linkMotion.get().position().subY() + (3 << Link.SUB_PIXEL_SHIFT));
        linkServices.get().soundSink.play(GameplaySoundEvent.PIT_FALL);
    }

    void tickPitFall() {
        this.fallingFrameCounter(this.fallingFrameCounter() + 1);
        if (this.fallingFrameCounter() < LinkRomConstants.FALL_DURATION_FRAMES) {
            return;
        }

        this.fallingIntoPit(false);
        this.groundStatus(GroundStatus.NORMAL);
        this.hasPitSlipTarget(false);
        this.pitSlippingCounter(0);
        this.fallingFrameCounter(0);
        // LinkMotionFallingDownHandler ($02:512B-5136): only $50
        // takes pit damage; the well's solid byte follows the warp path.
        if (this.pitSlipPhysicsFlag() != PhysicsFlags.NORMAL_PIT) {
            linkSignals.get().pitWarpPending = true;
            return;
        }
        if (linkServices.get().playerState != null) {
            linkDamage.get().applyPitDamage();
        }
        if (linkMotion.get().position().hasRoomEntryPosition()) {
            linkMotion.get().position().subX(linkMotion.get().position().roomEntrySubX());
            linkMotion.get().position().subY(linkMotion.get().position().roomEntrySubY());
        } else if (linkMotion.get().position().hasLastSafePosition()) {
            linkMotion.get().position().subX(linkMotion.get().position().lastSafeSubX());
            linkMotion.get().position().subY(linkMotion.get().position().lastSafeSubY());
        }
    }

    void updateLastSafePositionIfPossible() {
        if (elevation.airborne() || this.fallingIntoPit() || this.groundStatus() == GroundStatus.PIT || linkOverPit()) {
            return;
        }
        linkMotion.get().position().lastSafeSubX(linkMotion.get().position().subX());
        linkMotion.get().position().lastSafeSubY(linkMotion.get().position().subY());
        linkMotion.get().position().hasLastSafePosition(true);
    }
}
