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
        player.setDeathCount(99);
        player.setActivePowerUp(linksawakening.state.PlayerState.ACTIVE_POWER_UP_PIECE_OF_POWER);
        player.applyRomEnemyDamage(1);
        assertEquals(1, player.powerUpHits());
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
        assertEquals(0, player.activePowerUp());
        assertEquals(0, player.powerUpHits());
        assertEquals(100, player.deathCount());
        assertEquals(0x0100, player.deathCountBcd());
        assertEquals(1, link.gameOverStage());
        assertEquals(1, link.romPhysicsModifier()); // $FF9C aliases hGameOverStage.
        assertEquals(16, link.gameOverCountdown());
        game.headlessFrame();
        assertEquals(2, link.gameOverStage());
        assertEquals(15, link.gameOverCountdown());
        game.headlessFrame();
        assertEquals(3, link.gameOverStage());
        assertEquals(14, link.gameOverCountdown());
        for (int i = 13; i >= 1; i--) {
            game.headlessFrame();
            assertEquals(3, link.gameOverStage());
            assertEquals(i, link.gameOverCountdown());
        }
        game.headlessFrame();
        assertEquals(4, link.gameOverStage());
        assertEquals(0, link.gameOverCountdown());
        for (int i = 0; i < 20; i++) game.headlessFrame();
        assertEquals(0, player.activePowerUp());
        assertEquals(0, player.powerUpHits());
        assertEquals(100, player.deathCount());
        assertEquals(4, link.gameOverStage());
        byte[] actual = game.headlessFrame();
        byte[] romBytes;
        try (var input = TestRom.open()) { romBytes = input.readAllBytes(); }
        var rom = new Rom(romBytes);
        var gpu = game.diagnostics().gpu();
        for (int i = 0; i < 0x500; i++) {
            assertEquals((byte) rom.u8(linksawakening.rom.RomSymbols.SaveMenuTiles.plus(i)),
                gpu.readVRAM(0x800 + i), "Save-menu VRAM byte " + i);
        }
        int[] initialTiles = new int[1024], initialAttrs = new int[1024];
        java.util.Arrays.fill(initialAttrs, 5);
        for (int yTile = 0; yTile < 32; yTile++) for (int xTile = 0; xTile < 20; xTile++) {
            initialTiles[yTile * 32 + xTile] = 0xAE + ((xTile ^ yTile) & 1);
        }
        var room = game.diagnostics().roomSession().renderSnapshot();
        var frame = new linksawakening.ui.FileMenuFrameSnapshot("game_over", 0, false, false, 0, 0,
            new int[5],
            linksawakening.rom.RomBank.decodeBackgroundMap(romBytes, 8, 0x698F, initialTiles),
            linksawakening.rom.RomBank.decodeBackgroundMap(romBytes, 0x24, 0x61E5, initialAttrs),
            room.palettes(), room.entities().spriteSelection().objectPalettes(), java.util.List.of(
                new linksawakening.ui.GameOverMenu(rom).cursor()));
        byte[] expectedFrame = new byte[actual.length];
        new linksawakening.render.GameFrameSceneBuilder().build(
            linksawakening.render.GameFrameState.empty()
                .withScreen(linksawakening.render.RenderScreen.FILE_SAVE).withFileMenuFrame(frame))
            .drawTo(expectedFrame, gpu);
        assertArrayEquals(expectedFrame, actual, "ROM game-over background fills the session frame");
    }
}
