package linksawakening.app;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.*;

import linksawakening.app.debug.DebugMenu;
import linksawakening.config.AppConfig;
import linksawakening.testing.RequiresRom;
import org.junit.jupiter.api.Test;

@RequiresRom
class DebugMenuIntegrationTest {
    @Test
    void menuPausesEditsTeleportsAndConsumesEscape() throws Exception {
        var game = new GameSession();
        game.initializeHeadlessNewGame();
        var config = SessionTestAccess.field(game, "appConfig");
        config.setAccessible(true);
        config.set(SessionTestAccess.owner(game, "appConfig"), AppConfig.parse("{\"debugEnabled\":true}"));
        game.onKeyEvent(GLFW_KEY_F2, GLFW_PRESS);
        game.onKeyEvent(GLFW_KEY_RIGHT, GLFW_PRESS);
        game.onKeyEvent(GLFW_KEY_F4, GLFW_PRESS);
        var menu = menu(game);
        assertTrue(menu.isOpen());
        int frame = game.diagnostics().frameCounter();
        game.headlessFrame();
        game.headlessFrame();
        assertEquals(frame, game.diagnostics().frameCounter());
        // Teleport to Tail Cave using the same menu actions as desktop.
        menu.entries().get(0).write().accept(1);
        menu.entries().get(1).write().accept(0);
        menu.entries().get(2).write().accept(0x17);
        menu.entries().get(3).write().accept(72);
        menu.entries().get(4).write().accept(96);
        game.onKeyEvent(GLFW_KEY_ENTER, GLFW_PRESS);
        assertEquals(0x17, game.diagnostics().roomSession().currentRoomId());
        assertEquals(1, game.diagnostics().roomSession().mapCategory());
        assertEquals(72, game.diagnostics().link().pixelX());
        assertEquals(96, game.diagnostics().link().pixelY());
        // Exercise category changes and the separate Color Dungeon tables.
        teleport(game, menu, 2, 0, 0x19);
        assertEquals(2, game.diagnostics().roomSession().mapCategory());
        teleport(game, menu, 1, 255, 0x12);
        assertEquals(255, game.diagnostics().roomSession().mapId());
        teleport(game, menu, 0, 0, 0x92);
        assertEquals(0, game.diagnostics().roomSession().mapCategory());
        assertEquals(0x92, game.diagnostics().roomSession().currentRoomId());
        game.onKeyEvent(GLFW_KEY_TAB, GLFW_PRESS);
        var sword = menu.entries().stream()
                .filter(e -> e.label().equals("A"))
                .findFirst()
                .orElseThrow();
        sword.write().accept(0);
        assertEquals(0, game.diagnostics().playerState().itemA());
        game.onKeyEvent(GLFW_KEY_TAB, GLFW_PRESS);
        game.diagnostics().playerState().applyEntityPickup(0x2E);
        menu.entries().getFirst().write().accept(123);
        assertEquals(123, game.diagnostics().playerState().rupees());
        assertEquals(0, game.diagnostics().playerState().addRupeeBuffer());
        game.onKeyEvent(GLFW_KEY_ESCAPE, GLFW_PRESS);
        assertFalse(menu.isOpen());
        var running = SessionTestAccess.field(game, "running");
        running.setAccessible(true);
        assertTrue(running.getBoolean(game));
        game.headlessFrame();
        assertEquals((frame + 1) & 255, game.diagnostics().frameCounter());
    }

    @Test
    void openingHouseMenuRemainsInteractiveAndEscapeDoesNotQuitGameplay() throws Exception {
        var game = new GameSession();
        game.initializeHeadlessNewGame();
        var config = SessionTestAccess.field(game, "appConfig");
        config.setAccessible(true);
        config.set(SessionTestAccess.owner(game, "appConfig"), AppConfig.parse("{\"debugEnabled\":true}"));
        // Keep the real Marin/Tarin wake-up sequence; do not skip it with F2.
        var before = game.diagnostics().headlessState();
        game.onKeyEvent(GLFW_KEY_F4, GLFW_PRESS);
        assertTrue(menu(game).isOpen());
        byte[] first = game.headlessFrame();
        game.onKeyEvent(GLFW_KEY_DOWN, GLFW_PRESS);
        assertEquals(1, menu(game).selected());
        assertFalse(java.util.Arrays.equals(first, game.headlessFrame()));
        game.onKeyEvent(GLFW_KEY_TAB, GLFW_PRESS);
        assertEquals("DEBUG 2/5 INVENTORY", menu(game).title());
        game.onKeyEvent(GLFW_KEY_RIGHT, GLFW_PRESS);
        assertEquals(1, game.diagnostics().playerState().itemA());
        game.headlessFrame();
        assertEquals(before.frame(), game.diagnostics().frameCounter());
        game.onKeyEvent(GLFW_KEY_F4, GLFW_PRESS);
        assertFalse(menu(game).isOpen());
        game.headlessFrame();
        assertEquals((before.frame() + 1) & 255, game.diagnostics().frameCounter());
        game.onKeyEvent(GLFW_KEY_F4, GLFW_PRESS);
        assertTrue(menu(game).isOpen());
        game.headlessFrame();
        game.onKeyEvent(GLFW_KEY_ESCAPE, GLFW_PRESS);
        assertFalse(menu(game).isOpen());
        game.onKeyEvent(GLFW_KEY_ESCAPE, GLFW_PRESS);
        var running = SessionTestAccess.field(game, "running");
        running.setAccessible(true);
        assertTrue(running.getBoolean(game));
        game.headlessFrame(); // Wake-up blocks and discards the save request.
        assertTrue(running.getBoolean(game));
    }

    @Test
    void configDisablesMenu() throws Exception {
        var game = new GameSession();
        game.initializeHeadlessNewGame();
        var config = SessionTestAccess.field(game, "appConfig");
        config.setAccessible(true);
        config.set(SessionTestAccess.owner(game, "appConfig"), AppConfig.parse("{\"debugEnabled\":false}"));
        game.onKeyEvent(GLFW_KEY_F4, GLFW_PRESS);
        assertNull(menu(game));
    }

    @Test
    void menuWaitsForRoomTransitionsAndDialogs() throws Exception {
        var game = new GameSession();
        game.initializeHeadlessNewGame();
        var config = SessionTestAccess.field(game, "appConfig");
        config.setAccessible(true);
        config.set(SessionTestAccess.owner(game, "appConfig"), AppConfig.parse("{\"debugEnabled\":true}"));
        game.onKeyEvent(GLFW_KEY_F2, GLFW_PRESS);
        game.diagnostics().transitionController().startFadeOut(() -> {});
        game.onKeyEvent(GLFW_KEY_F4, GLFW_PRESS);
        assertNull(menu(game));
        for (int i = 0; i < 32; i++) game.diagnostics().transitionController().tick();
        game.diagnostics().dialogController().open("Test dialog");
        game.onKeyEvent(GLFW_KEY_F4, GLFW_PRESS);
        assertNull(menu(game));
    }

    private void teleport(GameSession game, DebugMenu menu, int category, int map, int room) {
        menu.entries().get(0).write().accept(category);
        menu.entries().get(1).write().accept(map);
        menu.entries().get(2).write().accept(room);
        game.onKeyEvent(GLFW_KEY_ENTER, GLFW_PRESS);
        game.headlessFrame();
        assertEquals("Teleported. F4 resumes.", menu.message());
    }

    private DebugMenu menu(GameSession game) throws Exception {
        var field = SessionTestAccess.field(game, "debugMenu");
        field.setAccessible(true);
        return (DebugMenu) field.get(SessionTestAccess.owner(game, "debugMenu"));
    }
}
