package linksawakening.app.gameplay;

import linksawakening.entity.Link;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.ui.FileMenuFrameSnapshot;

/** Bank 0 death dispatch and bank 1 animation; screen loading follows in a later stage. */
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

    boolean tick(GameplaySimulation context, boolean dialog) {
        var link = context.link();
        handledFrame = link != null && link.isPassingOut();
        if (link == null) return false;
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
        boolean itemBlocked = link.dialogGotItemActive()
                || link.romItemAnimationFrameBlocked()
                || ((context.roomSession().gotItemPresentationActive()
                        || context.roomSession().swordPickupSequenceActive()));
        if (LinkPassOutAnimation.shouldStart(
                context.playerState().health(),
                itemBlocked ? 1 : 0,
                context.inventoryController().shouldBlockOverworldInput() ? 1 : 0,
                dialog,
                transition,
                link.isPassingOut())) {
            handledFrame = true;
            animation = new LinkPassOutAnimation(context.romData());
            link.beginPassOut();
            context.musicDirector().playDirectMusic(0);
            context.gameplaySoundSink().play(GameplaySoundEvent.LINK_DIES);
        } else if (link.isPassingOut() && animation != null && !dialog && !transition) {
            animation.advanceFrame();
        }
        if (!link.isPassingOut() || animation == null) return false;
        context.scrollController().setScriptedScreenShakeHorizontal(false, 0);
        link.presentPassOut(animation.countdown(), animation.animationState(), animation.paletteEffect());
        link.update();
        if (animation.finished()) gameOver = new GameOverPresentation(context);
        return true;
    }
}
