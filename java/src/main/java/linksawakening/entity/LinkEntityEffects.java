package linksawakening.entity;

import linksawakening.world.transition.Warp;

import static linksawakening.entity.Link.*;

/** ROM-faithful entityeffects collaborator for Link. */
final class LinkEntityEffects {
    private final Link link;
    boolean likeLikeCaptured;
    boolean likeLikeCapturePinsPosition;
    int likeLikeCaptureSubX;
    int likeLikeCaptureSubY;
    int carryingLiftedObjectState;
    int carryingLiftedObjectRomDirection = 3;
    boolean roosterCarryActive;
    /** Mirrors wC10A while Marin's scripted bed-exit jump owns Link motion. */
    boolean marinBedJumpMotionLocked;

    LinkEntityEffects(Link link) { this.link = link; }

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
        link.linkMotion.subX = likeLikeCaptureSubX;
        link.linkMotion.subY = likeLikeCaptureSubY;
        link.linkAirborne.airborne = false;
        link.linkAirborne.zSubPixels = 0;
        link.linkAirborne.zVelocity = 0;
        link.linkAirborne.fallingIntoPit = false;
        link.linkAirborne.groundStatus = Link.GROUND_STATUS_NORMAL;
        link.linkStateMachine.motionState = Link.LINK_MOTION_DEFAULT;
        link.linkStateMachine.physicsModifier = 0;
        link.linkSwimming.swimmingSpeedX = 0;
        link.linkSwimming.swimmingSpeedY = 0;
        link.linkSwimming.swimmingFastCountdown = 0;
        link.linkSwimming.divingCountdown = 0;
        link.movingThisFrame = false;
        link.linkMotion.romCollisionType = 0;
        link.linkMotion.lastRomSpeedX = 0;
        link.linkMotion.lastRomSpeedY = 0;
        link.linkMotion.forcedSpeedPending = false;
        link.linkMotion.forcedSpeedXOnly = false;
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
            throw new IllegalArgumentException("Carry state must be an unsigned byte: "
                + carryState);
        }
        if (romDirection < 0 || romDirection > 3) {
            throw new IllegalArgumentException("ROM link.direction must be between 0 and 3");
        }
        carryingLiftedObjectState = carryState;
        carryingLiftedObjectRomDirection = romDirection;
        if (carryState != 0) {
            link.direction = Link.javaDirectionForRomDirection(romDirection);
        }
    }

    /** Applies the Link HRAM writes emitted by RoosterEntityHandler. */
    void applyRoosterFlightState(int positionZ, int velocityZ,
                                        int speedX, int speedY, int romDirection) {
        if ((positionZ & ~0xFF) != 0 || (velocityZ & ~0xFF) != 0) {
            throw new IllegalArgumentException("Rooster Link Z values must be unsigned bytes");
        }
        if (romDirection < 0 || romDirection > 3) {
            throw new IllegalArgumentException("Rooster ROM link.direction must be between 0 and 3");
        }
        link.linkStateMachine.motionState = Link.LINK_MOTION_DEFAULT;
        link.linkAirborne.airborne = true;
        link.linkAirborne.zSubPixels = (positionZ & 0xFF) << Link.SUB_PIXEL_SHIFT;
        link.linkAirborne.zVelocity = velocityZ & 0xFF;
        link.linkAirborne.groundStatus = Link.GROUND_STATUS_NORMAL;
        link.linkAirborne.fallingIntoPit = false;
        link.linkStateMachine.physicsModifier = 0;
        roosterCarryActive = true;
        link.direction = Link.javaDirectionForRomDirection(romDirection);
        link.linkMotion.applyRomSpeed(speedX, speedY);
    }

    /** Applies Hinox's direct hLinkPosition/throw writes for the next motion frame. */
    /** Applies Hinox's held position; only the source's countdown sets blocking. */
    void applyHinoxGrabState(int romPositionX, int romPositionY, int positionZ,
                                    int speedX, int speedY, int velocityZ,
                                    int airborneState) {
        applyHinoxGrabState(romPositionX, romPositionY, positionZ, speedX, speedY,
            velocityZ, airborneState, true, true);
    }

    void applyHinoxGrabState(int romPositionX, int romPositionY, int positionZ,
                                    int speedX, int speedY, int velocityZ,
                                    int airborneState, boolean motionBlocked,
                                    boolean applyHeldLinkPose) {
        Link.validateRomByte(romPositionX, "Hinox Link X");
        Link.validateRomByte(romPositionY, "Hinox Link Y");
        Link.validateRomByte(positionZ, "Hinox Link Z");
        Link.validateRomByte(speedX, "Hinox Link X speed");
        Link.validateRomByte(speedY, "Hinox Link Y speed");
        Link.validateRomByte(velocityZ, "Hinox Link Z velocity");
        Link.validateRomByte(airborneState, "Hinox Link link.linkAirborne.airborne state");
        link.linkMotion.setPixelPosition((romPositionX - 0x08) & 0xFF,
            (romPositionY - 0x10) & 0xFF);
        link.linkStateMachine.romInteractiveMotionBlocked = motionBlocked;
        if (applyHeldLinkPose) {
            link.linkAnimator.romAnimationStateOverride = 0x6A;
        }
        link.movingThisFrame = false;
        if (applyHeldLinkPose) {
            link.linkAirborne.zSubPixels = (positionZ & 0xFF) << Link.SUB_PIXEL_SHIFT;
        }
        link.linkAirborne.zVelocity = velocityZ & 0xFF;
        link.linkAirborne.airborne = airborneState != 0;
        if (speedX != 0 || speedY != 0) {
            link.linkMotion.applyRomSpeed(speedX, speedY);
        }
    }

    /** Applies entity-$61 WarpState3Handler's direct Link writes each frame. */
    void applyWarpState3(int romPositionX, int visualRomPositionY,
                                int immunityCountdown) {
        Link.validateRomByte(romPositionX, "Warp Link X");
        Link.validateRomByte(visualRomPositionY, "Warp Link visual Y");
        Link.validateRomByte(immunityCountdown, "Warp Link immunity");
        link.linkMotion.setPixelPosition((romPositionX - 0x08) & 0xFF,
            (visualRomPositionY - 0x10) & 0xFF);
        link.linkStateMachine.romInteractiveMotionBlocked = true;
        link.linkAnimator.romAnimationStateOverride = -1;
        link.romAttackStepAnimationCountdown = 0;
        link.linkDamage.collisionIgnoreFramesRemaining = 0;
        link.movingThisFrame = false;
        link.linkAirborne.airborne = false;
        link.linkAirborne.zSubPixels = 0;
        link.linkAirborne.zVelocity = 0;
        link.linkAirborne.fallingIntoPit = false;
        link.linkAirborne.groundStatus = Link.GROUND_STATUS_NORMAL;
        if (immunityCountdown != 0) {
            link.linkDamage.collisionDamageImmune = true;
        }
        if (link.playerState != null) {
            link.playerState.setInvincibilityCounter(0);
            link.playerState.clearSubtractHealthBuffer();
        }
    }

    /** Applies Marin's final writes when Link jumps out of the bed. */
    void leaveMarinWakeUpBed() {
        link.linkStateMachine.motionState = Link.LINK_MOTION_DEFAULT;
        link.linkStateMachine.romInteractiveMotionBlocked = false;
        link.linkAnimator.romAnimationStateOverride = -1;
        // The entity handler has already drawn the bed on the launch frame.
        // Ordinary Link rendering takes over at the next physics link.update.
        link.linkAnimator.marinWakeUpBedVariant = 3;
        marinBedJumpMotionLocked = true;
        link.movingThisFrame = false;
        link.linkAirborne.airborne = true;
        link.linkAirborne.zSubPixels = 1 << Link.SUB_PIXEL_SHIFT;
        link.linkAirborne.zVelocity = 0x12;
        link.linkAirborne.groundStatus = Link.GROUND_STATUS_NORMAL;
        link.linkAirborne.fallingIntoPit = false;
        link.direction = Link.DIRECTION_RIGHT;
        link.linkMotion.lastRomSpeedX = 0x0C;
        link.linkMotion.lastRomSpeedY = 0;
        link.linkMotion.forcedSpeedPending = false;
        link.linkMotion.forcedSpeedXOnly = false;
    }

    /** Ends Rooster's custom linkAirborne.airborne carry state when it is thrown or cleared. */
    void clearRoosterCarryState() {
        roosterCarryActive = false;
        link.linkAirborne.airborne = false;
        link.linkAirborne.zSubPixels = 0;
        link.linkAirborne.zVelocity = 0;
        link.linkAirborne.groundStatus = Link.GROUND_STATUS_NORMAL;
        link.linkAirborne.fallingIntoPit = false;
        link.linkStateMachine.motionState = Link.LINK_MOTION_DEFAULT;
        link.linkStateMachine.physicsModifier = 0;
        link.linkMotion.forcedSpeedPending = false;
        link.linkMotion.forcedSpeedXOnly = false;
    }
}
