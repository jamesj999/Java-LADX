package linksawakening.entity;

/** Delegates actions operations to state owners used by ROM {@code ApplyLinkMotionState}. */
final class DefaultLinkActionsRole implements LinkActionsRole {
    private final LinkEntityEffects linkEntityEffects;

    DefaultLinkActionsRole(LinkAssembly assembly) {
        linkEntityEffects = assembly.linkEntityEffects.get();
    }

    @Override
    public boolean isLikeLikeCaptured() {
        return linkEntityEffects.accessIsLikeLikeCaptured();
    }

    @Override
    public boolean isRoosterCarryActive() {
        return linkEntityEffects.accessIsRoosterCarryActive();
    }

    @Override
    public int carryingLiftedObjectState() {
        return linkEntityEffects.accessCarryingLiftedObjectState();
    }

    @Override
    public boolean isCarryingLiftedObject() {
        return linkEntityEffects.accessIsCarryingLiftedObject();
    }

    @Override
    public boolean isLiftTransitionBlockingMotion() {
        return linkEntityEffects.accessIsLiftTransitionBlockingMotion();
    }

    @Override
    public void applyLikeLikeCapture(int romEntityX, int romEntityY) {
        linkEntityEffects.accessApplyLikeLikeCapture(romEntityX, romEntityY);
    }

    @Override
    public void applyLikeLikeCapture(int romEntityX, int romEntityY, boolean pinPosition) {
        linkEntityEffects.accessApplyLikeLikeCapture(romEntityX, romEntityY, pinPosition);
    }

    @Override
    public void releaseLikeLikeCapture() {
        linkEntityEffects.accessReleaseLikeLikeCapture();
    }

    @Override
    public void setCarryingLiftedObjectState(int carryState, int romDirection) {
        linkEntityEffects.accessSetCarryingLiftedObjectState(carryState, romDirection);
    }

    @Override
    public void applyRoosterFlightState(int positionZ, int velocityZ, int speedX, int speedY, int romDirection) {
        linkEntityEffects.accessApplyRoosterFlightState(positionZ, velocityZ, speedX, speedY, romDirection);
    }

    @Override
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

    @Override
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

    @Override
    public void applyWarpState3(int romPositionX, int visualRomPositionY, int immunityCountdown) {
        linkEntityEffects.accessApplyWarpState3(romPositionX, visualRomPositionY, immunityCountdown);
    }

    @Override
    public void leaveMarinWakeUpBed() {
        linkEntityEffects.accessLeaveMarinWakeUpBed();
    }

    @Override
    public void clearRoosterCarryState() {
        linkEntityEffects.accessClearRoosterCarryState();
    }

    @Override
    public void setGrabbingObject(boolean grabbing, boolean pulling) {
        linkEntityEffects.accessSetGrabbingObject(grabbing, pulling);
    }
}
