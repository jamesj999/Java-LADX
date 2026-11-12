package linksawakening.entity;

import static linksawakening.entity.Link.*;

/** Temporary legacy Link API delegates. */
abstract class LinkStateView {
    protected abstract LinkRoles legacyRoles();

    /** @deprecated Use sideView().setSideScrolling instead. */
    @Deprecated(forRemoval = true)
    public void setSideScrolling(boolean enabled) {
        legacyRoles().sideView().setSideScrolling(enabled);
    }

    /** @deprecated Use motion().debugNoClip instead. */
    @Deprecated(forRemoval = true)
    public boolean debugNoClip() {
        return legacyRoles().motion().debugNoClip();
    }

    /** @deprecated Use status().setCollisionIgnoreFrames instead. */
    @Deprecated(forRemoval = true)
    public void setCollisionIgnoreFrames(int frames) {
        legacyRoles().status().setCollisionIgnoreFrames(frames);
    }

    /** @deprecated Use status().setNormalRecoilRoomBoundaries instead. */
    @Deprecated(forRemoval = true)
    public void setNormalRecoilRoomBoundaries(boolean normalBoundaries) {
        legacyRoles().status().setNormalRecoilRoomBoundaries(normalBoundaries);
    }

    /** @deprecated Use sideView().standingOnSideViewEntity instead. */
    @Deprecated(forRemoval = true)
    public boolean standingOnSideViewEntity() {
        return legacyRoles().sideView().standingOnSideViewEntity();
    }

    /** @deprecated Use equipment().markRomLinkPushing instead. */
    @Deprecated(forRemoval = true)
    public void markRomLinkPushing(int countdown) {
        legacyRoles().equipment().markRomLinkPushing(countdown);
    }

    /** @deprecated Use equipment().tickRomLinkPushing instead. */
    @Deprecated(forRemoval = true)
    public void tickRomLinkPushing() {
        legacyRoles().equipment().tickRomLinkPushing();
    }

    /** @deprecated Use equipment().isRomLinkPushing instead. */
    @Deprecated(forRemoval = true)
    public boolean isRomLinkPushing() {
        return legacyRoles().equipment().isRomLinkPushing();
    }

    /** @deprecated Use animation().isTarinShieldPresentationVisible instead. */
    @Deprecated(forRemoval = true)
    public boolean isTarinShieldPresentationVisible() {
        return legacyRoles().animation().isTarinShieldPresentationVisible();
    }

    /** @deprecated Use animation().tarinShieldPalette instead. */
    @Deprecated(forRemoval = true)
    public int[] tarinShieldPalette() {
        return legacyRoles().animation().tarinShieldPalette();
    }

    /** @deprecated Use animation().isMarinWakeUpBedVisible instead. */
    @Deprecated(forRemoval = true)
    public boolean isMarinWakeUpBedVisible() {
        return legacyRoles().animation().isMarinWakeUpBedVisible();
    }

    /** @deprecated Use animation().marinWakeUpBedVariant instead. */
    @Deprecated(forRemoval = true)
    public int marinWakeUpBedVariant() {
        return legacyRoles().animation().marinWakeUpBedVariant();
    }

    /** @deprecated Use animation().romEntityZ instead. */
    @Deprecated(forRemoval = true)
    public int romEntityZ() {
        return legacyRoles().animation().romEntityZ();
    }

    /** @deprecated Use actions().isLikeLikeCaptured instead. */
    @Deprecated(forRemoval = true)
    public boolean isLikeLikeCaptured() {
        return legacyRoles().actions().isLikeLikeCaptured();
    }

    /** @deprecated Use status().recoveryBlocksInput instead. */
    @Deprecated(forRemoval = true)
    public boolean recoveryBlocksInput() {
        return legacyRoles().status().recoveryBlocksInput();
    }

    /** @deprecated Use status().consumePitWarp instead. */
    @Deprecated(forRemoval = true)
    public boolean consumePitWarp() {
        return legacyRoles().status().consumePitWarp();
    }

    /** @deprecated Use status().pitWarpPending instead. */
    @Deprecated(forRemoval = true)
    public boolean pitWarpPending() {
        return legacyRoles().status().pitWarpPending();
    }

    /** @deprecated Use position().consumeDoorWarp instead. */
    @Deprecated(forRemoval = true)
    public boolean consumeDoorWarp() {
        return legacyRoles().position().consumeDoorWarp();
    }

    /** @deprecated Use airborne().setPitWarpArrival instead. */
    @Deprecated(forRemoval = true)
    public void setPitWarpArrival(int z) {
        legacyRoles().airborne().setPitWarpArrival(z);
    }

    /** @deprecated Use status().consumeWaterSplash instead. */
    @Deprecated(forRemoval = true)
    public WaterSplash consumeWaterSplash() {
        return legacyRoles().status().consumeWaterSplash();
    }

    /** @deprecated Use status().isPassingOut instead. */
    @Deprecated(forRemoval = true)
    public boolean isPassingOut() {
        return legacyRoles().status().isPassingOut();
    }

    /** @deprecated Use status().gameOverScreenPending instead. */
    @Deprecated(forRemoval = true)
    public boolean gameOverScreenPending() {
        return legacyRoles().status().gameOverScreenPending();
    }

    /** @deprecated Use status().gameOverStage instead. */
    @Deprecated(forRemoval = true)
    public int gameOverStage() {
        return legacyRoles().status().gameOverStage();
    }

