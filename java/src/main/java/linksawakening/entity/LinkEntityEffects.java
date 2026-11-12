package linksawakening.entity;

import static linksawakening.entity.Link.*;

import linksawakening.state.Direction;
import linksawakening.state.GroundStatus;
import linksawakening.state.LinkMotionState;

/** ROM-faithful entityeffects collaborator for Link. */
final class LinkEntityEffects {
    private final LinkComponentRef<LinkAirborne> linkAirborne;
    private final LinkComponentRef<LinkAnimationState> linkAnimationState;
    private final LinkComponentRef<LinkAnimator> linkAnimator;
    private final LinkComponentRef<LinkDamage> linkDamage;
    private final LinkComponentRef<LinkGroundMotion> linkGroundMotion;
    private final LinkComponentRef<LinkItemState> linkItemState;
    private final LinkComponentRef<LinkMotion> linkMotion;
    private final LinkComponentRef<LinkServices> linkServices;
    private final LinkComponentRef<LinkStateMachine> linkStateMachine;
    private final LinkComponentRef<LinkSwimming> linkSwimming;

    LinkEntityEffects(Dependencies dependencies) {
        linkAirborne = dependencies.linkAirborne;
        linkAnimationState = dependencies.linkAnimationState;
        linkAnimator = dependencies.linkAnimator;
        linkDamage = dependencies.linkDamage;
        linkGroundMotion = dependencies.linkGroundMotion;
        linkItemState = dependencies.linkItemState;
        linkMotion = dependencies.linkMotion;
        linkServices = dependencies.linkServices;
        linkStateMachine = dependencies.linkStateMachine;
        linkSwimming = dependencies.linkSwimming;
    }

    static final class Dependencies {
        LinkComponentRef<LinkAirborne> linkAirborne;
        LinkComponentRef<LinkAnimationState> linkAnimationState;
        LinkComponentRef<LinkAnimator> linkAnimator;
        LinkComponentRef<LinkDamage> linkDamage;
        LinkComponentRef<LinkGroundMotion> linkGroundMotion;
        LinkComponentRef<LinkItemState> linkItemState;
        LinkComponentRef<LinkMotion> linkMotion;
        LinkComponentRef<LinkServices> linkServices;
        LinkComponentRef<LinkStateMachine> linkStateMachine;
        LinkComponentRef<LinkSwimming> linkSwimming;
    }

    boolean likeLikeCaptured;
    boolean likeLikeCapturePinsPosition;
    int likeLikeCaptureSubX;
    int likeLikeCaptureSubY;
    int carryingLiftedObjectState;
    int carryingLiftedObjectRomDirection = 3;
    boolean roosterCarryActive;
    boolean grabbingObject;
    boolean pullingObject;
    /** Mirrors wC10A while Marin's scripted bed-exit jump owns Link motion. */
    boolean marinBedJumpMotionLocked;

    /** Applies Like Like's hLinkPositionX/Y and hidden animation state. */
    /** Hiding and shield theft precede the ROM's default-motion position gate. */
    void applyLikeLikeCapture(int romEntityX, int romEntityY) {
        applyLikeLikeCapture(romEntityX, romEntityY, true);
    }

