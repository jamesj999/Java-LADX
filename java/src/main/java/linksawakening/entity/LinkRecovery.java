package linksawakening.entity;

import static linksawakening.entity.Link.*;
import static linksawakening.entity.LinkRomConstants.*;

import linksawakening.state.LinkMotionState;

/** Link recovery state and operations used by ROM {@code ApplyLinkMotionState}. */
final class LinkRecovery {
    private final LinkComponentRef<LinkAirborne> linkAirborne;
    private final LinkComponentRef<LinkAnimationState> linkAnimationState;
    private final LinkComponentRef<LinkAnimator> linkAnimator;
    private final LinkComponentRef<LinkDamage> linkDamage;
    private final LinkComponentRef<LinkMotion> linkMotion;
    private final LinkComponentRef<LinkServices> linkServices;
    private final LinkComponentRef<LinkStateMachine> linkStateMachine;

    LinkRecovery(Dependencies dependencies) {
        linkAirborne = dependencies.linkAirborne;
        linkAnimationState = dependencies.linkAnimationState;
        linkAnimator = dependencies.linkAnimator;
        linkDamage = dependencies.linkDamage;
        linkMotion = dependencies.linkMotion;
        linkServices = dependencies.linkServices;
        linkStateMachine = dependencies.linkStateMachine;
    }

    static final class Dependencies {
        LinkComponentRef<LinkAirborne> linkAirborne;
        LinkComponentRef<LinkAnimationState> linkAnimationState;
        LinkComponentRef<LinkAnimator> linkAnimator;
        LinkComponentRef<LinkDamage> linkDamage;
        LinkComponentRef<LinkMotion> linkMotion;
        LinkComponentRef<LinkServices> linkServices;
        LinkComponentRef<LinkStateMachine> linkStateMachine;
    }

    int passOutCountdown = -1;
    int passOutPaletteEffect;
    int gameOverCountdown;

    boolean accessIsPassingOut() {
        return linkAirborne.get().accessRomMotionState() == LinkMotionState.PASS_OUT.romValue();
    }

    boolean accessGameOverScreenPending() {
        return accessIsPassingOut() && passOutCountdown == 0;
    }

    int accessGameOverStage() {
        return accessIsPassingOut() ? linkStateMachine.get().accessRomPhysicsModifier() : 0;
    }

    int accessGameOverCountdown() {
        return gameOverCountdown;
    }

    void accessPresentGameOverStage(int stage, int countdown) {
        linkStateMachine.get().physicsModifier = stage;
        gameOverCountdown = countdown;
    }

    int accessPassOutCountdown() {
        return passOutCountdown;
    }

    int accessPassOutBgPaletteEffectAddress() {
        return accessIsPassingOut() && !accessGameOverScreenPending() ? 0x10 : 0;
    }

    int accessPassOutPaletteEffect() {
        return passOutPaletteEffect;
    }

    int accessPassOutPaletteFrames() {
        return accessIsPassingOut() ? 192 - passOutCountdown : 0;
    }

    void accessFinishGameOver() {
        linkStateMachine.get().resetTransientStateForDebug();
        passOutCountdown = -1;
        passOutPaletteEffect = 0;
        gameOverCountdown = 0;
    }

    void accessBeginPassOut() {
        linkStateMachine.get().motionState = LinkMotionState.PASS_OUT;
        linkStateMachine.get().physicsModifier = 0;
        if (linkServices.get().playerState != null)
            linkServices.get().playerState.vitals().setInvincibilityCounter(0);
        linkDamage.get().collisionIgnoreFramesRemaining = 0;
        linkMotion.get().forcedSpeedPending = false;
        linkMotion.get().lastRomSpeedX = 0;
        linkMotion.get().lastRomSpeedY = 0;
        linkAnimationState.get().movingThisFrame = false;
    }

    void accessPresentPassOut(int countdown, int animationState, int paletteEffect) {
        passOutCountdown = countdown;
        passOutPaletteEffect = paletteEffect;
        linkAnimator.get().romAnimationStateOverride = animationState;
    }
}
