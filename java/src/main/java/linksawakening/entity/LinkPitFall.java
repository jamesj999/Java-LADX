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

    private int fallingFrameCounter;

    int fallingFrameCounter() {
        return fallingFrameCounter;
    }

    private int pitSlipTargetTopLeftX;

    private int pitSlipTargetTopLeftY;

    private int pitSlipPhysicsFlag = PhysicsFlags.NORMAL_PIT;

    private boolean hasPitSlipTarget;

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
            if (!fallingIntoPit) {
                pitSlipPhysicsFlag = PhysicsFlags.SOLID;
                startPitFall(false);
            }
        } else if (linkOverPit()) {
            enterPitGroundState();
        } else if (!fallingIntoPit) {
            groundStatus = GroundStatus.NORMAL;
            hasPitSlipTarget = false;
        }
    }

    void enterPitGroundState() {
        if (groundStatus != GroundStatus.PIT) {
            pitSlippingCounter = 0;
            OverworldCollision.PitCell pit = linkServices.get().collision == null
                    ? null
                    : linkServices
                            .get()
                            .collision
                            .normalPitUnderLink(
                                    linkMotion.get().position().pixelX(),
                                    linkMotion.get().position().pixelY());
            if (pit != null) {
                pitSlipTargetTopLeftX = pit.targetTopLeftX();
                pitSlipTargetTopLeftY = pit.targetTopLeftY();
                pitSlipPhysicsFlag = pit.physicsFlag();
                hasPitSlipTarget = true;
            }
        }
        groundStatus = GroundStatus.PIT;
    }

    void slipTowardPitCenter() {
        if (!hasPitSlipTarget) {
            groundStatus = GroundStatus.NORMAL;
            return;
        }

        pitSlippingCounter = pitSlippingCounter + 1;
        if ((pitSlippingCounter & 0x03) != 0) {
            return;
        }

        int x = linkMotion.get().position().pixelX();
        int y = linkMotion.get().position().pixelY();
        if (x < pitSlipTargetTopLeftX) {
            linkMotion.get().position().subX(linkMotion.get().position().subX() + (1 << Link.SUB_PIXEL_SHIFT));
        } else if (x > pitSlipTargetTopLeftX) {
            linkMotion.get().position().subX(linkMotion.get().position().subX() - (1 << Link.SUB_PIXEL_SHIFT));
        }
        if (y < pitSlipTargetTopLeftY) {
            linkMotion.get().position().subY(linkMotion.get().position().subY() + (1 << Link.SUB_PIXEL_SHIFT));
        } else if (y > pitSlipTargetTopLeftY) {
            linkMotion.get().position().subY(linkMotion.get().position().subY() - (1 << Link.SUB_PIXEL_SHIFT));
        }

        if (Math.abs(linkMotion.get().position().pixelX() - pitSlipTargetTopLeftX) <= 1
                && Math.abs(linkMotion.get().position().pixelY() - pitSlipTargetTopLeftY) <= 1) {
            startPitFall();
        }
    }

    void startPitFall() {
        startPitFall(true);
    }

    void startPitFall(boolean slipping) {
        // $02:7644 routes $50 and $51 through the same slip/fall path.
        groundStatus = GroundStatus.PIT;
        fallingIntoPit = true;
        elevation.airborne(false);
        elevation.zSubPixels(0);
        elevation.zVelocity(0);
        fallingFrameCounter = 0;
        linkAnimationState.get().walkTickCounter = 0;
        linkAnimationState.get().walkFrame = 0;
        linkAnimationState.get().movingThisFrame = false;
        if (slipping)
            linkMotion.get().position().subY(linkMotion.get().position().subY() + (3 << Link.SUB_PIXEL_SHIFT));
        linkServices.get().soundSink.play(GameplaySoundEvent.PIT_FALL);
    }

    void tickPitFall() {
        fallingFrameCounter = fallingFrameCounter + 1;
        if (fallingFrameCounter < LinkRomConstants.FALL_DURATION_FRAMES) {
            return;
        }

        fallingIntoPit = false;
        groundStatus = GroundStatus.NORMAL;
        hasPitSlipTarget = false;
        pitSlippingCounter = 0;
        fallingFrameCounter = 0;
        // LinkMotionFallingDownHandler ($02:512B-5136): only $50
        // takes pit damage; the well's solid byte follows the warp path.
        if (pitSlipPhysicsFlag != PhysicsFlags.NORMAL_PIT) {
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
        if (elevation.airborne() || fallingIntoPit || groundStatus == GroundStatus.PIT || linkOverPit()) {
            return;
        }
        linkMotion.get().position().lastSafeSubX(linkMotion.get().position().subX());
        linkMotion.get().position().lastSafeSubY(linkMotion.get().position().subY());
        linkMotion.get().position().hasLastSafePosition(true);
    }
}
