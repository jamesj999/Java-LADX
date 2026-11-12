package linksawakening.entity;

import static linksawakening.entity.Link.*;

/** Actions operations from LinkMotionDefault and its related ROM routines. */
public interface LinkActionsRole {
    boolean isLikeLikeCaptured();

    boolean isRoosterCarryActive();

    int carryingLiftedObjectState();

    boolean isCarryingLiftedObject();

    boolean isLiftTransitionBlockingMotion();

    void applyLikeLikeCapture(int romEntityX, int romEntityY);

    void applyLikeLikeCapture(int romEntityX, int romEntityY, boolean pinPosition);

    void releaseLikeLikeCapture();

    void setCarryingLiftedObjectState(int carryState, int romDirection);

    void applyRoosterFlightState(int positionZ, int velocityZ, int speedX, int speedY, int romDirection);

    void applyHinoxGrabState(
            int romPositionX,
            int romPositionY,
            int positionZ,
            int speedX,
            int speedY,
            int velocityZ,
            int airborneState);

    void applyHinoxGrabState(
            int romPositionX,
            int romPositionY,
            int positionZ,
            int speedX,
            int speedY,
            int velocityZ,
            Link.GrabFlags arguments);

    void applyWarpState3(int romPositionX, int visualRomPositionY, int immunityCountdown);

    void leaveMarinWakeUpBed();

    void clearRoosterCarryState();

    void setGrabbingObject(boolean grabbing, boolean pulling);
}
