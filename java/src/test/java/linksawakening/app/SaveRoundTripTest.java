package linksawakening.app;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import linksawakening.app.screen.Screen;
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
            set(game, "saveRamStore", store);
            call(game, "router", ScreenRouter.class).startFileSelection();
            press(game, "a");
            RoomSession room = get(game, "roomSession", RoomSession.class);
            for (int frame = 0; frame < 40; frame++) game.headlessFrame();
            assertEquals(0, room.mapCategory());
            assertEquals(0xA2, room.currentRoomId());
            game.headlessButton("down", true);
            for (int frame = 0; frame < 8; frame++) game.headlessFrame();
            game.headlessButton("down", false);
            game.headlessButton("up", true);
            for (int frame = 0; frame < 100; frame++) game.headlessFrame();
            game.headlessButton("up", false);
            assertEquals(1, room.mapCategory());
            assertEquals(0xA3, room.currentRoomId());
        } finally {
            call(game, "cleanup", Void.class);
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
            set(game, "saveRamStore", SaveRamStore.open(path));
            call(game, "router", ScreenRouter.class).startFileSelection();
            press(game, "a");
            press(game, "start");
            assertEquals(
                    RenderScreen.FILE_MENU,
                    get(game, "currentScreen", Screen.class).renderScreen());
            SaveRamStore reopened = SaveRamStore.open(path);
            assertEquals(0, reopened.saveFilesMask());
            assertArrayEquals(new int[5], reopened.savedNames()[0]);
            press(game, "a");
            assertTrue(get(game, "fileMenuController", linksawakening.ui.FileMenuController.class)
                    .snapshot()
                    .creation());
        } finally {
            call(game, "cleanup", Void.class);
        }
    }

    private void roundTrip(Path path, int gameOverOption) throws Exception {
        GameSession game = new GameSession();
        game.initializeHeadlessIntro();
        try {
            SaveRamStore store = SaveRamStore.open(path);
            set(game, "saveRamStore", store);
            call(game, "router", ScreenRouter.class).startFileSelection();
            press(game, "a"); // empty file -> name entry
            press(game, "a"); // enter first ROM character
            press(game, "start");
            assertEquals(
                    RenderScreen.FILE_MENU,
                    get(game, "currentScreen", Screen.class).renderScreen());
            press(game, "a");
            assertEquals(
                    RenderScreen.OVERWORLD,
                    get(game, "currentScreen", Screen.class).renderScreen());
            PlayerState player = get(game, "playerState", PlayerState.class);
            RoomSession room = get(game, "roomSession", RoomSession.class);
            // Represent progress made during play; exercise the session snapshot, not just the codec.
            player.setShieldLevel(1);
            player.setItemB(PlayerState.INVENTORY_SHIELD);
            player.setRupees(237);
            room.setTarinFlag(1);
            game.headlessFrame();
            SaveCoordinator saves = call(game, "saves", SaveCoordinator.class);
            if (gameOverOption < 0) {
                playWakeUpPrefix(game);
                String chord = gameOverOption == -2 ? "select+b+a+start" : "start+a+b+select";
                game.headlessButton(chord, true);
                game.headlessFrame();
                game.headlessButton(chord, false);
                game.headlessFrame();
                assertEquals(
                        RenderScreen.FILE_SAVE,
                        get(game, "currentScreen", Screen.class).renderScreen());
                press(game, "down");
                press(game, "a");
            } else {
                player.setHealth(0);
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
                set(relaunched, "saveRamStore", reopened);
                call(relaunched, "router", ScreenRouter.class).startFileSelection();
                press(relaunched, "a");
                assertEquals(
                        RenderScreen.OVERWORLD,
                        get(relaunched, "currentScreen", Screen.class).renderScreen());
                PlayerState restored = get(relaunched, "playerState", PlayerState.class);
                assertEquals(expected.rupees(), restored.rupees());
                assertEquals(expected.health(), restored.health());
                assertEquals(expected.itemB(), restored.itemB());
                assertEquals(expected.shieldLevel(), restored.shieldLevel());
                assertEquals(
                        expected.tarinFlag(),
                        get(relaunched, "roomSession", RoomSession.class).tarinFlag());
                RoomSession restoredRoom = get(relaunched, "roomSession", RoomSession.class);
                assertArrayEquals(expected.overworldRoomStatus(), restoredRoom.overworldRoomStatusSnapshot());
                assertArrayEquals(expected.indoorARoomStatus(), restoredRoom.indoorARoomStatusSnapshot());
                assertArrayEquals(expected.indoorBRoomStatus(), restoredRoom.indoorBRoomStatusSnapshot());
                assertArrayEquals(expected.colorDungeonRoomStatus(), restoredRoom.colorDungeonRoomStatusSnapshot());
                assertArrayEquals(expected.dungeonItemFlags(), restoredRoom.dungeonItemFlagsSnapshot());
                assertArrayEquals(expected.colorDungeonItemFlags(), restoredRoom.colorDungeonItemFlagsSnapshot());
                assertArrayEquals(expected.dungeonProgressFlags(), restoredRoom.dungeonProgressFlagsSnapshot());
                var restoredLink = get(relaunched, "link", linksawakening.entity.Link.class);
                assertEquals(expected.spawnPositionX(), restoredLink.roomEntryRomPositionX());
                assertEquals(expected.spawnPositionY(), restoredLink.roomEntryRomPositionY());
                assertEquals(expected.spawnMapRoom(), restoredRoom.currentRoomId());
            } finally {
                call(relaunched, "cleanup", Void.class);
            }
        } finally {
            call(game, "cleanup", Void.class);
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
        assertFalse(get(game, "dialogController", linksawakening.dialog.DialogController.class)
                .isActive());
    }

    private static void press(GameSession game, String button) {
        game.headlessButton(button, true);
        game.headlessFrame();
        game.headlessButton(button, false);
        game.headlessFrame();
    }

    private static void set(GameSession game, String name, Object value) throws Exception {
        var field = GameSession.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(game, value);
    }

    private static <T> T get(GameSession game, String name, Class<T> type) throws Exception {
        var field = GameSession.class.getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(game));
    }

    private static <T> T call(GameSession game, String name, Class<T> type) throws Exception {
        var method = GameSession.class.getDeclaredMethod(name);
        method.setAccessible(true);
        return type.cast(method.invoke(game));
    }
}
