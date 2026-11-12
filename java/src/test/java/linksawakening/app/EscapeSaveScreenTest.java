package linksawakening.app;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;

import java.nio.file.Files;
import java.nio.file.Path;
import linksawakening.app.screen.Screen;
import linksawakening.input.InputState;
import linksawakening.render.RenderScreen;
import linksawakening.testing.RequiresRom;
import linksawakening.world.transition.ScrollController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@RequiresRom
final class EscapeSaveScreenTest {
    @Test
    void escapeIsConsumedAtTheFrameBoundaryAndIgnoredOnTheSaveScreen() throws Exception {
        GameSession game = readyGame();
        game.headlessButton("escape", true);
        assertEquals(RenderScreen.OVERWORLD, screen(game));
        assertTrue(running(game));
        assertFalse(field(game, "inputState", InputState.class).isDown(GLFW_KEY_ESCAPE));
        game.headlessFrame();
        assertEquals(RenderScreen.FILE_SAVE, screen(game));
        game.headlessButton("escape", false);
        press(game, "escape");
        assertEquals(RenderScreen.FILE_SAVE, screen(game));
        assertTrue(running(game));
        press(game, "a"); // Existing first option returns to gameplay.
        assertEquals(RenderScreen.OVERWORLD, screen(game));
        game.headlessFrame();
        assertEquals(RenderScreen.OVERWORLD, screen(game));
    }

    enum Blocker {
        DIALOG,
        INVENTORY,
        SCROLL,
        TRANSITION,
        PASS_OUT
    }

    @ParameterizedTest
    @EnumSource(Blocker.class)
    void blockedEscapeIsDroppedRatherThanOpenedLater(Blocker blocker) throws Exception {
        GameSession game = readyGame();
        var state = game.diagnostics();
        switch (blocker) {
            case DIALOG -> state.dialogController().open("Test");
            case INVENTORY -> {
                press(game, "start");
                frames(game, 60);
                assertTrue(state.inventoryMenu().isFullyOpen());
            }
            case SCROLL -> state.scrollController()
                    .start(ScrollController.RIGHT, 80, 80, state.roomSession().renderSnapshot(), 160);
            case TRANSITION -> state.transitionController().startFadeOut(() -> {});
            case PASS_OUT -> state.link().beginPassOut();
        }
        press(game, "escape");
        assertEquals(RenderScreen.OVERWORLD, screen(game));
        assertTrue(running(game));
        switch (blocker) {
            case DIALOG -> {
                for (int i = 0; i < 100 && state.dialogController().isActive(); i++) {
                    state.dialogController().advance();
                    game.headlessFrame();
                }
                assertFalse(state.dialogController().isActive());
            }
            case INVENTORY -> {
                press(game, "start");
                frames(game, 60);
                assertTrue(state.inventoryMenu().isFullyClosed());
            }
            case SCROLL -> state.scrollController().tick(160);
            case TRANSITION -> frames(game, 40);
            case PASS_OUT -> state.link().finishGameOver();
        }
        frames(game, 2);
        assertEquals(RenderScreen.OVERWORLD, screen(game), "Blocked request must be discarded");
        press(game, "escape");
        assertEquals(RenderScreen.FILE_SAVE, screen(game), "A fresh unblocked request must work");
    }

    @Test
    void wakingUpBlocksAndDropsEscape() throws Exception {
        GameSession game = new GameSession();
        game.initializeHeadlessNewGame();
        press(game, "escape");
        assertEquals(RenderScreen.OVERWORLD, screen(game));
        assertTrue(running(game));
        finishWakeUp(game);
        assertEquals(RenderScreen.OVERWORLD, screen(game));
        press(game, "escape");
        assertEquals(RenderScreen.FILE_SAVE, screen(game));
    }

    enum QuitScreen {
        INTRO,
        TITLE,
        FILE_SELECTION,
        NAME_ENTRY
    }

