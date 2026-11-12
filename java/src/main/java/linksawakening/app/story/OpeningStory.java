package linksawakening.app.story;

import java.util.Objects;
import linksawakening.app.services.InputServices;
import linksawakening.app.services.PlayerServices;
import linksawakening.app.services.UiServices;
import linksawakening.app.services.WorldServices;
import linksawakening.app.state.SessionState;
import linksawakening.audio.music.MusicTrackIds;
import linksawakening.gameplay.LinkDialogPosition;
import linksawakening.gameplay.OverworldDialogBlockers;
import linksawakening.gameplay.SignpostDialogRef;
import linksawakening.save.SaveSlotState;
import linksawakening.startup.MarinWakeUpMotion;
import linksawakening.startup.TarinShieldMotion;
import linksawakening.state.Direction;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.room.ActiveRoom;
import linksawakening.world.room.ChestContentsTable;

/**
 * Runs Marin wake-up and Tarin shield state machines against live session state. One instance is retained
 * for the owning session.
 */
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

    public boolean tickNewGameWakeUp() {
        if (state.opening().wakeUp() == null || state.opening().wakeUp().complete()) {
            return false;
        }
        boolean directionPressed = input.inputState().isDown(input.inputConfig().upKey())
                || input.inputState().isDown(input.inputConfig().downKey())
                || input.inputState().isDown(input.inputConfig().leftKey())
                || input.inputState().isDown(input.inputConfig().rightKey());
        MarinWakeUpMotion.Update update = state.opening()
                .wakeUp()
                .tick(state.clock().value(), ui.dialogController().isActive(), directionPressed);
        if (update.linkMotionBlocked()) {
            player.link().position().setPixelPosition(update.linkRomX() - 0x08, update.linkRomY() - 0x10);
            player.link().motion().blockNextRomMotionFrame();
            player.link().animation().showMarinWakeUpBed(update.bedSpriteVariant());
        }
        if (update.openWakeDialog()) {
            ui.dialogController()
                    .openPreformattedForLinkY(
                            ui.dialogTextLoader().load(new SignpostDialogRef(0, update.dialogLowId())),
                            LinkDialogPosition.dialogYFromTopLeft(
                                    player.link().position().pixelY()));
        }
        if (update.leaveBed()) {
            player.link().actions().leaveMarinWakeUpBed();
        }
        return true;
    }

    public boolean marinWakeUpBlocksKeyInput() {
        return state.opening().wakeUp() != null && !state.opening().wakeUp().complete();
    }

    public void updateNewGameMarinPresentation() {
        {
            world.roomSession()
                    .story()
                    .updateHouseTarinPresentation(
                            state.clock().value(),
                            player.link().position().romEntityX(),
                            player.link().position().romEntityY());
        }
        if (state.opening().wakeUp() == null
                || !world.roomSession().rooms().hasActiveRoom()
                || world.roomSession().rooms().activeRoom().mapId() != 0x10
                || world.roomSession().rooms().activeRoom().roomId() != 0xA3) {
            return;
        }
        var loadedEntities = world.roomSession().rooms().activeRoom().entities().loadedEntities();
        for (int entityIndex = 0; entityIndex < loadedEntities.size(); entityIndex++) {
            RoomEntity entity = loadedEntities.get(entityIndex);

            if (entity.type() != 0x3E) {
                continue;
            }
            MarinWakeUpMotion.MarinPresentation presentation = state.opening()
                    .wakeUp()
                    .tickMarinPresentation(
                            state.clock().value(),
                            entity.x(),
                            entity.y(),
                            player.link().position().romEntityX(),
                            player.link().position().romEntityY());
            world.roomSession()
                    .story()
                    .applyMarinWakeUpPresentation(
                            presentation.romX(), presentation.romY(), presentation.spriteVariant());
            return;
        }
    }

    public boolean tryOpenTarinShieldDialog(int key) {
        if ((state.opening().wakeUp() != null && !state.opening().wakeUp().complete())
                || state.opening().shield() == null
                || key != input.inputConfig().aKey()
                || !world.roomSession().rooms().hasActiveRoom()
                || currentOverworldDialogBlockers().blocksOpening()) {
            return false;
        }
        ActiveRoom room = world.roomSession().rooms().activeRoom();
        if (room.mapId() != 0x10 || room.roomId() != 0xA3) {
            return false;
        }
        var loadedEntities2 = room.entities().loadedEntities();
        for (int entityIndex = 0; entityIndex < loadedEntities2.size(); entityIndex++) {
            RoomEntity entity = loadedEntities2.get(entityIndex);

            if (entity.type() == 0x3F
                    && TarinShieldMotion.canTalkToEntity(
                            entity.x(),
                            entity.y(),
                            player.link().position().romEntityX(),
                            player.link().position().romEntityY(),
                            Direction.fromJava(player.link().animation().direction())
                                    .romIndex(),
                            player.link().airborne().isAirborne(),
                            true,
                            ui.dialogController().isActive())) {
                TarinShieldMotion.Update update = state.opening()
                        .shield()
                        .tick(
                                false,
                                player.link().position().romEntityY(),
                                true,
                                player.playerState().equipmentLevels().shieldLevel());
                if (update.dialogLowId() >= 0) {
                    ui.dialogController()
                            .openPreformattedForLinkY(
                                    ui.dialogTextLoader().load(new SignpostDialogRef(0, update.dialogLowId())),
                                    LinkDialogPosition.dialogYFromTopLeft(
                                            player.link().position().pixelY()));
                }
                return true;
            }
        }
        return false;
    }

    public boolean tryOpenMarinFollowUpDialog(int key) {
        if (state.opening().wakeUp() == null
                || !state.opening().wakeUp().complete()
                || key != input.inputConfig().aKey()
                || !world.roomSession().rooms().hasActiveRoom()
                || currentOverworldDialogBlockers().blocksOpening()) {
            return false;
        }
        ActiveRoom room = world.roomSession().rooms().activeRoom();
        if (room.mapId() != 0x10 || room.roomId() != 0xA3) {
            return false;
        }
        var loadedEntities3 = room.entities().loadedEntities();
        for (int entityIndex = 0; entityIndex < loadedEntities3.size(); entityIndex++) {
            RoomEntity entity = loadedEntities3.get(entityIndex);

            if (entity.type() == 0x3E
                    && state.opening()
                            .wakeUp()
                            .canOpenFollowUpDialog(
                                    entity.x(),
                                    entity.y(),
                                    player.link().position().romEntityX(),
                                    player.link().position().romEntityY(),
                                    Direction.fromJava(player.link().animation().direction())
                                            .romIndex(),
                                    player.link().airborne().isAirborne(),
                                    true,
                                    ui.dialogController().isActive())) {
                ui.dialogController()
                        .openPreformattedForLinkY(
                                ui.dialogTextLoader()
                                        .load(new SignpostDialogRef(
                                                0, state.opening().wakeUp().followUpDialogLowId())),
                                LinkDialogPosition.dialogYFromTopLeft(
                                        player.link().position().pixelY()));
                return true;
            }
        }
        return false;
    }

    public boolean tickNewGameTarinShield() {
        if (state.opening().shield() == null
                || !world.roomSession().rooms().hasActiveRoom()
                || world.roomSession().rooms().activeRoom().mapId() != 0x10
                || world.roomSession().rooms().activeRoom().roomId() != 0xA3) {
            return false;
        }
        TarinShieldMotion.Update update = state.opening()
                .shield()
                .tick(
                        ui.dialogController().isActive(),
                        player.link().position().romEntityY(),
                        false,
                        player.playerState().equipmentLevels().shieldLevel());
        if (update.presentShield()) {
            player.link().animation().showTarinShieldPresentation(actions.shieldPalette());
        } else {
            player.link().animation().clearTarinShieldPresentation();
        }
        if (update.linkY() != player.link().position().romEntityY()) {
            player.link().position().setPixelPosition(player.link().position().pixelX(), update.linkY() - 0x10);
            player.link().motion().blockNextRomMotionFrame();
        }
        if (update.grantShield()) {
            player.playerState().chestRewards().applyChestReward(ChestContentsTable.CHEST_SHIELD);
            player.link().animation().showStandingShieldDownPose();
            // MUSIC_OBTAIN_ITEM is a jingle in the ROM.  The host music
            // player loops direct tracks, so resume the house track when
            // TarinShield2Handler awards the shield and opens Dialog091.
            playDirectMusic(newGameHouseMusicTrack());
        }
        if (update.playObtainItemMusic()) {
            playDirectMusic(MusicTrackIds.MUSIC_OBTAIN_ITEM);
        }
        if (update.dialogLowId() >= 0 && (!ui.dialogController().isActive())) {
            ui.dialogController()
                    .openPreformattedForLinkY(
                            ui.dialogTextLoader().load(new SignpostDialogRef(0, update.dialogLowId())),
                            LinkDialogPosition.dialogYFromTopLeft(
                                    player.link().position().pixelY()));
        }
        return update.linkMotionBlocked();
    }

    private OverworldDialogBlockers currentOverworldDialogBlockers() {
        return actions.blockers().current();
    }

    private void playDirectMusic(int track) {
        actions.music().playDirectMusic(track);
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
            if (player.playerState().equipmentLevels().swordLevel() == 0) {
                target.restoreWake(MarinWakeUpMotion.postWake());
                target.refreshPresentation();
            }
            if (player.playerState().equipmentLevels().shieldLevel() == 0) {
                target.restoreShield(new TarinShieldMotion());
            }
        }
    }
}
