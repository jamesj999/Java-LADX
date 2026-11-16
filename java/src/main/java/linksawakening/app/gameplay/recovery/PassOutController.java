package linksawakening.app.gameplay.recovery;

import java.util.Objects;
import linksawakening.app.gameplay.GameplaySimulation;
import linksawakening.entity.Link;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.ui.FileMenuFrameSnapshot;

/**
 * Bank 0 death dispatch and bank 1 animation; screen loading follows in a later stage. One instance is
 * retained for the owning session.
 */
public final class PassOutController {
    // Optional identity/sequence state: no death presentation exists before the death trigger.
    private Link owner;
    private Sequence sequence = Inactive.INSTANCE;

    private sealed interface Sequence permits Inactive, Dying, GameOver {}

    private enum Inactive implements Sequence {
        INSTANCE
    }

    private record Dying(LinkPassOutAnimation animation) implements Sequence {
        Dying {
            Objects.requireNonNull(animation);
        }
    }

    private record GameOver(GameOverPresentation menu) implements Sequence {
        GameOver {
            Objects.requireNonNull(menu);
        }
    }

    private boolean handledFrame;

    public boolean handledFrame() {
        return handledFrame;
    }

    public void clear() {
        owner = null;
        sequence = Inactive.INSTANCE;
    }

    public FileMenuFrameSnapshot snapshot() {
        return sequence instanceof GameOver over ? over.menu().snapshot() : null;
    }

    public boolean tick(GameplaySimulation context, boolean dialog) {
        var link = context.link();
        handledFrame = link.status().isPassingOut();
        if (owner != link) {
            owner = link;
            sequence = Inactive.INSTANCE;
        }
        if (sequence instanceof GameOver over) {
            over.menu().tick(context);
            return true;
        }
        boolean transition = context.scrollController().isActive()
                || context.transitionController().isInputBlocked();
        boolean itemBlocked = link.animation().dialogGotItemActive()
                || link.sideView().romItemAnimationFrameBlocked()
                || (context.roomSession().entityState().gotItemPresentationActive()
                        || context.roomSession().entityState().swordPickupSequenceActive());
        if (LinkPassOutAnimation.shouldStart(
                context.playerState().vitals().health(),
                itemBlocked ? 1 : 0,
                context.inventoryController().shouldBlockOverworldInput() ? 1 : 0,
                dialog,
                transition,
                link.status().isPassingOut())) {
            handledFrame = true;
            sequence = new Dying(new LinkPassOutAnimation(context.romData()));
            link.status().beginPassOut();
            context.musicDirector().playDirectMusic(0);
            context.gameplaySoundSink().play(GameplaySoundEvent.LINK_DIES);
        } else if (link.status().isPassingOut() && sequence instanceof Dying dying && !dialog && !transition) {
            dying.animation().advanceFrame();
        }
        if (!link.status().isPassingOut() || !(sequence instanceof Dying dying)) return false;
        var animation = dying.animation();
        context.scrollController().setScriptedScreenShakeHorizontal(false, 0);
        link.status().presentPassOut(animation.countdown(), animation.animationState(), animation.paletteEffect());
        link.motion().update();
        if (animation.finished()) sequence = new GameOver(new GameOverPresentation(context));
        return true;
    }
}