    @ParameterizedTest
    @EnumSource(QuitScreen.class)
    void escapeStillQuitsOutsideGameplay(QuitScreen target) throws Exception {
        GameSession game = new GameSession();
        game.initializeHeadlessIntro();
        if (target != QuitScreen.INTRO) {
            press(game, "start");
            assertEquals(RenderScreen.TITLE, screen(game));
        }
        if (target == QuitScreen.FILE_SELECTION || target == QuitScreen.NAME_ENTRY) {
            press(game, "start");
            assertEquals(RenderScreen.FILE_MENU, screen(game));
        }
        if (target == QuitScreen.NAME_ENTRY) press(game, "a");
        assertTrue(running(game));
        game.headlessButton("escape", true);
        assertFalse(running(game));
    }

    @Test
    void romChordStillOpensTheSameScreen() throws Exception {
        GameSession game = readyGame();
        game.headlessButton("start+a+b+select", true);
        game.headlessFrame();
        assertEquals(RenderScreen.FILE_SAVE, screen(game));
        assertTrue(running(game));
    }

    @Test
    void dxInventoryReturnKeepsMotionAndSaveBlockedUntilTheFadeFinishes() throws Exception {
        GameSession game = readyGame();
        var menu = game.diagnostics().inventoryMenu();
        press(game, "start");
        frames(game, 60);
        assertTrue(menu.isFullyOpen());
        menu.requestToggle();
        game.headlessButton("down", true);
        int x = game.diagnostics().headlessState().x(),
                y = game.diagnostics().headlessState().y();
        for (int frame = 0; menu.shouldBlockOverworldInput(); frame++) {
            assertTrue(frame < 60, "DX return must finish");
            game.headlessButton("escape", true);
            game.headlessFrame();
            game.headlessButton("escape", false);
            assertEquals(RenderScreen.OVERWORLD, screen(game));
            assertEquals(x, game.diagnostics().headlessState().x());
            assertEquals(y, game.diagnostics().headlessState().y());
        }
        // $00:1A0F restores normal motion then returns; movement begins next frame.
        game.headlessFrame();
        assertTrue(game.diagnostics().headlessState().y() > y);
        assertEquals(RenderScreen.OVERWORLD, screen(game), "Blocked Escape must not queue a save");
        game.headlessButton("down", false);
        press(game, "escape");
        assertEquals(RenderScreen.FILE_SAVE, screen(game));
    }

    private static GameSession readyGame() throws Exception {
        GameSession game = new GameSession();
        game.initializeHeadlessNewGame();
        finishWakeUp(game);
        return game;
    }

    private static void finishWakeUp(GameSession game) throws Exception {
        // Replay only the existing opening route's wake-up and bed-exit prefix.
        for (String command : Files.readAllLines(Path.of("../tools/opening/tail-cave-warp-route.tsv")).stream()
                .filter(line -> !line.isBlank() && !line.startsWith("#"))
                .limit(8)
                .toList()) {
            String[] fields = command.split("\\s+");
            int held = Integer.parseInt(fields[1]);
            if (held > 0) game.headlessButton(fields[0], true);
            frames(game, held);
            if (held > 0) game.headlessButton(fields[0], false);
            frames(game, Integer.parseInt(fields[2]));
        }
        assertFalse(game.diagnostics().dialogController().isActive());
    }

    private static void press(GameSession game, String button) {
        game.headlessButton(button, true);
        game.headlessFrame();
        game.headlessButton(button, false);
        game.headlessFrame();
    }

    private static void frames(GameSession game, int count) {
        for (int i = 0; i < count; i++) game.headlessFrame();
    }

    private static RenderScreen screen(GameSession game) throws Exception {
        return field(game, "currentScreen", Screen.class).renderScreen();
    }

    private static boolean running(GameSession game) throws Exception {
        return field(game, "running", Boolean.class);
    }

    private static <T> T field(GameSession game, String name, Class<T> type) throws Exception {
        var field = GameSession.class.getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(game));
    }
}
