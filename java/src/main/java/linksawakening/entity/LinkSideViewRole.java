package linksawakening.entity;

import static linksawakening.entity.Link.*;

/** Sideview operations from LinkMotionDefault and its related ROM routines. */
public interface LinkSideViewRole {
    void setSideScrolling(boolean enabled);

    boolean standingOnSideViewEntity();

    boolean romItemAnimationFrameBlocked();

    void applySideViewPlatformContact(int horizontalDelta, int romPositionY, int romSpeedY, boolean standing);

    void applySideViewPlatformContact(int horizontalDelta, int romPositionY, int romSpeedY);

    void clearSideViewPlatformState();

    void clearStandingOnSideViewEntity();

    void applySideViewPotContact(int romPositionY, int romSpeedY);
}
