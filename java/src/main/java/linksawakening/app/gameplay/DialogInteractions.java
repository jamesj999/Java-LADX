package linksawakening.app.gameplay;


import linksawakening.dialog.DialogController;
import linksawakening.entity.Link;
import linksawakening.gameplay.DialogTextLoader;
import linksawakening.gameplay.GameplayDialogInput;
import linksawakening.gameplay.LinkDialogPosition;
import linksawakening.gameplay.OverworldDialogBlockers;
import linksawakening.gameplay.OverworldDialogInteraction;
import linksawakening.gameplay.SignpostDialogRef;
import linksawakening.input.InputConfig;
import linksawakening.state.PlayerState;
import linksawakening.world.ActiveRoom;
import linksawakening.world.RoomSession;


import static linksawakening.world.RoomConstants.*;
import static org.lwjgl.glfw.GLFW.*;


import java.util.function.*;

/** Focused frame interactions using the current session collaborators. */
public record DialogInteractions(InputConfig inputConfig, OverworldDialogInteraction overworldDialogInteraction, RoomSession roomSession, Link link, PlayerState playerState, DialogController dialogController, DialogTextLoader dialogTextLoader, Supplier<OverworldDialogBlockers> blockers) {
    public boolean tryOpenOverworldDialog(int key) {
        if (!GameplayDialogInput.isActionButtonKey(key, inputConfig)
            || overworldDialogInteraction == null
            || roomSession == null
            || !roomSession.hasActiveRoom()
            || link == null) {
            return false;
        }
        ActiveRoom room = roomSession.activeRoom();
        return overworldDialogInteraction.tryOpenSignpostDialog(
            room.roomId(),
            room.mapCategory(),
            room.roomObjectsArea(),
            link.pixelX(),
            link.pixelY(),
            link.direction(),
            true,
            blockers.get(),
            dialogController);
    }

    public boolean tryOpenChest(int key) {
        if (!GameplayDialogInput.isActionButtonKey(key, inputConfig)
            || roomSession == null
            || !roomSession.hasActiveRoom()
            || link == null
            || playerState == null
            || blockers.get().blocksOpening()) {
            return false;
        }
        RoomSession.ChestOpenResult result = roomSession.tryOpenChest(
            link.pixelX(), link.pixelY(), link.direction(), true, playerState.swordLevel());
        return result.opened();
    }

    public void openEntityDialogRequests() {
        if (roomSession == null || dialogController == null || dialogTextLoader == null
            || link == null || dialogController.isActive()) {
            return;
        }
        for (var request : roomSession.consumeEntityDialogRequests()) {
            SignpostDialogRef dialogRef = new SignpostDialogRef(
                request.tableId(), request.dialogLowId());
            if (request.forceTop()) {
                dialogController.openPreformatted(dialogTextLoader.load(dialogRef),
                    DialogController.BoxPosition.TOP);
                return;
            }
            dialogController.openPreformattedForLinkY(
                dialogTextLoader.load(dialogRef),
                LinkDialogPosition.dialogYFromTopLeft(link.pixelY()));
            return;
        }
    }
}
