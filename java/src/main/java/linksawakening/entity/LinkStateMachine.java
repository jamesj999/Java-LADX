package linksawakening.entity;

import linksawakening.state.PlayerState;

/** ROM-faithful statemachine collaborator for Link. */
final class LinkStateMachine {
    private final Link link;
    int motionState = Link.LINK_MOTION_DEFAULT;
    int physicsModifier;
    int revolvingDoorFrame;
    boolean revolvingDoorTransitionPending;
    int revolvingDoorPaletteVariant = -1;
    boolean romInteractiveMotionBlocked;

    LinkStateMachine(Link link) { this.link = link; }

    void update() {
        link.bedJumpRenderZ = -1;
        link.frameCounter = (link.frameCounter + 1) & 0xFF;
        if (link.pegasusBootsCollisionCountdown > 0) {
            link.pegasusBootsCollisionCountdown--;
        }
        link.tickRomAttackStepAnimationCountdown();
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
        if (!interactiveMotionBlocked) {
            link.romAnimationStateOverride = -1;
            link.swordAcquisitionBaseRomDirection = -1;
            link.swordAcquisitionAnimationState = -1;
        }
        if (interactiveMotionBlocked && !(link.linkSideView.sideScrolling && link.airborne)) {
            link.zVelocity = 0;
            link.movingThisFrame = false;
            link.walkTickCounter = 0;
            link.walkFrame = 0;
            return;
        }

        // 6A94's side-view landing branch clears Link's link.airborne presentation
        // before the next normal motion update. Main keeps wC147 set through
        // this update and clears it only before the following entity tick.
        if (link.linkSideView.standingOnSideViewEntity && !link.linkSideView.sideScrolling) {
            link.airborne = false;
            link.zSubPixels = 0;
            link.zVelocity = 0;
            link.fallingIntoPit = false;
            link.groundStatus = Link.GROUND_STATUS_NORMAL;
        }

        if (link.debugNoClip) {
            updateDebugNoClip();
            return;
        }

        if (link.linkSideView.sideScrolling) {
            link.linkSideView.updateSideViewMotion(platformVerticalSpeedForThisUpdate);
            return;
        }

        if (link.likeLikeCaptured && link.likeLikeCapturePinsPosition) {
            link.linkMotion.subX = link.likeLikeCaptureSubX;
            link.linkMotion.subY = link.likeLikeCaptureSubY;
            link.airborne = false;
            link.zSubPixels = 0;
            link.zVelocity = 0;
            link.movingThisFrame = false;
            return;
        }

        if (link.fallingIntoPit) {
            link.tickPitFall();
            return;
        }

        if (link.overworldLedgeFallActive) {
            link.linkMotion.lastRomSpeedY = 0xFF;
            link.linkMotion.subY--;
            link.tickJump();
            return;
        }

        if (motionState == 0x05) {
            tickRevolvingDoor();
            return;
        }

        if (motionState == Link.LINK_MOTION_SWIMMING) {
            link.updateSwimming();
            return;
        }

        if (link.linkDamage.hasRecoil()) {
            link.linkDamage.tickRecoil();
            link.tickJump();
            return;
        }

        int mask = link.buildJoypadMask();
        int newDirection = Link.JOYPAD_TO_DIRECTION[mask];
        // $02:4406 skips facing selection while link.airborne; the Feather
        // launch still follows this frame's ordinary ground movement.
        if (newDirection != -1 && (!link.airborne || featherLaunchFrame)
            && !link.marinBedJumpMotionLocked
            && !link.directionalLedgeJumpMotionLocked
            && !link.itemsLockFacing(true) && !link.isLiftTransitionBlockingMotion()) {
            link.direction = newDirection;
        }
        updatePegasusBootsUse();
        if (!link.airborne && link.groundStatus != Link.GROUND_STATUS_PIT) {
            link.refreshGroundStatus();
            link.updateLastSafePositionIfPossible();
        }

        if (link.groundStatus == Link.GROUND_STATUS_PIT) {
            link.movingThisFrame = false;
            link.walkTickCounter = 0;
            link.walkFrame = 0;
            link.slipTowardPitCenter();
            return;
        }

        // Equipped items (sword swing, etc.) can block motion for a window of
        // frames. Mirrors the disassembly's hLinkInteractiveMotionBlocked flag
        // set by UpdateLinkAnimation while wSwordAnimationState != NONE. The
        // ROM still refreshes hLinkDirection from held single-axis input before
        // this early-out path, so link.direction update must happen above.
        // $02:43C3 bypasses the item motion gate while Link is above ground.
        if (link.itemsBlockMotion(true) && !link.airborne) {
            link.movingThisFrame = false;
            link.walkTickCounter = 0;
            link.walkFrame = 0;
            if (link.linkDamage.collisionIgnoreFramesRemaining > 0) {
                link.linkDamage.collisionIgnoreFramesRemaining--;
            }
            link.tickJump();
            return;
        }

        int speedX;
        int speedY;
        if (link.directionalLedgeJumpMotionLocked) {
            speedX = link.directionalLedgeJumpSpeedX;
            speedY = link.directionalLedgeJumpSpeedY;
        } else if (link.marinBedJumpMotionLocked) {
            speedX = 0x0C;
            speedY = 0;
        } else if (link.playerState != null && link.playerState.runningWithPegasusBoots()) {
            speedX = pegasusRunningSpeedX();
            speedY = pegasusRunningSpeedY();
        } else if (link.linkMotion.forcedSpeedPending) {
            speedX = link.linkMotion.forcedSpeedX;
            speedY = link.linkMotion.forcedSpeedXOnly
                ? (byte) link.romTables.linkSpeedY(mask) : link.linkMotion.forcedSpeedY;
            link.linkMotion.forcedSpeedPending = false;
            link.linkMotion.forcedSpeedXOnly = false;
        } else if (link.airborne && !featherLaunchFrame) {
            // $02:4402 skips the ground speed lookup while link.airborne.
            // ApplyLinkGroundMotion steers these retained speeds after XY.
            speedX = carriedSpeedX;
            speedY = carriedSpeedY;
        } else {
            speedX = (byte) link.romTables.linkSpeedX(mask);
            speedY = (byte) link.romTables.linkSpeedY(mask);
        }
        if (platformVerticalSpeedForThisUpdate >= 0) {
            speedY = (byte) platformVerticalSpeedForThisUpdate;
        }
        boolean bedJumpFrame = link.marinBedJumpMotionLocked;
        if (bedJumpFrame) {
            link.marinWakeUpBedVariant = -1;
            // Marin sets wIsLinkInTheAir=2, so UpdateLinkAnimation skips
            // the feather/ledge flip. Walking animation precedes gravity:
            // first pose $0A, then $0B, including the landing frame.
            link.romAnimationStateOverride = LinkAnimator.ANIMATION_STATE[link.direction][link.movingThisFrame ? 1 : 0];
            // ApplyGotItem caches wC145 before this frame changes Z.
            link.bedJumpRenderZ = link.zPixels();
            link.tickJump();
        }
        link.linkMotion.lastRomSpeedX = speedX & 0xFF;
        link.linkMotion.lastRomSpeedY = speedY & 0xFF;
        link.movingThisFrame = (speedX != 0 || speedY != 0);
        boolean applyGroundMotion = link.gelClinging
            ? (link.gelMotionFrame & 3) == 0 : link.airborne || link.shouldApplyGroundMotion();

        if (applyGroundMotion && speedX != 0 && (!bedJumpFrame || link.airborne)) {
            link.linkMotion.tryMoveAxis(speedX, true);
        }
        if (applyGroundMotion && speedY != 0 && (!bedJumpFrame || link.airborne)) {
            link.linkMotion.tryMoveAxis(speedY, false);
        }

        if (link.linkDamage.collisionIgnoreFramesRemaining > 0) {
            link.linkDamage.collisionIgnoreFramesRemaining--;
        }

        if (!bedJumpFrame) link.tickJump();
        handlePegasusBootsCollision();

        if (!link.airborne) {
            if (!link.enterSwimmingIfNeeded()) {
                link.updateLastSafePositionIfPossible();
            }
        }

        if (link.movingThisFrame) {
            link.walkTickCounter++;
            if (link.walkTickCounter >= Link.WALK_FRAME_TICKS) {
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
            link.romAnimationStateOverride = link.romTables.revolvingDoorAnimation(revolvingDoorFrame);
        }
        if (++revolvingDoorFrame >= 0x38) {
            link.linkMotion.subY = (-5 - 16) << Link.SUB_PIXEL_SHIFT;
            link.direction = Link.DIRECTION_UP;
            link.romAnimationStateOverride = 0x04;
        }
        if (revolvingDoorFrame == 0x48) {
            motionState = Link.LINK_MOTION_DEFAULT;
            revolvingDoorPaletteVariant = -1;
            revolvingDoorTransitionPending = true;
        }
    }

    void updateDebugNoClip() {
        int mask = link.buildJoypadMask();
        int newDirection = Link.JOYPAD_TO_DIRECTION[mask];
        if (newDirection != -1) {
            link.direction = newDirection;
        }
        int speedX = (byte) link.romTables.linkSpeedX(mask);
        int speedY = (byte) link.romTables.linkSpeedY(mask);
        link.linkMotion.lastRomSpeedX = speedX & 0xFF;
        link.linkMotion.lastRomSpeedY = speedY & 0xFF;
        link.movingThisFrame = speedX != 0 || speedY != 0;
        link.linkMotion.subX += speedX;
        link.linkMotion.subY += speedY;
        if (link.movingThisFrame) {
            link.walkTickCounter++;
            if (link.walkTickCounter >= Link.WALK_FRAME_TICKS) {
                link.walkTickCounter = 0;
                link.walkFrame ^= 1;
            }
        } else {
            link.walkTickCounter = 0;
            link.walkFrame = 0;
        }
    }

    void updatePegasusBootsUse() {
        if (link.playerState == null || link.inputState == null || link.inputConfig == null) {
            return;
        }
        boolean aHeld = link.playerState.itemA() == PlayerState.INVENTORY_PEGASUS_BOOTS
            && link.inputState.isDown(link.inputConfig.aKey());
        boolean bHeld = link.playerState.itemB() == PlayerState.INVENTORY_PEGASUS_BOOTS
            && link.inputState.isDown(link.inputConfig.bKey());
        if (!aHeld && !bHeld) {
            link.pegasusBootsChargeMeter = 0;
            return;
        }
        if (link.playerState.runningWithPegasusBoots()
            || romInteractiveMotionBlocked || link.airborne
            || link.zPixels() != 0 || link.groundStatus == Link.GROUND_STATUS_PIT
            || link.isCarryingLiftedObject() || motionState == Link.LINK_MOTION_SWIMMING) {
            return;
        }

        int heldBootsSlots = (aHeld ? 1 : 0) + (bHeld ? 1 : 0);
        for (int slot = 0; slot < heldBootsSlots; slot++) {
            if (link.pegasusBootsChargeMeter >= Link.PEGASUS_BOOTS_MAX_CHARGE) {
                break;
            }
            link.pegasusBootsChargeMeter++;
            if (link.pegasusBootsChargeMeter == Link.PEGASUS_BOOTS_MAX_CHARGE) {
                link.playerState.setRunningWithPegasusBoots(true);
                link.linkMotion.forcedSpeedPending = false;
                link.linkMotion.forcedSpeedXOnly = false;
                break;
            }
        }
    }

    int pegasusRunningSpeedX() {
        return switch (link.direction) {
            case Link.DIRECTION_LEFT -> -Link.PEGASUS_BOOTS_RUNNING_SPEED;
            case Link.DIRECTION_RIGHT -> Link.PEGASUS_BOOTS_RUNNING_SPEED;
            default -> 0;
        };
    }

    int pegasusRunningSpeedY() {
        return switch (link.direction) {
            case Link.DIRECTION_UP -> -Link.PEGASUS_BOOTS_RUNNING_SPEED;
            case Link.DIRECTION_DOWN -> Link.PEGASUS_BOOTS_RUNNING_SPEED;
            default -> 0;
        };
    }

    void handlePegasusBootsCollision() {
        if (link.playerState == null || !link.playerState.runningWithPegasusBoots()
            || (link.linkMotion.romCollisionType & 0x0F) == 0) {
            return;
        }

        int reflectedSpeedX = (-link.signedByte(link.linkMotion.lastRomSpeedX)) >> 2;
        int reflectedSpeedY = (-link.signedByte(link.linkMotion.lastRomSpeedY)) >> 2;
        link.resetPegasusBoots();
        link.linkMotion.forcedSpeedX = reflectedSpeedX;
        link.linkMotion.forcedSpeedY = reflectedSpeedY;
        link.linkMotion.forcedSpeedPending = true;
        link.linkMotion.lastRomSpeedX = reflectedSpeedX & 0xFF;
        link.linkMotion.lastRomSpeedY = reflectedSpeedY & 0xFF;
        link.airborne = true;
        link.zSubPixels = 0;
        link.zVelocity = Link.PEGASUS_BOOTS_COLLISION_VELOCITY_Z;

        int romDirection = link.romDirectionForJavaDirection(link.direction);
        int[] xOffsets = {0x10, 0xF0, 0x08, 0x08};
        int[] yOffsets = {0x0C, 0x0C, 0xF0, 0x10};
        link.pegasusBootsCollisionPosX = (link.romEntityX() + xOffsets[romDirection]) & 0xFF;
        link.pegasusBootsCollisionPosY = (link.romEntityY() + yOffsets[romDirection]) & 0xFF;
        link.pegasusBootsCollisionCountdown = Link.PEGASUS_BOOTS_COLLISION_COUNTDOWN;
        link.pendingPegasusScreenShakeCountdown = Link.PEGASUS_BOOTS_COLLISION_SHAKE;
        link.pendingPegasusScreenShakePhase = (romDirection & 0x02) << 1;
    }
}
