package linksawakening.app.gameplay;

import static org.junit.jupiter.api.Assertions.*;

import linksawakening.app.GameSession;
import linksawakening.rom.Rom;
import linksawakening.testing.RequiresRom;
import linksawakening.testing.TestRom;
import org.junit.jupiter.api.Test;

@RequiresRom
class PassOutSessionTest {
    @Test
    void zeroHealthLocksInputAndRunsRomAnimationUntilScreenLoad() throws Exception {
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
        player.vitals().setInvincibilityCounter(64);
        player.progression().setDeathCount(99);
        player.equipmentLevels().setActivePowerUp(linksawakening.state.PowerUp.PIECE_OF_POWER.romValue());
        player.vitals().applyRomEnemyDamage(1);
        assertEquals(1, player.equipmentLevels().powerUpHits());
        player.vitals().setHealth(0);
        int x = link.position().pixelX(), y = link.position().pixelY();
        game.headlessButton("right", true);
        game.headlessFrame();
        assertEquals(7, link.airborne().romMotionState());
        assertEquals(0x10, link.status().passOutBgPaletteEffectAddress());
        game.headlessButton("start", true);
        game.headlessButton("a", true);
        assertEquals(0, link.motion().romPhysicsModifier());
        assertEquals(0, player.vitals().invincibilityCounter());
        var expected = new LinkPassOutAnimation(Rom.of(TestRom.open().readAllBytes()));
        for (int i = 0; i <= 191; i++) {
            assertEquals(expected.countdown(), link.status().passOutCountdown());
            assertEquals(expected.animationState(), link.animation().romAnimationState());
            assertEquals(expected.paletteEffect(), link.status().passOutPaletteEffect());
            assertTrue(game.diagnostics().inventoryMenu().isFullyClosed());
            assertFalse(game.diagnostics().dialogController().isActive());
            assertEquals(x, link.position().pixelX());
            assertEquals(y, link.position().pixelY());
            assertEquals(i == 191, link.status().gameOverScreenPending());
            if (i < 191) {
                expected.advanceFrame();
                game.headlessFrame();
            }
        }
        assertEquals(0, player.equipmentLevels().activePowerUp());
        assertEquals(0, player.equipmentLevels().powerUpHits());
        assertEquals(100, player.progression().deathCount());
        assertEquals(0x0100, player.progression().deathCountBcd());
        assertEquals(1, link.status().gameOverStage());
        assertEquals(1, link.motion().romPhysicsModifier()); // $FF9C aliases hGameOverStage.
        assertEquals(16, link.status().gameOverCountdown());
        game.headlessFrame();
        assertEquals(2, link.status().gameOverStage());
        assertEquals(15, link.status().gameOverCountdown());
        game.headlessFrame();
        assertEquals(3, link.status().gameOverStage());
        assertEquals(14, link.status().gameOverCountdown());
        for (int i = 13; i >= 1; i--) {
            game.headlessFrame();
            assertEquals(3, link.status().gameOverStage());
            assertEquals(i, link.status().gameOverCountdown());
        }
        game.headlessFrame();
        assertEquals(4, link.status().gameOverStage());
        assertEquals(0, link.status().gameOverCountdown());
        for (int i = 0; i < 20; i++) game.headlessFrame();
        assertEquals(0, player.equipmentLevels().activePowerUp());
        assertEquals(0, player.equipmentLevels().powerUpHits());
        assertEquals(100, player.progression().deathCount());
        assertEquals(4, link.status().gameOverStage());
        byte[] actual = game.headlessFrame();
        byte[] romBytes;
        try (var input = TestRom.open()) {
            romBytes = input.readAllBytes();
        }
        var rom = Rom.of(romBytes);
        var gpu = game.diagnostics().gpu();
        for (int i = 0; i < 0x500; i++) {
            assertEquals(
                    (byte) rom.u8(linksawakening.rom.RomSymbols.SaveMenuTiles.plus(i)),
                    gpu.readVRAM(0x800 + i),
                    "Save-menu VRAM byte " + i);
        }
        int[] initialTiles = new int[1024], initialAttrs = new int[1024];
        java.util.Arrays.fill(initialAttrs, 5);
        for (int yTile = 0; yTile < 32; yTile++)
            for (int xTile = 0; xTile < 20; xTile++) {
                initialTiles[yTile * 32 + xTile] = 0xAE + ((xTile ^ yTile) & 1);
            }
        var room = game.diagnostics().roomSession().rooms().renderSnapshot();
        var frame = new linksawakening.ui.FileMenuFrameSnapshot.Builder()
                .sceneId("game_over")
                .selectedSlot(0)
                .commandRowArrowShifted(false)
                .creation(false)
                .selectedCharacter(0)
                .namePosition(0)
                .nameBytes(new int[5])
                .tilemap(linksawakening.rom.RomBank.decodeBackgroundMap(
                        rom, new linksawakening.rom.RomAddress(8, 0x698F), initialTiles))
                .attrmap(linksawakening.rom.RomBank.decodeBackgroundMap(
                        rom, new linksawakening.rom.RomAddress(0x24, 0x61E5), initialAttrs))
                .bgPalettes(room.palettes())
                .objectPalettes(room.entities().spriteSelection().objectPalettes())
                .sprites(java.util.List.of(new linksawakening.ui.GameOverMenu(rom).cursor()))
                .build();
        byte[] expectedFrame = new byte[actual.length];
        new linksawakening.render.GameFrameSceneBuilder()
                .build(linksawakening.render.GameFrameState.empty()
                        .withScreen(linksawakening.render.RenderScreen.FILE_SAVE)
                        .withFileMenuFrame(frame))
                .drawTo(expectedFrame, gpu);
        assertArrayEquals(expectedFrame, actual, "ROM game-over background fills the session frame");
    }
}
