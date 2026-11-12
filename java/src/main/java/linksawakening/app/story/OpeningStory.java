package linksawakening.app.story;

import static linksawakening.world.room.RoomConstants.*;

import java.util.Objects;
import linksawakening.app.*;
import linksawakening.app.debug.*;
import linksawakening.app.gameplay.*;
import linksawakening.app.host.*;
import linksawakening.app.screen.*;
import linksawakening.audio.music.MusicTrackIds;
import linksawakening.dialog.DialogController;
import linksawakening.entity.Link;
import linksawakening.gameplay.DialogTextLoader;
import linksawakening.gameplay.LinkDialogPosition;
import linksawakening.gameplay.OverworldDialogBlockers;
import linksawakening.gameplay.SignpostDialogRef;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.save.SaveSlotState;
import linksawakening.startup.MarinWakeUpMotion;
import linksawakening.startup.TarinShieldMotion;
import linksawakening.state.Direction;
import linksawakening.state.PlayerState;
import linksawakening.world.room.ActiveRoom;
import linksawakening.world.room.ChestContentsTable;
import linksawakening.world.room.RoomSession;
import linksawakening.world.transition.*;

public final class OpeningStory implements OpeningRestoration {
    private final PlayerServices player;
    private final WorldServices world;
    private final InputServices input;
    private final UiServices ui;
    private final SessionState state;
    private final OpeningActions actions;

    public OpeningStory(
            PlayerServices player,
            WorldServices world,
            InputServices input,
            UiServices ui,
            SessionState state,
            OpeningActions actions) {
        this.player = Objects.requireNonNull(player);
        this.world = Objects.requireNonNull(world);
        this.input = Objects.requireNonNull(input);
        this.ui = Objects.requireNonNull(ui);
        this.state = Objects.requireNonNull(state);
        this.actions = Objects.requireNonNull(actions);
    }

    public int frameCounter() {
        return state.clock().value();
    }

    public MarinWakeUpMotion newGameWakeUpMotion() {
        return state.opening().wakeUp();
    }

    public TarinShieldMotion newGameTarinShieldMotion() {
        return state.opening().shield();
    }

    public int[] newGameTarinShieldPalette() {
        return actions.shieldPalette();
    }

    public InputState inputState() {
        return input.inputState();
    }

    public InputConfig inputConfig() {
        return input.inputConfig();
    }

    public DialogController dialogController() {
        return ui.dialogController();
    }

