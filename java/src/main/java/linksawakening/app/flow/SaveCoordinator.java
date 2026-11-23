package linksawakening.app.flow;

import java.io.IOException;
import java.util.Objects;
import linksawakening.app.audio.MusicDirector;
import linksawakening.app.screen.SaveActions;
import linksawakening.app.screen.ScreenKind;
import linksawakening.app.screen.ScreenNavigator;
import linksawakening.app.services.InputServices;
import linksawakening.app.services.PlayerServices;
import linksawakening.app.services.RomServices;
import linksawakening.app.services.WorldServices;
import linksawakening.app.state.SaveState;
import linksawakening.app.state.SessionState;
import linksawakening.rom.RomAddress;
import linksawakening.save.SaveRamStore;
import linksawakening.save.SaveSlotState;
import linksawakening.startup.NewGameStartProfile;
import linksawakening.state.Direction;
import linksawakening.state.PowerUp;
import linksawakening.ui.FileSaveController;
import linksawakening.world.room.ActiveRoom;
import linksawakening.world.transition.Warp;

/**
 * Persistence and room-entry orchestration, using the current session snapshot. One instance is retained for
 * the owning session.
 */
public final class SaveCoordinator implements SaveActions {
    private final RomServices rom;
    private final WorldServices world;
    private final PlayerServices player;
    private final InputServices input;
    private final SessionState state;
    private final SaveState saveState;
    private final ScreenNavigator navigator;
    private final StoryActions storyActions;

    public SaveCoordinator(
            RomServices rom,
            WorldServices world,
            PlayerServices player,
            InputServices input,
            SessionState state,
            SaveState saveState,
            ScreenNavigator navigator,
            StoryActions storyActions) {
        this.rom = Objects.requireNonNull(rom);
        this.world = Objects.requireNonNull(world);
        this.player = Objects.requireNonNull(player);
        this.input = Objects.requireNonNull(input);
        this.state = Objects.requireNonNull(state);
        this.saveState = Objects.requireNonNull(saveState);
        this.navigator = Objects.requireNonNull(navigator);
        this.storyActions = Objects.requireNonNull(storyActions);
    }

    @Override
    public void startNewGame() {
        startNewGame(null);
    }

    public void startNewGame(SaveSlotState saved) {
        enterGameplay();

        NewGameStartProfile profile = NewGameStartProfile.romDefaults();
        profile.initializePlayerState(player.playerState());
        world.roomSession().story().initializeNewGameWorldState();
        if (saved != null) {
            restoreSavedState(saved);
            profile.applyStartingCapacities(player.playerState());
        }
        world.roomSession()
                .story()
                .setBirdKeyOwned(player.playerState().progression().birdKeyCount() != 0);
        world.roomSession()
                .story()
                .setTailKeyOwned(player.playerState().progression().tailKeyCount() != 0);
        if (saved == null) world.roomSession().story().setBowWowState(0);
        player.link().animation().setDirection(Direction.DOWN.javaIndex());
        rom.gpu().loadBaseTiles(rom.romData());
        world.roomSession().progress().restoreWorldMapRoom(profile.roomId());
        world.roomSession().rooms().loadIndoor(profile.mapId(), profile.roomId());
        world.roomSession()
                .progress()
                .setRespawnPoint(
                        new Warp(
                                Warp.CATEGORY_INDOOR,
                                profile.mapId(),
                                profile.roomId(),
                                profile.entryX(),
                                profile.entryY(),
                                -1),
                        0);
        player.link().position().setRoomEntryRomPosition(profile.entryX(), profile.entryY());
        state.opening()
                .initialize(
                        player.playerState().equipmentLevels().swordLevel(),
                        player.playerState().equipmentLevels().shieldLevel());
        storyActions.music().playDirectMusic(MusicDirector.newGameHouseMusicTrack());
        storyActions.story().updateNewGameMarinPresentation();
    }

    @Override
    public void startSavedGame(SaveSlotState saved) {
        enterGameplay();
        restoreSavedState(saved);
        loadSavedRoom(saved);
        player.link().position().setRoomEntryRomPosition(saved.spawnPositionX(), saved.spawnPositionY());
        world.roomSession()
                .progress()
                .setRespawnPoint(
                        new Warp(
                                saved.spawnIsIndoor() != 0 ? 1 : 0,
                                saved.spawnMapId(),
                                saved.spawnMapRoom(),
                                saved.spawnPositionX(),
                                saved.spawnPositionY(),
                                -1),
                        saved.spawnIndoorRoom());
        storyActions.story().restoreNewGameHouseRuntimeIfNeeded(saved);
    }

