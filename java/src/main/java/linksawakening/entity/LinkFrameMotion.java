package linksawakening.entity;

import static linksawakening.entity.Link.*;
import static linksawakening.entity.LinkRomConstants.*;
import static linksawakening.entity.LinkStateMachine.*;

import linksawakening.state.Direction;
import linksawakening.state.GroundStatus;
import linksawakening.state.LinkMotionState;

/** LinkFrameMotion behavior on the shared LinkStateMachine state. */
abstract class LinkFrameMotion extends LinkMotionLifecycle {
    protected LinkFrameMotion(Dependencies dependencies) {
        super(dependencies);
    }

    private final Frame frame = new Frame();

    private static final class Frame {
        int carriedSpeedX;
        int carriedSpeedY;
        boolean featherLaunchFrame;
        int platformVerticalSpeedForThisUpdate;
        int mask;
        int speedX;
        int speedY;
    }

    protected void update() {
        if (!beginFrame()) return;
        if (!advanceSpecialMotion()) return;
        if (!advanceHazardMotion()) return;
        if (!prepareGroundMotion()) return;
        resolveSpeeds();
        applyMotion();
    }

    private boolean beginFrame() {
        if (motionState == LinkMotionState.PASS_OUT) {
            linkAnimationState.get().movingThisFrame = false;
            return false;
        }
        linkAnimator.get().bedJumpRenderZ = -1;
        linkAnimator.get().ledgeJumpRenderY = linkAirborne.get().overworldLedgeFallActive
                        || linkAirborne.get().directionalLedgeJumpMotionLocked
                ? linkMotion.get().accessPixelY() - linkAirborne.get().accessZPixels()
                : Integer.MIN_VALUE;
        frameCounter = (frameCounter + 1) & 0xFF;
        if (pegasusBootsCollisionCountdown > 0) {
            pegasusBootsCollisionCountdown--;
        }
        linkEquipment.get().tickRomAttackStepAnimationCountdown();
        frame.carriedSpeedX = (byte) linkMotion.get().lastRomSpeedX;
        frame.carriedSpeedY = (byte) linkMotion.get().lastRomSpeedY;
        frame.featherLaunchFrame = linkAnimationState.get().topDownFeatherStarted;
        linkAnimationState.get().topDownFeatherStarted = false;
        linkMotion.get().romCollisionType = 0;
        linkMotion.get().lastRomSpeedX = 0;
        linkMotion.get().lastRomSpeedY = 0;
        frame.platformVerticalSpeedForThisUpdate = linkSideView.get().sideViewPlatformVerticalSpeedPending;
        linkSideView.get().sideViewPlatformVerticalSpeedPending = -1;
        linkDamage.get().tickInvincibility();

        boolean interactiveMotionBlocked = romInteractiveMotionBlocked;
        romInteractiveMotionBlocked = false;
        romInteractiveMotionBlockValue = 0;
        if (!interactiveMotionBlocked) {
            linkAnimator.get().romAnimationStateOverride = -1;
            linkAnimator.get().swordAcquisitionBaseRomDirection = -1;
            linkAnimator.get().swordAcquisitionAnimationState = -1;
        }
        if (interactiveMotionBlocked && !(linkSideView.get().sideScrolling && linkAirborne.get().airborne)) {
            linkAirborne.get().zVelocity = 0;
            linkAnimationState.get().movingThisFrame = false;
            linkAnimationState.get().walkTickCounter = 0;
            linkAnimationState.get().walkFrame = 0;
            return false;
        }

        // 6A94's side-view landing branch clears Link's airborne presentation
        // before the next normal motion update. Main keeps wC147 set through
        // this update and clears it only before the following entity tick.
        if (linkSideView.get().standingOnSideViewEntity && !linkSideView.get().sideScrolling) {
            linkAirborne.get().airborne = false;
            linkAirborne.get().zSubPixels = 0;
            linkAirborne.get().zVelocity = 0;
            linkAirborne.get().fallingIntoPit = false;
            linkAirborne.get().groundStatus = GroundStatus.NORMAL;
        }

        return true;
    }

    private boolean advanceSpecialMotion() {
        if (debugNoClip) {
            updateDebugNoClip();
            return false;
        }

        if (motionState == LinkMotionState.RECOVER) {
            linkSwimming.get().updateRecover();
            return false;
        }

        if (linkAirborne.get().spinningPitWarpArrival) {
            // $02:4511 clears XY increments; $02:478C selects facing globally.
            linkAnimationState.get().direction = Direction.fromRom(
                            linkServices.get().romTables.pitWarpArrivalDirection(frameCounter))
                    .javaIndex();
            linkMotion.get().forcedSpeedPending = false;
            linkMotion.get().forcedSpeedXOnly = false;
            linkAnimationState.get().movingThisFrame = false;
            linkAirborne.get().tickJump();
            return false;
        }

        if (linkSideView.get().sideScrolling) {
            linkSideView.get().updateSideViewMotion(frame.platformVerticalSpeedForThisUpdate);
            return false;
        }

        return true;
    }

