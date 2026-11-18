package linksawakening;

import linksawakening.audio.music.MusicTrackIds;
import linksawakening.config.AppConfig;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.render.RenderScreen;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_TAB;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

final class MainFileMenuFlowTest {

    @Test
    void titleEnterIsTheFileMenuBoundary() {
        assertTrue(Main.shouldEnterFileSelection(Main.SCREEN_TITLE, GLFW_KEY_ENTER, GLFW_PRESS));
        assertFalse(Main.shouldEnterFileSelection(Main.SCREEN_OVERWORLD, GLFW_KEY_ENTER, GLFW_PRESS));
    }

    @Test
    void fileMenuHasItsOwnRenderScreenAndDisassemblyMusicTrack() {
        assertEquals(RenderScreen.FILE_MENU, Main.renderScreenFor(Main.SCREEN_FILE_MENU));
        assertEquals(MusicTrackIds.MUSIC_FILE_SELECT, Main.fileSelectionMusicTrack());
    }

    @Test
    void anyChordMemberCanCompleteTheFileSaveShortcut() {
        InputConfig inputConfig = new InputConfig(
            GLFW_KEY_ENTER, 1, 2, 3, 4, 5, 6, GLFW_KEY_TAB);
        InputState inputState = new InputState();
        inputState.onKeyEvent(5, GLFW_PRESS);
        inputState.onKeyEvent(6, GLFW_PRESS);
        inputState.onKeyEvent(GLFW_KEY_ENTER, GLFW_PRESS);
        assertFalse(Main.shouldEnterFileSave(inputConfig, inputState, GLFW_KEY_ENTER, GLFW_PRESS));
        inputState.onKeyEvent(GLFW_KEY_TAB, GLFW_PRESS);

        assertTrue(Main.shouldEnterFileSave(inputConfig, inputState, GLFW_KEY_TAB, GLFW_PRESS));
        assertTrue(Main.shouldEnterFileSave(inputConfig, inputState, GLFW_KEY_ENTER, GLFW_PRESS));
        assertTrue(Main.shouldEnterFileSave(inputConfig, inputState, 5, GLFW_PRESS));
        assertTrue(Main.shouldEnterFileSave(inputConfig, inputState, 6, GLFW_PRESS));
        assertFalse(Main.shouldEnterFileSave(inputConfig, inputState, 7, GLFW_PRESS));
        assertFalse(Main.shouldEnterFileSave(inputConfig, inputState, GLFW_KEY_TAB, GLFW_RELEASE));
        inputState.onKeyEvent(6, GLFW_RELEASE);
        assertFalse(Main.shouldEnterFileSave(inputConfig, inputState, GLFW_KEY_TAB, GLFW_PRESS));
    }

    @Test
    void selectingANewlyRegisteredFileUsesTheDedicatedNewGameBootstrap() throws Exception {
        String source = Files.readString(Path.of("src/main/java/linksawakening/Main.java"));
        String branchStart = "if (action.type() == FileMenuAction.Type.LOAD_GAME)";
        int start = source.indexOf(branchStart);
        int end = source.indexOf("} else if (action.type() == FileMenuAction.Type.ERASE_SLOT)", start);

        String branch = source.substring(start, end);
        assertTrue(branch.contains("fileLoad = new FileLoadTransition(saved);"));
        assertTrue(source.contains("if (saved.spawnPositionX() == 0) startNewGame(saved);"));
        assertFalse(branch.contains("startConfiguredGameplay();"));
    }

    @Test
    void newGameBootstrapResetsPersistentWorldStateBeforeLoadingTheHouse() throws Exception {
        String source = Files.readString(Path.of("src/main/java/linksawakening/Main.java"));
        int start = source.indexOf("private static void startNewGame()");
        int end = source.indexOf("private static void startSavedGame", start);
        String bootstrap = source.substring(start, end);

        int reset = bootstrap.indexOf("roomSession.initializeNewGameWorldState();");
        int houseLoad = bootstrap.indexOf("roomSession.loadIndoor(profile.mapId(), profile.roomId());");
        assertTrue(reset >= 0);
        assertTrue(houseLoad > reset);
    }

    @Test
    void configuredNewGameDispatchesOnlyTheDedicatedBootstrap() {
        int[] calls = new int[2];
        AppConfig config = AppConfig.parse("""
            { "itemProfile": "NEW_GAME" }
            """);

        Main.dispatchConfiguredGameplay(config, () -> calls[0]++, () -> calls[1]++);

        assertEquals(1, calls[0]);
        assertEquals(0, calls[1]);
    }

