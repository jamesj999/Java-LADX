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
    public void setOverworldWarpHoleRoom(boolean enabled) {
        linkAirborne.pit().overworldWarpHoleRoom = enabled;
    }

    @Override
    public boolean warpHoleControlsLink() {
        return linkAirborne.pit().warpHoleControlsLink();
    }

    @Override
    public boolean warpHoleReady() {
        return linkAirborne.pit().warpHoleCharge >= 96;
    }

    @Override
    public void startWarpHoleLanding() {
        linkAirborne.pit().warpHoleCharge = 0;
        linkAirborne.pit().warpHoleLanding = true;
        linkAirborne.accessSetPitWarpArrival(0x78);
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
