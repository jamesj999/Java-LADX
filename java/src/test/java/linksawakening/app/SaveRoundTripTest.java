package linksawakening.app;

import linksawakening.app.screen.Screen;
import linksawakening.render.RenderScreen;
import linksawakening.save.SaveRamStore;
import linksawakening.save.SaveSlotState;
import linksawakening.state.PlayerState;
import linksawakening.testing.RequiresRom;
import linksawakening.world.room.RoomSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Real session gather/restore with a fresh host store and session on reload. */
@RequiresRom
final class SaveRoundTripTest {
    @Test
    void saveAndQuitFlushesGameplayAndRestoresItAfterRelaunch(@TempDir Path directory) throws Exception {
        roundTrip(directory.resolve("azle.sav"), -1);
    }

    @Test
    void gameOverSaveAndContinueFlushesGameplay(@TempDir Path directory) throws Exception {
        roundTrip(directory.resolve("azle.sav"), 0);
    }

    @Test
    void gameOverSaveAndQuitFlushesGameplay(@TempDir Path directory) throws Exception {
        roundTrip(directory.resolve("azle.sav"), 1);
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
            // File creation returns to selection in the ROM; older versions entered gameplay.
            if (get(game, "currentScreen", Screen.class).renderScreen() == RenderScreen.FILE_MENU) {
                press(game, "a");
            }
            assertEquals(RenderScreen.OVERWORLD, get(game, "currentScreen", Screen.class).renderScreen());
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
                saves.startFileSave();
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
                assertEquals(RenderScreen.OVERWORLD,
                    get(relaunched, "currentScreen", Screen.class).renderScreen());
                PlayerState restored = get(relaunched, "playerState", PlayerState.class);
                assertEquals(expected.rupees(), restored.rupees());
                assertEquals(expected.health(), restored.health());
                assertEquals(expected.itemB(), restored.itemB());
                assertEquals(expected.shieldLevel(), restored.shieldLevel());
                assertEquals(expected.tarinFlag(), get(relaunched, "roomSession", RoomSession.class).tarinFlag());
                assertArrayEquals(expected.overworldRoomStatus(),
                    get(relaunched, "roomSession", RoomSession.class).overworldRoomStatusSnapshot());
            } finally {
                call(relaunched, "cleanup", Void.class);
            }
        } finally {
            call(game, "cleanup", Void.class);
        }
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
