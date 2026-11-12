package linksawakening.app.gameplay;

import static linksawakening.world.room.RoomConstants.*;

import java.util.Objects;
import java.util.function.Supplier;
import linksawakening.app.SessionContext;
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
import linksawakening.world.room.ActiveRoom;
import linksawakening.world.room.RoomSession;

/** Focused frame interactions using the current session collaborators. */
public final class DialogInteractions {
    private final SessionContext context;

    public DialogInteractions(SessionContext context) {
        this.context = Objects.requireNonNull(context);
    }

    public InputConfig inputConfig() {
        return context.inputConfig();
    }

    public OverworldDialogInteraction overworldDialogInteraction() {
        return context.overworldDialogInteraction();
    }

    public RoomSession roomSession() {
        return context.roomSession();
    }

    public Link link() {
        return context.link();
    }

    public PlayerState playerState() {
        return context.playerState();
    }

    public DialogController dialogController() {
        return context.dialogController();
    }

    public DialogTextLoader dialogTextLoader() {
        return context.dialogTextLoader();
    }

    public Supplier<OverworldDialogBlockers> blockers() {
        return context.dialogsBlockers();
    }

    public boolean tryOpenOverworldDialog(int key) {
        if (!GameplayDialogInput.isActionButtonKey(key, inputConfig())
                || overworldDialogInteraction() == null
                || !roomSession().rooms().hasActiveRoom()) {
            return false;
        }
        ActiveRoom room = roomSession().rooms().activeRoom();
        return overworldDialogInteraction()
                .tryOpenSignpostDialog(
                        room.roomId(),
                        room.mapCategory(),
                        room.roomObjectsArea(),
                        link().pixelX(),
                        link().pixelY(),
                        link().direction(),
                        true,
                        blockers().get(),
                        dialogController());
    }

    public boolean tryOpenChest(int key) {
        if (!GameplayDialogInput.isActionButtonKey(key, inputConfig())
                || !roomSession().rooms().hasActiveRoom()
                || blockers().get().blocksOpening()) {
            return false;
        }
        RoomSession.ChestOpenResult result = roomSession()
                .doors()
                .tryOpenChest(
                        link().pixelX(),
                        link().pixelY(),
                        link().direction(),
                        true,
                        playerState().swordLevel());
        return result.opened();
    }

    public void openEntityDialogRequests() {
        if (dialogTextLoader() == null || dialogController().isActive()) {
            return;
        }
        for (var request : roomSession().entityState().consumeEntityDialogRequests()) {
            SignpostDialogRef dialogRef = new SignpostDialogRef(request.tableId(), request.dialogLowId());
            if (request.forceTop()) {
                dialogController()
                        .openPreformatted(dialogTextLoader().load(dialogRef), DialogController.BoxPosition.TOP);
                return;
            }
            dialogController()
                    .openPreformattedForLinkY(
                            dialogTextLoader().load(dialogRef), LinkDialogPosition.dialogYFromTopLeft(link().pixelY()));
            return;
        }
    }
}