    /** @deprecated Use status().gameOverCountdown instead. */
    @Deprecated(forRemoval = true)
    public int gameOverCountdown() {
        return legacyRoles().status().gameOverCountdown();
    }

    /** @deprecated Use status().presentGameOverStage instead. */
    @Deprecated(forRemoval = true)
    public void presentGameOverStage(int stage, int countdown) {
        legacyRoles().status().presentGameOverStage(stage, countdown);
    }

    /** @deprecated Use status().passOutCountdown instead. */
    @Deprecated(forRemoval = true)
    public int passOutCountdown() {
        return legacyRoles().status().passOutCountdown();
    }

    /** @deprecated Use status().passOutBgPaletteEffectAddress instead. */
    @Deprecated(forRemoval = true)
    public int passOutBgPaletteEffectAddress() {
        return legacyRoles().status().passOutBgPaletteEffectAddress();
    }

    /** @deprecated Use status().passOutPaletteEffect instead. */
    @Deprecated(forRemoval = true)
    public int passOutPaletteEffect() {
        return legacyRoles().status().passOutPaletteEffect();
    }

    /** @deprecated Use status().passOutPaletteFrames instead. */
    @Deprecated(forRemoval = true)
    public int passOutPaletteFrames() {
        return legacyRoles().status().passOutPaletteFrames();
    }

    /** @deprecated Use status().finishGameOver instead. */
    @Deprecated(forRemoval = true)
    public void finishGameOver() {
        legacyRoles().status().finishGameOver();
    }

    /** @deprecated Use status().beginPassOut instead. */
    @Deprecated(forRemoval = true)
    public void beginPassOut() {
        legacyRoles().status().beginPassOut();
    }

    /** @deprecated Use status().presentPassOut instead. */
    @Deprecated(forRemoval = true)
    public void presentPassOut(int countdown, int animationState, int paletteEffect) {
        legacyRoles().status().presentPassOut(countdown, animationState, paletteEffect);
    }

    /** @deprecated Use airborne().romMotionState instead. */
    @Deprecated(forRemoval = true)
    public int romMotionState() {
        return legacyRoles().airborne().romMotionState();
    }

    /** @deprecated Use motion().isSwimming instead. */
    @Deprecated(forRemoval = true)
    public boolean isSwimming() {
        return legacyRoles().motion().isSwimming();
    }

    /** @deprecated Use motion().isDiving instead. */
    @Deprecated(forRemoval = true)
    public boolean isDiving() {
        return legacyRoles().motion().isDiving();
    }

    /** @deprecated Use motion().romPhysicsModifier instead. */
    @Deprecated(forRemoval = true)
    public int romPhysicsModifier() {
        return legacyRoles().motion().romPhysicsModifier();
    }

    /** @deprecated Use equipment().romAttackStepAnimationCountdown instead. */
    @Deprecated(forRemoval = true)
    public int romAttackStepAnimationCountdown() {
        return legacyRoles().equipment().romAttackStepAnimationCountdown();
    }

    /** @deprecated Use position().romCollisionType instead. */
    @Deprecated(forRemoval = true)
    public int romCollisionType() {
        return legacyRoles().position().romCollisionType();
    }

    /** @deprecated Use position().romCollisionProbePixelX instead. */
    @Deprecated(forRemoval = true)
    public int romCollisionProbePixelX() {
        return legacyRoles().position().romCollisionProbePixelX();
    }

    /** @deprecated Use position().romCollisionProbePixelY instead. */
    @Deprecated(forRemoval = true)
    public int romCollisionProbePixelY() {
        return legacyRoles().position().romCollisionProbePixelY();
    }

    /** @deprecated Use position().romSpeedX instead. */
    @Deprecated(forRemoval = true)
    public int romSpeedX() {
        return legacyRoles().position().romSpeedX();
    }

    /** @deprecated Use position().romSpeedY instead. */
    @Deprecated(forRemoval = true)
    public int romSpeedY() {
        return legacyRoles().position().romSpeedY();
    }

    /** @deprecated Use motion().pegasusBootsChargeMeter instead. */
    @Deprecated(forRemoval = true)
    public int pegasusBootsChargeMeter() {
        return legacyRoles().motion().pegasusBootsChargeMeter();
    }

    /** @deprecated Use motion().pegasusBootsCollisionCountdown instead. */
    @Deprecated(forRemoval = true)
    public int pegasusBootsCollisionCountdown() {
        return legacyRoles().motion().pegasusBootsCollisionCountdown();
    }

    /** @deprecated Use motion().pegasusBootsCollisionPosX instead. */
    @Deprecated(forRemoval = true)
    public int pegasusBootsCollisionPosX() {
        return legacyRoles().motion().pegasusBootsCollisionPosX();
    }

    /** @deprecated Use motion().pegasusBootsCollisionPosY instead. */
    @Deprecated(forRemoval = true)
    public int pegasusBootsCollisionPosY() {
        return legacyRoles().motion().pegasusBootsCollisionPosY();
    }

    /** @deprecated Use motion().setGelClinging instead. */
    @Deprecated(forRemoval = true)
    public void setGelClinging(boolean clinging, int worldFrame) {
        legacyRoles().motion().setGelClinging(clinging, worldFrame);
    }

    /** @deprecated Use position().romSwordCollisionY instead. */
    @Deprecated(forRemoval = true)
    public int romSwordCollisionY() {
        return legacyRoles().position().romSwordCollisionY();
    }
}