    void applyLikeLikeCapture(int romEntityX, int romEntityY, boolean pinPosition) {
        if ((romEntityX & ~0xFF) != 0 || (romEntityY & ~0xFF) != 0) {
            throw new IllegalArgumentException("Like Like capture coordinates must be bytes");
        }
        likeLikeCaptured = true;
        likeLikeCapturePinsPosition = pinPosition;
        if (!pinPosition) {
            return;
        }
        likeLikeCaptureSubX = ((romEntityX - 0x08) & 0xFF) << Link.SUB_PIXEL_SHIFT;
        likeLikeCaptureSubY = ((romEntityY - 0x10) & 0xFF) << Link.SUB_PIXEL_SHIFT;
        linkMotion.get().subX = likeLikeCaptureSubX;
        linkMotion.get().subY = likeLikeCaptureSubY;
        linkAirborne.get().airborne = false;
        linkAirborne.get().zSubPixels = 0;
        linkAirborne.get().zVelocity = 0;
        linkAirborne.get().fallingIntoPit = false;
        linkAirborne.get().groundStatus = GroundStatus.NORMAL;
        linkStateMachine.get().motionState = LinkMotionState.DEFAULT;
        linkStateMachine.get().physicsModifier = 0;
        linkSwimming.get().swimmingSpeedX = 0;
        linkSwimming.get().swimmingSpeedY = 0;
        linkSwimming.get().swimmingFastCountdown = 0;
        linkSwimming.get().divingCountdown = 0;
        linkAnimationState.get().movingThisFrame = false;
        linkMotion.get().romCollisionType = 0;
        linkMotion.get().lastRomSpeedX = 0;
        linkMotion.get().lastRomSpeedY = 0;
        linkMotion.get().forcedSpeedPending = false;
        linkMotion.get().forcedSpeedXOnly = false;
    }

    /** Releases Link when the swallowed Like Like handler accepts A or B. */
    void releaseLikeLikeCapture() {
        likeLikeCaptured = false;
        likeLikeCapturePinsPosition = false;
    }

    /**
     * Applies wIsCarryingLiftedObject and the direction written by
     * EntityLiftedHandler. The direction argument uses the ROM order:
     * right, left, up, down.
     */
    void setCarryingLiftedObjectState(int carryState, int romDirection) {
        if ((carryState & ~0xFF) != 0) {
            throw new IllegalArgumentException("Carry state must be an unsigned byte: " + carryState);
        }
        if (romDirection < 0 || romDirection > 3) {
            throw new IllegalArgumentException("ROM linkAnimationState.get().direction must be between 0 and 3");
        }
        carryingLiftedObjectState = carryState;
        carryingLiftedObjectRomDirection = romDirection;
        if (carryState != 0) {
            linkAnimationState.get().direction = Direction.fromRom(romDirection).javaIndex();
        }
    }

    /** Applies the Link HRAM writes emitted by RoosterEntityHandler. */
    void applyRoosterFlightState(int positionZ, int velocityZ, int speedX, int speedY, int romDirection) {
        if ((positionZ & ~0xFF) != 0 || (velocityZ & ~0xFF) != 0) {
            throw new IllegalArgumentException("Rooster Link Z values must be unsigned bytes");
        }
        if (romDirection < 0 || romDirection > 3) {
            throw new IllegalArgumentException(
                    "Rooster ROM linkAnimationState.get().direction must be between 0 and 3");
        }
        linkStateMachine.get().motionState = LinkMotionState.DEFAULT;
        linkAirborne.get().airborne = true;
        linkAirborne.get().zSubPixels = (positionZ & 0xFF) << Link.SUB_PIXEL_SHIFT;
        linkAirborne.get().zVelocity = velocityZ & 0xFF;
        linkAirborne.get().groundStatus = GroundStatus.NORMAL;
        linkAirborne.get().fallingIntoPit = false;
        linkStateMachine.get().physicsModifier = 0;
        roosterCarryActive = true;
        linkAnimationState.get().direction = Direction.fromRom(romDirection).javaIndex();
        linkMotion.get().applyRomSpeed(speedX, speedY);
    }

    /** Applies Hinox's direct hLinkPosition/throw writes for the next motion frame. */
    /** Applies Hinox's held position; only the source's countdown sets blocking. */
    void applyHinoxGrabState(
            int romPositionX,
            int romPositionY,
            int positionZ,
            int speedX,
            int speedY,
            int velocityZ,
            int airborneState) {
        applyHinoxGrabState(
                romPositionX, romPositionY, positionZ, speedX, speedY, velocityZ, airborneState, true, true);
    }