    private void restoreSavedState(SaveSlotState saved) {
        state.opening().clear();
        player.playerState().playerSaveLoader().applySavedGame(saved);
        world.roomSession()
                .story()
                .setBirdKeyOwned(player.playerState().progression().birdKeyCount() != 0);
        world.roomSession()
                .story()
                .setTailKeyOwned(player.playerState().progression().tailKeyCount() != 0);
        world.roomSession()
                .story()
                .restoreRoomStatuses(
                        saved.overworldRoomStatus(),
                        saved.indoorARoomStatus(),
                        saved.indoorBRoomStatus(),
                        saved.colorDungeonRoomStatus());
        world.roomSession().dungeon().restoreDungeonItemFlags(saved.dungeonItemFlags(), saved.colorDungeonItemFlags());
        world.roomSession().dungeon().restoreDungeonProgressFlags(saved.dungeonProgressFlags());
        world.roomSession().story().setBowWowState(saved.bowWowState());
        world.roomSession().story().setTarinFlag(saved.tarinFlag());
        world.roomSession().story().setRichardSpokenFlag(saved.richardSpokenFlag());
        world.roomSession()
                .story()
                .setHouseNpcTradeItem(player.playerState().progression().tradeSequenceItem());
        world.roomSession()
                .story()
                .setChestPlayerLevels(
                        player.playerState().equipmentLevels().shieldLevel(),
                        player.playerState().equipmentLevels().swordLevel(),
                        player.playerState().equipmentLevels().powerBraceletLevel());
        world.roomSession().progress().restoreWorldMapRoom(saved.worldMapRoom());
        world.roomSession()
                .story()
                .restoreFollowers(
                        saved.followerFlags(),
                        saved.powerBraceletLevel(),
                        saved.spawnPositionX(),
                        saved.spawnPositionY());
    }

    /** $01:42D9-4335: options 0/1 save; option 2 skips the SRAM reload via wDBD1. */
    @Override
    public void finishGameOver(int option) {
        if (player.playerState().vitals().health() == 0) {
            player.playerState()
                    .vitals()
                    .setHealth(rom.romData()
                            .u8(new RomAddress(1, 0x5295)
                                    .plus(player.playerState().vitals().maxHearts())));
        }
        state.opening().clear();
        player.link().status().finishGameOver();
        player.playerState().playerResources().clearResourceBuffersForDebug();
        player.playerState().equipmentLevels().setActivePowerUp(PowerUp.NONE.romValue());
        world.roomSession().progress().clearGameOverPowerCounter();
        world.roomSession().progress().resetGameOverSwitchBlocks();
        if (option != 2) saveCurrentPlayerState();
        if (option == 1) {
            navigator.restartIntroAfterSave();
            return;
        }
        if (option == 0 && state.screens().currentSaveSlot() >= 0) {
            startSavedGame(saveState.saveRamStore().readSlot(state.screens().currentSaveSlot()));
        } else {
            // Capture live progress for Continue without writing the selected SRAM slot.
            int slot = state.screens().currentSaveSlot();
            SaveRamStore recovery = slot < 0
                    ? SaveRamStore.inMemory()
                    : saveState.saveRamStore().detachedCopy();
            slot = Math.max(0, slot);
            writePlayerSnapshot(recovery, slot);
            startSavedGame(recovery.readSlot(slot));
        }
        player.playerState().vitals().setInvincibilityCounter(0x80);
        input.inputState().tickEdges();
    }

    @Override
    public void createFile(int slot, int[] name) {
        persistFileMenuMutation(() -> saveState.saveRamStore().createNewGame(slot, name));
        if (linksawakening.ui.FileMenuRomData.matchesSpecialFileName(name)) {
            storyActions.music().playDirectMusic(linksawakening.audio.music.MusicTrackIds.MUSIC_FILE_SELECT_ZELDA);
        }
    }

    @Override
    public void persistFileMenuMutation(Runnable mutation) {
        mutation.run();
        try {
            saveState.saveRamStore().flush();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to persist file-menu mutation", exception);
        }
        navigator.startFileSelection();
    }

