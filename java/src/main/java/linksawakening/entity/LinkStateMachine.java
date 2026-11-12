package linksawakening.entity;

import static linksawakening.entity.Link.*;
import static linksawakening.entity.LinkRomConstants.*;

import linksawakening.state.Direction;
import linksawakening.state.GroundStatus;
import linksawakening.state.InventoryItem;
import linksawakening.state.LinkMotionState;

final class LinkStateMachine {
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

    LinkStateMachine(Dependencies dependencies) {
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
    }

    static final class Dependencies {
        LinkComponentRef<DialogGotItemSequence> dialogGotItemSequence;
        LinkComponentRef<LinkAirborne> linkAirborne;
        LinkComponentRef<LinkAnimationState> linkAnimationState;
        LinkComponentRef<LinkAnimator> linkAnimator;
        LinkComponentRef<LinkControls> linkControls;
        LinkComponentRef<LinkDamage> linkDamage;
        LinkComponentRef<LinkEntityEffects> linkEntityEffects;
        LinkComponentRef<LinkEquipment> linkEquipment;
        LinkComponentRef<LinkGroundMotion> linkGroundMotion;
        LinkComponentRef<LinkItemState> linkItemState;
        LinkComponentRef<LinkMotion> linkMotion;
        LinkComponentRef<LinkServices> linkServices;
        LinkComponentRef<LinkSideView> linkSideView;
        LinkComponentRef<LinkSignals> linkSignals;
        LinkComponentRef<LinkSwimming> linkSwimming;
    }

    LinkMotionState motionState = LinkMotionState.DEFAULT;
    int physicsModifier;
    int recoverCountdown;
    boolean recoveringInWater;
    int slowWalkingSpeed;
    int itemUsageContext;
    int revolvingDoorFrame;
    boolean revolvingDoorTransitionPending;
    int revolvingDoorPaletteVariant = -1;
    boolean romInteractiveMotionBlocked;
    int romInteractiveMotionBlockValue;

