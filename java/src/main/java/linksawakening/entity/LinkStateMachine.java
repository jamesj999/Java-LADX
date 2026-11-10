package linksawakening.entity;

import linksawakening.state.PlayerState;
import static linksawakening.entity.Link.*;
import static linksawakening.entity.LinkRomConstants.*;

final class LinkStateMachine {
    private final Link link;
    int motionState = Link.LINK_MOTION_DEFAULT;
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

    LinkStateMachine(Link link) { this.link = link; }

    void update() {
        link.linkAnimator.bedJumpRenderZ = -1;
        frameCounter = (frameCounter + 1) & 0xFF;
        if (pegasusBootsCollisionCountdown > 0) {
            pegasusBootsCollisionCountdown--;
        }
        link.linkEquipment.tickRomAttackStepAnimationCountdown();
        int carriedSpeedX = (byte) link.linkMotion.lastRomSpeedX;
        int carriedSpeedY = (byte) link.linkMotion.lastRomSpeedY;
        boolean featherLaunchFrame = link.topDownFeatherStarted;
        link.topDownFeatherStarted = false;
        link.linkMotion.romCollisionType = 0;
        link.linkMotion.lastRomSpeedX = 0;
        link.linkMotion.lastRomSpeedY = 0;
        int platformVerticalSpeedForThisUpdate = link.linkSideView.sideViewPlatformVerticalSpeedPending;
        link.linkSideView.sideViewPlatformVerticalSpeedPending = -1;
        link.linkDamage.tickInvincibility();

        boolean interactiveMotionBlocked = romInteractiveMotionBlocked;
        romInteractiveMotionBlocked = false;
        romInteractiveMotionBlockValue = 0;
        if (!interactiveMotionBlocked) {
            link.linkAnimator.romAnimationStateOverride = -1;
            link.linkAnimator.swordAcquisitionBaseRomDirection = -1;
            link.linkAnimator.swordAcquisitionAnimationState = -1;
        }
        if (interactiveMotionBlocked && !(link.linkSideView.sideScrolling && link.linkAirborne.airborne)) {
            link.linkAirborne.zVelocity = 0;
            link.movingThisFrame = false;
            link.walkTickCounter = 0;
            link.walkFrame = 0;
            return;
        }

        // 6A94's side-view landing branch clears Link's link.linkAirborne.airborne presentation
        // before the next normal motion update. Main keeps wC147 set through
        // this update and clears it only before the following entity tick.
        if (link.linkSideView.standingOnSideViewEntity && !link.linkSideView.sideScrolling) {
            link.linkAirborne.airborne = false;
            link.linkAirborne.zSubPixels = 0;
            link.linkAirborne.zVelocity = 0;
            link.linkAirborne.fallingIntoPit = false;
            link.linkAirborne.groundStatus = Link.GROUND_STATUS_NORMAL;
        }

        if (debugNoClip) {
            updateDebugNoClip();
            return;
        }

        if (motionState == Link.LINK_MOTION_RECOVER) {
            link.linkSwimming.updateRecover();
            return;
        }

        if (link.linkSideView.sideScrolling) {
            link.linkSideView.updateSideViewMotion(platformVerticalSpeedForThisUpdate);
            return;
        }

        if (link.linkEntityEffects.likeLikeCaptured && link.linkEntityEffects.likeLikeCapturePinsPosition) {
            link.linkMotion.subX = link.linkEntityEffects.likeLikeCaptureSubX;
            link.linkMotion.subY = link.linkEntityEffects.likeLikeCaptureSubY;
            link.linkAirborne.airborne = false;
            link.linkAirborne.zSubPixels = 0;
            link.linkAirborne.zVelocity = 0;
            link.movingThisFrame = false;
            return;
        }

        if (link.pitWarpPending) return;

        if (link.linkAirborne.fallingIntoPit) {
            link.linkAirborne.tickPitFall();
            return;
        }

        if (link.linkAirborne.overworldLedgeFallActive) {
            link.linkMotion.lastRomSpeedY = 0xFF;
            link.linkMotion.subY--;
            link.linkAirborne.tickJump();
            return;
        }

        if (motionState == 0x05) {
            tickRevolvingDoor();
            return;
        }

        if (motionState == Link.LINK_MOTION_SWIMMING) {
            link.linkSwimming.updateSwimming();
            return;
        }

        if (link.linkDamage.hasRecoil()) {
            link.linkDamage.tickRecoil();
            link.linkAirborne.tickJump();
            return;
        }

        int mask = link.buildJoypadMask();
        int newDirection = LinkRomConstants.JOYPAD_TO_DIRECTION[mask];
        // $02:4406 skips facing selection while link.linkAirborne.airborne; the Feather
        // launch still follows this frame's ordinary ground movement.
        if (newDirection != -1 && (!link.linkAirborne.airborne || featherLaunchFrame)
            && !link.linkEntityEffects.marinBedJumpMotionLocked
            && !link.linkAirborne.directionalLedgeJumpMotionLocked
            && !link.linkEquipment.itemsLockFacing(true) && !link.isLiftTransitionBlockingMotion()) {
            link.direction = newDirection;
        }
        updatePegasusBootsUse();
        if (!link.linkAirborne.airborne && link.linkAirborne.groundStatus != Link.GROUND_STATUS_PIT) {
            link.linkAirborne.refreshGroundStatus();
            link.linkAirborne.updateLastSafePositionIfPossible();
        }

        if (link.linkAirborne.groundStatus == Link.GROUND_STATUS_PIT) {
            link.movingThisFrame = false;
            link.walkTickCounter = 0;
            link.walkFrame = 0;
            link.linkAirborne.slipTowardPitCenter();
            return;
        }

        // Equipped items (sword swing, etc.) can block motion for a window of
        // frames. Mirrors the disassembly's hLinkInteractiveMotionBlocked flag
        // set by UpdateLinkAnimation while wSwordAnimationState != NONE. The
        // ROM still refreshes hLinkDirection from held single-axis input before
        // this early-out path, so link.direction update must happen above.
        // $02:43C3 bypasses the item motion gate while Link is above ground.
        if (link.linkEquipment.itemsBlockMotion(true) && !link.linkAirborne.airborne) {
            link.movingThisFrame = false;
            link.walkTickCounter = 0;
            link.walkFrame = 0;
            if (link.linkDamage.collisionIgnoreFramesRemaining > 0) {
                link.linkDamage.collisionIgnoreFramesRemaining--;
            }
            link.linkAirborne.tickJump();
            return;
        }

        // Source $43E6/$4440/$445F/$4503. The grass OAM flip depends on
        // this byte, including steps suppressed by slow ground or a wall.
        if (link.linkAirborne.airborne) link.romConsecutiveStepsCount = 0xFF;
        else if (link.playerState != null && link.playerState.runningWithPegasusBoots())
            link.romConsecutiveStepsCount = (link.romConsecutiveStepsCount + 2) & 0xFF;
        else if (mask != 0) link.romConsecutiveStepsCount = (link.romConsecutiveStepsCount + 1) & 0xFF;
        else if (pegasusBootsChargeMeter == 0) link.romConsecutiveStepsCount = 7;

        int speedX;
        int speedY;
        if (link.linkAirborne.directionalLedgeJumpMotionLocked) {
            speedX = link.linkAirborne.directionalLedgeJumpSpeedX;
            speedY = link.linkAirborne.directionalLedgeJumpSpeedY;
        } else if (link.linkEntityEffects.marinBedJumpMotionLocked) {
            speedX = 0x0C;
            speedY = 0;
        } else if (link.playerState != null && link.playerState.runningWithPegasusBoots()) {
            speedX = pegasusRunningSpeedX();
            speedY = pegasusRunningSpeedY();
        } else if (link.linkMotion.forcedSpeedPending) {
            speedX = link.linkMotion.forcedSpeedX;
            speedY = link.linkMotion.forcedSpeedXOnly
                ? (byte) link.romTables.linkSpeedY(groundSpeedIndex(mask)) : link.linkMotion.forcedSpeedY;
            link.linkMotion.forcedSpeedPending = false;
            link.linkMotion.forcedSpeedXOnly = false;
        } else if (link.linkAirborne.airborne && !featherLaunchFrame) {
            // $02:4402 skips the ground speed lookup while link.linkAirborne.airborne.
            // ApplyLinkGroundMotion steers these retained speeds after XY.
            speedX = carriedSpeedX;
            speedY = carriedSpeedY;
        } else {
            speedX = (byte) link.romTables.linkSpeedX(groundSpeedIndex(mask));
            speedY = (byte) link.romTables.linkSpeedY(groundSpeedIndex(mask));
        }
        if (platformVerticalSpeedForThisUpdate >= 0) {
            speedY = (byte) platformVerticalSpeedForThisUpdate;
        }
        boolean bedJumpFrame = link.linkEntityEffects.marinBedJumpMotionLocked;
        if (bedJumpFrame) {
            link.linkAnimator.marinWakeUpBedVariant = -1;
            // Marin sets wIsLinkInTheAir=2, so UpdateLinkAnimation skips
            // the feather/ledge flip. Walking animation precedes gravity:
            // first pose $0A, then $0B, including the landing frame.
            link.linkAnimator.romAnimationStateOverride = LinkAnimator.ANIMATION_STATE[link.direction][link.movingThisFrame ? 1 : 0];
            // ApplyGotItem caches wC145 before this frame changes Z.
            link.linkAnimator.bedJumpRenderZ = link.zPixels();
            link.linkAirborne.tickJump();
        }
        link.linkMotion.lastRomSpeedX = speedX & 0xFF;
        link.linkMotion.lastRomSpeedY = speedY & 0xFF;
        link.movingThisFrame = (speedX != 0 || speedY != 0);
        boolean applyGroundMotion = gelClinging
            ? (gelMotionFrame & 3) == 0 : link.linkAirborne.airborne || link.shouldApplyGroundMotion();

        if (applyGroundMotion && speedX != 0 && (!bedJumpFrame || link.linkAirborne.airborne)) {
            link.linkMotion.tryMoveAxis(speedX, true);
        }
        if (applyGroundMotion && speedY != 0 && (!bedJumpFrame || link.linkAirborne.airborne)) {
            link.linkMotion.tryMoveAxis(speedY, false);
        }

        if (link.linkDamage.collisionIgnoreFramesRemaining > 0) {
            link.linkDamage.collisionIgnoreFramesRemaining--;
        }

        if (!bedJumpFrame) link.linkAirborne.tickJump();
        handlePegasusBootsCollision();

        if (!link.linkAirborne.airborne) {
            if (!link.linkSwimming.enterSwimmingIfNeeded()) {
                link.linkAirborne.updateLastSafePositionIfPossible();
            }
        }

        if (link.movingThisFrame) {
            link.walkTickCounter++;
            if (link.walkTickCounter >= LinkRomConstants.WALK_FRAME_TICKS) {
                link.walkTickCounter = 0;
                link.walkFrame ^= 1;
            }
        } else {
            link.walkTickCounter = 0;
            link.walkFrame = 0;
        }
    }

