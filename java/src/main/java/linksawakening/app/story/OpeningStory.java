package linksawakening.app.story;

import static linksawakening.world.room.RoomConstants.*;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;
import linksawakening.app.SessionContext;
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
import linksawakening.state.PlayerState;
import linksawakening.world.room.ActiveRoom;
import linksawakening.world.room.ChestContentsTable;
import linksawakening.world.room.RoomSession;

public final class OpeningStory {
    private final SessionContext context;

    public OpeningStory(SessionContext context) {
        this.context = Objects.requireNonNull(context);
    }

    public int frameCounter() {
        return context.frameCounter();
    }

    public MarinWakeUpMotion newGameWakeUpMotion() {
        return context.newGameWakeUpMotion();
    }

    public TarinShieldMotion newGameTarinShieldMotion() {
        return context.newGameTarinShieldMotion();
    }

    public int[] newGameTarinShieldPalette() {
        return context.newGameTarinShieldPalette();
    }

    public InputState inputState() {
        return context.inputState();
    }

    public InputConfig inputConfig() {
        return context.inputConfig();
    }

    public DialogController dialogController() {
        return context.dialogController();
    }

    public DialogTextLoader dialogTextLoader() {
        return context.dialogTextLoader();
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

    public Supplier<OverworldDialogBlockers> blockers() {
        return context.blockers();
    }

    public IntConsumer music() {
        return context.music();
    }

    public IntSupplier houseMusic() {
        return context.houseMusic();
    }

    public IntUnaryOperator romDirection() {
        return context.romDirection();
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
                            romDirectionForLink(link().direction()),
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
                                    romDirectionForLink(link().direction()),
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
        return blockers().get();
    }

    private void playDirectMusic(int track) {
        music().accept(track);
    }

    private int newGameHouseMusicTrack() {
        return houseMusic().getAsInt();
    }

    private int romDirectionForLink(int direction) {
        return romDirection().applyAsInt(direction);
    }

    public void restoreNewGameHouseRuntimeIfNeeded(
            SaveSlotState saved,
            Consumer<MarinWakeUpMotion> wakeMotion,
            Consumer<TarinShieldMotion> shieldMotion,
            Runnable refreshPresentation) {
        if (saved.spawnIsIndoor() != 0 && saved.spawnMapId() == 0x10 && saved.spawnMapRoom() == 0xA3) {
            if (playerState().swordLevel() == 0) {
                wakeMotion.accept(MarinWakeUpMotion.postWake());
                refreshPresentation.run();
            }
            if (playerState().shieldLevel() == 0) {
                shieldMotion.accept(new TarinShieldMotion());
            }
        }
    }
}
