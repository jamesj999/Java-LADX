package linksawakening.app.gameplay;

import linksawakening.gameplay.LinkDialogPosition;
import linksawakening.gameplay.SignpostDialogRef;

import static linksawakening.world.room.RoomConstants.*;

import static linksawakening.app.gameplay.GameplaySimulation.*;

final class DialogStoryPhase {
    boolean tick(GameplaySimulation context) {
        var roomSession = context.roomSession();
        var transitionController = context.transitionController();
        var debugNoClip = context.debugNoClip();
        var scrollController = context.scrollController();
        var inventoryMenu = context.inventoryMenu();
        var dialogController = context.dialogController();
        var dialogTextLoader = context.dialogTextLoader();
        var link = context.link();
        var story = context.story();
        var dialogSounds = context.dialogSounds();
            if (roomSession != null) {
                // $00:0F22 runs before world transitions and dialog gates.
                // The separate inventory gameplay handler does not run it.
                roomSession.advanceBoomerangSoundClock(
                    inventoryMenu == null || inventoryMenu.isFullyClosed());
            }
            scrollController.tickScreenShake();
            if (roomSession != null && link != null && dialogController != null
                && !transitionController.isInputBlocked()) {
                int dungeonTitle = roomSession.tickDungeonTitleMessage(
                    dialogController.isActive(), inventoryMenu.isFullyClosed(),
                    link.romMotionState(), debugNoClip);
                if (dungeonTitle >= 0) {
                    dialogController.openPreformattedForLinkY(
                        dialogTextLoader.load(new SignpostDialogRef(0, dungeonTitle)),
                        LinkDialogPosition.dialogYFromTopLeft(link.pixelY()));
                }
            }
            if (dialogController != null) {
                dialogController.tick();
                dialogSounds.run();
            }
            story.updateNewGameMarinPresentation();
            if (story.tickNewGameWakeUp()) {
                completeGameplayFrame(dialogController);
                return false;
            }
            if (story.tickNewGameTarinShield()) {
                completeGameplayFrame(dialogController);
                return false;
            }

        return true;
    }
}
