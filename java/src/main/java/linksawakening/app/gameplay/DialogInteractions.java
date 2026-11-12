package linksawakening.app.gameplay;

import static linksawakening.world.room.RoomConstants.*;

import java.util.Objects;
import linksawakening.app.*;
import linksawakening.app.debug.*;
import linksawakening.app.host.*;
import linksawakening.app.screen.*;
import linksawakening.app.story.*;
import linksawakening.dialog.DialogController;
import linksawakening.entity.Link;
import linksawakening.gameplay.DialogTextLoader;
import linksawakening.gameplay.GameplayDialogInput;
import linksawakening.gameplay.LinkDialogPosition;
import linksawakening.gameplay.OverworldDialogInteraction;
import linksawakening.gameplay.SignpostDialogRef;
import linksawakening.input.InputConfig;
import linksawakening.state.PlayerState;
import linksawakening.world.room.ActiveRoom;
import linksawakening.world.room.ChestOpenResult;
import linksawakening.world.room.RoomSession;
import linksawakening.world.transition.*;

/** Focused frame interactions using the current session collaborators. */
public final class DialogInteractions {
    private final InputServices input;
    private final WorldServices world;
    private final PlayerServices player;
    private final UiServices ui;
    private final DialogBlockers dialogBlockers;

    public DialogInteractions(
            InputServices input,
            WorldServices world,
            PlayerServices player,
            UiServices ui,
            DialogBlockers dialogBlockers) {
        this.input = Objects.requireNonNull(input);
        this.world = Objects.requireNonNull(world);
        this.player = Objects.requireNonNull(player);
        this.ui = Objects.requireNonNull(ui);
        this.dialogBlockers = Objects.requireNonNull(dialogBlockers);
    }

    public InputConfig inputConfig() {
        return input.inputConfig();
    }

    public OverworldDialogInteraction overworldDialogInteraction() {
        return ui.overworldDialogInteraction();
    }

    public RoomSession roomSession() {
        return world.roomSession();
    }

    public Link link() {
        return player.link();
    }

    public PlayerState playerState() {
        return player.playerState();
    }

    public DialogController dialogController() {
        return ui.dialogController();
    }

    public DialogTextLoader dialogTextLoader() {
        return ui.dialogTextLoader();
    }

    public linksawakening.app.gameplay.DialogBlockers blockers() {
        return dialogBlockers;
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
                        blockers().current(),
                        dialogController());
    }

    public boolean tryOpenChest(int key) {
        if (!GameplayDialogInput.isActionButtonKey(key, inputConfig())
                || !roomSession().rooms().hasActiveRoom()
                || blockers().current().blocksOpening()) {
            return false;
        }
        ChestOpenResult result = roomSession()
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
        var request = roomSession().entityFrameResults().presentation().consumeFirstEntityDialogRequest();
        if (request == null) return;
        SignpostDialogRef dialogRef = new SignpostDialogRef(request.tableId(), request.dialogLowId());
        if (request.forceTop())
            dialogController().openPreformatted(dialogTextLoader().load(dialogRef), DialogController.BoxPosition.TOP);
        else
            dialogController()
                    .openPreformattedForLinkY(
                            dialogTextLoader().load(dialogRef), LinkDialogPosition.dialogYFromTopLeft(link().pixelY()));
    }
}
