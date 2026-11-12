package linksawakening.app;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import linksawakening.app.flow.SaveCoordinator;
import linksawakening.render.RenderScreen;
import linksawakening.save.SaveRamStore;
import linksawakening.save.SaveSlotState;
import linksawakening.state.PlayerState;
import linksawakening.testing.RequiresRom;
import linksawakening.world.room.RoomSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Real session gather/restore with a fresh host store and session on reload. */
@RequiresRom
final class SaveRoundTripTest {
    @Test
    void restoredHouseDoorSpawnWaitsForLinkToWalkIntoDoor() throws Exception {
        GameSession game = new GameSession((pcm, rate) -> {});
        game.initializeHeadlessIntro();
        try {
            SaveRamStore store = SaveRamStore.inMemory();
            store.createNewGame(0, new int[] {1, 0, 0, 0, 0});
            // Marin house's exit record in IndoorsA3: outside A2, ($58,$52).
            store.writeSpawnLocation(0, 0, 0, 0xA2, 0x58, 0x52, 0);
            store.writeTarinFlag(0, 1);
            game.saveState().saveRamStore = store;
            game.flows().router().startFileSelection();
            press(game, "a");
            RoomSession room = game.services().world().roomSession();
            for (int frame = 0; frame < 40; frame++) game.headlessFrame();
            assertEquals(0, room.rooms().mapCategory());
            assertEquals(0xA2, room.rooms().currentRoomId());
            game.headlessButton("down", true);
            for (int frame = 0; frame < 8; frame++) game.headlessFrame();
            game.headlessButton("down", false);
            game.headlessButton("up", true);
            for (int frame = 0; frame < 100; frame++) game.headlessFrame();
            game.headlessButton("up", false);
            assertEquals(1, room.rooms().mapCategory());
            assertEquals(0xA3, room.rooms().currentRoomId());
        } finally {
            game.close();
        }
    }

    @Test
    void saveAndQuitFlushesGameplayAndRestoresItAfterRelaunch(@TempDir Path directory) throws Exception {
        roundTrip(directory.resolve("azle.sav"), -1);
    }

    @Test
    void saveChordAlsoWorksWithStartPressedLast(@TempDir Path directory) throws Exception {
        roundTrip(directory.resolve("azle.sav"), -2);
    }

    @Test
    void gameOverSaveAndContinueFlushesGameplay(@TempDir Path directory) throws Exception {
        roundTrip(directory.resolve("azle.sav"), 0);
    }

    @Test
    void gameOverSaveAndQuitFlushesGameplay(@TempDir Path directory) throws Exception {
        roundTrip(directory.resolve("azle.sav"), 1);
    }

    @Test
    void blankNameReturnsToAnEmptySlotAndCannotStartAnonymousGameplay(@TempDir Path directory) throws Exception {
        GameSession game = new GameSession();
        game.initializeHeadlessIntro();
        try {
            Path path = directory.resolve("azle.sav");
            game.saveState().saveRamStore = SaveRamStore.open(path);
            game.flows().router().startFileSelection();
            press(game, "a");
            press(game, "start");
            assertEquals(
                    RenderScreen.FILE_MENU, game.state().screens().currentScreen.renderScreen());
            SaveRamStore reopened = SaveRamStore.open(path);
            assertEquals(0, reopened.saveFilesMask());
            assertArrayEquals(new int[5], reopened.savedNames()[0]);
            press(game, "a");
            assertTrue(game.state().screens().fileMenuController.snapshot().creation());
        } finally {
            game.close();
        }
    }