    @Test
    void configuredDebugProfileDispatchesOnlyTheConfiguredLocationBootstrap() {
        int[] calls = new int[2];
        AppConfig config = AppConfig.parse("""
            { "itemProfile": "DEBUG_ALL_ITEMS" }
            """);

        Main.dispatchConfiguredGameplay(config, () -> calls[0]++, () -> calls[1]++);

        assertEquals(0, calls[0]);
        assertEquals(1, calls[1]);
    }

    @Test
    void startupTreatsSavedAndNewGamePositionsAsRomOamCoordinates() throws Exception {
        String source = Files.readString(Path.of("src/main/java/linksawakening/Main.java"));

        assertTrue(source.contains("link.setRoomEntryRomPosition(profile.entryX(), profile.entryY());"));
        assertTrue(source.contains("link.setRoomEntryRomPosition(saved.spawnPositionX(),"));
    }

    @Test
    void newGameRunsMarinsWakeSequenceWithRomBackedDialog() throws Exception {
        String source = Files.readString(Path.of("src/main/java/linksawakening/Main.java"));

        assertTrue(source.contains("new MarinWakeUpMotion()"));
        assertTrue(source.contains("tickNewGameWakeUp()"));
        assertTrue(source.contains("dialogTextLoader.load(new SignpostDialogRef(0,"));
        assertTrue(source.contains("tryOpenMarinFollowUpDialog(key)"));
    }

    @Test
    void wakeSequenceConsumesOverworldKeysBeforeSaveAndInventoryActions() throws Exception {
        String source = Files.readString(Path.of("src/main/java/linksawakening/Main.java"));
        int wakeGate = source.indexOf("if (marinWakeUpBlocksKeyInput()");
        int saveChord = source.indexOf("if (shouldEnterFileSave", wakeGate);
        int inventoryToggle = source.indexOf("inventoryController.dispatchToggleInput()", wakeGate);

        assertTrue(wakeGate >= 0);
        assertTrue(wakeGate < saveChord);
        assertTrue(wakeGate < inventoryToggle);
    }

    @Test
    void wakeReleaseUsesHeldDirectionsAndMarinFollowUpUsesOnlyA() throws Exception {
        String source = Files.readString(Path.of("src/main/java/linksawakening/Main.java"));
        int wakeTick = source.indexOf("private static boolean tickNewGameWakeUp()");
        int wakeTickEnd = source.indexOf("private static boolean marinWakeUpBlocksKeyInput()", wakeTick);
        String wakeBody = source.substring(wakeTick, wakeTickEnd);
        int followUp = source.indexOf("private static boolean tryOpenMarinFollowUpDialog(int key)");
        int followUpEnd = source.indexOf("private static OverworldDialogBlockers", followUp);
        String followUpBody = source.substring(followUp, followUpEnd);

        assertTrue(wakeBody.contains("inputState.isDown(inputConfig.upKey())"));
        assertFalse(wakeBody.contains("inputState.wasPressed"));
        assertTrue(followUpBody.contains("key != inputConfig.aKey()"));
        assertFalse(followUpBody.contains("GameplayDialogInput.isActionButtonKey"));
    }

    @Test
    void tarinTalkUsesOnlyAAndUnshieldedHouseSavesRestoreHisSequence() throws Exception {
        String source = Files.readString(Path.of("src/main/java/linksawakening/Main.java"));
        int tarinTalk = source.indexOf("private static boolean tryOpenTarinShieldDialog(int key)");
        int tarinTalkEnd = source.indexOf("private static boolean tryOpenMarinFollowUpDialog", tarinTalk);
        String tarinBody = source.substring(tarinTalk, tarinTalkEnd);
        int savedGame = source.indexOf("private static void startSavedGame");
        int savedGameEnd = source.indexOf("private static void startIntroCutscene", savedGame);
        String savedBody = source.substring(savedGame, savedGameEnd);

        assertTrue(tarinBody.contains("key != inputConfig.aKey()"));
        assertFalse(tarinBody.contains("GameplayDialogInput.isActionButtonKey"));
        assertTrue(savedBody.contains("restoreNewGameHouseRuntimeIfNeeded(saved)"));
        assertTrue(savedBody.contains("MarinWakeUpMotion.postWake()"));
    }

