package linksawakening.entity;

import static linksawakening.entity.LinkAirborne.*;

import linksawakening.physics.PhysicsFlags;
import linksawakening.state.GroundStatus;

/** ROM-faithful airborne collaborator for Link. */

/** Shared state and dependency ownership for LinkAirborne. */
abstract class LinkAirborneState {

    protected final LinkComponentRef<DialogGotItemSequence> dialogGotItemSequence;
    protected final LinkComponentRef<LinkAnimationState> linkAnimationState;
    protected final LinkComponentRef<LinkAnimator> linkAnimator;
    protected final LinkComponentRef<LinkControls> linkControls;
    protected final LinkComponentRef<LinkDamage> linkDamage;
    protected final LinkComponentRef<LinkEntityEffects> linkEntityEffects;
    protected final LinkComponentRef<LinkItemState> linkItemState;
    protected final LinkComponentRef<LinkMotion> linkMotion;
    protected final LinkComponentRef<LinkServices> linkServices;
    protected final LinkComponentRef<LinkSideView> linkSideView;
    protected final LinkComponentRef<LinkSignals> linkSignals;
    protected final LinkComponentRef<LinkStateMachine> linkStateMachine;
    protected final LinkComponentRef<LinkSwimming> linkSwimming;

    static final class Dependencies {
        LinkComponentRef<DialogGotItemSequence> dialogGotItemSequence;
        LinkComponentRef<LinkAnimationState> linkAnimationState;
        LinkComponentRef<LinkAnimator> linkAnimator;
        LinkComponentRef<LinkControls> linkControls;
        LinkComponentRef<LinkDamage> linkDamage;
        LinkComponentRef<LinkEntityEffects> linkEntityEffects;
        LinkComponentRef<LinkItemState> linkItemState;
        LinkComponentRef<LinkMotion> linkMotion;
        LinkComponentRef<LinkServices> linkServices;
        LinkComponentRef<LinkSideView> linkSideView;
        LinkComponentRef<LinkSignals> linkSignals;
        LinkComponentRef<LinkStateMachine> linkStateMachine;
        LinkComponentRef<LinkSwimming> linkSwimming;
    }

    boolean airborne;
    /** wD475, set by the top-view pit warp and cleared at ground level. */
    boolean spinningPitWarpArrival;

    int zSubPixels;
    int zVelocity;
    boolean fallingIntoPit;
    GroundStatus groundStatus = GroundStatus.NORMAL;
    int pitSlippingCounter;
    int fallingFrameCounter;
    int pitSlipTargetTopLeftX;
    int pitSlipTargetTopLeftY;
    int pitSlipPhysicsFlag = PhysicsFlags.NORMAL_PIT;
    boolean hasPitSlipTarget;
    /** Mirrors LINK_MOTION_UNSTUCKING after an overworld $10 ledge collision. */
    boolean overworldLedgeFallActive;
    /** Mirrors wC10A while a directional ledge jump owns Link's XY speed. */
    boolean directionalLedgeJumpMotionLocked;

    int directionalLedgeJumpSpeedX;
    int directionalLedgeJumpSpeedY;
    /** Mirrors wC191, the collision-probe countdown for directional ledges. */
    int directionalLedgeCollisionCounter;

    LinkAirborneState(Dependencies dependencies) {
        dialogGotItemSequence = dependencies.dialogGotItemSequence;
        linkAnimationState = dependencies.linkAnimationState;
        linkAnimator = dependencies.linkAnimator;
        linkControls = dependencies.linkControls;
        linkDamage = dependencies.linkDamage;
        linkEntityEffects = dependencies.linkEntityEffects;
        linkItemState = dependencies.linkItemState;
        linkMotion = dependencies.linkMotion;
        linkServices = dependencies.linkServices;
        linkSideView = dependencies.linkSideView;
        linkSignals = dependencies.linkSignals;
        linkStateMachine = dependencies.linkStateMachine;
        linkSwimming = dependencies.linkSwimming;
    }

    static int steerAirSpeed(int speedByte, int target) {
        int speed = (byte) speedByte;
        return (speed + Integer.compare(target, speed)) & 0xFF;
    }

    protected abstract void tickJump();

    protected abstract void tickJump(boolean animate);

    protected abstract void playLandingSound();

    protected abstract void tickOverworldLedgeFall();

    protected abstract boolean swordSuppressesJumpAnimation();

    protected abstract void tickJumpAnimation();

    protected abstract boolean linkOverPit();

    protected abstract void refreshGroundStatus();

    protected abstract void enterPitGroundState();

    protected abstract void slipTowardPitCenter();

    protected abstract void startPitFall();

    protected abstract void startPitFall(boolean slipping);

    protected abstract void tickPitFall();

    protected abstract void updateLastSafePositionIfPossible();

    protected abstract void accessSetPitWarpArrival(int z);

    protected abstract int accessRomMotionState();

    protected abstract boolean accessIsAirborne();

    protected abstract int accessZVelocity();

    protected abstract boolean accessIsFallingIntoPit();

    protected abstract void accessUseRocsFeather();

    protected abstract void accessBounceFromGoomba();

    protected abstract int accessZPixels();

    protected abstract int accessTickDialogGotItem(boolean dialogActive);
}
