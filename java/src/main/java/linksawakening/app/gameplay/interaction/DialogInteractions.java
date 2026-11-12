package linksawakening.app.gameplay.interaction;

import java.util.Objects;
import linksawakening.app.services.InputServices;
import linksawakening.app.services.PlayerServices;
import linksawakening.app.services.UiServices;
import linksawakening.app.services.WorldServices;
import linksawakening.dialog.DialogController;
import linksawakening.gameplay.GameplayDialogInput;
import linksawakening.gameplay.LinkDialogPosition;
import linksawakening.gameplay.OverworldDialogInteraction;
import linksawakening.gameplay.SignpostDialogRef;
import linksawakening.world.room.ActiveRoom;
import linksawakening.world.room.ChestOpenResult;

/**
 * Routes action-button interactions to signposts, chests, room dialogs and opening-story handlers. One
 * instance is retained for the owning session.
 */
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

    public boolean tryOpenOverworldDialog(int key) {
        if (!GameplayDialogInput.isActionButtonKey(key, input.inputConfig())
                || !world.roomSession().rooms().hasActiveRoom()) {
            return false;
        }
        ActiveRoom room = world.roomSession().rooms().activeRoom();
        return ui.overworldDialogInteraction()
                .tryOpenSignpostDialog(
                        room.roomId(),
                        room.mapCategory(),
                        room.roomObjectsArea(),
                        player.link().position().pixelX(),
                        player.link().position().pixelY(),
                        player.link().animation().direction(),
                        new OverworldDialogInteraction.Interaction(
                                true, dialogBlockers.current(), ui.dialogController()));
    }

    public boolean tryOpenChest(int key) {
        if (!GameplayDialogInput.isActionButtonKey(key, input.inputConfig())
                || !world.roomSession().rooms().hasActiveRoom()
                || dialogBlockers.current().blocksOpening()) {
            return false;
        }
        ChestOpenResult result = world.roomSession()
                .doors()
                .tryOpenChest(
                        player.link().position().pixelX(),
                        player.link().position().pixelY(),
                        player.link().animation().direction(),
                        true,
                        player.playerState().equipmentLevels().swordLevel());
        return result.opened();
    }

    public void openEntityDialogRequests() {
        if (ui.dialogController().isActive()) {
            return;
        }
        var request = world.roomSession().entityFrameResults().presentation().consumeFirstEntityDialogRequest();
        if (request == null) return;
        SignpostDialogRef dialogRef = new SignpostDialogRef(request.tableId(), request.dialogLowId());
        if (request.forceTop())
            ui.dialogController()
                    .openPreformatted(ui.dialogTextLoader().load(dialogRef), DialogController.BoxPosition.TOP);
        else
            ui.dialogController()
                    .openPreformattedForLinkY(
                            ui.dialogTextLoader().load(dialogRef),
                            LinkDialogPosition.dialogYFromTopLeft(
                                    player.link().position().pixelY()));
    }
}