    private boolean advanceHazardMotion() {
        if (linkEntityEffects.get().likeLikeCaptured && linkEntityEffects.get().likeLikeCapturePinsPosition) {
            linkMotion.get().subX = linkEntityEffects.get().likeLikeCaptureSubX;
            linkMotion.get().subY = linkEntityEffects.get().likeLikeCaptureSubY;
            linkAirborne.get().airborne = false;
            linkAirborne.get().zSubPixels = 0;
            linkAirborne.get().zVelocity = 0;
            linkAnimationState.get().movingThisFrame = false;
            return false;
        }

        if (linkSignals.get().pitWarpPending) return false;

        if (linkAirborne.get().fallingIntoPit) {
            linkAirborne.get().tickPitFall();
            return false;
        }

        if (linkAirborne.get().overworldLedgeFallActive) {
            linkMotion.get().lastRomSpeedX = frame.carriedSpeedX & 0xFF;
            linkMotion.get().lastRomSpeedY = frame.carriedSpeedY & 0xFF;
            if (physicsModifier == 0) linkMotion.get().beginOverworldLedgeUnstuckingMotion();
            linkAirborne.get().tickOverworldLedgeFall();
            return false;
        }

        if (motionState == LinkMotionState.REVOLVING_DOOR) {
            tickRevolvingDoor();
            return false;
        }

        if (motionState == LinkMotionState.SWIMMING) {
            linkSwimming.get().updateSwimming();
            return false;
        }

        if (linkDamage.get().hasRecoil()) {
            linkDamage.get().tickRecoil();
            linkAirborne.get().tickJump();
            return false;
        }

        return true;
    }

    private boolean prepareGroundMotion() {
        linkAnimator.get().cachePushingAnimation();
        frame.mask = linkControls.get().accessBuildJoypadMask();
        int newDirection = LinkRomConstants.JOYPAD_TO_DIRECTION[frame.mask];
        // $02:4406 skips facing selection while airborne; the Feather
        // launch still follows this frame's ordinary ground movement.
        if (newDirection != -1
                && (!linkAirborne.get().airborne || frame.featherLaunchFrame)
                && !linkEntityEffects.get().marinBedJumpMotionLocked
                && !linkAirborne.get().directionalLedgeJumpMotionLocked
                && !linkEquipment.get().itemsLockFacing(true)
                && !linkEntityEffects.get().accessIsLiftTransitionBlockingMotion()) {
            linkAnimationState.get().direction = newDirection;
        }
        updatePegasusBootsUse();
        if (!linkAirborne.get().airborne && linkAirborne.get().groundStatus != GroundStatus.PIT) {
            linkAirborne.get().refreshGroundStatus();
            linkAirborne.get().updateLastSafePositionIfPossible();
        }

        if (linkAirborne.get().fallingIntoPit) return false;
        if (linkAirborne.get().groundStatus == GroundStatus.PIT) {
            linkAnimationState.get().movingThisFrame = false;
            linkAnimationState.get().walkTickCounter = 0;
            linkAnimationState.get().walkFrame = 0;
            linkAirborne.get().slipTowardPitCenter();
            return false;
        }

        // Equipped items (sword swing, etc.) can block motion for a window of
        // frames. Mirrors the disassembly's hLinkInteractiveMotionBlocked flag
        // set by UpdateLinkAnimation while wSwordAnimationState != NONE. The
        // ROM still refreshes hLinkDirection from held single-axis input before
        // this early-out path, so direction update must happen above.
        // $02:43C3 bypasses the item motion gate while Link is above ground.
        if (linkEquipment.get().itemsBlockMotion(true) && !linkAirborne.get().airborne) {
            linkAnimationState.get().movingThisFrame = false;
            linkAnimationState.get().walkTickCounter = 0;
            linkAnimationState.get().walkFrame = 0;
            if (linkDamage.get().collisionIgnoreFramesRemaining > 0) {
                linkDamage.get().collisionIgnoreFramesRemaining--;
            }
            linkAirborne.get().tickJump();
            return false;
        }

        return true;
    }

