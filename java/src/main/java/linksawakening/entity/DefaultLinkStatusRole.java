package linksawakening.entity;

import linksawakening.entity.Link.WaterSplash;

/** Delegates status operations to state owners used by ROM {@code ApplyLinkMotionState}. */
final class DefaultLinkStatusRole implements LinkStatusRole {
    private final LinkDamage linkDamage;
    private final LinkMotionRole motion;
    private final LinkRecovery linkRecovery;
    private final LinkSignals linkSignals;

    DefaultLinkStatusRole(LinkAssembly assembly, LinkMotionRole motion) {
        this.motion = motion;
        linkDamage = assembly.linkDamage.get();
        linkRecovery = assembly.linkRecovery.get();
        linkSignals = assembly.linkSignals.get();
    }

    @Override
    public void setCollisionIgnoreFrames(int frames) {
        linkDamage.accessSetCollisionIgnoreFrames(frames);
    }

    @Override
    public void setNormalRecoilRoomBoundaries(boolean normalBoundaries) {
        linkDamage.accessSetNormalRecoilRoomBoundaries(normalBoundaries);
    }

    @Override
    public boolean recoveryBlocksInput() {
        return linkSignals.accessRecoveryBlocksInput();
    }

    @Override
    public boolean consumePitWarp() {
        return linkSignals.accessConsumePitWarp();
    }

    @Override
    public boolean pitWarpPending() {
        return linkSignals.accessPitWarpPending();
    }

    @Override
    public WaterSplash consumeWaterSplash() {
        return linkSignals.accessConsumeWaterSplash();
    }

    @Override
    public boolean isPassingOut() {
        return linkRecovery.accessIsPassingOut();
    }

    @Override
    public boolean gameOverScreenPending() {
        return linkRecovery.accessGameOverScreenPending();
    }

    @Override
    public int gameOverStage() {
        return linkRecovery.accessGameOverStage();
    }

    @Override
    public int gameOverCountdown() {
        return linkRecovery.accessGameOverCountdown();
    }

    @Override
    public void presentGameOverStage(int stage, int countdown) {
        linkRecovery.accessPresentGameOverStage(stage, countdown);
    }

    @Override
    public int passOutCountdown() {
        return linkRecovery.accessPassOutCountdown();
    }

    @Override
    public int passOutBgPaletteEffectAddress() {
        return linkRecovery.accessPassOutBgPaletteEffectAddress();
    }

    @Override
    public int passOutPaletteEffect() {
        return linkRecovery.accessPassOutPaletteEffect();
    }

    @Override
    public int passOutPaletteFrames() {
        return linkRecovery.accessPassOutPaletteFrames();
    }

    @Override
    public void finishGameOver() {
        motion.resetTransientStateForDebug();
        linkRecovery.accessFinishGameOver();
    }

    @Override
    public void beginPassOut() {
        linkRecovery.accessBeginPassOut();
    }

    @Override
    public void presentPassOut(int countdown, int animationState, int paletteEffect) {
        linkRecovery.accessPresentPassOut(countdown, animationState, paletteEffect);
    }

    @Override
    public boolean isCollisionDamageImmune() {
        return linkDamage.accessIsCollisionDamageImmune();
    }
}