    void tickRevolvingDoor() {
        link.linkMotion.subX = (0x50 - 8) << Link.SUB_PIXEL_SHIFT;
        link.linkMotion.subY = 0;
        link.movingThisFrame = false;
        if (revolvingDoorFrame == 0 || revolvingDoorFrame == 0x28) revolvingDoorPaletteVariant = 0;
        if (revolvingDoorFrame == 0x18) revolvingDoorPaletteVariant = 1;
        if (revolvingDoorFrame < 0x38) {
            link.linkAnimator.romAnimationStateOverride = link.romTables.revolvingDoorAnimation(revolvingDoorFrame);
        }
        if (++revolvingDoorFrame >= 0x38) {
            link.linkMotion.subY = (-5 - 16) << Link.SUB_PIXEL_SHIFT;
            link.direction = Link.DIRECTION_UP;
            link.linkAnimator.romAnimationStateOverride = 0x04;
        }
        if (revolvingDoorFrame == 0x48) {
            motionState = Link.LINK_MOTION_DEFAULT;
            revolvingDoorPaletteVariant = -1;
            revolvingDoorTransitionPending = true;
        }
    }

    // $02:4409-4427 selects the second row only for Piece of Power.
    private int groundSpeedIndex(int mask) {
        return (mask & 0x0F) | (link.playerState != null
            && link.playerState.activePowerUp() == 1 ? 0x10 : 0);
    }

