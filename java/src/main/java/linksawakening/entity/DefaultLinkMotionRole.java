package linksawakening.entity;

import linksawakening.entity.Link.ScreenShakeRequest;
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

    @Override
    public boolean debugNoClip() {
        return linkStateMachine.status().accessDebugNoClip();
    }

    @Override
    public boolean isSwimming() {
        return linkStateMachine.status().accessIsSwimming();
    }

    @Override
    public boolean isDiving() {
        return linkStateMachine.accessIsDiving();
    }

    @Override
    public int romPhysicsModifier() {
        return linkStateMachine.roomPhysics().accessRomPhysicsModifier();
    }

    @Override
    public int pegasusBootsChargeMeter() {
        return linkStateMachine.dash().accessPegasusBootsChargeMeter();
    }

    @Override
    public int pegasusBootsCollisionCountdown() {
        return linkStateMachine.dash().accessPegasusBootsCollisionCountdown();
    }

    @Override
    public int pegasusBootsCollisionPosX() {
        return linkStateMachine.dash().accessPegasusBootsCollisionPosX();
    }

    @Override
    public int pegasusBootsCollisionPosY() {
        return linkStateMachine.dash().accessPegasusBootsCollisionPosY();
    }

    @Override
    public void setGelClinging(boolean clinging, int worldFrame) {
        linkStateMachine.accessSetGelClinging(clinging, worldFrame);
    }

    @Override
    public void update() {
        linkGroundMotion.accessUpdate();
    }

    @Override
    public boolean grassGroundEffectActive() {
        return linkGroundMotion.accessGrassGroundEffectActive();
    }

    @Override
    public void clearGroundEffects() {
        linkGroundMotion.accessClearGroundEffects();
    }

    @Override
    public boolean consumeRevolvingDoorTransition() {
        return linkStateMachine.door().accessConsumeRevolvingDoorTransition();
    }

    @Override
    public boolean shouldApplyGroundMotion() {
        return linkGroundMotion.accessShouldApplyGroundMotion();
    }

    @Override
    public void setDebugNoClip(boolean enabled) {
        linkStateMachine.setDebugNoClip(enabled);
    }

    /** Clears scripted presentation/capture state before a debug game reset. */
    @Override
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
        linkAirborne.pit().warpHoleCharge = 0;
        linkAirborne.pit().warpHoleLanding = false;
        linkAirborne.spinningPitWarpArrival(false);
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
        linkMotion.position().forcedSpeedPending(false);
        linkMotion.position().forcedSpeedXOnly(false);
        linkDamage.collisionIgnoreFramesRemaining = 0;
        linkItemState.romLinkPushing = 0;
        linkAnimator.pushingAnimationState(-1);
        linkItemState.romAttackStepAnimationCountdown = 0;
        linkAnimationState.movingThisFrame = false;
        linkAnimationState.walkTickCounter = 0;
        linkAnimationState.walkFrame = 0;
    }

    @Override
    public void finishRoomScroll(boolean downward) {
        linkStateMachine.roomPhysics().finishRoomScroll(downward);
    }

    @Override
    public void writeRomInteractiveMotionBlock(int value) {
        linkStateMachine.status().writeRomInteractiveMotionBlock(value);
    }

    @Override
    public int romInteractiveMotionBlockValue() {
        return linkStateMachine.status().romInteractiveMotionBlockValue();
    }

    @Override
    public int romSlowWalkingSpeed() {
        return linkStateMachine.status().slowWalkingSpeed();
    }

    @Override
    public int romItemUsageContext() {
        return linkStateMachine.status().itemUsageContext();
    }

    @Override
    public void blockNextRomMotionFrame() {
        linkStateMachine.status().blockNextRomMotionFrame();
    }

    @Override
    public void initializeRoomPhysicsForRoom(long loadToken, int mapId, int roomId, int mapCategory) {
        linkStateMachine.roomPhysics().initializeRoomPhysicsForRoom(loadToken, mapId, roomId, mapCategory);
    }

    @Override
    public void setRoomPhysicsForRoom(long loadToken, int mapId, int roomId, int mapCategory) {
        linkStateMachine.roomPhysics().setRoomPhysicsForRoom(loadToken, mapId, roomId, mapCategory);
    }

    @Override
    public void setRomPhysicsModifier(int modifier) {
        linkStateMachine.roomPhysics().setRomPhysicsModifier(modifier);
    }

    @Override
    public ScreenShakeRequest consumePegasusScreenShakeRequest() {
        return linkStateMachine.dash().consumePegasusScreenShakeRequest();
    }

    @Override
    public void resetPegasusBoots() {
        linkStateMachine.dash().resetPegasusBoots();
    }
}
