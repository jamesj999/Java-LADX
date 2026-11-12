package linksawakening.entity;

import static linksawakening.entity.Link.*;
import static linksawakening.entity.LinkRomConstants.*;

/** Source-compatible Link operations delegated to their state owners. */
abstract class LinkRoomView extends LinkActionView {
    LinkRoomView(LinkServices services) {
        super(services);
    }

    public void setRomPhysicsModifier(int modifier) {
        linkStateMachine.accessSetRomPhysicsModifier(modifier);
    }

    public ScreenShakeRequest consumePegasusScreenShakeRequest() {
        return linkStateMachine.accessConsumePegasusScreenShakeRequest();
    }

    public void resetPegasusBoots() {
        linkStateMachine.accessResetPegasusBoots();
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

    public void submitOam(linksawakening.render.OamSink sink, int offsetX, int offsetY) {
        linkAnimator.accessSubmitOam(sink, offsetX, offsetY);
    }
}
