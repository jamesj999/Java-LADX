package linksawakening.entity;

import static linksawakening.entity.Link.SUB_PIXEL_SHIFT;
import static linksawakening.entity.LinkAirborne.*;

import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.physics.PhysicsFlags;
import linksawakening.state.GroundStatus;
import linksawakening.state.InventoryItem;
import linksawakening.state.LinkMotionState;

/** ROM-faithful airborne collaborator for Link. */

/** LinkAirborneAccess behavior on the shared LinkAirborne state. */
abstract class LinkAirborneAccess extends LinkJumpMotion {
    protected LinkAirborneAccess(Dependencies dependencies) {
        super(dependencies);
    }

    protected void startPitFall() {
        startPitFall(true);
    }

    protected void startPitFall(boolean slipping) {
        // $02:7644 routes $50 and $51 through the same slip/fall path.
        groundStatus = GroundStatus.PIT;
        fallingIntoPit = true;
        airborne = false;
        zSubPixels = 0;
        zVelocity = 0;
        fallingFrameCounter = 0;
        linkAnimationState.get().walkTickCounter = 0;
        linkAnimationState.get().walkFrame = 0;
        linkAnimationState.get().movingThisFrame = false;
        if (slipping) linkMotion.get().subY += 3 << Link.SUB_PIXEL_SHIFT;
        linkServices.get().soundSink.play(GameplaySoundEvent.PIT_FALL);
    }

    protected void tickPitFall() {
        fallingFrameCounter++;
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
        if (linkMotion.get().hasRoomEntryPosition) {
            linkMotion.get().subX = linkMotion.get().roomEntrySubX;
            linkMotion.get().subY = linkMotion.get().roomEntrySubY;
        } else if (linkMotion.get().hasLastSafePosition) {
            linkMotion.get().subX = linkMotion.get().lastSafeSubX;
            linkMotion.get().subY = linkMotion.get().lastSafeSubY;
        }
    }

    protected void updateLastSafePositionIfPossible() {
        if (airborne || fallingIntoPit || groundStatus == GroundStatus.PIT || linkOverPit()) {
            return;
        }
        linkMotion.get().lastSafeSubX = linkMotion.get().subX;
        linkMotion.get().lastSafeSubY = linkMotion.get().subY;
        linkMotion.get().hasLastSafePosition = true;
    }

    protected void accessSetPitWarpArrival(int z) {
        linkStateMachine.get().motionState = LinkMotionState.DEFAULT;
        this.zSubPixels = z << SUB_PIXEL_SHIFT;
        this.zVelocity = 0;
        this.airborne = z != 0;
        this.spinningPitWarpArrival = z != 0;
    }

    protected int accessRomMotionState() {
        if (this.groundStatus == GroundStatus.PIT || this.fallingIntoPit) {
            return 0x06;
        }
        return linkStateMachine.get().motionState.romValue();
    }

    protected boolean accessIsAirborne() {
        return this.airborne;
    }

    protected int accessZVelocity() {
        return this.zVelocity & 0xFF;
    }

    protected boolean accessIsFallingIntoPit() {
        return this.fallingIntoPit;
    }

    protected void accessUseRocsFeather() {
        if (this.airborne
                || linkSideView.get().sideViewFeatherPending
                || this.groundStatus == GroundStatus.PIT
                || this.fallingIntoPit
                || linkStateMachine.get().accessIsSwimming()) {
            return;
        }
        if (linkSideView.get().sideScrolling) {
            // The ROM runs $02:68B7 before UseRocsFeather ($00:14E6).
            // Defer its extra XY step until normal side-view motion finishes.
            linkSideView.get().sideViewFeatherPending = true;
            return;
        }
        linkAnimationState.get().topDownFeatherStarted = true;
        this.airborne = true;
        this.zSubPixels = 0;
        this.zVelocity = 0x20;
        linkAnimator.get().jumpAnimationCounter = 0;
        linkAnimator.get().jumpAnimationFrame = 0;
        linkServices.get().soundSink.play(GameplaySoundEvent.ROC_FEATHER_JUMP);
    }

    protected void accessBounceFromGoomba() {
        this.airborne = true;
        this.zVelocity = 0x10;
        this.fallingIntoPit = false;
        this.groundStatus = GroundStatus.NORMAL;
        linkSwimming.get().leaveSwimming();
    }

    protected int accessZPixels() {
        return Math.max(0, this.zSubPixels >> SUB_PIXEL_SHIFT);
    }

    protected int accessTickDialogGotItem(boolean dialogActive) {
        int openForItem = dialogGotItemSequence.get().advance(dialogActive);
        if (dialogGotItemSequence.get().presentedItem() == 0) return openForItem;
        // ApplyGotItem caches body Y before func_21E1 updates Z; the held OAM
        // entry uses the updated Z. Preserve the fractional position at ground.
        linkAnimator.get().bedJumpRenderZ = accessZPixels();
        if (linkServices.get().itemRegistry != null
                && linkServices.get().itemRegistry.lookup(InventoryItem.SWORD.romValue())
                        instanceof linksawakening.equipment.Sword sword) {
            sword.resetSpinAttack();
        }
        linkItemState.get().romAttackStepAnimationCountdown = 0;
        linkDamage.get().collisionIgnoreFramesRemaining = 0;
        int z = (this.zSubPixels + (byte) this.zVelocity) & 0xFFF;
        this.zVelocity = (byte) (this.zVelocity - 2);
        if (((z >>> SUB_PIXEL_SHIFT) & 0x80) != 0) {
            z = 0;
            this.zVelocity = 0;
        }
        this.zSubPixels = z;
        linkAnimator.get().romAnimationStateOverride = 0x6B;
        linkAnimationState.get().movingThisFrame = false;
        linkMotion.get().lastRomSpeedX = linkMotion.get().lastRomSpeedY = 0;
        return openForItem;
    }
}