    @Test
    void fileMenuStartupUsesThePersistentSaveImageForMaskAndNames() throws Exception {
        String source = Files.readString(Path.of("src/main/java/linksawakening/Main.java"));
        int start = source.indexOf("fileMenuController = new FileMenuController(");
        int end = source.indexOf("private static void persistFileMenuMutation", start);
        String constructor = source.substring(start, end);

        assertTrue(constructor.contains("saveRamStore.saveFilesMask()"));
        assertTrue(constructor.contains("saveRamStore.savedNames()"));
        assertFalse(constructor.contains("new int[3][5]"));
    }

    @Test
    void registeringAFilePersistsTheSelectedSlotAndReturnsToSelection() throws Exception {
        String source = Files.readString(Path.of("src/main/java/linksawakening/Main.java"));
        int start = source.indexOf("if (action.type() == FileMenuAction.Type.REGISTER_FILE)");
        int end = source.indexOf("} else if (action.type() == FileMenuAction.Type.LOAD_GAME)", start);
        String branch = source.substring(start, end);

        assertTrue(branch.contains("saveRamStore.createNewGame(action.selectedSlot(), action.nameBytes());"));
        assertTrue(branch.contains("saveRamStore.flush();"));
        assertTrue(branch.contains("refreshFileSelection();"));
        assertFalse(branch.contains("startNewGame(saved);"));
    }

    @Test
    void copyAndErasePersistThenRebuildTheFileMenu() throws Exception {
        String source = Files.readString(Path.of("src/main/java/linksawakening/Main.java"));

        assertTrue(source.contains("saveRamStore.eraseSlot(action.selectedSlot())"));
        assertTrue(source.contains("saveRamStore.copySlot(action.selectedSlot(), action.targetSlot())"));
        assertTrue(source.contains("saveRamStore.flush();"));
        assertTrue(source.contains("startFileSelection();"));
    }

    @Test
    void saveAndQuitUsesTheModeledPlayerStateWriter() throws Exception {
        String source = Files.readString(Path.of("src/main/java/linksawakening/Main.java"));

        assertTrue(source.contains("saveCurrentPlayerState();"));
        assertTrue(source.contains("target.writePlayerState(slot, playerState);"));
        assertTrue(source.contains("saveCurrentSpawnLocation(target, slot);"));
        assertTrue(source.contains("target.writeSpawnLocation("));
        assertTrue(source.contains("target.writeRoomStatuses(slot,"));
        assertTrue(source.contains("target.writeDungeonItemFlags(slot,"));
    }

    @Test
    void initializedFileSelectionLoadsTheSavedRoomInsteadOfThrowing() throws Exception {
        String source = Files.readString(Path.of("src/main/java/linksawakening/Main.java"));
        int start = source.indexOf("} else if (action.type() == FileMenuAction.Type.LOAD_GAME)");
        int end = source.indexOf("inputState.tickEdges();", start);
        String branch = source.substring(start, end);

        assertTrue(branch.contains("new FileLoadTransition(saved)"));
        assertTrue(source.contains("else startSavedGame(saved);"));
        assertTrue(source.contains("roomSession.restoreRoomStatuses(saved.overworldRoomStatus(),"));
        assertTrue(source.contains("roomSession.setTarinFlag(saved.tarinFlag());"));
        assertTrue(source.contains(
            "target.writeTarinFlag(slot, roomSession.tarinFlag());"));
        assertTrue(source.contains("roomSession.restoreDungeonItemFlags(saved.dungeonItemFlags(),"));
        assertTrue(source.contains("link.setRoomEntryRomPosition(saved.spawnPositionX(),"));
        int tarinRestore = source.indexOf("roomSession.setTarinFlag(saved.tarinFlag());");
        int playerLevels = source.indexOf(
            "roomSession.setChestPlayerLevels(playerState.shieldLevel(),", tarinRestore);
        int savedLoad = source.indexOf("private static void startSavedGame");
        int restore = source.indexOf("restoreSavedState(saved);", savedLoad);
        assertTrue(source.contains("saved.spawnIndoorRoom()"));
        assertTrue(playerLevels > tarinRestore);
        assertTrue(restore > savedLoad);
        assertTrue(source.indexOf("roomSession.loadIndoorFromSavedPosition(", savedLoad) > restore);
        assertTrue(source.indexOf("roomSession.loadInitialOverworld(saved.spawnMapRoom())", savedLoad) > restore);
        assertFalse(branch.contains("UnsupportedOperationException"));
    }
}