    public DialogTextLoader dialogTextLoader() {
        return ui.dialogTextLoader();
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

    public linksawakening.app.gameplay.DialogBlockers blockers() {
        return actions.blockers();
    }

    public MusicRequests music() {
        return actions.music();
    }

    public boolean tickNewGameWakeUp() {
        if (newGameWakeUpMotion() == null || newGameWakeUpMotion().complete()) {
            return false;
        }
        boolean directionPressed = inputState().isDown(inputConfig().upKey())
                || inputState().isDown(inputConfig().downKey())
                || inputState().isDown(inputConfig().leftKey())
                || inputState().isDown(inputConfig().rightKey());
        MarinWakeUpMotion.Update update =
                newGameWakeUpMotion().tick(frameCounter(), dialogController().isActive(), directionPressed);
        if (update.linkMotionBlocked()) {
            link().setPixelPosition(update.linkRomX() - 0x08, update.linkRomY() - 0x10);
            link().blockNextRomMotionFrame();
            link().showMarinWakeUpBed(update.bedSpriteVariant());
        }
        if (update.openWakeDialog()) {
            dialogController()
                    .openPreformattedForLinkY(
                            dialogTextLoader().load(new SignpostDialogRef(0, update.dialogLowId())),
                            LinkDialogPosition.dialogYFromTopLeft(link().pixelY()));
        }
        if (update.leaveBed()) {
            link().leaveMarinWakeUpBed();
        }
        return true;
    }

    public boolean marinWakeUpBlocksKeyInput() {
        return newGameWakeUpMotion() != null && !newGameWakeUpMotion().complete();
    }

    public void updateNewGameMarinPresentation() {
        {
            roomSession()
                    .story()
                    .updateHouseTarinPresentation(frameCounter(), link().romEntityX(), link().romEntityY());
        }
        if (newGameWakeUpMotion() == null
                || !roomSession().rooms().hasActiveRoom()
                || roomSession().rooms().activeRoom().mapId() != 0x10
                || roomSession().rooms().activeRoom().roomId() != 0xA3) {
            return;
        }
        var loadedEntities = roomSession().rooms().activeRoom().entities().loadedEntities();
        for (int entityIndex = 0; entityIndex < loadedEntities.size(); entityIndex++) {
            linksawakening.world.entity.RoomEntity entity = loadedEntities.get(entityIndex);

            if (entity.type() != 0x3E) {
                continue;
            }
            MarinWakeUpMotion.MarinPresentation presentation = newGameWakeUpMotion()
                    .tickMarinPresentation(
                            frameCounter(), entity.x(), entity.y(), link().romEntityX(), link().romEntityY());
            roomSession()
                    .story()
                    .applyMarinWakeUpPresentation(
                            presentation.romX(), presentation.romY(), presentation.spriteVariant());
            return;
        }
    }

    public boolean tryOpenTarinShieldDialog(int key) {
        if ((newGameWakeUpMotion() != null && !newGameWakeUpMotion().complete())
                || newGameTarinShieldMotion() == null
                || key != inputConfig().aKey()
                || !roomSession().rooms().hasActiveRoom()
                || dialogTextLoader() == null
                || currentOverworldDialogBlockers().blocksOpening()) {
            return false;
        }
        ActiveRoom room = roomSession().rooms().activeRoom();
        if (room.mapId() != 0x10 || room.roomId() != 0xA3) {
            return false;
        }
        var loadedEntities2 = room.entities().loadedEntities();
        for (int entityIndex = 0; entityIndex < loadedEntities2.size(); entityIndex++) {
            linksawakening.world.entity.RoomEntity entity = loadedEntities2.get(entityIndex);

            if (entity.type() == 0x3F
                    && TarinShieldMotion.canTalkToEntity(
                            entity.x(),
                            entity.y(),
                            link().romEntityX(),
                            link().romEntityY(),
                            Direction.fromJava(link().direction()).romIndex(),
                            link().isAirborne(),
                            true,
                            dialogController().isActive())) {
                TarinShieldMotion.Update update = newGameTarinShieldMotion()
                        .tick(false, link().romEntityY(), true, playerState().shieldLevel());
                if (update.dialogLowId() >= 0) {
                    dialogController()
                            .openPreformattedForLinkY(
                                    dialogTextLoader().load(new SignpostDialogRef(0, update.dialogLowId())),
                                    LinkDialogPosition.dialogYFromTopLeft(link().pixelY()));
                }
                return true;
            }
        }
        return false;
    }

    public boolean tryOpenMarinFollowUpDialog(int key) {
        if (newGameWakeUpMotion() == null
                || !newGameWakeUpMotion().complete()
                || key != inputConfig().aKey()
                || !roomSession().rooms().hasActiveRoom()
                || dialogTextLoader() == null
                || currentOverworldDialogBlockers().blocksOpening()) {
            return false;
        }
        ActiveRoom room = roomSession().rooms().activeRoom();
        if (room.mapId() != 0x10 || room.roomId() != 0xA3) {
            return false;
        }
        var loadedEntities3 = room.entities().loadedEntities();
        for (int entityIndex = 0; entityIndex < loadedEntities3.size(); entityIndex++) {
            linksawakening.world.entity.RoomEntity entity = loadedEntities3.get(entityIndex);

            if (entity.type() == 0x3E
                    && newGameWakeUpMotion()
                            .canOpenFollowUpDialog(
                                    entity.x(),
                                    entity.y(),
                                    link().romEntityX(),
                                    link().romEntityY(),
                                    Direction.fromJava(link().direction()).romIndex(),
                                    link().isAirborne(),
                                    true,
                                    dialogController().isActive())) {
                dialogController()
                        .openPreformattedForLinkY(
                                dialogTextLoader()
                                        .load(new SignpostDialogRef(
                                                0, newGameWakeUpMotion().followUpDialogLowId())),
                                LinkDialogPosition.dialogYFromTopLeft(link().pixelY()));
                return true;
            }
        }
        return false;
    }

    public boolean tickNewGameTarinShield() {
        if (newGameTarinShieldMotion() == null
                || !roomSession().rooms().hasActiveRoom()
                || roomSession().rooms().activeRoom().mapId() != 0x10
                || roomSession().rooms().activeRoom().roomId() != 0xA3) {
            return false;
        }
        TarinShieldMotion.Update update = newGameTarinShieldMotion()
                .tick(
                        dialogController().isActive(),
                        link().romEntityY(),
                        false,
                        playerState().shieldLevel());
        if (update.presentShield()) {
            link().showTarinShieldPresentation(newGameTarinShieldPalette());
        } else {
            link().clearTarinShieldPresentation();
        }
        if (update.linkY() != link().romEntityY()) {
            link().setPixelPosition(link().pixelX(), update.linkY() - 0x10);
            link().blockNextRomMotionFrame();
        }
        if (update.grantShield()) {
            playerState().applyChestReward(ChestContentsTable.CHEST_SHIELD);
            link().showStandingShieldDownPose();
            // MUSIC_OBTAIN_ITEM is a jingle in the ROM.  The host music
            // player loops direct tracks, so resume the house track when
            // TarinShield2Handler awards the shield and opens Dialog091.
            playDirectMusic(newGameHouseMusicTrack());
        }
        if (update.playObtainItemMusic()) {
            playDirectMusic(MusicTrackIds.MUSIC_OBTAIN_ITEM);
        }
        if (update.dialogLowId() >= 0 && (!dialogController().isActive())) {
            dialogController()
                    .openPreformattedForLinkY(
                            dialogTextLoader().load(new SignpostDialogRef(0, update.dialogLowId())),
                            LinkDialogPosition.dialogYFromTopLeft(link().pixelY()));
        }
        return update.linkMotionBlocked();
    }

    private OverworldDialogBlockers currentOverworldDialogBlockers() {
        return blockers().current();
    }

    private void playDirectMusic(int track) {
        music().playDirectMusic(track);
    }

    private int newGameHouseMusicTrack() {
        return MusicTrackIds.MUSIC_INTRO_WAKE_UP;
    }

    public void restoreWake(MarinWakeUpMotion motion) {
        state.opening().wakeUp(motion);
    }

    public void restoreShield(TarinShieldMotion motion) {
        state.opening().shield(motion);
    }

    public void refreshPresentation() {
        updateNewGameMarinPresentation();
    }

    public void restoreNewGameHouseRuntimeIfNeeded(SaveSlotState saved) {
        restoreNewGameHouseRuntimeIfNeeded(saved, this);
    }

    public void restoreNewGameHouseRuntimeIfNeeded(SaveSlotState saved, OpeningRestoration target) {
        if (saved.spawnIsIndoor() != 0 && saved.spawnMapId() == 0x10 && saved.spawnMapRoom() == 0xA3) {
            if (playerState().swordLevel() == 0) {
                target.restoreWake(MarinWakeUpMotion.postWake());
                target.refreshPresentation();
            }
            if (playerState().shieldLevel() == 0) {
                target.restoreShield(new TarinShieldMotion());
            }
        }
    }
}
