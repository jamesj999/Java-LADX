package linksawakening.entity;

import static linksawakening.entity.Link.*;

import linksawakening.state.GroundStatus;
import linksawakening.state.LinkMotionState;

/** Delegates motion operations to state owners used by ROM {@code ApplyLinkMotionState}. */
final class DefaultLinkMotionRole implements LinkMotionRole {
    private final DialogGotItemSequence dialogGotItemSequence;
    private final LinkAirborne linkAirborne;
    private final LinkAnimationState linkAnimationState;
    private final LinkAnimator linkAnimator;
    private final LinkDamage linkDamage;
    private final LinkEntityEffects linkEntityEffects;
    private final LinkGroundMotion linkGroundMotion;
    private final LinkItemState linkItemState;
    private final LinkMotion linkMotion;
    private final LinkSideView linkSideView;
    private final LinkSignals linkSignals;

    private final LinkStateMachine linkStateMachine;

    DefaultLinkMotionRole(LinkAssembly assembly) {
        linkStateMachine = assembly.linkStateMachine.get();
        this.dialogGotItemSequence = assembly.dialogGotItemSequence.get();
        this.linkAirborne = assembly.linkAirborne.get();
        this.linkAnimationState = assembly.linkAnimationState.get();
        this.linkAnimator = assembly.linkAnimator.get();
        this.linkDamage = assembly.linkDamage.get();
        this.linkEntityEffects = assembly.linkEntityEffects.get();
        this.linkGroundMotion = assembly.linkGroundMotion.get();
        this.linkItemState = assembly.linkItemState.get();
        this.linkMotion = assembly.linkMotion.get();
        this.linkSideView = assembly.linkSideView.get();
        this.linkSignals = assembly.linkSignals.get();
    }

    public boolean debugNoClip() {
        return linkStateMachine.status().accessDebugNoClip();
    }

    public boolean isSwimming() {
        return linkStateMachine.status().accessIsSwimming();
    }

    public boolean isDiving() {
        return linkStateMachine.accessIsDiving();
    }

    public int romPhysicsModifier() {
        return linkStateMachine.roomPhysics().accessRomPhysicsModifier();
    }

    public int pegasusBootsChargeMeter() {
        return linkStateMachine.dash().accessPegasusBootsChargeMeter();
    }

    public int pegasusBootsCollisionCountdown() {
        return linkStateMachine.dash().accessPegasusBootsCollisionCountdown();
    }

    public int pegasusBootsCollisionPosX() {
        return linkStateMachine.dash().accessPegasusBootsCollisionPosX();
    }

    public int pegasusBootsCollisionPosY() {
        return linkStateMachine.dash().accessPegasusBootsCollisionPosY();
    }

    public void setGelClinging(boolean clinging, int worldFrame) {
        linkStateMachine.accessSetGelClinging(clinging, worldFrame);
    }

    public void update() {
        linkGroundMotion.accessUpdate();
    }

    public boolean grassGroundEffectActive() {
        return linkGroundMotion.accessGrassGroundEffectActive();
    }

    public void clearGroundEffects() {
        linkGroundMotion.accessClearGroundEffects();
    }

    public boolean consumeRevolvingDoorTransition() {
        return linkStateMachine.door().accessConsumeRevolvingDoorTransition();
    }

    public boolean shouldApplyGroundMotion() {
        return linkGroundMotion.accessShouldApplyGroundMotion();
    }

    public void setDebugNoClip(boolean enabled) {
        linkStateMachine.setDebugNoClip(enabled);
    }