    void updateDebugNoClip() {
        int mask = link.buildJoypadMask();
        int newDirection = LinkRomConstants.JOYPAD_TO_DIRECTION[mask];
        if (newDirection != -1) {
            link.direction = newDirection;
        }
        int speedX = (byte) link.romTables.linkSpeedX(groundSpeedIndex(mask));
        int speedY = (byte) link.romTables.linkSpeedY(groundSpeedIndex(mask));
        link.linkMotion.lastRomSpeedX = speedX & 0xFF;
        link.linkMotion.lastRomSpeedY = speedY & 0xFF;
        link.movingThisFrame = speedX != 0 || speedY != 0;
        link.linkMotion.subX += speedX;
        link.linkMotion.subY += speedY;
        if (link.movingThisFrame) {
            link.walkTickCounter++;
            if (link.walkTickCounter >= LinkRomConstants.WALK_FRAME_TICKS) {
                link.walkTickCounter = 0;
                link.walkFrame ^= 1;
            }
        } else {
            link.walkTickCounter = 0;
            link.walkFrame = 0;
        }
    }

    /** Mirrors CheckItemsToUse's two Pegasus-Boots slot branches and UsePegasusBoots. */
    void updatePegasusBootsUse() {
        if (link.playerState == null || link.inputState == null || link.inputConfig == null) {
            return;
        }
        boolean aHeld = link.playerState.itemA() == PlayerState.INVENTORY_PEGASUS_BOOTS
            && link.inputState.isDown(link.inputConfig.aKey());
        boolean bHeld = link.playerState.itemB() == PlayerState.INVENTORY_PEGASUS_BOOTS
            && link.inputState.isDown(link.inputConfig.bKey());
        if (!aHeld && !bHeld) {
            pegasusBootsChargeMeter = 0;
            return;
        }
        if (link.playerState.runningWithPegasusBoots()
            || romInteractiveMotionBlocked || link.linkAirborne.airborne
            || link.zPixels() != 0 || link.linkAirborne.groundStatus == Link.GROUND_STATUS_PIT
            || link.isCarryingLiftedObject() || motionState == Link.LINK_MOTION_SWIMMING) {
            return;
        }

        int heldBootsSlots = (aHeld ? 1 : 0) + (bHeld ? 1 : 0);
        for (int slot = 0; slot < heldBootsSlots; slot++) {
            if (pegasusBootsChargeMeter >= LinkRomConstants.PEGASUS_BOOTS_MAX_CHARGE) {
                break;
            }
            pegasusBootsChargeMeter++;
            if (pegasusBootsChargeMeter == LinkRomConstants.PEGASUS_BOOTS_MAX_CHARGE) {
                link.playerState.setRunningWithPegasusBoots(true);
                link.linkMotion.forcedSpeedPending = false;
                link.linkMotion.forcedSpeedXOnly = false;
                break;
            }
        }
    }

