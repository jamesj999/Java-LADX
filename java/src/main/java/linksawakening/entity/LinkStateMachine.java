package linksawakening.entity;

import static linksawakening.entity.Link.*;
import static linksawakening.entity.LinkRomConstants.*;

import linksawakening.state.Direction;
import linksawakening.state.GroundStatus;
import linksawakening.state.LinkMotionState;

/** Dispatches ROM ApplyLinkMotionState in frame order. */
final class LinkStateMachine {

    private final LinkDash dash;

    LinkDash dash() {
        return dash;
    }

    private final LinkRoomPhysics roomPhysics;

    LinkRoomPhysics roomPhysics() {
        return roomPhysics;
    }

    private final LinkDoorTraversal door;

    LinkDoorTraversal door() {
        return door;
    }

    private final LinkMotionStatus status;

    LinkMotionStatus status() {
        return status;
    }

    private final LinkComponentRef<DialogGotItemSequence> dialogGotItemSequence;
    private final LinkComponentRef<LinkAirborne> linkAirborne;
    private final LinkComponentRef<LinkAnimationState> linkAnimationState;
    private final LinkComponentRef<LinkAnimator> linkAnimator;
    private final LinkComponentRef<LinkControls> linkControls;
    private final LinkComponentRef<LinkDamage> linkDamage;
    private final LinkComponentRef<LinkEntityEffects> linkEntityEffects;
    private final LinkComponentRef<LinkEquipment> linkEquipment;
    private final LinkComponentRef<LinkGroundMotion> linkGroundMotion;
    private final LinkComponentRef<LinkItemState> linkItemState;
    private final LinkComponentRef<LinkMotion> linkMotion;
    private final LinkComponentRef<LinkServices> linkServices;
    private final LinkComponentRef<LinkSideView> linkSideView;
    private final LinkComponentRef<LinkSignals> linkSignals;
    private final LinkComponentRef<LinkSwimming> linkSwimming;

