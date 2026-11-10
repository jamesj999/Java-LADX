package linksawakening.app.gameplay;

import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.rom.Rom;

/** Bank 0 death dispatch and bank 1 animation; screen loading follows in a later stage. */
public final class PassOutController {
    private linksawakening.entity.Link owner;
    private LinkPassOutAnimation animation;

    boolean tick(GameplaySimulation context, boolean dialog) {
        var link = context.link();
        if (link == null || context.playerState() == null) return false;
        if (owner != link) { owner = link; animation = null; }
        boolean transition = context.scrollController().isActive() || context.transitionController().isInputBlocked();
        boolean itemBlocked = link.romItemAnimationFrameBlocked()
            || (context.roomSession() != null && (context.roomSession().gotItemPresentationActive()
                || context.roomSession().swordPickupSequenceActive()));
        if (LinkPassOutAnimation.shouldStart(context.playerState().health(), itemBlocked ? 1 : 0,
                context.inventoryController().shouldBlockOverworldInput() ? 1 : 0,
                dialog, transition, link.isPassingOut())) {
            animation = new LinkPassOutAnimation(new Rom(context.romData()));
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
        return true;
    }
}