    int pegasusRunningSpeedX() {
        return switch (link.direction) {
            case Link.DIRECTION_LEFT -> -LinkRomConstants.PEGASUS_BOOTS_RUNNING_SPEED;
            case Link.DIRECTION_RIGHT -> LinkRomConstants.PEGASUS_BOOTS_RUNNING_SPEED;
            default -> 0;
        };
    }

    int pegasusRunningSpeedY() {
        return switch (link.direction) {
            case Link.DIRECTION_UP -> -LinkRomConstants.PEGASUS_BOOTS_RUNNING_SPEED;
            case Link.DIRECTION_DOWN -> LinkRomConstants.PEGASUS_BOOTS_RUNNING_SPEED;
            default -> 0;
        };
    }

    /** Mirrors bank2.asm:74AD's dash collision response and func_020_49BA. */
    void handlePegasusBootsCollision() {
        if (link.playerState == null || !link.playerState.runningWithPegasusBoots()
            || (link.linkMotion.romCollisionType & 0x0F) == 0) {
            return;
        }

        int reflectedSpeedX = (-Link.signedByte(link.linkMotion.lastRomSpeedX)) >> 2;
        int reflectedSpeedY = (-Link.signedByte(link.linkMotion.lastRomSpeedY)) >> 2;
        link.resetPegasusBoots();
        link.linkMotion.forcedSpeedX = reflectedSpeedX;
        link.linkMotion.forcedSpeedY = reflectedSpeedY;
        link.linkMotion.forcedSpeedPending = true;
        link.linkMotion.lastRomSpeedX = reflectedSpeedX & 0xFF;
        link.linkMotion.lastRomSpeedY = reflectedSpeedY & 0xFF;
        link.linkAirborne.airborne = true;
        link.linkAirborne.zSubPixels = 0;
        link.linkAirborne.zVelocity = LinkRomConstants.PEGASUS_BOOTS_COLLISION_VELOCITY_Z;

        int romDirection = Link.romDirectionForJavaDirection(link.direction);
        int[] xOffsets = {0x10, 0xF0, 0x08, 0x08};
        int[] yOffsets = {0x0C, 0x0C, 0xF0, 0x10};
        pegasusBootsCollisionPosX = (link.linkMotion.romEntityX() + xOffsets[romDirection]) & 0xFF;
        pegasusBootsCollisionPosY = (link.linkMotion.romEntityY() + yOffsets[romDirection]) & 0xFF;
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
            link.linkAirborne.groundStatus = Link.GROUND_STATUS_NORMAL;
            link.linkAirborne.fallingIntoPit = false;
            link.linkAirborne.airborne = false;
            link.linkAirborne.zSubPixels = 0;
            link.linkAirborne.zVelocity = 0;
        }
    }

    /** Clears scripted presentation/capture state before a debug game reset. */
    void resetTransientStateForDebug() {
        debugNoClip = false;
        link.clearGroundEffects();
        link.romConsecutiveStepsCount = 7;
        romInteractiveMotionBlocked = false;
        romInteractiveMotionBlockValue = 0;
        link.linkSideView.standingOnSideViewEntity = false;
        link.linkSideView.sideViewPlatformVerticalSpeedPending = -1;
        link.linkDamage.collisionDamageImmune = false;
        link.linkAnimator.romAnimationStateOverride = -1;
        link.linkAnimator.swordAcquisitionBaseRomDirection = -1;
        link.linkAnimator.swordAcquisitionAnimationState = -1;
        link.linkAnimator.tarinShieldPresentation = false;
        link.linkAnimator.tarinShieldPalette = null;
        link.linkAnimator.marinWakeUpBedVariant = -1;
        link.linkEntityEffects.marinBedJumpMotionLocked = false;
        link.linkAnimator.bedJumpRenderZ = -1;
        link.linkAirborne.directionalLedgeJumpMotionLocked = false;
        link.linkAirborne.overworldLedgeFallActive = false;
        link.linkEntityEffects.likeLikeCaptured = false;
        link.linkEntityEffects.likeLikeCapturePinsPosition = false;
        link.linkAirborne.fallingIntoPit = false;
        link.linkEntityEffects.carryingLiftedObjectState = 0;
        link.topDownFeatherStarted = false;
        link.linkEntityEffects.roosterCarryActive = false;
        link.linkAirborne.airborne = false;
        link.linkAirborne.groundStatus = Link.GROUND_STATUS_NORMAL;
        motionState = Link.LINK_MOTION_DEFAULT;
        revolvingDoorFrame = 0;
        revolvingDoorTransitionPending = false;
        revolvingDoorPaletteVariant = -1;
        physicsModifier = 0;
        recoverCountdown = 0;
        recoveringInWater = false;
        link.pendingWaterSplash = null;
        link.pitWarpPending = false;
        physicsRoomLoadToken = -1;
        physicsRoomMapId = -1;
        physicsRoomId = -1;
        physicsRoomCategory = -1;
        link.linkAirborne.zSubPixels = 0;
        link.linkAirborne.zVelocity = 0;
        link.linkMotion.forcedSpeedPending = false;
        link.linkMotion.forcedSpeedXOnly = false;
        link.linkDamage.collisionIgnoreFramesRemaining = 0;
        link.romLinkPushing = 0;
        link.romAttackStepAnimationCountdown = 0;
        link.movingThisFrame = false;
        link.walkTickCounter = 0;
        link.walkFrame = 0;
    }

    /** ApplyRoomTransition's completion path ($02:793D-$7977). */
    void finishRoomScroll(boolean downward) {
        link.linkMotion.lastRomSpeedX = 0;
        link.linkMotion.lastRomSpeedY = 0;
        link.linkAirborne.zVelocity = 0;
        if (!downward || debugNoClip || link.collision == null) {
            return;
        }
        // Run the ordinary downward link.collision handler, including its ledge
        // half-tile filters, before applying the source's fallback.
        int probeX = link.linkMotion.pixelX();
        int probeY = link.linkMotion.pixelY();
        link.linkMotion.lastRomSpeedY = 0x01;
        boolean blocked = link.linkMotion.leadingEdgeBlocked(probeX, probeY, Link.DIRECTION_DOWN);
        int object = link.collision.objectIdAtPoint(
            probeX + COLLISION_POINTS_X[Link.DIRECTION_DOWN][1],
            probeY + COLLISION_POINTS_Y[Link.DIRECTION_DOWN][1]);
        // Switch blocks $DB/$DC are exempt from fallback unstucking.
        if (object != 0xDB && object != 0xDC && !link.linkAirborne.overworldLedgeFallActive
            && (object == 0xE1 || blocked)) {
            // func_002_6EAD aligns X and enters LINK_MOTION_UNSTUCKING.
            // This transition fallback does not play the ledge jingle.
            link.linkMotion.subX = (link.linkMotion.romEntityX() & 0xF0) << Link.SUB_PIXEL_SHIFT;
            link.linkMotion.subY -= 8 << Link.SUB_PIXEL_SHIFT;
            motionState = 0x02;
            physicsModifier = 0;
            link.linkAirborne.overworldLedgeFallActive = true;
            link.linkMotion.beginOverworldLedgeUnstuckingMotion();
        }
    }

    /**
     * Applies the source room-entry default once per loaded room. The load
     * serial also distinguishes a save/new-game reload of the same room, so a
     * prior ladder or diving modifier cannot leak across the reload.
     */
    void initializeRoomPhysicsForRoom(long loadToken, int mapId, int roomId,
                                             int mapCategory) {
        if (physicsRoomLoadToken == loadToken
            && physicsRoomMapId == mapId
            && physicsRoomId == roomId
            && physicsRoomCategory == mapCategory) {
            return;
        }
        link.clearGroundEffects();
        physicsModifier = mapCategory == 2 ? 0x01 : 0x00;
        physicsRoomLoadToken = loadToken;
        physicsRoomMapId = mapId;
        physicsRoomId = roomId;
        physicsRoomCategory = mapCategory;
        link.linkDamage.collisionDamageImmune = false;
    }

    /** Applies the room-entry physics default after a completed warp. */
    void setRoomPhysicsForRoom(long loadToken, int mapId, int roomId,
                                      int mapCategory) {
        link.clearGroundEffects();
        physicsModifier = mapCategory == 2 ? 0x01 : 0x00;
        physicsRoomLoadToken = loadToken;
        physicsRoomMapId = mapId;
        physicsRoomId = roomId;
        physicsRoomCategory = mapCategory;
        link.linkDamage.collisionDamageImmune = false;
    }

    /** Mirrors source ladder/diving updates to hLinkPhysicsModifier. */
    void setRomPhysicsModifier(int modifier) {
        physicsModifier = modifier & 0xFF;
    }

    /** Applies the ROM's next-frame hLinkInteractiveMotionBlocked=$02 write. */
    void blockNextRomMotionFrame() { writeRomInteractiveMotionBlock(2); }

    void writeRomInteractiveMotionBlock(int value) {
        if ((value & ~255) != 0) throw new IllegalArgumentException("Interaction block must be an unsigned byte");
        // Keep the existing movement lock separate from the exact ROM equipment gate.
        romInteractiveMotionBlockValue = value;
        romInteractiveMotionBlocked = value != 0;
    }

    /** Mirrors ResetPegasusBoots for handlers that interrupt a dash. */
    void resetPegasusBoots() {
        pegasusBootsChargeMeter = 0;
        if (link.playerState != null) {
            link.playerState.setRunningWithPegasusBoots(false);
        }
    }

    /** Returns and clears the one-shot shake request written this frame. */
    ScreenShakeRequest consumePegasusScreenShakeRequest() {
        ScreenShakeRequest request = new ScreenShakeRequest(
            pendingPegasusScreenShakeCountdown, pendingPegasusScreenShakePhase);
        pendingPegasusScreenShakeCountdown = 0;
        pendingPegasusScreenShakePhase = 0;
        return request.countdown() == 0 ? null : request;
    }
}
