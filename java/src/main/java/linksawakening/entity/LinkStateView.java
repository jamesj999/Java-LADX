package linksawakening.entity;

import static linksawakening.entity.Link.*;
import static linksawakening.entity.LinkRomConstants.*;

/** Source-compatible Link operations delegated to their state owners. */
abstract class LinkStateView {
    final LinkAssembly assembly;
    final LinkAnimator linkAnimator;
    final LinkSideView linkSideView;
    final LinkStateMachine linkStateMachine;
    final LinkEquipment linkEquipment;
    final LinkEntityEffects linkEntityEffects;
    final LinkMotion linkMotion;
    final LinkAirborne linkAirborne;
    final LinkSwimming linkSwimming;
    final LinkDamage linkDamage;
    final LinkAnimationState linkAnimationState;
    final LinkItemState linkItemState;
    final LinkGroundMotion linkGroundMotion;
    final LinkSignals linkSignals;
    final LinkRecovery linkRecovery;
    final LinkServices linkServices;
    final LinkControls linkControls;
    final DialogGotItemSequence dialogGotItemSequence;

    LinkStateView(LinkServices services) {
        assembly = new LinkAssembly(services);
        linkAnimator = assembly.linkAnimator.get();
        linkSideView = assembly.linkSideView.get();
        linkStateMachine = assembly.linkStateMachine.get();
        linkEquipment = assembly.linkEquipment.get();
        linkEntityEffects = assembly.linkEntityEffects.get();
        linkMotion = assembly.linkMotion.get();
        linkAirborne = assembly.linkAirborne.get();
        linkSwimming = assembly.linkSwimming.get();
        linkDamage = assembly.linkDamage.get();
        linkAnimationState = assembly.linkAnimationState.get();
        linkItemState = assembly.linkItemState.get();
        linkGroundMotion = assembly.linkGroundMotion.get();
        linkSignals = assembly.linkSignals.get();
        linkRecovery = assembly.linkRecovery.get();
        linkServices = assembly.linkServices.get();
        linkControls = assembly.linkControls.get();
        dialogGotItemSequence = assembly.dialogGotItemSequence.get();
    }

    public void setSideScrolling(boolean enabled) {
        linkSideView.accessSetSideScrolling(enabled);
    }

    public boolean debugNoClip() {
        return linkStateMachine.accessDebugNoClip();
    }

    public void setCollisionIgnoreFrames(int frames) {
        linkDamage.accessSetCollisionIgnoreFrames(frames);
    }

    public void setNormalRecoilRoomBoundaries(boolean normalBoundaries) {
        linkDamage.accessSetNormalRecoilRoomBoundaries(normalBoundaries);
    }

    public boolean standingOnSideViewEntity() {
        return linkSideView.accessStandingOnSideViewEntity();
    }

    public void markRomLinkPushing(int countdown) {
        linkItemState.accessMarkRomLinkPushing(countdown);
    }

    public void tickRomLinkPushing() {
        linkItemState.accessTickRomLinkPushing();
    }

    public boolean isRomLinkPushing() {
        return linkItemState.accessIsRomLinkPushing();
    }

    public boolean isTarinShieldPresentationVisible() {
        return linkAnimator.accessIsTarinShieldPresentationVisible();
    }

    public int[] tarinShieldPalette() {
        return linkAnimator.accessTarinShieldPalette();
    }

    public boolean isMarinWakeUpBedVisible() {
        return linkAnimator.accessIsMarinWakeUpBedVisible();
    }

    public int marinWakeUpBedVariant() {
        return linkAnimator.accessMarinWakeUpBedVariant();
    }

    public int romEntityZ() {
        return linkAnimationState.accessRomEntityZ();
    }

    public boolean isLikeLikeCaptured() {
        return linkEntityEffects.accessIsLikeLikeCaptured();
    }

    public boolean recoveryBlocksInput() {
        return linkSignals.accessRecoveryBlocksInput();
    }

    public boolean consumePitWarp() {
        return linkSignals.accessConsumePitWarp();
    }

    public boolean pitWarpPending() {
        return linkSignals.accessPitWarpPending();
    }

    public boolean consumeDoorWarp() {
        return linkMotion.accessConsumeDoorWarp();
    }

    public void setPitWarpArrival(int z) {
        linkAirborne.accessSetPitWarpArrival(z);
    }

    public WaterSplash consumeWaterSplash() {
        return linkSignals.accessConsumeWaterSplash();
    }

    public boolean isPassingOut() {
        return linkRecovery.accessIsPassingOut();
    }

    public boolean gameOverScreenPending() {
        return linkRecovery.accessGameOverScreenPending();
    }

    public int gameOverStage() {
        return linkRecovery.accessGameOverStage();
    }

    public int gameOverCountdown() {
        return linkRecovery.accessGameOverCountdown();
    }

    public void presentGameOverStage(int stage, int countdown) {
        linkRecovery.accessPresentGameOverStage(stage, countdown);
    }

    public int passOutCountdown() {
        return linkRecovery.accessPassOutCountdown();
    }

    public int passOutBgPaletteEffectAddress() {
        return linkRecovery.accessPassOutBgPaletteEffectAddress();
    }

    public int passOutPaletteEffect() {
        return linkRecovery.accessPassOutPaletteEffect();
    }

    public int passOutPaletteFrames() {
        return linkRecovery.accessPassOutPaletteFrames();
    }

    public void finishGameOver() {
        linkRecovery.accessFinishGameOver();
    }

    public void beginPassOut() {
        linkRecovery.accessBeginPassOut();
    }

    public void presentPassOut(int countdown, int animationState, int paletteEffect) {
        linkRecovery.accessPresentPassOut(countdown, animationState, paletteEffect);
    }

    public int romMotionState() {
        return linkAirborne.accessRomMotionState();
    }

    public boolean isSwimming() {
        return linkStateMachine.accessIsSwimming();
    }

    public boolean isDiving() {
        return linkStateMachine.accessIsDiving();
    }

    public int romPhysicsModifier() {
        return linkStateMachine.accessRomPhysicsModifier();
    }

    public int romAttackStepAnimationCountdown() {
        return linkItemState.accessRomAttackStepAnimationCountdown();
    }

    public int romCollisionType() {
        return linkMotion.accessRomCollisionType();
    }

    public int romCollisionProbePixelX() {
        return linkMotion.accessRomCollisionProbePixelX();
    }

    public int romCollisionProbePixelY() {
        return linkMotion.accessRomCollisionProbePixelY();
    }

    public int romSpeedX() {
        return linkMotion.accessRomSpeedX();
    }

    public int romSpeedY() {
        return linkMotion.accessRomSpeedY();
    }

    public int pegasusBootsChargeMeter() {
        return linkStateMachine.accessPegasusBootsChargeMeter();
    }

    public int pegasusBootsCollisionCountdown() {
        return linkStateMachine.accessPegasusBootsCollisionCountdown();
    }

    public int pegasusBootsCollisionPosX() {
        return linkStateMachine.accessPegasusBootsCollisionPosX();
    }

    public int pegasusBootsCollisionPosY() {
        return linkStateMachine.accessPegasusBootsCollisionPosY();
    }

    public void setGelClinging(boolean clinging, int worldFrame) {
        linkStateMachine.accessSetGelClinging(clinging, worldFrame);
    }

    public int romSwordCollisionY() {
        return linkMotion.accessRomSwordCollisionY();
    }
}