    void applyHinoxGrabState(
            int romPositionX,
            int romPositionY,
            int positionZ,
            int speedX,
            int speedY,
            int velocityZ,
            int airborneState,
            boolean motionBlocked,
            boolean applyHeldLinkPose) {
        Link.validateRomByte(romPositionX, "Hinox Link X");
        Link.validateRomByte(romPositionY, "Hinox Link Y");
        Link.validateRomByte(positionZ, "Hinox Link Z");
        Link.validateRomByte(speedX, "Hinox Link X speed");
        Link.validateRomByte(speedY, "Hinox Link Y speed");
        Link.validateRomByte(velocityZ, "Hinox Link Z velocity");
        Link.validateRomByte(airborneState, "Hinox Link linkAirborne.get().airborne state");
        linkMotion.get().setPixelPosition((romPositionX - 0x08) & 0xFF, (romPositionY - 0x10) & 0xFF);
        linkStateMachine.get().romInteractiveMotionBlocked = motionBlocked;
        if (applyHeldLinkPose) {
            linkAnimator.get().romAnimationStateOverride = 0x6A;
        }
        linkAnimationState.get().movingThisFrame = false;
        if (applyHeldLinkPose) {
            linkAirborne.get().zSubPixels = (positionZ & 0xFF) << Link.SUB_PIXEL_SHIFT;
        }
        linkAirborne.get().zVelocity = velocityZ & 0xFF;
        linkAirborne.get().airborne = airborneState != 0;
        if (speedX != 0 || speedY != 0) {
            linkMotion.get().applyRomSpeed(speedX, speedY);
        }
    }

    /** Applies entity-$61 WarpState3Handler's direct Link writes each frame. */
    void applyWarpState3(int romPositionX, int visualRomPositionY, int immunityCountdown) {
        Link.validateRomByte(romPositionX, "Warp Link X");
        Link.validateRomByte(visualRomPositionY, "Warp Link visual Y");
        Link.validateRomByte(immunityCountdown, "Warp Link immunity");
        linkMotion.get().setPixelPosition((romPositionX - 0x08) & 0xFF, (visualRomPositionY - 0x10) & 0xFF);
        linkStateMachine.get().romInteractiveMotionBlocked = true;
        linkAnimator.get().romAnimationStateOverride = -1;
        linkItemState.get().romAttackStepAnimationCountdown = 0;
        linkDamage.get().collisionIgnoreFramesRemaining = 0;
        linkAnimationState.get().movingThisFrame = false;
        linkAirborne.get().airborne = false;
        linkAirborne.get().zSubPixels = 0;
        linkAirborne.get().zVelocity = 0;
        linkAirborne.get().fallingIntoPit = false;
        linkAirborne.get().groundStatus = GroundStatus.NORMAL;
        if (immunityCountdown != 0) {
            linkDamage.get().collisionDamageImmune = true;
        }
        if (linkServices.get().playerState != null) {
            linkServices.get().playerState.setInvincibilityCounter(0);
            linkServices.get().playerState.clearSubtractHealthBuffer();
        }
    }

    /** Applies Marin's final writes when Link jumps out of the bed. */
    void leaveMarinWakeUpBed() {
        linkStateMachine.get().motionState = LinkMotionState.DEFAULT;
        linkStateMachine.get().romInteractiveMotionBlocked = false;
        linkAnimator.get().romAnimationStateOverride = -1;
        // The entity handler has already drawn the bed on the launch frame.
        // Ordinary Link rendering takes over at the next physics linkGroundMotion.get().accessUpdate.
        linkAnimator.get().marinWakeUpBedVariant = 3;
        marinBedJumpMotionLocked = true;
        linkAnimationState.get().movingThisFrame = false;
        linkAirborne.get().airborne = true;
        linkAirborne.get().zSubPixels = 1 << Link.SUB_PIXEL_SHIFT;
        linkAirborne.get().zVelocity = 0x12;
        linkAirborne.get().groundStatus = GroundStatus.NORMAL;
        linkAirborne.get().fallingIntoPit = false;
        linkAnimationState.get().direction = Direction.RIGHT.javaIndex();
        linkMotion.get().lastRomSpeedX = 0x0C;
        linkMotion.get().lastRomSpeedY = 0;
        linkMotion.get().forcedSpeedPending = false;
        linkMotion.get().forcedSpeedXOnly = false;
    }

