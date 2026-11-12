package linksawakening.entity;

import static linksawakening.entity.Link.*;

/** @deprecated Temporary external API migration layer. */
@Deprecated(forRemoval = true)
abstract class LinkRoomView extends LinkActionView {

    /** @deprecated Use motion().setRomPhysicsModifier instead. */
    @Deprecated(forRemoval = true)
    public void setRomPhysicsModifier(int modifier) {
        legacyRoles().motion().setRomPhysicsModifier(modifier);
    }

    /** @deprecated Use motion().consumePegasusScreenShakeRequest instead. */
    @Deprecated(forRemoval = true)
    public ScreenShakeRequest consumePegasusScreenShakeRequest() {
        return legacyRoles().motion().consumePegasusScreenShakeRequest();
    }

    /** @deprecated Use motion().resetPegasusBoots instead. */
    @Deprecated(forRemoval = true)
    public void resetPegasusBoots() {
        legacyRoles().motion().resetPegasusBoots();
    }

    /** @deprecated Use sideView().applySideViewPlatformContact instead. */
    @Deprecated(forRemoval = true)
    public void applySideViewPlatformContact(int horizontalDelta, int romPositionY, int romSpeedY, boolean standing) {
        legacyRoles().sideView().applySideViewPlatformContact(horizontalDelta, romPositionY, romSpeedY, standing);
    }

    /** @deprecated Use sideView().applySideViewPlatformContact instead. */
    @Deprecated(forRemoval = true)
    public void applySideViewPlatformContact(int horizontalDelta, int romPositionY, int romSpeedY) {
        legacyRoles().sideView().applySideViewPlatformContact(horizontalDelta, romPositionY, romSpeedY);
    }

    /** @deprecated Use sideView().clearSideViewPlatformState instead. */
    @Deprecated(forRemoval = true)
    public void clearSideViewPlatformState() {
        legacyRoles().sideView().clearSideViewPlatformState();
    }

    /** @deprecated Use sideView().clearStandingOnSideViewEntity instead. */
    @Deprecated(forRemoval = true)
    public void clearStandingOnSideViewEntity() {
        legacyRoles().sideView().clearStandingOnSideViewEntity();
    }

    /** @deprecated Use sideView().applySideViewPotContact instead. */
    @Deprecated(forRemoval = true)
    public void applySideViewPotContact(int romPositionY, int romSpeedY) {
        legacyRoles().sideView().applySideViewPotContact(romPositionY, romSpeedY);
    }

    /** @deprecated Use animation().submitOam instead. */
    @Deprecated(forRemoval = true)
    public void submitOam(linksawakening.render.OamSink sink, int offsetX, int offsetY) {
        legacyRoles().animation().submitOam(sink, offsetX, offsetY);
    }
}
