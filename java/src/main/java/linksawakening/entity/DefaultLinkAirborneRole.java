package linksawakening.entity;

/** Delegates airborne operations to state owners used by ROM {@code ApplyLinkMotionState}. */
final class DefaultLinkAirborneRole implements LinkAirborneRole {
    private final LinkAirborne linkAirborne;

    DefaultLinkAirborneRole(LinkAssembly assembly) {
        linkAirborne = assembly.linkAirborne.get();
    }

    @Override
    public void setPitWarpArrival(int z) {
        linkAirborne.accessSetPitWarpArrival(z);
    }

    @Override
    public int romMotionState() {
        return linkAirborne.accessRomMotionState();
    }

    @Override
    public boolean isAirborne() {
        return linkAirborne.accessIsAirborne();
    }

    @Override
    public int zVelocity() {
        return linkAirborne.accessZVelocity();
    }

    @Override
    public boolean isFallingIntoPit() {
        return linkAirborne.accessIsFallingIntoPit();
    }

    @Override
    public void useRocsFeather() {
        linkAirborne.accessUseRocsFeather();
    }

    @Override
    public void bounceFromGoomba() {
        linkAirborne.accessBounceFromGoomba();
    }

    @Override
    public int zPixels() {
        return linkAirborne.accessZPixels();
    }

    @Override
    public int tickDialogGotItem(boolean dialogActive) {
        return linkAirborne.accessTickDialogGotItem(dialogActive);
    }
}
