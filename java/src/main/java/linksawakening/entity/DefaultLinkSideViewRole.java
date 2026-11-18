package linksawakening.entity;

/** Delegates sideView operations to state owners used by ROM {@code ApplyLinkMotionState}. */
final class DefaultLinkSideViewRole implements LinkSideViewRole {
    private final LinkSideView linkSideView;

    DefaultLinkSideViewRole(LinkAssembly assembly) {
        linkSideView = assembly.linkSideView.get();
    }

    @Override
    public void setSideScrolling(boolean enabled) {
        linkSideView.accessSetSideScrolling(enabled);
    }

    @Override
    public boolean standingOnSideViewEntity() {
        return linkSideView.accessStandingOnSideViewEntity();
    }

    @Override
    public boolean romItemAnimationFrameBlocked() {
        return linkSideView.accessRomItemAnimationFrameBlocked();
    }

    @Override
    public void applySideViewPlatformContact(int horizontalDelta, int romPositionY, int romSpeedY, boolean standing) {
        linkSideView.accessApplySideViewPlatformContact(horizontalDelta, romPositionY, romSpeedY, standing);
    }

    @Override
    public void applySideViewPlatformContact(int horizontalDelta, int romPositionY, int romSpeedY) {
        linkSideView.accessApplySideViewPlatformContact(horizontalDelta, romPositionY, romSpeedY);
    }

    @Override
    public void clearSideViewPlatformState() {
        linkSideView.accessClearSideViewPlatformState();
    }

    @Override
    public void clearStandingOnSideViewEntity() {
        linkSideView.accessClearStandingOnSideViewEntity();
    }

    @Override
    public void applySideViewPotContact(int romPositionY, int romSpeedY) {
        linkSideView.accessApplySideViewPotContact(romPositionY, romSpeedY);
    }
}