    private void resolveSpeeds() { // Source $43E6/$4440/$445F/$4503. The grass OAM flip depends on
        // this byte, including steps suppressed by slow ground or a wall.
        if (linkAirborne.get().airborne) linkAnimationState.get().romConsecutiveStepsCount = 0xFF;
        else if (linkServices.get().playerState != null
                && linkServices.get().playerState.runningWithPegasusBoots())
            linkAnimationState.get().romConsecutiveStepsCount =
                    (linkAnimationState.get().romConsecutiveStepsCount + 2) & 0xFF;
        else if (frame.mask != 0)
            linkAnimationState.get().romConsecutiveStepsCount =
                    (linkAnimationState.get().romConsecutiveStepsCount + 1) & 0xFF;
        else if (pegasusBootsChargeMeter == 0) linkAnimationState.get().romConsecutiveStepsCount = 7;

        if (linkAirborne.get().directionalLedgeJumpMotionLocked) {
            frame.speedX = linkAirborne.get().directionalLedgeJumpSpeedX;
            frame.speedY = linkAirborne.get().directionalLedgeJumpSpeedY;
        } else if (linkEntityEffects.get().marinBedJumpMotionLocked) {
            frame.speedX = 0x0C;
            frame.speedY = 0;
        } else if (linkServices.get().playerState != null
                && linkServices.get().playerState.runningWithPegasusBoots()) {
            frame.speedX = pegasusRunningSpeedX();
            frame.speedY = pegasusRunningSpeedY();
        } else if (linkMotion.get().forcedSpeedPending) {
            frame.speedX = linkMotion.get().forcedSpeedX;
            frame.speedY = linkMotion.get().forcedSpeedXOnly
                    ? (byte) linkServices.get().romTables.linkSpeedY(groundSpeedIndex(frame.mask))
                    : linkMotion.get().forcedSpeedY;
            linkMotion.get().forcedSpeedPending = false;
            linkMotion.get().forcedSpeedXOnly = false;
        } else if (linkAirborne.get().airborne && !frame.featherLaunchFrame) {
            // $02:4402 skips the ground speed lookup while airborne.
            // ApplyLinkGroundMotion steers these retained speeds after XY.
            frame.speedX = frame.carriedSpeedX;
            frame.speedY = frame.carriedSpeedY;
        } else {
            frame.speedX = (byte) linkServices.get().romTables.linkSpeedX(groundSpeedIndex(frame.mask));
            frame.speedY = (byte) linkServices.get().romTables.linkSpeedY(groundSpeedIndex(frame.mask));
        }
        if (frame.platformVerticalSpeedForThisUpdate >= 0) {
            frame.speedY = (byte) frame.platformVerticalSpeedForThisUpdate;
        }
    }

    private void applyMotion() {
        boolean bedJumpFrame = linkEntityEffects.get().marinBedJumpMotionLocked;
        if (bedJumpFrame) {
            linkAnimator.get().marinWakeUpBedVariant = -1;
            // Marin sets wIsLinkInTheAir=2, so UpdateLinkAnimation skips
            // the feather/ledge flip. Walking animation precedes gravity:
            // first pose $0A, then $0B, including the landing frame.
            linkAnimator.get().romAnimationStateOverride = LinkAnimator.ANIMATION_STATE[
                    linkAnimationState.get().direction][linkAnimationState.get().movingThisFrame ? 1 : 0];
            // ApplyGotItem caches wC145 before this frame changes Z.
            linkAnimator.get().bedJumpRenderZ = linkAirborne.get().accessZPixels();
            linkAirborne.get().tickJump();
        }
        linkMotion.get().lastRomSpeedX = frame.speedX & 0xFF;
        linkMotion.get().lastRomSpeedY = frame.speedY & 0xFF;
        linkAnimationState.get().movingThisFrame = (frame.speedX != 0 || frame.speedY != 0);
        boolean applyGroundMotion = gelClinging
                ? (gelMotionFrame & 3) == 0
                : linkAirborne.get().airborne || linkGroundMotion.get().accessShouldApplyGroundMotion();

        if (applyGroundMotion && frame.speedX != 0 && (!bedJumpFrame || linkAirborne.get().airborne)) {
            linkMotion.get().tryMoveAxis(frame.speedX, true);
        }
        if (applyGroundMotion && frame.speedY != 0 && (!bedJumpFrame || linkAirborne.get().airborne)) {
            linkMotion.get().tryMoveAxis(frame.speedY, false);
        }

        if (linkDamage.get().collisionIgnoreFramesRemaining > 0) {
            linkDamage.get().collisionIgnoreFramesRemaining--;
        }

        if (!bedJumpFrame) linkAirborne.get().tickJump();
        handlePegasusBootsCollision();

        if (!linkAirborne.get().airborne) {
            if (!linkSwimming.get().enterSwimmingIfNeeded()) {
                linkAirborne.get().updateLastSafePositionIfPossible();
            }
        }

        if (linkAnimationState.get().movingThisFrame) {
            linkAnimationState.get().walkTickCounter++;
            if (linkAnimationState.get().walkTickCounter >= LinkRomConstants.WALK_FRAME_TICKS) {
                linkAnimationState.get().walkTickCounter = 0;
                linkAnimationState.get().walkFrame ^= 1;
            }
        } else {
            linkAnimationState.get().walkTickCounter = 0;
            linkAnimationState.get().walkFrame = 0;
        }
    }
}
