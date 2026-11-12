package linksawakening.app;

import static linksawakening.world.room.RoomConstants.*;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;
import linksawakening.entity.Link;
import linksawakening.gpu.GPU;
import linksawakening.input.InputState;
import linksawakening.save.SaveRamStore;
import linksawakening.save.SaveSlotState;
import linksawakening.scene.BackgroundScene;
import linksawakening.startup.NewGameStartProfile;
import linksawakening.state.PlayerState;
import linksawakening.ui.FileSaveController;
import linksawakening.world.room.ActiveRoom;
import linksawakening.world.room.RoomSession;
import linksawakening.world.transition.Warp;

/** Persistence and room-entry orchestration, using the current session snapshot. */
public record SaveCoordinator(
        PlayerState playerState,
        RoomSession roomSession,
        Link link,
        GPU gpu,
        byte[] romData,
        SaveRamStore saveRamStore,
        int currentSaveSlot,
        InputState inputState,
        Runnable enterGameplay,
        Runnable clearOpening,
        Runnable initializeOpening,
        Runnable playHouseMusic,
        Runnable refreshOpening,
        Consumer<SaveSlotState> restoreOpening,
        Runnable startFileSelection,
        Runnable enterFileSave,
        Consumer<FileSaveController> showFileSave,
        Function<String, BackgroundScene> fileMenuBackground) {
    public void startNewGame() {
        enterGameplay.run();

        NewGameStartProfile profile = NewGameStartProfile.romDefaults();
        profile.initializePlayerState(playerState);
        roomSession.initializeNewGameWorldState();
        roomSession.setBirdKeyOwned(playerState.birdKeyCount() != 0);
        roomSession.setTailKeyOwned(playerState.tailKeyCount() != 0);
        roomSession.setBowWowState(0);
        link.setDirection(Link.DIRECTION_DOWN);
        gpu.loadBaseTiles(romData);
        roomSession.loadIndoor(profile.mapId(), profile.roomId());
        roomSession.setRespawnPoint(
                new Warp(
                        Warp.CATEGORY_INDOOR,
                        profile.mapId(),
                        profile.roomId(),
                        profile.entryX(),
                        profile.entryY(),
                        -1),
                0);
        link.setRoomEntryRomPosition(profile.entryX(), profile.entryY());
        initializeOpening.run();
        playHouseMusic.run();
        refreshOpening.run();
    }

    public void startSavedGame(SaveSlotState saved) {
        enterGameplay.run();
        clearOpening.run();
        playerState.applySavedGame(saved);
        roomSession.setBirdKeyOwned(playerState.birdKeyCount() != 0);
        roomSession.setTailKeyOwned(playerState.tailKeyCount() != 0);
        roomSession.restoreRoomStatuses(
                saved.overworldRoomStatus(),
                saved.indoorARoomStatus(),
                saved.indoorBRoomStatus(),
                saved.colorDungeonRoomStatus());
        roomSession.restoreDungeonItemFlags(saved.dungeonItemFlags(), saved.colorDungeonItemFlags());
        roomSession.restoreDungeonProgressFlags(saved.dungeonProgressFlags());
        roomSession.setBowWowState(saved.bowWowState());
        roomSession.setTarinFlag(saved.tarinFlag());
        roomSession.setRichardSpokenFlag(saved.richardSpokenFlag());
        roomSession.setHouseNpcTradeItem(playerState.tradeSequenceItem());
        roomSession.setChestPlayerLevels(
                playerState.shieldLevel(), playerState.swordLevel(), playerState.powerBraceletLevel());
        if (saved.spawnIsIndoor() != 0) {
            gpu.loadBaseTiles(romData);
            roomSession.loadIndoorFromSavedPosition(saved.spawnMapId(), saved.spawnMapRoom(), saved.spawnIndoorRoom());
            link.setDirection(Link.DIRECTION_UP);
        } else {
            roomSession.loadInitialOverworld(saved.spawnMapRoom());
            link.setDirection(Link.DIRECTION_DOWN);
        }
        link.setRoomEntryRomPosition(saved.spawnPositionX(), saved.spawnPositionY());
        roomSession.setRespawnPoint(
                new Warp(
                        saved.spawnIsIndoor() != 0 ? 1 : 0,
                        saved.spawnMapId(),
                        saved.spawnMapRoom(),
                        saved.spawnPositionX(),
                        saved.spawnPositionY(),
                        -1),
                saved.spawnIndoorRoom());
        restoreOpening.accept(saved);
    }

    /** $01:42D9-4335: options 0/1 save; option 2 skips the SRAM reload via wDBD1. */
    public void finishGameOver(int option) {
        if (playerState.health() == 0) {
            playerState.setHealth(new linksawakening.rom.Rom(romData)
                    .u8(new linksawakening.rom.RomAddress(1, 0x5295).plus(playerState.maxHearts())));
        }
        if (option != 2) saveCurrentPlayerState();
        clearOpening.run();
        link.finishGameOver();
        playerState.clearResourceBuffersForDebug();
        playerState.setActivePowerUp(PlayerState.ACTIVE_POWER_UP_NONE);
        roomSession.clearGameOverPowerCounter();
        roomSession.resetGameOverSwitchBlocks();
        if (option == 1) {
            startFileSelection.run();
            return;
        }
        if (option == 0 && currentSaveSlot >= 0) {
            startSavedGame(saveRamStore.readSlot(currentSaveSlot));
        } else {
            Warp spawn = roomSession.respawnPoint();
            if (spawn == null) throw new IllegalStateException("No game-over spawn point");
            gpu.loadBaseTiles(romData);
            if (spawn.category() == Warp.CATEGORY_OVERWORLD) {
                roomSession.loadInitialOverworld(spawn.destRoom());
                link.setDirection(Link.DIRECTION_DOWN);
            } else {
                roomSession.loadIndoorFromSavedPosition(
                        spawn.destMap(), spawn.destRoom(), roomSession.respawnIndoorRoom());
                link.setDirection(Link.DIRECTION_UP);
            }
            link.setRoomEntryRomPosition(spawn.destX(), spawn.destY());
            enterGameplay.run();
        }
        playerState.setInvincibilityCounter(0x80);
        inputState.tickEdges();
    }

    public void persistFileMenuMutation(Runnable mutation) {
        mutation.run();
        try {
            saveRamStore.flush();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to persist file-menu mutation", exception);
        }
        startFileSelection.run();
    }

    public void startFileSave() {
        enterFileSave.run();
        gpu.loadSaveMenuTiles(romData);
        showFileSave.accept(new FileSaveController(fileMenuBackground::apply));
        inputState.tickEdges();
    }

    public void saveCurrentPlayerState() {
        if (currentSaveSlot < 0 || playerState == null || saveRamStore == null) {
            return;
        }
        saveRamStore.writePlayerState(currentSaveSlot, playerState);
        saveCurrentSpawnLocation();
        if (roomSession != null) {
            saveRamStore.writeRoomStatuses(
                    currentSaveSlot,
                    roomSession.overworldRoomStatusSnapshot(),
                    roomSession.indoorARoomStatusSnapshot(),
                    roomSession.indoorBRoomStatusSnapshot(),
                    roomSession.colorDungeonRoomStatusSnapshot());
            saveRamStore.writeDungeonItemFlags(
                    currentSaveSlot,
                    roomSession.dungeonItemFlagsSnapshot(),
                    roomSession.colorDungeonItemFlagsSnapshot());
            saveRamStore.writeDungeonProgressFlags(currentSaveSlot, roomSession.dungeonProgressFlagsSnapshot());
            saveRamStore.writeBowWowState(currentSaveSlot, roomSession.bowWowState());
            saveRamStore.writeTarinFlag(currentSaveSlot, roomSession.tarinFlag());
            saveRamStore.writeRichardSpokenFlag(currentSaveSlot, roomSession.richardSpokenFlag());
        }
        try {
            saveRamStore.flush();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to persist player save state", exception);
        }
    }

    public void saveCurrentSpawnLocation() {
        if (roomSession == null || !roomSession.hasActiveRoom() || link == null) {
            return;
        }

        Warp spawn = roomSession.respawnPoint();
        if (spawn != null) {
            saveRamStore.writeSpawnLocation(
                    currentSaveSlot,
                    spawn.category() == 0 ? 0 : 1,
                    spawn.destMap(),
                    spawn.destRoom(),
                    spawn.destX(),
                    spawn.destY(),
                    roomSession.respawnIndoorRoom());
            return;
        }
        ActiveRoom room = roomSession.activeRoom();
        boolean isIndoor = room.mapCategory() != Warp.CATEGORY_OVERWORLD;
        int indoorRoom = roomSession.indoorRoomPositionForSave();
        if (indoorRoom < 0) {
            // Small houses/caves have no map-layout table. The source leaves
            // wIndoorRoom at its previous value in that case.
            indoorRoom = saveRamStore.readSlot(currentSaveSlot).spawnIndoorRoom();
        }
        saveRamStore.writeSpawnLocation(
                currentSaveSlot,
                isIndoor ? 1 : 0,
                isIndoor ? room.mapId() : 0,
                room.roomId(),
                link.roomEntryRomPositionX(),
                link.roomEntryRomPositionY(),
                indoorRoom);
    }
}
