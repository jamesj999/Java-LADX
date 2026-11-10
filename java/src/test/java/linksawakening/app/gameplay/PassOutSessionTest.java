package linksawakening.app.gameplay;

import linksawakening.app.GameSession;
import linksawakening.testing.RequiresRom;
import linksawakening.testing.TestRom;
import linksawakening.rom.Rom;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@RequiresRom
class PassOutSessionTest {
    @Test void zeroHealthLocksInputAndRunsRomAnimationUntilScreenLoad() throws Exception {
        var game = new GameSession();
        game.initializeHeadlessNewGame();
        var link = game.diagnostics().link();
        // Finish the opening bed pose before exercising ordinary gameplay motion.
        for (int i = 0; i < 520; i++) game.headlessFrame();
        for (int i = 0; i < 600 && game.diagnostics().dialogController().isActive(); i++) {
            game.headlessButton("a", (i & 1) == 0);
            game.headlessFrame();
        }
        game.headlessButton("a", false);
        assertFalse(game.diagnostics().dialogController().isActive());
        game.headlessButton("right", true);
        for (int i = 0; i < 32; i++) game.headlessFrame();
        game.headlessButton("right", false);
        var player = game.diagnostics().playerState();
        player.setInvincibilityCounter(64);
        player.setHealth(0);
        int x = link.pixelX(), y = link.pixelY();
        game.headlessButton("right", true);
        game.headlessFrame();
        assertEquals(7, link.romMotionState());
        assertEquals(0x10, link.passOutBgPaletteEffectAddress());
        game.headlessButton("start", true);
        game.headlessButton("a", true);
        assertEquals(0, link.romPhysicsModifier());
        assertEquals(0, player.invincibilityCounter());
        var expected = new LinkPassOutAnimation(new Rom(TestRom.open().readAllBytes()));
        for (int i = 0; i <= 191; i++) {
            assertEquals(expected.countdown(), link.passOutCountdown());
            assertEquals(expected.animationState(), link.romAnimationState());
            assertEquals(expected.paletteEffect(), link.passOutPaletteEffect());
            assertTrue(game.diagnostics().inventoryMenu().isFullyClosed());
            assertFalse(game.diagnostics().dialogController().isActive());
            assertEquals(x, link.pixelX());
            assertEquals(y, link.pixelY());
            assertEquals(i == 191, link.gameOverScreenPending());
            if (i < 191) { expected.advanceFrame(); game.headlessFrame(); }
        }
    }
}
