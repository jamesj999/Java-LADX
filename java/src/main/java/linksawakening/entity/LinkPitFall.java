package linksawakening.entity;

import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.physics.OverworldCollision;
import linksawakening.physics.PhysicsFlags;
import linksawakening.state.GroundStatus;

/** Owns pit probing, slipping and fall duration for ApplyLinkGroundPhysics_part2 ($02:7644). */
final class LinkPitFall {

    private boolean fallingIntoPit;
    boolean overworldWarpHoleRoom;
    int warpHoleCharge;
    boolean warpHoleLanding;

    boolean warpHoleControlsLink() {
        return warpHoleCharge != 0 || warpHoleLanding || (overworldWarpHoleRoom && fallingIntoPit);
    }

    private GroundStatus groundStatus = GroundStatus.NORMAL;

    private int pitSlippingCounter;

    private int fallingFrameCounter;

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

    boolean fallingIntoPit() {
        return fallingIntoPit;
    }

    boolean fallingIntoPit(boolean value) {
        fallingIntoPit = value;
        return fallingIntoPit;
    }

    GroundStatus groundStatus() {
        return groundStatus;
    }

    GroundStatus groundStatus(GroundStatus value) {
        groundStatus = value;
        return groundStatus;
    }

    int fallingFrameCounter() {
        return fallingFrameCounter;
    }

    boolean linkOverPit() {
        return linkServices
                .get()
                .collision
                .linkOnPit(
                        linkMotion.get().position().pixelX(),
                        linkMotion.get().position().pixelY());
    }

    void refreshGroundStatus() {
        {
            linkServices
                    .get()
                    .collision
                    .refreshLinkGroundInteraction(
                            linkMotion.get().position().pixelX(),
                            linkMotion.get().position().pixelY());
        }
        // ApplyLinkGroundPhysics ($02:75CA-75D4) bypasses the solid
        // physics byte for OBJECT_WELL and enters the fall without slipping.
        if (!linkServices.get().collision.usesIndoorPhysicsTable()
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
        OverworldCollision.PitCell pit = linkServices
                .get()
                .collision
                .normalPitUnderLink(
                        linkMotion.get().position().pixelX(),
                        linkMotion.get().position().pixelY());
        hasPitSlipTarget = pit.present();
        if (hasPitSlipTarget) {
            pitSlipTargetTopLeftX = pit.targetTopLeftX();
            pitSlipTargetTopLeftY = pit.targetTopLeftY();
            pitSlipPhysicsFlag = pit.physicsFlag();
        }
        groundStatus = GroundStatus.PIT;
    }

    void resetSlippingOnDefaultGround(int slowWalkingSpeed, int itemUsageContext) {
        if (pitSlippingCounter == 0) return;
        var position = linkMotion.get().position();
        int physics =
                linkServices.get().collision.objectPhysicsFlagAtPoint(position.romEntityX(), position.romEntityY() - 4);
        // Only the shared default ground handler clears wPitSlippingCounter ($02:77A2).
        if (PhysicsFlags.reachesDefaultLinkGroundPhysics(physics, false, slowWalkingSpeed, itemUsageContext)) {
            clearSlippingCounter();
        }
    }

    void clearSlippingCounter() {
        pitSlippingCounter = 0;
    }

    boolean allowsInputMovement(int frame) {
        // $02:4497..44A5. Movement slows further after 24 consecutive pit updates.
        return (frame & (pitSlippingCounter < 0x18 ? 1 : 7)) == 0;
    }

    void slipTowardPitCenter(int frame) {
        if (!hasPitSlipTarget) {
            groundStatus = GroundStatus.NORMAL;
            return;
        }

        pitSlippingCounter = (pitSlippingCounter + 1) & 0xFF;
        if ((frame & 0x03) != 0) {
            return;
        }

        int x = linkMotion.get().position().pixelX();
        int y = linkMotion.get().position().pixelY();
        // $02:7662..76A2. Equality moves left on X and down on Y.
        int stepX = x < pitSlipTargetTopLeftX ? 1 : -1;
        int stepY = y <= pitSlipTargetTopLeftY ? 1 : -1;
        var position = linkMotion.get().position();
        position.subX(position.subX() + (stepX << Link.SUB_PIXEL_SHIFT));
        position.subY(position.subY() + (stepY << Link.SUB_PIXEL_SHIFT));
        if (((position.pixelX() + 2) & 0x0F) < 4 && ((position.pixelY() + 2) & 0x0F) < 4) {
            startPitFall();
        }
    }

    void startPitFall() {
        startPitFall(true);
    }

    void startPitFall(boolean slipping) {
        // $02:7644 routes $50 and $51 through the same slip/fall path.
        boolean startsFall = !fallingIntoPit;
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
        // Two landing-frame pulls can write the same single wave-SFX register.
        if (startsFall) linkServices.get().soundSink.play(GameplaySoundEvent.PIT_FALL);
    }

    void tickPitFall() {
        fallingFrameCounter = fallingFrameCounter + 1;
        if (fallingFrameCounter < LinkRomConstants.FALL_DURATION_FRAMES) {
            return;
        }

        fallingIntoPit = false;
        groundStatus = GroundStatus.NORMAL;
        hasPitSlipTarget = false;
        fallingFrameCounter = 0;
        if (overworldWarpHoleRoom) {
            warpHoleCharge = 1;
            return;
        }
        // LinkMotionFallingDownHandler ($02:512B-5136): only $50
        // takes pit damage; the well's solid byte follows the warp path.
        if (pitSlipPhysicsFlag != PhysicsFlags.NORMAL_PIT) {
            linkSignals.get().pitWarpPending = true;
            return;
        }
        {
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
