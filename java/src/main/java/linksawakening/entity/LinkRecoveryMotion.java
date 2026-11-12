package linksawakening.entity;

import static linksawakening.entity.Link.*;
import static linksawakening.entity.LinkRomConstants.*;
import static linksawakening.entity.LinkStateMachine.*;

/** LinkRecoveryMotion behavior on the shared LinkStateMachine state. */
abstract class LinkRecoveryMotion extends LinkBootsMotion {
    protected LinkRecoveryMotion(Dependencies dependencies) {
        super(dependencies);
    }

    protected void accessSetGelClinging(boolean clinging, int worldFrame) {
        this.gelClinging = clinging;
        this.gelMotionFrame = worldFrame & 0xFF;
        linkGroundMotion.get().worldMotionFrame = worldFrame & 0xFF;
    }

    protected boolean accessConsumeRevolvingDoorTransition() {
        boolean pending = this.revolvingDoorTransitionPending;
        this.revolvingDoorTransitionPending = false;
        return pending;
    }

    protected void accessSetDebugNoClip(boolean enabled) {
        this.setDebugNoClip(enabled);
    }

    protected void accessResetTransientStateForDebug() {
        this.resetTransientStateForDebug();
    }

    protected void accessFinishRoomScroll(boolean downward) {
        this.finishRoomScroll(downward);
    }

    protected void accessWriteRomInteractiveMotionBlock(int value) {
        this.writeRomInteractiveMotionBlock(value);
    }

    protected int accessRomInteractiveMotionBlockValue() {
        return this.romInteractiveMotionBlockValue;
    }

    protected int accessRomSlowWalkingSpeed() {
        return this.slowWalkingSpeed;
    }

    protected int accessRomItemUsageContext() {
        return this.itemUsageContext;
    }

    protected void accessBlockNextRomMotionFrame() {
        this.blockNextRomMotionFrame();
    }

    protected void accessInitializeRoomPhysicsForRoom(long loadToken, int mapId, int roomId, int mapCategory) {
        this.initializeRoomPhysicsForRoom(loadToken, mapId, roomId, mapCategory);
    }

    protected void accessSetRoomPhysicsForRoom(long loadToken, int mapId, int roomId, int mapCategory) {
        this.setRoomPhysicsForRoom(loadToken, mapId, roomId, mapCategory);
    }

    protected void accessSetRomPhysicsModifier(int modifier) {
        this.setRomPhysicsModifier(modifier);
    }

    protected ScreenShakeRequest accessConsumePegasusScreenShakeRequest() {
        return this.consumePegasusScreenShakeRequest();
    }

    protected void accessResetPegasusBoots() {
        this.resetPegasusBoots();
    }
}