    /** Clears scripted presentation/capture state before a debug game reset. */
    public void resetTransientStateForDebug() {
        dialogGotItemSequence.reset();
        linkStateMachine.status().debugNoClip(false);
        linkGroundMotion.accessClearGroundEffects();
        linkAnimationState.romConsecutiveStepsCount = 7;
        linkStateMachine.status().romInteractiveMotionBlocked(false);
        linkStateMachine.status().romInteractiveMotionBlockValue(0);
        linkSideView.standingOnSideViewEntity = false;
        linkSideView.sideViewPlatformVerticalSpeedPending = -1;
        linkDamage.collisionDamageImmune = false;
        linkAnimator.romAnimationStateOverride(-1);
        linkAnimator.swordAcquisitionBaseRomDirection(-1);
        linkAnimator.swordAcquisitionAnimationState(-1);
        linkAnimator.tarinShieldPresentation(false);
        linkAnimator.tarinShieldPalette(null);
        linkAnimator.marinWakeUpBedVariant(-1);
        linkEntityEffects.marinBedJumpMotionLocked = false;
        linkAnimator.bedJumpRenderZ(-1);
        linkAnimator.ledgeJumpRenderY(Integer.MIN_VALUE);
        linkAirborne.directionalLedgeJumpMotionLocked(false);
        linkAirborne.overworldLedgeFallActive(false);
        linkEntityEffects.likeLikeCaptured = false;
        linkEntityEffects.likeLikeCapturePinsPosition = false;
        linkAirborne.pit().fallingIntoPit(false);
        linkEntityEffects.carryingLiftedObjectState = 0;
        linkAnimationState.topDownFeatherStarted = false;
        linkEntityEffects.roosterCarryActive = false;
        linkAirborne.elevation().airborne(false);
        linkAirborne.pit().groundStatus(GroundStatus.NORMAL);
        linkStateMachine.status().motionState(LinkMotionState.DEFAULT);
        linkStateMachine.door().revolvingDoorFrame(0);
        linkStateMachine.door().revolvingDoorTransitionPending(false);
        linkStateMachine.door().revolvingDoorPaletteVariant(-1);
        linkStateMachine.roomPhysics().physicsModifier(0);
        linkStateMachine.status().recoverCountdown(0);
        linkStateMachine.status().recoveringInWater(false);
        linkSignals.pendingWaterSplash = null;
        linkSignals.pitWarpPending = false;
        linkStateMachine.roomPhysics().physicsRoomLoadToken(-1);
        linkStateMachine.roomPhysics().physicsRoomMapId(-1);
        linkStateMachine.roomPhysics().physicsRoomId(-1);
        linkStateMachine.roomPhysics().physicsRoomCategory(-1);
        linkAirborne.elevation().zSubPixels(0);
        linkAirborne.elevation().zVelocity(0);
        linkAirborne.spinningPitWarpArrival(false);
        linkMotion.forcedSpeedPending = false;
        linkMotion.forcedSpeedXOnly = false;
        linkDamage.collisionIgnoreFramesRemaining = 0;
        linkItemState.romLinkPushing = 0;
        linkAnimator.pushingAnimationState(-1);
        linkItemState.romAttackStepAnimationCountdown = 0;
        linkAnimationState.movingThisFrame = false;
        linkAnimationState.walkTickCounter = 0;
        linkAnimationState.walkFrame = 0;
    }

    public void finishRoomScroll(boolean downward) {
        linkStateMachine.roomPhysics().finishRoomScroll(downward);
    }

    public void writeRomInteractiveMotionBlock(int value) {
        linkStateMachine.status().writeRomInteractiveMotionBlock(value);
    }

    public int romInteractiveMotionBlockValue() {
        return linkStateMachine.status().romInteractiveMotionBlockValue();
    }

    public int romSlowWalkingSpeed() {
        return linkStateMachine.status().slowWalkingSpeed();
    }

    public int romItemUsageContext() {
        return linkStateMachine.status().itemUsageContext();
    }

    public void blockNextRomMotionFrame() {
        linkStateMachine.status().blockNextRomMotionFrame();
    }

    public void initializeRoomPhysicsForRoom(long loadToken, int mapId, int roomId, int mapCategory) {
        linkStateMachine.roomPhysics().initializeRoomPhysicsForRoom(loadToken, mapId, roomId, mapCategory);
    }

    public void setRoomPhysicsForRoom(long loadToken, int mapId, int roomId, int mapCategory) {
        linkStateMachine.roomPhysics().setRoomPhysicsForRoom(loadToken, mapId, roomId, mapCategory);
    }

    public void setRomPhysicsModifier(int modifier) {
        linkStateMachine.roomPhysics().setRomPhysicsModifier(modifier);
    }

    public ScreenShakeRequest consumePegasusScreenShakeRequest() {
        return linkStateMachine.dash().consumePegasusScreenShakeRequest();
    }

    public void resetPegasusBoots() {
        linkStateMachine.dash().resetPegasusBoots();
    }
}
