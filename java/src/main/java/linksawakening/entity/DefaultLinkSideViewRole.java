package linksawakening.entity;

import static linksawakening.entity.Link.*;

/** Delegates sideView operations to state owners used by ROM {@code ApplyLinkMotionState}. */
final class DefaultLinkSideViewRole implements LinkSideViewRole {
    private final LinkSideView linkSideView;

    DefaultLinkSideViewRole(LinkAssembly assembly) {
        linkSideView = assembly.linkSideView.get();
    }

    public void setSideScrolling(boolean enabled) {
        linkSideView.accessSetSideScrolling(enabled);
    }

    public boolean standingOnSideViewEntity() {
        return linkSideView.accessStandingOnSideViewEntity();
    }

    public boolean romItemAnimationFrameBlocked() {
        return linkSideView.accessRomItemAnimationFrameBlocked();
    }

    public void applySideViewPlatformContact(int horizontalDelta, int romPositionY, int romSpeedY, boolean standing) {
        linkSideView.accessApplySideViewPlatformContact(horizontalDelta, romPositionY, romSpeedY, standing);
    }

    public void applySideViewPlatformContact(int horizontalDelta, int romPositionY, int romSpeedY) {
        linkSideView.accessApplySideViewPlatformContact(horizontalDelta, romPositionY, romSpeedY);
    }

    public void clearSideViewPlatformState() {
        linkSideView.accessClearSideViewPlatformState();
    }

    public void clearStandingOnSideViewEntity() {
        linkSideView.accessClearStandingOnSideViewEntity();
    }

    public void applySideViewPotContact(int romPositionY, int romSpeedY) {
        linkSideView.accessApplySideViewPotContact(romPositionY, romSpeedY);
    }
}