    LinkStateMachine(LinkAssembly.MotionDependencies dependencies) {
        dialogGotItemSequence = dependencies.dialogGotItemSequence;
        linkAirborne = dependencies.linkAirborne;
        linkAnimationState = dependencies.linkAnimationState;
        linkAnimator = dependencies.linkAnimator;
        linkControls = dependencies.linkControls;
        linkDamage = dependencies.linkDamage;
        linkEntityEffects = dependencies.linkEntityEffects;
        linkEquipment = dependencies.linkEquipment;
        linkGroundMotion = dependencies.linkGroundMotion;
        linkItemState = dependencies.linkItemState;
        linkMotion = dependencies.linkMotion;
        linkServices = dependencies.linkServices;
        linkSideView = dependencies.linkSideView;
        linkSignals = dependencies.linkSignals;
        linkSwimming = dependencies.linkSwimming;

        status = new LinkMotionStatus();
        dash = new LinkDash(linkAirborne, linkAnimationState, linkEntityEffects, linkMotion, linkServices, status);
        roomPhysics = new LinkRoomPhysics(linkAirborne, linkDamage, linkGroundMotion, linkMotion, linkServices, status);
        door = new LinkDoorTraversal(linkAnimationState, linkAnimator, linkMotion, linkServices, status);
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

    void update() {
        if (!beginFrame()) return;
        if (!advanceSpecialMotion()) return;
        if (!advanceHazardMotion()) return;
        if (!prepareGroundMotion()) return;
        resolveSpeeds();
        applyMotion();
    }

    private boolean beginFrame() {
        if (status.motionState() == LinkMotionState.PASS_OUT) {
            linkAnimationState.get().movingThisFrame = false;
            return false;
        }
        linkAnimator.get().bedJumpRenderZ(-1);
        linkAnimator
                .get()
                .ledgeJumpRenderY(
                        linkAirborne.get().overworldLedgeFallActive()
                                        || linkAirborne.get().directionalLedgeJumpMotionLocked()
                                ? linkMotion.get().position().pixelY()
                                        - linkAirborne.get().accessZPixels()
                                : Integer.MIN_VALUE);
        status.frameCounter((status.frameCounter() + 1) & 0xFF);
        if (dash.pegasusBootsCollisionCountdown() > 0) {
            dash.pegasusBootsCollisionCountdown(dash.pegasusBootsCollisionCountdown() - 1);
        }
        linkEquipment.get().tickRomAttackStepAnimationCountdown();
        frame.carriedSpeedX = (byte) linkMotion.get().position().lastRomSpeedX();
        frame.carriedSpeedY = (byte) linkMotion.get().position().lastRomSpeedY();
        frame.featherLaunchFrame = linkAnimationState.get().topDownFeatherStarted;
        linkAnimationState.get().topDownFeatherStarted = false;
        linkMotion.get().collisionState().romCollisionType(0);
        linkMotion.get().position().lastRomSpeedX(0);
        linkMotion.get().position().lastRomSpeedY(0);
        frame.platformVerticalSpeedForThisUpdate = linkSideView.get().sideViewPlatformVerticalSpeedPending;
        linkSideView.get().sideViewPlatformVerticalSpeedPending = -1;
        linkDamage.get().tickInvincibility();

        boolean interactiveMotionBlocked = status.romInteractiveMotionBlocked();
        status.romInteractiveMotionBlocked(false);
        status.romInteractiveMotionBlockValue(0);
        if (!interactiveMotionBlocked) {
            linkAnimator.get().romAnimationStateOverride(-1);
            linkAnimator.get().swordAcquisitionBaseRomDirection(-1);
            linkAnimator.get().swordAcquisitionAnimationState(-1);
        }
        if (interactiveMotionBlocked
                && !(linkSideView.get().sideScrolling
                        && linkAirborne.get().elevation().airborne())) {
            linkAirborne.get().elevation().zVelocity(0);
            linkAnimationState.get().movingThisFrame = false;
            linkAnimationState.get().walkTickCounter = 0;
            linkAnimationState.get().walkFrame = 0;
            return false;
        }

        // 6A94's side-view landing branch clears Link's airborne presentation
        // before the next normal motion update. Main keeps wC147 set through
        // this update and clears it only before the following entity tick.
        if (linkSideView.get().standingOnSideViewEntity && !linkSideView.get().sideScrolling) {
            linkAirborne.get().elevation().airborne(false);
            linkAirborne.get().elevation().zSubPixels(0);
            linkAirborne.get().elevation().zVelocity(0);
            linkAirborne.get().pit().fallingIntoPit(false);
            linkAirborne.get().pit().groundStatus(GroundStatus.NORMAL);
        }

        return true;
    }

    private boolean advanceSpecialMotion() {
        if (status.debugNoClip()) {
            updateDebugNoClip();
            return false;
        }

        if (status.motionState() == LinkMotionState.RECOVER) {
            linkSwimming.get().updateRecover();
            return false;
        }

        if (linkAirborne.get().spinningPitWarpArrival()) {
            // $02:4511 clears XY increments; $02:478C selects facing globally.
            linkAnimationState.get().direction = Direction.fromRom(
                    linkServices.get().linkMotionTables.pitWarpArrivalDirection(status.frameCounter()));
            linkMotion.get().position().forcedSpeedPending(false);
            linkMotion.get().position().forcedSpeedXOnly(false);
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
            linkMotion.get().position().subX(linkEntityEffects.get().likeLikeCaptureSubX);
            linkMotion.get().position().subY(linkEntityEffects.get().likeLikeCaptureSubY);
            linkAirborne.get().elevation().airborne(false);
            linkAirborne.get().elevation().zSubPixels(0);
            linkAirborne.get().elevation().zVelocity(0);
            linkAnimationState.get().movingThisFrame = false;
            return false;
        }

        if (linkSignals.get().pitWarpPending) return false;

        if (linkAirborne.get().pit().fallingIntoPit()) {
            linkAirborne.get().pit().tickPitFall();
            return false;
        }

        if (linkAirborne.get().overworldLedgeFallActive()) {
            linkMotion.get().position().lastRomSpeedX(frame.carriedSpeedX & 0xFF);
            linkMotion.get().position().lastRomSpeedY(frame.carriedSpeedY & 0xFF);
            if (roomPhysics.physicsModifier() == 0) linkMotion.get().beginOverworldLedgeUnstuckingMotion();
            linkAirborne.get().tickOverworldLedgeFall();
            return false;
        }

        if (status.motionState() == LinkMotionState.REVOLVING_DOOR) {
            door.tickRevolvingDoor();
            return false;
        }

        if (status.motionState() == LinkMotionState.SWIMMING) {
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
                && (!linkAirborne.get().elevation().airborne() || frame.featherLaunchFrame)
                && !linkEntityEffects.get().marinBedJumpMotionLocked
                && !linkAirborne.get().directionalLedgeJumpMotionLocked()
                && !linkEquipment.get().itemsLockFacing(true)
                && !linkEntityEffects.get().accessIsLiftTransitionBlockingMotion()) {
            linkAnimationState.get().direction = Direction.fromJava(newDirection);
        }
        dash.updatePegasusBootsUse();
        if (!linkAirborne.get().elevation().airborne()
                && linkAirborne.get().pit().groundStatus() != GroundStatus.PIT) {
            linkAirborne.get().pit().refreshGroundStatus();
            linkAirborne.get().pit().updateLastSafePositionIfPossible();
        }

        if (linkAirborne.get().pit().fallingIntoPit()) return false;
        if (linkAirborne.get().pit().groundStatus() == GroundStatus.PIT) {
            linkAnimationState.get().movingThisFrame = false;
            linkAnimationState.get().walkTickCounter = 0;
            linkAnimationState.get().walkFrame = 0;
            linkAirborne.get().pit().slipTowardPitCenter();
            return false;
        }

        // Equipped items (sword swing, etc.) can block motion for a window of
        // frames. Mirrors the disassembly's hLinkInteractiveMotionBlocked flag
        // set by UpdateLinkAnimation while wSwordAnimationState != NONE. The
        // ROM still refreshes hLinkDirection from held single-axis input before
        // this early-out path, so direction update must happen above.
        // $02:43C3 bypasses the item motion gate while Link is above ground.
        if (linkEquipment.get().itemsBlockMotion(true)
                && !linkAirborne.get().elevation().airborne()) {
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
        if (linkAirborne.get().elevation().airborne()) linkAnimationState.get().romConsecutiveStepsCount = 0xFF;
        else if (linkServices.get().playerState != null
                && linkServices.get().playerState.equipmentLevels().runningWithPegasusBoots())
            linkAnimationState.get().romConsecutiveStepsCount =
                    (linkAnimationState.get().romConsecutiveStepsCount + 2) & 0xFF;
        else if (frame.mask != 0)
            linkAnimationState.get().romConsecutiveStepsCount =
                    (linkAnimationState.get().romConsecutiveStepsCount + 1) & 0xFF;
        else if (dash.pegasusBootsChargeMeter() == 0) linkAnimationState.get().romConsecutiveStepsCount = 7;

        if (linkAirborne.get().directionalLedgeJumpMotionLocked()) {
            frame.speedX = linkAirborne.get().directionalLedgeJumpSpeedX();
            frame.speedY = linkAirborne.get().directionalLedgeJumpSpeedY();
        } else if (linkEntityEffects.get().marinBedJumpMotionLocked) {
            frame.speedX = 0x0C;
            frame.speedY = 0;
        } else if (linkServices.get().playerState != null
                && linkServices.get().playerState.equipmentLevels().runningWithPegasusBoots()) {
            frame.speedX = dash.pegasusRunningSpeedX();
            frame.speedY = dash.pegasusRunningSpeedY();
        } else if (linkMotion.get().position().forcedSpeedPending()) {
            frame.speedX = linkMotion.get().position().forcedSpeedX();
            frame.speedY = linkMotion.get().position().forcedSpeedXOnly()
                    ? (byte) linkServices.get().linkMotionTables.linkSpeedY(groundSpeedIndex(frame.mask))
                    : linkMotion.get().position().forcedSpeedY();
            linkMotion.get().position().forcedSpeedPending(false);
            linkMotion.get().position().forcedSpeedXOnly(false);
        } else if (linkAirborne.get().elevation().airborne() && !frame.featherLaunchFrame) {
            // $02:4402 skips the ground speed lookup while airborne.
            // ApplyLinkGroundMotion steers these retained speeds after XY.
            frame.speedX = frame.carriedSpeedX;
            frame.speedY = frame.carriedSpeedY;
        } else {
            frame.speedX = (byte) linkServices.get().linkMotionTables.linkSpeedX(groundSpeedIndex(frame.mask));
            frame.speedY = (byte) linkServices.get().linkMotionTables.linkSpeedY(groundSpeedIndex(frame.mask));
        }
        if (frame.platformVerticalSpeedForThisUpdate >= 0) {
            frame.speedY = (byte) frame.platformVerticalSpeedForThisUpdate;
        }
    }

    private void applyMotion() {
        boolean bedJumpFrame = linkEntityEffects.get().marinBedJumpMotionLocked;
        if (bedJumpFrame) {
            linkAnimator.get().marinWakeUpBedVariant(-1);
            // Marin sets wIsLinkInTheAir=2, so UpdateLinkAnimation skips
            // the feather/ledge flip. Walking animation precedes gravity:
            // first pose $0A, then $0B, including the landing frame.
            linkAnimator
                    .get()
                    .romAnimationStateOverride(
                            LinkPoseTables.ANIMATION_STATE[
                                    linkAnimationState.get().direction.javaIndex()][
                                    linkAnimationState.get().movingThisFrame ? 1 : 0]);
            // ApplyGotItem caches wC145 before this frame changes Z.
            linkAnimator.get().bedJumpRenderZ(linkAirborne.get().accessZPixels());
            linkAirborne.get().tickJump();
        }
        linkMotion.get().position().lastRomSpeedX(frame.speedX & 0xFF);
        linkMotion.get().position().lastRomSpeedY(frame.speedY & 0xFF);
        linkAnimationState.get().movingThisFrame = (frame.speedX != 0 || frame.speedY != 0);
        boolean applyGroundMotion = status.gelClinging()
                ? (status.gelMotionFrame() & 3) == 0
                : linkAirborne.get().elevation().airborne()
                        || linkGroundMotion.get().accessShouldApplyGroundMotion();

        if (applyGroundMotion
                && frame.speedX != 0
                && (!bedJumpFrame || linkAirborne.get().elevation().airborne())) {
            linkMotion.get().tryMoveAxis(frame.speedX, true);
        }
        if (applyGroundMotion
                && frame.speedY != 0
                && (!bedJumpFrame || linkAirborne.get().elevation().airborne())) {
            linkMotion.get().tryMoveAxis(frame.speedY, false);
        }

        if (linkDamage.get().collisionIgnoreFramesRemaining > 0) {
            linkDamage.get().collisionIgnoreFramesRemaining--;
        }

        if (!bedJumpFrame) linkAirborne.get().tickJump();
        dash.handlePegasusBootsCollision();

        if (!linkAirborne.get().elevation().airborne()) {
            if (!linkSwimming.get().enterSwimmingIfNeeded()) {
                linkAirborne.get().pit().updateLastSafePositionIfPossible();
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

    // $02:4409-4427 selects the second row only for Piece of Power.
    int groundSpeedIndex(int mask) {
        return (mask & 0x0F)
                | (linkServices.get().playerState != null
                                && linkServices
                                                .get()
                                                .playerState
                                                .equipmentLevels()
                                                .activePowerUp()
                                        == 1
                        ? 0x10
                        : 0);
    }

    void updateDebugNoClip() {
        int mask = linkControls.get().accessBuildJoypadMask();
        int newDirection = LinkRomConstants.JOYPAD_TO_DIRECTION[mask];
        if (newDirection != -1) {
            linkAnimationState.get().direction = Direction.fromJava(newDirection);
        }
        int speedX = (byte) linkServices.get().linkMotionTables.linkSpeedX(groundSpeedIndex(mask));
        int speedY = (byte) linkServices.get().linkMotionTables.linkSpeedY(groundSpeedIndex(mask));
        linkMotion.get().position().lastRomSpeedX(speedX & 0xFF);
        linkMotion.get().position().lastRomSpeedY(speedY & 0xFF);
        linkAnimationState.get().movingThisFrame = speedX != 0 || speedY != 0;
        linkMotion.get().position().subX(linkMotion.get().position().subX() + (speedX));
        linkMotion.get().position().subY(linkMotion.get().position().subY() + (speedY));
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

    /** Enables the development movement mode; normal gameplay leaves this off. */
    void setDebugNoClip(boolean enabled) {
        status.debugNoClip(enabled);
        if (enabled) {
            linkAirborne.get().pit().groundStatus(GroundStatus.NORMAL);
            linkAirborne.get().pit().fallingIntoPit(false);
            linkAirborne.get().elevation().airborne(false);
            linkAirborne.get().elevation().zSubPixels(0);
            linkAirborne.get().elevation().zVelocity(0);
        }
    }

    boolean accessIsDiving() {
        return status.accessIsSwimming() && roomPhysics.physicsModifier() != 0;
    }

    void accessSetGelClinging(boolean clinging, int worldFrame) {
        status.gelClinging(clinging);
        status.gelMotionFrame(worldFrame & 0xFF);
        linkGroundMotion.get().worldMotionFrame = worldFrame & 0xFF;
    }
}
