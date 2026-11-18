package linksawakening.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.stream.IntStream;
import linksawakening.gpu.Framebuffer;
import linksawakening.render.RenderScreen;
import linksawakening.save.SaveRamStore;
import linksawakening.testing.RequiresRom;
import org.junit.jupiter.api.Test;

@RequiresRom
final class WorldMapScreenTest {
    @Test
    void selectRendersVisitedRoomsAndInspectorAndReturnsToTheSameIndoorRoom() {
        try (var game = new GameSession((pcm, rate) -> {})) {
            game.initializeHeadlessIntro();
            var store = SaveRamStore.inMemory();
            store.createNewGame(0, new int[] {1, 0, 0, 0, 0});
            store.writeSpawnLocation(0, 1, 0x10, 0xA3, 0x58, 0x52, 0);
            store.writeWorldMapRoom(0, 0xA2);
            game.saveState().saveRamStore = store;
            game.state().screens().selectSaveSlot(0);
            game.saves().startSavedGame(store.readSlot(0));
            var rooms = game.services().world().roomSession();
            var room = rooms.rooms().requireActiveRoom();
            assertEquals(0xA2, rooms.progress().worldMapRoom());

            byte[] roomFrame = game.headlessFrame();
            assertTrue(Arrays.equals(roomFrame, press(game, "select")));
            assertEquals(RenderScreen.WORLD_MAP, game.renderer().currentRenderScreen());
            assertWhite(frames(game, 16));
            byte[] hidden = frames(game, 25);
            assertFalse(Arrays.equals(roomFrame, hidden));

            press(game, "select");
            assertEquals(RenderScreen.WORLD_MAP, game.renderer().currentRenderScreen());
            assertWhite(frames(game, 16));
            frames(game, 31);
            assertEquals(RenderScreen.WORLD_MAP, game.renderer().currentRenderScreen());
            game.headlessFrame();
            assertEquals(RenderScreen.OVERWORLD, game.renderer().currentRenderScreen());

            var dungeon = rooms.dungeon();
            byte[] visited = dungeon.overworldRoomStatusSnapshot();
            visited[0x44] |= (byte) 0x80;
            rooms.story()
                    .restoreRoomStatuses(
                            visited,
                            dungeon.indoorARoomStatusSnapshot(),
                            dungeon.indoorBRoomStatusSnapshot(),
                            dungeon.colorDungeonRoomStatusSnapshot());
            press(game, "select");
            byte[] revealed = frames(game, 41);
            assertTrue(IntStream.range(40, 48).anyMatch(y -> {
                int row = (y * Framebuffer.WIDTH + 48) * 4;
                return !Arrays.equals(hidden, row, row + 32, revealed, row, row + 32);
            }));

            press(game, "a");
            byte[] inspector = revealed;
            for (int i = 0; i < 120; i++) inspector = game.headlessFrame();
            int top = 8 * Framebuffer.WIDTH * 4;
            int end = 32 * Framebuffer.WIDTH * 4;
            assertFalse(Arrays.equals(revealed, top, end, inspector, top, end));
            press(game, "b");
            assertEquals(RenderScreen.WORLD_MAP, game.renderer().currentRenderScreen());
            for (int i = 0; i < 20; i++) game.headlessFrame();
            press(game, "select");
            frames(game, 48);
            assertEquals(RenderScreen.OVERWORLD, game.renderer().currentRenderScreen());
            assertSame(room, rooms.rooms().requireActiveRoom());
            game.saves().saveCurrentPlayerState();
            assertEquals(0xA2, store.readSlot(0).worldMapRoom());
            rooms.rooms().loadInitialOverworld(0x45);
            assertEquals(0x45, rooms.progress().worldMapRoom());
            assertEquals(0x80, dungeon.overworldRoomStatusSnapshot()[0x45] & 0x80);
        }
    }

    private static byte[] frames(GameSession game, int count) {
        byte[] pixels = game.headlessFrame();
        for (int i = 1; i < count; i++) pixels = game.headlessFrame();
        return pixels;
    }

    private static void assertWhite(byte[] pixels) {
        byte[] white = new byte[pixels.length];
        Arrays.fill(white, (byte) 0xFF);
        assertTrue(Arrays.equals(white, pixels));
    }

    private static byte[] press(GameSession game, String button) {
        game.headlessButton(button, true);
        byte[] pixels = game.headlessFrame();
        game.headlessButton(button, false);
        return pixels;
    }
}
