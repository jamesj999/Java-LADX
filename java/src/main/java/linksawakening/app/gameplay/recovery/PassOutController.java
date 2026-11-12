package linksawakening.app.gameplay.recovery;

import linksawakening.app.gameplay.GameplaySimulation;
import linksawakening.entity.Link;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.ui.FileMenuFrameSnapshot;

/**
 * Bank 0 death dispatch and bank 1 animation; screen loading follows in a later stage. One instance is
 * retained for the owning session.
 */
public final class PassOutController {
    private Link owner;
    private LinkPassOutAnimation animation;
    private GameOverPresentation gameOver;
    private boolean handledFrame;

    public boolean handledFrame() {
        return handledFrame;
    }

    public void clear() {
        owner = null;
        animation = null;
        gameOver = null;
    }

    public FileMenuFrameSnapshot snapshot() {
        return gameOver == null ? null : gameOver.snapshot();
    }

    public boolean tick(GameplaySimulation context, boolean dialog) {
        var link = context.link();
        handledFrame = link.status().isPassingOut();
        if (owner != link) {
            owner = link;
            animation = null;
            gameOver = null;
        }
        if (gameOver != null) {
            gameOver.tick(context);
            return true;
        }
        boolean transition = context.scrollController().isActive()
                || context.transitionController().isInputBlocked();
        boolean itemBlocked = link.animation().dialogGotItemActive()
                || link.sideView().romItemAnimationFrameBlocked()
                || ((context.roomSession().entityState().gotItemPresentationActive()
                        || context.roomSession().entityState().swordPickupSequenceActive()));
        if (LinkPassOutAnimation.shouldStart(
                context.playerState().vitals().health(),
                itemBlocked ? 1 : 0,
                context.inventoryController().shouldBlockOverworldInput() ? 1 : 0,
                dialog,
                transition,
                link.status().isPassingOut())) {
            handledFrame = true;
            animation = new LinkPassOutAnimation(context.romData());
            link.status().beginPassOut();
            context.musicDirector().playDirectMusic(0);
            context.gameplaySoundSink().play(GameplaySoundEvent.LINK_DIES);
        } else if (link.status().isPassingOut() && animation != null && !dialog && !transition) {
            animation.advanceFrame();
        }
        if (!link.status().isPassingOut() || animation == null) return false;
        context.scrollController().setScriptedScreenShakeHorizontal(false, 0);
        link.status().presentPassOut(animation.countdown(), animation.animationState(), animation.paletteEffect());
        link.motion().update();
        if (animation.finished()) gameOver = new GameOverPresentation(context);
        return true;
    }
}