    private void roundTrip(Path path, int gameOverOption) throws Exception {
        GameSession game = new GameSession();
        game.initializeHeadlessIntro();
        try {
            SaveRamStore store = SaveRamStore.open(path);
            game.saveState().saveRamStore = store;
            game.flows().router().startFileSelection();
            press(game, "a"); // empty file -> name entry
            press(game, "a"); // enter first ROM character
            press(game, "start");
            assertEquals(
                    RenderScreen.FILE_MENU, game.state().screens().currentScreen.renderScreen());
            press(game, "a");
            assertEquals(
                    RenderScreen.OVERWORLD, game.state().screens().currentScreen.renderScreen());
            PlayerState player = game.services().player().playerState();
            RoomSession room = game.services().world().roomSession();
            // Represent progress made during play; exercise the session snapshot, not just the codec.
            player.equipmentLevels().setShieldLevel(1);
            player.inventory().setItemB(linksawakening.state.InventoryItem.SHIELD.romValue());
            player.wallet().setRupees(237);
            room.story().setTarinFlag(1);
            game.headlessFrame();
            SaveCoordinator saves = game.flows().saves();
            if (gameOverOption < 0) {
                playWakeUpPrefix(game);
                String chord = gameOverOption == -2 ? "select+b+a+start" : "start+a+b+select";
                game.headlessButton(chord, true);
                game.headlessFrame();
                game.headlessButton(chord, false);
                game.headlessFrame();
                assertEquals(
                        RenderScreen.FILE_SAVE,
                        game.state().screens().currentScreen.renderScreen());
                press(game, "down");
                press(game, "a");
            } else {
                player.vitals().setHealth(0);
                saves.finishGameOver(gameOverOption);
            }
            SaveSlotState expected = store.readSlot(0);
            assertEquals(1, store.saveFilesMask());
            assertEquals(237, expected.rupees());
            assertEquals(1, expected.shieldLevel());
            assertEquals(1, expected.tarinFlag());
            SaveRamStore reopened = SaveRamStore.open(path);
            assertArrayEquals(expected.rawSlot(), reopened.readSlot(0).rawSlot());
            GameSession relaunched = new GameSession();
            relaunched.initializeHeadlessIntro();
            try {
                relaunched.saveState().saveRamStore = reopened;
                relaunched.flows().router().startFileSelection();
                press(relaunched, "a");
                assertEquals(
                        RenderScreen.OVERWORLD,
                        relaunched.state().screens().currentScreen.renderScreen());
                PlayerState restored = relaunched.services().player().playerState();
                assertEquals(expected.rupees(), restored.wallet().rupees());
                assertEquals(expected.health(), restored.vitals().health());
                assertEquals(expected.itemB(), restored.inventory().itemB());
                assertEquals(expected.shieldLevel(), restored.equipmentLevels().shieldLevel());
                assertEquals(
                        expected.tarinFlag(),
                        relaunched.services().world().roomSession().story().tarinFlag());
                RoomSession restoredRoom = relaunched.services().world().roomSession();
                assertArrayEquals(
                        expected.overworldRoomStatus(), restoredRoom.dungeon().overworldRoomStatusSnapshot());
                assertArrayEquals(
                        expected.indoorARoomStatus(), restoredRoom.dungeon().indoorARoomStatusSnapshot());
                assertArrayEquals(
                        expected.indoorBRoomStatus(), restoredRoom.dungeon().indoorBRoomStatusSnapshot());
                assertArrayEquals(
                        expected.colorDungeonRoomStatus(),
                        restoredRoom.dungeon().colorDungeonRoomStatusSnapshot());
                assertArrayEquals(
                        expected.dungeonItemFlags(), restoredRoom.dungeon().dungeonItemFlagsSnapshot());
                assertArrayEquals(
                        expected.colorDungeonItemFlags(), restoredRoom.dungeon().colorDungeonItemFlagsSnapshot());
                assertArrayEquals(
                        expected.dungeonProgressFlags(), restoredRoom.dungeon().dungeonProgressFlagsSnapshot());
                var restoredLink = relaunched.services().player().link();
                assertEquals(expected.spawnPositionX(), restoredLink.position().roomEntryRomPositionX());
                assertEquals(expected.spawnPositionY(), restoredLink.position().roomEntryRomPositionY());
                assertEquals(expected.spawnMapRoom(), restoredRoom.rooms().currentRoomId());
            } finally {
                relaunched.close();
            }
        } finally {
            game.close();
        }
    }

    private static void playWakeUpPrefix(GameSession game) throws Exception {
        // Existing opening replay through the bed exit (first eight commands).
        var commands = java.nio.file.Files.readAllLines(Path.of("../tools/opening/tail-cave-warp-route.tsv")).stream()
                .filter(line -> !line.isBlank() && !line.startsWith("#"))
                .limit(8)
                .toList();
        for (String command : commands) {
            String[] fields = command.split("\\s+");
            int held = Integer.parseInt(fields[1]);
            int idle = Integer.parseInt(fields[2]);
            if (held > 0) game.headlessButton(fields[0], true);
            for (int frame = 0; frame < held; frame++) game.headlessFrame();
            if (held > 0) game.headlessButton(fields[0], false);
            for (int frame = 0; frame < idle; frame++) game.headlessFrame();
        }
        assertFalse(game.services().ui().dialogController().isActive());
    }

    private static void press(GameSession game, String button) {
        game.headlessButton(button, true);
        game.headlessFrame();
        game.headlessButton(button, false);
        game.headlessFrame();
    }
}