    void update() {
        if (motionState == LinkMotionState.PASS_OUT) {
            linkAnimationState.get().movingThisFrame = false;
            return;
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
        int carriedSpeedX = (byte) linkMotion.get().lastRomSpeedX;
        int carriedSpeedY = (byte) linkMotion.get().lastRomSpeedY;
        boolean featherLaunchFrame = linkAnimationState.get().topDownFeatherStarted;
        linkAnimationState.get().topDownFeatherStarted = false;
        linkMotion.get().romCollisionType = 0;
        linkMotion.get().lastRomSpeedX = 0;
        linkMotion.get().lastRomSpeedY = 0;
        int platformVerticalSpeedForThisUpdate = linkSideView.get().sideViewPlatformVerticalSpeedPending;
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
            return;
        }

        // 6A94's side-view landing branch clears Link's linkAirborne.get().airborne presentation
        // before the next normal motion update. Main keeps wC147 set through
        // this update and clears it only before the following entity tick.
        if (linkSideView.get().standingOnSideViewEntity && !linkSideView.get().sideScrolling) {
            linkAirborne.get().airborne = false;
            linkAirborne.get().zSubPixels = 0;
            linkAirborne.get().zVelocity = 0;
            linkAirborne.get().fallingIntoPit = false;
            linkAirborne.get().groundStatus = GroundStatus.NORMAL;
        }

        if (debugNoClip) {
            updateDebugNoClip();
            return;
        }

        if (motionState == LinkMotionState.RECOVER) {
            linkSwimming.get().updateRecover();
            return;
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
            return;
        }

        if (linkSideView.get().sideScrolling) {
            linkSideView.get().updateSideViewMotion(platformVerticalSpeedForThisUpdate);
            return;
        }

        if (linkEntityEffects.get().likeLikeCaptured && linkEntityEffects.get().likeLikeCapturePinsPosition) {
            linkMotion.get().subX = linkEntityEffects.get().likeLikeCaptureSubX;
            linkMotion.get().subY = linkEntityEffects.get().likeLikeCaptureSubY;
            linkAirborne.get().airborne = false;
            linkAirborne.get().zSubPixels = 0;
            linkAirborne.get().zVelocity = 0;
            linkAnimationState.get().movingThisFrame = false;
            return;
        }

        if (linkSignals.get().pitWarpPending) return;

        if (linkAirborne.get().fallingIntoPit) {
            linkAirborne.get().tickPitFall();
            return;
        }

        if (linkAirborne.get().overworldLedgeFallActive) {
            linkMotion.get().lastRomSpeedX = carriedSpeedX & 0xFF;
            linkMotion.get().lastRomSpeedY = carriedSpeedY & 0xFF;
            if (physicsModifier == 0) linkMotion.get().beginOverworldLedgeUnstuckingMotion();
            linkAirborne.get().tickOverworldLedgeFall();
            return;
        }

        if (motionState == LinkMotionState.REVOLVING_DOOR) {
            tickRevolvingDoor();
            return;
        }

        if (motionState == LinkMotionState.SWIMMING) {
            linkSwimming.get().updateSwimming();
            return;
        }

        if (linkDamage.get().hasRecoil()) {
            linkDamage.get().tickRecoil();
            linkAirborne.get().tickJump();
            return;
        }

        linkAnimator.get().cachePushingAnimation();
        int mask = linkControls.get().accessBuildJoypadMask();
        int newDirection = LinkRomConstants.JOYPAD_TO_DIRECTION[mask];
        // $02:4406 skips facing selection while linkAirborne.get().airborne; the Feather
        // launch still follows this frame's ordinary ground movement.
        if (newDirection != -1
                && (!linkAirborne.get().airborne || featherLaunchFrame)
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

        if (linkAirborne.get().fallingIntoPit) return;
        if (linkAirborne.get().groundStatus == GroundStatus.PIT) {
            linkAnimationState.get().movingThisFrame = false;
            linkAnimationState.get().walkTickCounter = 0;
            linkAnimationState.get().walkFrame = 0;
            linkAirborne.get().slipTowardPitCenter();
            return;
        }

        // Equipped items (sword swing, etc.) can block motion for a window of
        // frames. Mirrors the disassembly's hLinkInteractiveMotionBlocked flag
        // set by UpdateLinkAnimation while wSwordAnimationState != NONE. The
        // ROM still refreshes hLinkDirection from held single-axis input before
        // this early-out path, so linkAnimationState.get().direction update must happen above.
        // $02:43C3 bypasses the item motion gate while Link is above ground.
        if (linkEquipment.get().itemsBlockMotion(true) && !linkAirborne.get().airborne) {
            linkAnimationState.get().movingThisFrame = false;
            linkAnimationState.get().walkTickCounter = 0;
            linkAnimationState.get().walkFrame = 0;
            if (linkDamage.get().collisionIgnoreFramesRemaining > 0) {
                linkDamage.get().collisionIgnoreFramesRemaining--;
            }
            linkAirborne.get().tickJump();
            return;
        }

        // Source $43E6/$4440/$445F/$4503. The grass OAM flip depends on
        // this byte, including steps suppressed by slow ground or a wall.
        if (linkAirborne.get().airborne) linkAnimationState.get().romConsecutiveStepsCount = 0xFF;
        else if (linkServices.get().playerState != null
                && linkServices.get().playerState.runningWithPegasusBoots())
            linkAnimationState.get().romConsecutiveStepsCount =
                    (linkAnimationState.get().romConsecutiveStepsCount + 2) & 0xFF;
        else if (mask != 0)
            linkAnimationState.get().romConsecutiveStepsCount =
                    (linkAnimationState.get().romConsecutiveStepsCount + 1) & 0xFF;
        else if (pegasusBootsChargeMeter == 0) linkAnimationState.get().romConsecutiveStepsCount = 7;

        int speedX;
        int speedY;
        if (linkAirborne.get().directionalLedgeJumpMotionLocked) {
            speedX = linkAirborne.get().directionalLedgeJumpSpeedX;
            speedY = linkAirborne.get().directionalLedgeJumpSpeedY;
        } else if (linkEntityEffects.get().marinBedJumpMotionLocked) {
            speedX = 0x0C;
            speedY = 0;
        } else if (linkServices.get().playerState != null
                && linkServices.get().playerState.runningWithPegasusBoots()) {
            speedX = pegasusRunningSpeedX();
            speedY = pegasusRunningSpeedY();
        } else if (linkMotion.get().forcedSpeedPending) {
            speedX = linkMotion.get().forcedSpeedX;
            speedY = linkMotion.get().forcedSpeedXOnly
                    ? (byte) linkServices.get().romTables.linkSpeedY(groundSpeedIndex(mask))
                    : linkMotion.get().forcedSpeedY;
            linkMotion.get().forcedSpeedPending = false;
            linkMotion.get().forcedSpeedXOnly = false;
        } else if (linkAirborne.get().airborne && !featherLaunchFrame) {
            // $02:4402 skips the ground speed lookup while linkAirborne.get().airborne.
            // ApplyLinkGroundMotion steers these retained speeds after XY.
            speedX = carriedSpeedX;
            speedY = carriedSpeedY;
        } else {
            speedX = (byte) linkServices.get().romTables.linkSpeedX(groundSpeedIndex(mask));
            speedY = (byte) linkServices.get().romTables.linkSpeedY(groundSpeedIndex(mask));
        }
        if (platformVerticalSpeedForThisUpdate >= 0) {
            speedY = (byte) platformVerticalSpeedForThisUpdate;
        }
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
        linkMotion.get().lastRomSpeedX = speedX & 0xFF;
        linkMotion.get().lastRomSpeedY = speedY & 0xFF;
        linkAnimationState.get().movingThisFrame = (speedX != 0 || speedY != 0);
        boolean applyGroundMotion = gelClinging
                ? (gelMotionFrame & 3) == 0
                : linkAirborne.get().airborne || linkGroundMotion.get().accessShouldApplyGroundMotion();

        if (applyGroundMotion && speedX != 0 && (!bedJumpFrame || linkAirborne.get().airborne)) {
            linkMotion.get().tryMoveAxis(speedX, true);
        }
        if (applyGroundMotion && speedY != 0 && (!bedJumpFrame || linkAirborne.get().airborne)) {
            linkMotion.get().tryMoveAxis(speedY, false);
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

    void tickRevolvingDoor() {
        linkMotion.get().subX = (0x50 - 8) << Link.SUB_PIXEL_SHIFT;
        linkMotion.get().subY = 0;
        linkAnimationState.get().movingThisFrame = false;
        if (revolvingDoorFrame == 0 || revolvingDoorFrame == 0x28) revolvingDoorPaletteVariant = 0;
        if (revolvingDoorFrame == 0x18) revolvingDoorPaletteVariant = 1;
        if (revolvingDoorFrame < 0x38) {
            linkAnimator.get().romAnimationStateOverride =
                    linkServices.get().romTables.revolvingDoorAnimation(revolvingDoorFrame);
        }
        if (++revolvingDoorFrame >= 0x38) {
            linkMotion.get().subY = (-5 - 16) << Link.SUB_PIXEL_SHIFT;
            linkAnimationState.get().direction = Direction.UP.javaIndex();
            linkAnimator.get().romAnimationStateOverride = 0x04;
        }
        if (revolvingDoorFrame == 0x48) {
            motionState = LinkMotionState.DEFAULT;
            revolvingDoorPaletteVariant = -1;
            revolvingDoorTransitionPending = true;
        }
    }

    // $02:4409-4427 selects the second row only for Piece of Power.
    private int groundSpeedIndex(int mask) {
        return (mask & 0x0F)
                | (linkServices.get().playerState != null
                                && linkServices.get().playerState.activePowerUp() == 1
                        ? 0x10
                        : 0);
    }

    void updateDebugNoClip() {
        int mask = linkControls.get().accessBuildJoypadMask();
        int newDirection = LinkRomConstants.JOYPAD_TO_DIRECTION[mask];
        if (newDirection != -1) {
            linkAnimationState.get().direction = newDirection;
        }
        int speedX = (byte) linkServices.get().romTables.linkSpeedX(groundSpeedIndex(mask));
        int speedY = (byte) linkServices.get().romTables.linkSpeedY(groundSpeedIndex(mask));
        linkMotion.get().lastRomSpeedX = speedX & 0xFF;
        linkMotion.get().lastRomSpeedY = speedY & 0xFF;
        linkAnimationState.get().movingThisFrame = speedX != 0 || speedY != 0;
        linkMotion.get().subX += speedX;
        linkMotion.get().subY += speedY;
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

    /** Mirrors CheckItemsToUse's two Pegasus-Boots slot branches and UsePegasusBoots. */
    void updatePegasusBootsUse() {
        if (linkServices.get().playerState == null
                || linkServices.get().inputState == null
                || linkServices.get().inputConfig == null) {
            return;
        }
        boolean aHeld = linkServices.get().playerState.itemA() == InventoryItem.PEGASUS_BOOTS.romValue()
                && linkServices
                        .get()
                        .inputState
                        .isDown(linkServices.get().inputConfig.aKey());
        boolean bHeld = linkServices.get().playerState.itemB() == InventoryItem.PEGASUS_BOOTS.romValue()
                && linkServices
                        .get()
                        .inputState
                        .isDown(linkServices.get().inputConfig.bKey());
        if (!aHeld && !bHeld) {
            pegasusBootsChargeMeter = 0;
            return;
        }
        if (linkServices.get().playerState.runningWithPegasusBoots()
                || romInteractiveMotionBlocked
                || linkAirborne.get().airborne
                || linkAirborne.get().accessZPixels() != 0
                || linkAirborne.get().groundStatus == GroundStatus.PIT
                || linkEntityEffects.get().accessIsCarryingLiftedObject()
                || motionState == LinkMotionState.SWIMMING) {
            return;
        }

        int heldBootsSlots = (aHeld ? 1 : 0) + (bHeld ? 1 : 0);
        for (int slot = 0; slot < heldBootsSlots; slot++) {
            if (pegasusBootsChargeMeter >= LinkRomConstants.PEGASUS_BOOTS_MAX_CHARGE) {
                break;
            }
            pegasusBootsChargeMeter++;
            if (pegasusBootsChargeMeter == LinkRomConstants.PEGASUS_BOOTS_MAX_CHARGE) {
                linkServices.get().playerState.setRunningWithPegasusBoots(true);
                linkMotion.get().forcedSpeedPending = false;
                linkMotion.get().forcedSpeedXOnly = false;
                break;
            }
        }
    }

    int pegasusRunningSpeedX() {
        return switch (Direction.fromJavaOrUnknown(linkAnimationState.get().direction)) {
            case LEFT -> -LinkRomConstants.PEGASUS_BOOTS_RUNNING_SPEED;
            case RIGHT -> LinkRomConstants.PEGASUS_BOOTS_RUNNING_SPEED;
            default -> 0;
        };
    }

    int pegasusRunningSpeedY() {
        return switch (Direction.fromJavaOrUnknown(linkAnimationState.get().direction)) {
            case UP -> -LinkRomConstants.PEGASUS_BOOTS_RUNNING_SPEED;
            case DOWN -> LinkRomConstants.PEGASUS_BOOTS_RUNNING_SPEED;
            default -> 0;
        };
    }

    /** Mirrors bank2.asm:74AD's dash collision response and func_020_49BA. */
    void handlePegasusBootsCollision() {
        if (linkServices.get().playerState == null
                || !linkServices.get().playerState.runningWithPegasusBoots()
                || (linkMotion.get().romCollisionType & 0x0F) == 0) {
            return;
        }

        int reflectedSpeedX = (-Link.signedByte(linkMotion.get().lastRomSpeedX)) >> 2;
        int reflectedSpeedY = (-Link.signedByte(linkMotion.get().lastRomSpeedY)) >> 2;
        accessResetPegasusBoots();
        linkMotion.get().forcedSpeedX = reflectedSpeedX;
        linkMotion.get().forcedSpeedY = reflectedSpeedY;
        linkMotion.get().forcedSpeedPending = true;
        linkMotion.get().lastRomSpeedX = reflectedSpeedX & 0xFF;
        linkMotion.get().lastRomSpeedY = reflectedSpeedY & 0xFF;
        linkAirborne.get().airborne = true;
        linkAirborne.get().zSubPixels = 0;
        linkAirborne.get().zVelocity = LinkRomConstants.PEGASUS_BOOTS_COLLISION_VELOCITY_Z;

        int romDirection =
                Direction.fromJava(linkAnimationState.get().direction).romIndex();
        int[] xOffsets = {0x10, 0xF0, 0x08, 0x08};
        int[] yOffsets = {0x0C, 0x0C, 0xF0, 0x10};
        pegasusBootsCollisionPosX = (linkMotion.get().romEntityX() + xOffsets[romDirection]) & 0xFF;
        pegasusBootsCollisionPosY = (linkMotion.get().romEntityY() + yOffsets[romDirection]) & 0xFF;
        pegasusBootsCollisionCountdown = LinkRomConstants.PEGASUS_BOOTS_COLLISION_COUNTDOWN;
        pendingPegasusScreenShakeCountdown = LinkRomConstants.PEGASUS_BOOTS_COLLISION_SHAKE;
        pendingPegasusScreenShakePhase = (romDirection & 0x02) << 1;
    }

    long physicsRoomLoadToken = -1;
    int physicsRoomMapId = -1;
    int physicsRoomId = -1;
    int physicsRoomCategory = -1;
    boolean debugNoClip;
    int frameCounter;
    boolean gelClinging;
    int gelMotionFrame;
    int pegasusBootsChargeMeter;
    int pegasusBootsCollisionCountdown;
    int pegasusBootsCollisionPosX;
    int pegasusBootsCollisionPosY;
    int pendingPegasusScreenShakeCountdown;
    int pendingPegasusScreenShakePhase;

    /** Enables the development movement mode; normal gameplay leaves this off. */
    void setDebugNoClip(boolean enabled) {
        debugNoClip = enabled;
        if (enabled) {
            linkAirborne.get().groundStatus = GroundStatus.NORMAL;
            linkAirborne.get().fallingIntoPit = false;
            linkAirborne.get().airborne = false;
            linkAirborne.get().zSubPixels = 0;
            linkAirborne.get().zVelocity = 0;
        }
    }

    /** Clears scripted presentation/capture state before a debug game reset. */
    void resetTransientStateForDebug() {
        dialogGotItemSequence.get().reset();
        debugNoClip = false;
        linkGroundMotion.get().accessClearGroundEffects();
        linkAnimationState.get().romConsecutiveStepsCount = 7;
        romInteractiveMotionBlocked = false;
        romInteractiveMotionBlockValue = 0;
        linkSideView.get().standingOnSideViewEntity = false;
        linkSideView.get().sideViewPlatformVerticalSpeedPending = -1;
        linkDamage.get().collisionDamageImmune = false;
        linkAnimator.get().romAnimationStateOverride = -1;
        linkAnimator.get().swordAcquisitionBaseRomDirection = -1;
        linkAnimator.get().swordAcquisitionAnimationState = -1;
        linkAnimator.get().tarinShieldPresentation = false;
        linkAnimator.get().tarinShieldPalette = null;
        linkAnimator.get().marinWakeUpBedVariant = -1;
        linkEntityEffects.get().marinBedJumpMotionLocked = false;
        linkAnimator.get().bedJumpRenderZ = -1;
        linkAnimator.get().ledgeJumpRenderY = Integer.MIN_VALUE;
        linkAirborne.get().directionalLedgeJumpMotionLocked = false;
        linkAirborne.get().overworldLedgeFallActive = false;
        linkEntityEffects.get().likeLikeCaptured = false;
        linkEntityEffects.get().likeLikeCapturePinsPosition = false;
        linkAirborne.get().fallingIntoPit = false;
        linkEntityEffects.get().carryingLiftedObjectState = 0;
        linkAnimationState.get().topDownFeatherStarted = false;
        linkEntityEffects.get().roosterCarryActive = false;
        linkAirborne.get().airborne = false;
        linkAirborne.get().groundStatus = GroundStatus.NORMAL;
        motionState = LinkMotionState.DEFAULT;
        revolvingDoorFrame = 0;
        revolvingDoorTransitionPending = false;
        revolvingDoorPaletteVariant = -1;
        physicsModifier = 0;
        recoverCountdown = 0;
        recoveringInWater = false;
        linkSignals.get().pendingWaterSplash = null;
        linkSignals.get().pitWarpPending = false;
        physicsRoomLoadToken = -1;
        physicsRoomMapId = -1;
        physicsRoomId = -1;
        physicsRoomCategory = -1;
        linkAirborne.get().zSubPixels = 0;
        linkAirborne.get().zVelocity = 0;
        linkAirborne.get().spinningPitWarpArrival = false;
        linkMotion.get().forcedSpeedPending = false;
        linkMotion.get().forcedSpeedXOnly = false;
        linkDamage.get().collisionIgnoreFramesRemaining = 0;
        linkItemState.get().romLinkPushing = 0;
        linkAnimator.get().pushingAnimationState = -1;
        linkItemState.get().romAttackStepAnimationCountdown = 0;
        linkAnimationState.get().movingThisFrame = false;
        linkAnimationState.get().walkTickCounter = 0;
        linkAnimationState.get().walkFrame = 0;
    }

    /** ApplyRoomTransition's completion path ($02:793D-$7977). */
    void finishRoomScroll(boolean downward) {
        linkMotion.get().lastRomSpeedX = 0;
        linkMotion.get().lastRomSpeedY = 0;
        linkAirborne.get().zVelocity = 0;
        if (!downward || debugNoClip || linkServices.get().collision == null) {
            return;
        }
        // Run the ordinary downward linkServices.get().collision handler, including its ledge
        // half-tile filters, before applying the source's fallback.
        int probeX = linkMotion.get().pixelX();
        int probeY = linkMotion.get().pixelY();
        linkMotion.get().lastRomSpeedY = 0x01;
        boolean blocked = linkMotion.get().leadingEdgeBlocked(probeX, probeY, Direction.DOWN.javaIndex());
        int object = linkServices
                .get()
                .collision
                .objectIdAtPoint(
                        probeX + COLLISION_POINTS_X[Direction.DOWN.javaIndex()][1],
                        probeY + COLLISION_POINTS_Y[Direction.DOWN.javaIndex()][1]);
        // Switch blocks $DB/$DC are exempt from fallback unstucking.
        if (object != 0xDB
                && object != 0xDC
                && !linkAirborne.get().overworldLedgeFallActive
                && (object == 0xE1 || blocked)) {
            // func_002_6EAD aligns X and enters LINK_MOTION_UNSTUCKING.
            // This transition fallback does not play the ledge jingle.
            linkMotion.get().subX = (linkMotion.get().romEntityX() & 0xF0) << Link.SUB_PIXEL_SHIFT;
            linkMotion.get().subY -= 8 << Link.SUB_PIXEL_SHIFT;
            motionState = LinkMotionState.UNSTUCKING;
            physicsModifier = 0;
            linkAirborne.get().overworldLedgeFallActive = true;
            linkMotion.get().beginOverworldLedgeUnstuckingMotion();
        }
    }

    /**
     * Applies the source room-entry default once per loaded room. The load
     * serial also distinguishes a save/new-game reload of the same room, so a
     * prior ladder or diving modifier cannot leak across the reload.
     */
    void initializeRoomPhysicsForRoom(long loadToken, int mapId, int roomId, int mapCategory) {
        if (physicsRoomLoadToken == loadToken
                && physicsRoomMapId == mapId
                && physicsRoomId == roomId
                && physicsRoomCategory == mapCategory) {
            return;
        }
        linkGroundMotion.get().accessClearGroundEffects();
        physicsModifier = mapCategory == 2 ? 0x01 : 0x00;
        physicsRoomLoadToken = loadToken;
        physicsRoomMapId = mapId;
        physicsRoomId = roomId;
        physicsRoomCategory = mapCategory;
        linkDamage.get().collisionDamageImmune = false;
    }

    /** Applies the room-entry physics default after a completed warp. */
    void setRoomPhysicsForRoom(long loadToken, int mapId, int roomId, int mapCategory) {
        linkGroundMotion.get().accessClearGroundEffects();
        physicsModifier = mapCategory == 2 ? 0x01 : 0x00;
        physicsRoomLoadToken = loadToken;
        physicsRoomMapId = mapId;
        physicsRoomId = roomId;
        physicsRoomCategory = mapCategory;
        linkDamage.get().collisionDamageImmune = false;
    }

    /** Mirrors source ladder/diving updates to hLinkPhysicsModifier. */
    void setRomPhysicsModifier(int modifier) {
        physicsModifier = modifier & 0xFF;
    }

    /** Applies the ROM's next-frame hLinkInteractiveMotionBlocked=$02 write. */
    void blockNextRomMotionFrame() {
        writeRomInteractiveMotionBlock(2);
    }

    void writeRomInteractiveMotionBlock(int value) {
        if ((value & ~255) != 0) throw new IllegalArgumentException("Interaction block must be an unsigned byte");
        // Keep the existing movement lock separate from the exact ROM equipment gate.
        romInteractiveMotionBlockValue = value;
        romInteractiveMotionBlocked = value != 0;
    }

    /** Mirrors ResetPegasusBoots for handlers that interrupt a dash. */
    void resetPegasusBoots() {
        pegasusBootsChargeMeter = 0;
        if (linkServices.get().playerState != null) {
            linkServices.get().playerState.setRunningWithPegasusBoots(false);
        }
    }

    /** Returns and clears the one-shot shake request written this frame. */
    ScreenShakeRequest consumePegasusScreenShakeRequest() {
        ScreenShakeRequest request =
                new ScreenShakeRequest(pendingPegasusScreenShakeCountdown, pendingPegasusScreenShakePhase);
        pendingPegasusScreenShakeCountdown = 0;
        pendingPegasusScreenShakePhase = 0;
        return request.countdown() == 0 ? null : request;
    }

    boolean accessDebugNoClip() {
        return this.debugNoClip;
    }

    boolean accessIsSwimming() {
        return this.motionState == LinkMotionState.SWIMMING;
    }

    boolean accessIsDiving() {
        return accessIsSwimming() && this.physicsModifier != 0;
    }

    int accessRomPhysicsModifier() {
        return this.physicsModifier & 0xFF;
    }

    int accessPegasusBootsChargeMeter() {
        return this.pegasusBootsChargeMeter & 0xFF;
    }

    int accessPegasusBootsCollisionCountdown() {
        return this.pegasusBootsCollisionCountdown & 0xFF;
    }

    int accessPegasusBootsCollisionPosX() {
        return this.pegasusBootsCollisionPosX & 0xFF;
    }

    int accessPegasusBootsCollisionPosY() {
        return this.pegasusBootsCollisionPosY & 0xFF;
    }

    void accessSetGelClinging(boolean clinging, int worldFrame) {
        this.gelClinging = clinging;
        this.gelMotionFrame = worldFrame & 0xFF;
        linkGroundMotion.get().worldMotionFrame = worldFrame & 0xFF;
    }

    boolean accessConsumeRevolvingDoorTransition() {
        boolean pending = this.revolvingDoorTransitionPending;
        this.revolvingDoorTransitionPending = false;
        return pending;
    }

    void accessSetDebugNoClip(boolean enabled) {
        this.setDebugNoClip(enabled);
    }

    void accessResetTransientStateForDebug() {
        this.resetTransientStateForDebug();
    }

    void accessFinishRoomScroll(boolean downward) {
        this.finishRoomScroll(downward);
    }

    void accessWriteRomInteractiveMotionBlock(int value) {
        this.writeRomInteractiveMotionBlock(value);
    }

    int accessRomInteractiveMotionBlockValue() {
        return this.romInteractiveMotionBlockValue;
    }

    int accessRomSlowWalkingSpeed() {
        return this.slowWalkingSpeed;
    }

    int accessRomItemUsageContext() {
        return this.itemUsageContext;
    }

    void accessBlockNextRomMotionFrame() {
        this.blockNextRomMotionFrame();
    }

    void accessInitializeRoomPhysicsForRoom(long loadToken, int mapId, int roomId, int mapCategory) {
        this.initializeRoomPhysicsForRoom(loadToken, mapId, roomId, mapCategory);
    }

    void accessSetRoomPhysicsForRoom(long loadToken, int mapId, int roomId, int mapCategory) {
        this.setRoomPhysicsForRoom(loadToken, mapId, roomId, mapCategory);
    }

    void accessSetRomPhysicsModifier(int modifier) {
        this.setRomPhysicsModifier(modifier);
    }

    ScreenShakeRequest accessConsumePegasusScreenShakeRequest() {
        return this.consumePegasusScreenShakeRequest();
    }

    void accessResetPegasusBoots() {
        this.resetPegasusBoots();
    }
}