    /** Ends Rooster's custom linkAirborne.airborne carry state when it is thrown or cleared. */
    void clearRoosterCarryState() {
        roosterCarryActive = false;
        linkAirborne.get().airborne = false;
        linkAirborne.get().zSubPixels = 0;
        linkAirborne.get().zVelocity = 0;
        linkAirborne.get().groundStatus = GroundStatus.NORMAL;
        linkAirborne.get().fallingIntoPit = false;
        linkStateMachine.get().motionState = LinkMotionState.DEFAULT;
        linkStateMachine.get().physicsModifier = 0;
        linkMotion.get().forcedSpeedPending = false;
        linkMotion.get().forcedSpeedXOnly = false;
    }

    boolean accessIsLikeLikeCaptured() {
        return this.likeLikeCaptured;
    }

    boolean accessIsRoosterCarryActive() {
        return this.roosterCarryActive;
    }

    int accessCarryingLiftedObjectState() {
        return this.carryingLiftedObjectState;
    }

    boolean accessIsCarryingLiftedObject() {
        return this.carryingLiftedObjectState != 0 || this.roosterCarryActive;
    }

    boolean accessIsLiftTransitionBlockingMotion() {
        return this.carryingLiftedObjectState >= 2 && !this.roosterCarryActive;
    }

    void accessApplyLikeLikeCapture(int romEntityX, int romEntityY) {
        this.applyLikeLikeCapture(romEntityX, romEntityY);
    }

    void accessApplyLikeLikeCapture(int romEntityX, int romEntityY, boolean pinPosition) {
        this.applyLikeLikeCapture(romEntityX, romEntityY, pinPosition);
    }

    void accessReleaseLikeLikeCapture() {
        this.releaseLikeLikeCapture();
    }

    void accessSetCarryingLiftedObjectState(int carryState, int romDirection) {
        this.setCarryingLiftedObjectState(carryState, romDirection);
    }

    void accessApplyRoosterFlightState(int positionZ, int velocityZ, int speedX, int speedY, int romDirection) {
        this.applyRoosterFlightState(positionZ, velocityZ, speedX, speedY, romDirection);
    }

    void accessApplyHinoxGrabState(
            int romPositionX,
            int romPositionY,
            int positionZ,
            int speedX,
            int speedY,
            int velocityZ,
            int airborneState) {
        this.applyHinoxGrabState(romPositionX, romPositionY, positionZ, speedX, speedY, velocityZ, airborneState);
    }

    void accessApplyHinoxGrabState(
            int romPositionX,
            int romPositionY,
            int positionZ,
            int speedX,
            int speedY,
            int velocityZ,
            int airborneState,
            boolean motionBlocked,
            boolean applyHeldLinkPose) {
        this.applyHinoxGrabState(
                romPositionX,
                romPositionY,
                positionZ,
                speedX,
                speedY,
                velocityZ,
                airborneState,
                motionBlocked,
                applyHeldLinkPose);
    }

    void accessApplyWarpState3(int romPositionX, int visualRomPositionY, int immunityCountdown) {
        this.applyWarpState3(romPositionX, visualRomPositionY, immunityCountdown);
    }

    void accessLeaveMarinWakeUpBed() {
        this.leaveMarinWakeUpBed();
    }

    void accessClearRoosterCarryState() {
        this.clearRoosterCarryState();
    }

    void accessSetGrabbingObject(boolean grabbing, boolean pulling) {
        this.grabbingObject = grabbing;
        this.pullingObject = grabbing && pulling;
    }
}
