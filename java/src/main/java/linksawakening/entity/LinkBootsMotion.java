package linksawakening.entity;

import static linksawakening.entity.Link.*;
import static linksawakening.entity.LinkRomConstants.*;
import static linksawakening.entity.LinkStateMachine.*;

import linksawakening.state.Direction;
import linksawakening.state.GroundStatus;
import linksawakening.state.LinkMotionState;

/** LinkBootsMotion behavior on the shared LinkStateMachine state. */
abstract class LinkBootsMotion extends LinkRoomMotion {
    protected LinkBootsMotion(Dependencies dependencies) {
        super(dependencies);
    }

    /** Clears scripted presentation/capture state before a debug game reset. */
    protected void resetTransientStateForDebug() {
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
    protected void finishRoomScroll(boolean downward) {
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
    protected void initializeRoomPhysicsForRoom(long loadToken, int mapId, int roomId, int mapCategory) {
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
    protected void setRoomPhysicsForRoom(long loadToken, int mapId, int roomId, int mapCategory) {
        linkGroundMotion.get().accessClearGroundEffects();
        physicsModifier = mapCategory == 2 ? 0x01 : 0x00;
        physicsRoomLoadToken = loadToken;
        physicsRoomMapId = mapId;
        physicsRoomId = roomId;
        physicsRoomCategory = mapCategory;
        linkDamage.get().collisionDamageImmune = false;
    }

    /** Mirrors source ladder/diving updates to hLinkPhysicsModifier. */
    protected void setRomPhysicsModifier(int modifier) {
        physicsModifier = modifier & 0xFF;
    }

    /** Applies the ROM's next-frame hLinkInteractiveMotionBlocked=$02 write. */
    protected void blockNextRomMotionFrame() {
        writeRomInteractiveMotionBlock(2);
    }

    protected void writeRomInteractiveMotionBlock(int value) {
        if ((value & ~255) != 0) throw new IllegalArgumentException("Interaction block must be an unsigned byte");
        // Keep the existing movement lock separate from the exact ROM equipment gate.
        romInteractiveMotionBlockValue = value;
        romInteractiveMotionBlocked = value != 0;
    }

    /** Mirrors ResetPegasusBoots for handlers that interrupt a dash. */
    protected void resetPegasusBoots() {
        pegasusBootsChargeMeter = 0;
        if (linkServices.get().playerState != null) {
            linkServices.get().playerState.setRunningWithPegasusBoots(false);
        }
    }

    /** Returns and clears the one-shot shake request written this frame. */
    protected ScreenShakeRequest consumePegasusScreenShakeRequest() {
        ScreenShakeRequest request =
                new ScreenShakeRequest(pendingPegasusScreenShakeCountdown, pendingPegasusScreenShakePhase);
        pendingPegasusScreenShakeCountdown = 0;
        pendingPegasusScreenShakePhase = 0;
        return request.countdown() == 0 ? null : request;
    }

    protected boolean accessDebugNoClip() {
        return this.debugNoClip;
    }

    protected boolean accessIsSwimming() {
        return this.motionState == LinkMotionState.SWIMMING;
    }

    protected boolean accessIsDiving() {
        return accessIsSwimming() && this.physicsModifier != 0;
    }

    protected int accessRomPhysicsModifier() {
        return this.physicsModifier & 0xFF;
    }

    protected int accessPegasusBootsChargeMeter() {
        return this.pegasusBootsChargeMeter & 0xFF;
    }

    protected int accessPegasusBootsCollisionCountdown() {
        return this.pegasusBootsCollisionCountdown & 0xFF;
    }

    protected int accessPegasusBootsCollisionPosX() {
        return this.pegasusBootsCollisionPosX & 0xFF;
    }

    protected int accessPegasusBootsCollisionPosY() {
        return this.pegasusBootsCollisionPosY & 0xFF;
    }
}
