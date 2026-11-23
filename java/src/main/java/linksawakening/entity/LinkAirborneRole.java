package linksawakening.entity;

/** Airborne operations from LinkMotionDefault and its related ROM routines. */
public interface LinkAirborneRole {
    void setPitWarpArrival(int z);

    void setOverworldWarpHoleRoom(boolean enabled);

    boolean warpHoleControlsLink();

    boolean warpHoleReady();

    void startWarpHoleLanding();

    int romMotionState();

    boolean isAirborne();

    int zVelocity();

    boolean isFallingIntoPit();

    void useRocsFeather();

    void bounceFromGoomba();

    int zPixels();

    int tickDialogGotItem(boolean dialogActive);
}
