package linksawakening.entity;

import static linksawakening.entity.Link.*;

/** Airborne operations from LinkMotionDefault and its related ROM routines. */
public interface LinkAirborneRole {
    void setPitWarpArrival(int z);

    int romMotionState();

    boolean isAirborne();

    int zVelocity();

    boolean isFallingIntoPit();

    void useRocsFeather();

    void bounceFromGoomba();

    int zPixels();

    int tickDialogGotItem(boolean dialogActive);
}
