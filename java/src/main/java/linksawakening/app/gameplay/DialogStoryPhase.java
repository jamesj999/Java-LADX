package linksawakening.app.gameplay;

import static linksawakening.app.gameplay.GameplaySimulation.*;

import linksawakening.gameplay.LinkDialogPosition;
import linksawakening.gameplay.SignpostDialogRef;

/** Advances dialog and opening-story gates before player motion. Retained for one simulation. */
final class DialogStoryPhase {
    boolean tick(GameplaySimulation context) {
        if ((context.inventoryMenu().isFullyClosed())) {
            context.playerState().vitals().latchFullHearts();
        }
        {
            // $00:0F22 runs before world transitions and dialog gates.
            // The separate inventory gameplay handler does not run it.
            context.roomSession()
                    .entityState()
                    .advanceBoomerangSoundClock(
                            context.inventoryMenu().transition().worldHandlerActive());
        }
        context.scrollController().tickScreenShake();
        if (!context.transitionController().isInputBlocked()) {
            int dungeonTitle = context.roomSession()
                    .transitions()
                    .tickDungeonTitleMessage(
                            context.dialogController().isActive(),
                            context.inventoryMenu().isFullyClosed(),
                            context.link().airborne().romMotionState(),
                            context.debugNoClip());
            if (dungeonTitle >= 0) {
                context.dialogController()
                        .openPreformattedForLinkY(
                                context.dialogTextLoader().load(new SignpostDialogRef(0, dungeonTitle)),
                                LinkDialogPosition.dialogYFromTopLeft(
                                        context.link().position().pixelY()));
            }
        }
        {
            context.dialogController().tick();
            context.dialogSounds().route();
        }
        context.story().updateNewGameMarinPresentation();
        if (context.story().tickNewGameWakeUp()) {
            completeGameplayFrame(context.dialogController());
            return false;
        }
        if (context.story().tickNewGameTarinShield()) {
            completeGameplayFrame(context.dialogController());
            return false;
        }

        return true;
    }
}
