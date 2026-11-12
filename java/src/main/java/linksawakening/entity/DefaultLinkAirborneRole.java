package linksawakening.entity;

import static linksawakening.entity.Link.*;

/** Delegates airborne operations to state owners used by ROM {@code ApplyLinkMotionState}. */
final class DefaultLinkAirborneRole implements LinkAirborneRole {
    private final LinkAirborne linkAirborne;

    DefaultLinkAirborneRole(LinkAssembly assembly) {
        linkAirborne = assembly.linkAirborne.get();
    }

    public void setPitWarpArrival(int z) {
        linkAirborne.accessSetPitWarpArrival(z);
    }

    public int romMotionState() {
        return linkAirborne.accessRomMotionState();
    }

    public boolean isAirborne() {
        return linkAirborne.accessIsAirborne();
    }

    public int zVelocity() {
        return linkAirborne.accessZVelocity();
    }

    public boolean isFallingIntoPit() {
        return linkAirborne.accessIsFallingIntoPit();
    }

    public void useRocsFeather() {
        linkAirborne.accessUseRocsFeather();
    }

    public void bounceFromGoomba() {
        linkAirborne.accessBounceFromGoomba();
    }

    public int zPixels() {
        return linkAirborne.accessZPixels();
    }

    public int tickDialogGotItem(boolean dialogActive) {
        return linkAirborne.accessTickDialogGotItem(dialogActive);
    }
}
