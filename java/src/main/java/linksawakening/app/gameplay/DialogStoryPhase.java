package linksawakening.app.gameplay;

import static linksawakening.app.gameplay.GameplaySimulation.*;
import static linksawakening.world.room.RoomConstants.*;

import linksawakening.gameplay.LinkDialogPosition;
import linksawakening.gameplay.SignpostDialogRef;

final class DialogStoryPhase {
    boolean tick(GameplaySimulation context) {
        if (context.playerState() != null
                && (context.inventoryMenu() == null || context.inventoryMenu().isFullyClosed())) {
            context.playerState().latchFullHearts();
        }
        if (context.roomSession() != null) {
            // $00:0F22 runs before world transitions and dialog gates.
            // The separate inventory gameplay handler does not run it.
            context.roomSession()
                    .advanceBoomerangSoundClock(context.inventoryMenu() == null
                            || context.inventoryMenu().transition().worldHandlerActive());
        }
        context.scrollController().tickScreenShake();
        if (context.roomSession() != null
                && context.link() != null
                && context.dialogController() != null
                && !context.transitionController().isInputBlocked()) {
            int dungeonTitle = context.roomSession()
                    .tickDungeonTitleMessage(
                            context.dialogController().isActive(),
                            context.inventoryMenu().isFullyClosed(),
                            context.link().romMotionState(),
                            context.debugNoClip());
            if (dungeonTitle >= 0) {
                context.dialogController()
                        .openPreformattedForLinkY(
                                context.dialogTextLoader().load(new SignpostDialogRef(0, dungeonTitle)),
                                LinkDialogPosition.dialogYFromTopLeft(
                                        context.link().pixelY()));
            }
        }
        if (context.dialogController() != null) {
            context.dialogController().tick();
            context.dialogSounds().run();
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
