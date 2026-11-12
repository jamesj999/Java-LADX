package linksawakening.entity;

import static linksawakening.entity.Link.*;

/** Delegates actions operations to state owners used by ROM {@code ApplyLinkMotionState}. */
final class DefaultLinkActionsRole implements LinkActionsRole {
    private final LinkEntityEffects linkEntityEffects;

    DefaultLinkActionsRole(LinkAssembly assembly) {
        linkEntityEffects = assembly.linkEntityEffects.get();
    }

    public boolean isLikeLikeCaptured() {
        return linkEntityEffects.accessIsLikeLikeCaptured();
    }

    public boolean isRoosterCarryActive() {
        return linkEntityEffects.accessIsRoosterCarryActive();
    }

    public int carryingLiftedObjectState() {
        return linkEntityEffects.accessCarryingLiftedObjectState();
    }

    public boolean isCarryingLiftedObject() {
        return linkEntityEffects.accessIsCarryingLiftedObject();
    }

    public boolean isLiftTransitionBlockingMotion() {
        return linkEntityEffects.accessIsLiftTransitionBlockingMotion();
    }

    public void applyLikeLikeCapture(int romEntityX, int romEntityY) {
        linkEntityEffects.accessApplyLikeLikeCapture(romEntityX, romEntityY);
    }

    public void applyLikeLikeCapture(int romEntityX, int romEntityY, boolean pinPosition) {
        linkEntityEffects.accessApplyLikeLikeCapture(romEntityX, romEntityY, pinPosition);
    }

    public void releaseLikeLikeCapture() {
        linkEntityEffects.accessReleaseLikeLikeCapture();
    }

    public void setCarryingLiftedObjectState(int carryState, int romDirection) {
        linkEntityEffects.accessSetCarryingLiftedObjectState(carryState, romDirection);
    }

    public void applyRoosterFlightState(int positionZ, int velocityZ, int speedX, int speedY, int romDirection) {
        linkEntityEffects.accessApplyRoosterFlightState(positionZ, velocityZ, speedX, speedY, romDirection);
    }

    public void applyHinoxGrabState(
            int romPositionX,
            int romPositionY,
            int positionZ,
            int speedX,
            int speedY,
            int velocityZ,
            int airborneState) {
        linkEntityEffects.accessApplyHinoxGrabState(
                romPositionX, romPositionY, positionZ, speedX, speedY, velocityZ, airborneState);
    }

    public void applyHinoxGrabState(
            int romPositionX,
            int romPositionY,
            int positionZ,
            int speedX,
            int speedY,
            int velocityZ,
            Link.GrabFlags arguments) {
        int airborneState = arguments.airborneState();
        boolean motionBlocked = arguments.motionBlocked();
        boolean applyHeldLinkPose = arguments.applyHeldLinkPose();

        linkEntityEffects.accessApplyHinoxGrabState(
                romPositionX,
                romPositionY,
                positionZ,
                speedX,
                speedY,
                velocityZ,
                Link.GrabFlags.of(airborneState, motionBlocked, applyHeldLinkPose));
    }

    public void applyWarpState3(int romPositionX, int visualRomPositionY, int immunityCountdown) {
        linkEntityEffects.accessApplyWarpState3(romPositionX, visualRomPositionY, immunityCountdown);
    }

    public void leaveMarinWakeUpBed() {
        linkEntityEffects.accessLeaveMarinWakeUpBed();
    }

    public void clearRoosterCarryState() {
        linkEntityEffects.accessClearRoosterCarryState();
    }

    public void setGrabbingObject(boolean grabbing, boolean pulling) {
        linkEntityEffects.accessSetGrabbingObject(grabbing, pulling);
    }
}
