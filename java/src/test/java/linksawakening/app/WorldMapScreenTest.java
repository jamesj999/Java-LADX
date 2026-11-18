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
            assertEquals(0xA2, rooms.worldMapRoom());

            byte[] hidden = press(game, "select");
            assertEquals(RenderScreen.WORLD_MAP, game.renderer().currentRenderScreen());
            press(game, "select");
            var dungeon = rooms.dungeon();
            byte[] visited = dungeon.overworldRoomStatusSnapshot();
            visited[0x44] |= (byte) 0x80;
            rooms.story()
                    .restoreRoomStatuses(
                            visited,
                            dungeon.indoorARoomStatusSnapshot(),
                            dungeon.indoorBRoomStatusSnapshot(),
                            dungeon.colorDungeonRoomStatusSnapshot());
            byte[] revealed = press(game, "select");
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
            assertEquals(RenderScreen.OVERWORLD, game.renderer().currentRenderScreen());
            assertSame(room, rooms.rooms().requireActiveRoom());
            game.saves().saveCurrentPlayerState();
            assertEquals(0xA2, store.readSlot(0).worldMapRoom());
            rooms.rooms().loadInitialOverworld(0x45);
            assertEquals(0x45, rooms.worldMapRoom());
            assertEquals(0x80, dungeon.overworldRoomStatusSnapshot()[0x45] & 0x80);
        }
    }

    private static byte[] press(GameSession game, String button) {
        game.headlessButton(button, true);
        byte[] pixels = game.headlessFrame();
        game.headlessButton(button, false);
        return pixels;
    }
}
