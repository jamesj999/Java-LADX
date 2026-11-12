package linksawakening.entity;

import static linksawakening.entity.Link.*;

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

    public void setCollisionIgnoreFrames(int frames) {
        linkDamage.accessSetCollisionIgnoreFrames(frames);
    }

    public void setNormalRecoilRoomBoundaries(boolean normalBoundaries) {
        linkDamage.accessSetNormalRecoilRoomBoundaries(normalBoundaries);
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
        motion.resetTransientStateForDebug();
        linkRecovery.accessFinishGameOver();
    }

    public void beginPassOut() {
        linkRecovery.accessBeginPassOut();
    }

    public void presentPassOut(int countdown, int animationState, int paletteEffect) {
        linkRecovery.accessPresentPassOut(countdown, animationState, paletteEffect);
    }

    public boolean isCollisionDamageImmune() {
        return linkDamage.accessIsCollisionDamageImmune();
    }
}