    @Override
    public void startFileSave() {
        var previous = state.screens().menuTransition;
        state.screens().menuTransition =
                previous == null ? new linksawakening.ui.MenuTransition(10) : previous.enterAnotherMenu(10);
        enterFileSave();
        setFileSaveController(new FileSaveController(
                navigator::loadFileMenuBackgroundScene, state.sounds().sink()));
        input.inputState().tickEdges();
    }

    @Override
    public void saveCurrentPlayerState() {
        if (state.screens().currentSaveSlot() < 0) {
            return;
        }
        writePlayerSnapshot(saveState.saveRamStore(), state.screens().currentSaveSlot());
        try {
            saveState.saveRamStore().flush();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to persist player save state", exception);
        }
    }

    private void writePlayerSnapshot(SaveRamStore store, int slot) {
        var rooms = world.roomSession();
        var dungeon = rooms.dungeon();
        var story = rooms.story();
        store.writePlayerState(slot, player.playerState());
        saveCurrentSpawnLocation(store, slot);
        store.writeWorldMapRoom(slot, rooms.progress().worldMapRoom());
        store.writeRoomStatuses(
                slot,
                dungeon.overworldRoomStatusSnapshot(),
                dungeon.indoorARoomStatusSnapshot(),
                dungeon.indoorBRoomStatusSnapshot(),
                dungeon.colorDungeonRoomStatusSnapshot());
        store.writeDungeonItemFlags(slot, dungeon.dungeonItemFlagsSnapshot(), dungeon.colorDungeonItemFlagsSnapshot());
        store.writeDungeonProgressFlags(slot, dungeon.dungeonProgressFlagsSnapshot());
        store.writeFollowers(slot, story.followerFlags());
        store.writeBowWowState(slot, story.bowWowState());
        store.writeTarinFlag(slot, story.tarinFlag());
        store.writeRichardSpokenFlag(slot, story.richardSpokenFlag());
    }

    private void saveCurrentSpawnLocation(SaveRamStore store, int slot) {
        if (!world.roomSession().rooms().hasActiveRoom()) {
            return;
        }

        Warp spawn = world.roomSession().progress().respawnPoint();
        if (spawn != null) {
            store.writeSpawnLocation(
                    slot,
                    spawn.category() == 0 ? 0 : 1,
                    spawn.destMap(),
                    spawn.destRoom(),
                    spawn.destX(),
                    spawn.destY(),
                    world.roomSession().progress().respawnIndoorRoom());
            return;
        }
        ActiveRoom room = world.roomSession().rooms().activeRoomOrNull();
        boolean isIndoor = room.mapCategory() != Warp.CATEGORY_OVERWORLD;
        int indoorRoom = world.roomSession().rooms().indoorRoomPositionForSave();
        if (indoorRoom < 0) {
            // Small houses/caves have no map-layout table. The source leaves
            // wIndoorRoom at its previous value in that case.
            indoorRoom = store.readSlot(slot).spawnIndoorRoom();
        }
        store.writeSpawnLocation(
                slot,
                isIndoor ? 1 : 0,
                isIndoor ? room.mapId() : 0,
                room.roomId(),
                player.link().position().roomEntryRomPositionX(),
                player.link().position().roomEntryRomPositionY(),
                indoorRoom);
    }

    private void enterGameplay() {
        state.screens().fileMenu().clear();
        state.screens().fileSave().clear();
        navigator.show(ScreenKind.GAMEPLAY);
    }

    private void enterFileSave() {
        navigator.show(ScreenKind.FILE_SAVE);
        state.screens().fileMenu().clear();
    }

    private void setFileSaveController(FileSaveController controller) {
        state.screens().fileSave().open(controller);
    }

    private void loadSavedRoom(SaveSlotState saved) {
        if (saved.spawnIsIndoor() != 0) {
            rom.gpu().loadBaseTiles(rom.romData());
            world.roomSession()
                    .rooms()
                    .loadIndoorFromSavedPosition(saved.spawnMapId(), saved.spawnMapRoom(), saved.spawnIndoorRoom());
            player.link().animation().setDirection(Direction.UP.javaIndex());
        } else {
            world.roomSession().rooms().loadInitialOverworld(saved.spawnMapRoom());
            player.link().animation().setDirection(Direction.DOWN.javaIndex());
        }
    }
}
