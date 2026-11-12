package linksawakening.entity;

import static linksawakening.entity.Link.*;
import static linksawakening.entity.LinkRomConstants.*;
import static linksawakening.entity.LinkStateMachine.*;

import linksawakening.state.LinkMotionState;

/** Link motion lifecycle state and operations used by ROM {@code ApplyLinkMotionState}. */
abstract class LinkMotionLifecycle {

    protected final LinkComponentRef<DialogGotItemSequence> dialogGotItemSequence;
    protected final LinkComponentRef<LinkAirborne> linkAirborne;
    protected final LinkComponentRef<LinkAnimationState> linkAnimationState;
    protected final LinkComponentRef<LinkAnimator> linkAnimator;
    protected final LinkComponentRef<LinkControls> linkControls;
    protected final LinkComponentRef<LinkDamage> linkDamage;
    protected final LinkComponentRef<LinkEntityEffects> linkEntityEffects;
    protected final LinkComponentRef<LinkEquipment> linkEquipment;
    protected final LinkComponentRef<LinkGroundMotion> linkGroundMotion;
    protected final LinkComponentRef<LinkItemState> linkItemState;
    protected final LinkComponentRef<LinkMotion> linkMotion;
    protected final LinkComponentRef<LinkServices> linkServices;
    protected final LinkComponentRef<LinkSideView> linkSideView;
    protected final LinkComponentRef<LinkSignals> linkSignals;
    protected final LinkComponentRef<LinkSwimming> linkSwimming;

    static final class Dependencies {
        LinkComponentRef<DialogGotItemSequence> dialogGotItemSequence;
        LinkComponentRef<LinkAirborne> linkAirborne;
        LinkComponentRef<LinkAnimationState> linkAnimationState;
        LinkComponentRef<LinkAnimator> linkAnimator;
        LinkComponentRef<LinkControls> linkControls;
        LinkComponentRef<LinkDamage> linkDamage;
        LinkComponentRef<LinkEntityEffects> linkEntityEffects;
        LinkComponentRef<LinkEquipment> linkEquipment;
        LinkComponentRef<LinkGroundMotion> linkGroundMotion;
        LinkComponentRef<LinkItemState> linkItemState;
        LinkComponentRef<LinkMotion> linkMotion;
        LinkComponentRef<LinkServices> linkServices;
        LinkComponentRef<LinkSideView> linkSideView;
        LinkComponentRef<LinkSignals> linkSignals;
        LinkComponentRef<LinkSwimming> linkSwimming;
    }

    LinkMotionState motionState = LinkMotionState.DEFAULT;
    int physicsModifier;
    int recoverCountdown;
    boolean recoveringInWater;
    int slowWalkingSpeed;
    int itemUsageContext;
    int revolvingDoorFrame;
    boolean revolvingDoorTransitionPending;
    int revolvingDoorPaletteVariant = -1;
    boolean romInteractiveMotionBlocked;
    int romInteractiveMotionBlockValue;

    long physicsRoomLoadToken = -1;
    int physicsRoomMapId = -1;
    int physicsRoomId = -1;
    int physicsRoomCategory = -1;
    boolean debugNoClip;
    int frameCounter;
    boolean gelClinging;
    int gelMotionFrame;
    int pegasusBootsChargeMeter;
    int pegasusBootsCollisionCountdown;
    int pegasusBootsCollisionPosX;
    int pegasusBootsCollisionPosY;
    int pendingPegasusScreenShakeCountdown;
    int pendingPegasusScreenShakePhase;

    LinkMotionLifecycle(Dependencies dependencies) {
        dialogGotItemSequence = dependencies.dialogGotItemSequence;
        linkAirborne = dependencies.linkAirborne;
        linkAnimationState = dependencies.linkAnimationState;
        linkAnimator = dependencies.linkAnimator;
        linkControls = dependencies.linkControls;
        linkDamage = dependencies.linkDamage;
        linkEntityEffects = dependencies.linkEntityEffects;
        linkEquipment = dependencies.linkEquipment;
        linkGroundMotion = dependencies.linkGroundMotion;
        linkItemState = dependencies.linkItemState;
        linkMotion = dependencies.linkMotion;
        linkServices = dependencies.linkServices;
        linkSideView = dependencies.linkSideView;
        linkSignals = dependencies.linkSignals;
        linkSwimming = dependencies.linkSwimming;
    }

    protected abstract void update();

    protected abstract void tickRevolvingDoor();

    protected abstract int groundSpeedIndex(int mask);

    protected abstract void updateDebugNoClip();

    protected abstract void updatePegasusBootsUse();

    protected abstract int pegasusRunningSpeedX();

    protected abstract int pegasusRunningSpeedY();

    protected abstract void handlePegasusBootsCollision();

    protected abstract void setDebugNoClip(boolean enabled);

    protected abstract void resetTransientStateForDebug();

    protected abstract void finishRoomScroll(boolean downward);

    protected abstract void initializeRoomPhysicsForRoom(long loadToken, int mapId, int roomId, int mapCategory);

    protected abstract void setRoomPhysicsForRoom(long loadToken, int mapId, int roomId, int mapCategory);

    protected abstract void setRomPhysicsModifier(int modifier);

    protected abstract void blockNextRomMotionFrame();

    protected abstract void writeRomInteractiveMotionBlock(int value);

    protected abstract void resetPegasusBoots();

    protected abstract ScreenShakeRequest consumePegasusScreenShakeRequest();

    protected abstract boolean accessDebugNoClip();

    protected abstract boolean accessIsSwimming();

    protected abstract boolean accessIsDiving();

    protected abstract int accessRomPhysicsModifier();

    protected abstract int accessPegasusBootsChargeMeter();

    protected abstract int accessPegasusBootsCollisionCountdown();

    protected abstract int accessPegasusBootsCollisionPosX();

    protected abstract int accessPegasusBootsCollisionPosY();

    protected abstract void accessSetGelClinging(boolean clinging, int worldFrame);

    protected abstract boolean accessConsumeRevolvingDoorTransition();

    protected abstract void accessSetDebugNoClip(boolean enabled);

    protected abstract void accessResetTransientStateForDebug();

    protected abstract void accessFinishRoomScroll(boolean downward);

    protected abstract void accessWriteRomInteractiveMotionBlock(int value);

    protected abstract int accessRomInteractiveMotionBlockValue();

    protected abstract int accessRomSlowWalkingSpeed();

    protected abstract int accessRomItemUsageContext();

    protected abstract void accessBlockNextRomMotionFrame();

    protected abstract void accessInitializeRoomPhysicsForRoom(long loadToken, int mapId, int roomId, int mapCategory);

    protected abstract void accessSetRoomPhysicsForRoom(long loadToken, int mapId, int roomId, int mapCategory);

    protected abstract void accessSetRomPhysicsModifier(int modifier);

    protected abstract ScreenShakeRequest accessConsumePegasusScreenShakeRequest();

    protected abstract void accessResetPegasusBoots();
}
