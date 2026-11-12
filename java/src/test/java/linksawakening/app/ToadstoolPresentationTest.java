package linksawakening.app;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.*;

import linksawakening.config.AppConfig;
import linksawakening.testing.RequiresRom;
import org.junit.jupiter.api.Test;

@RequiresRom
final class ToadstoolPresentationTest {
    @Test
    void equippedUseHoldsItemOpens00ePausesAndReturnsToNormalWithoutSpendingIt() throws Exception {
        var game = game();
        var d = game.diagnostics();
        d.roomSession().rooms().loadIndoorFromSavedPosition(0x10, 0xA3, 0);
        d.link().position().setRoomEntryPixelPosition(40, 80);
        game.headlessFrame();
        use(game, "a");
        assertEquals(2, d.link().animation().dialogGotItem());
        assertEquals(0x2A, d.link().animation().dialogGotItemCountdown());
        assertEquals(0, d.link().animation().presentedDialogGotItem());
        for (int frame = 1; frame <= 40; frame++) {
            game.headlessFrame();
            assertEquals(0x2A - frame, d.link().animation().dialogGotItemCountdown());
            assertEquals(0x6B, d.link().animation().romAnimationState());
            assertEquals(40, d.link().position().pixelX());
            assertEquals(80, d.link().position().pixelY());
            assertEquals(frame == 40, d.dialogController().isActive());
        }
        for (int frame = 0; frame < 150; frame++) game.headlessFrame();
        assertEquals(2, d.link().animation().dialogGotItemCountdown());
        assertTrue(d.dialogController().visibleText().startsWith("It's the toad-"));
        // Finish all pages using real button edges, preserving the production dialog timing.
        for (int frame = 0; frame < 2000 && d.dialogController().isActive(); frame++) {
            game.headlessButton("a", frame % 24 < 8);
            game.headlessFrame();
        }
        game.headlessButton("a", false);
        assertFalse(d.dialogController().isActive());
        for (int frame = 0; frame < 3; frame++) game.headlessFrame();
        assertFalse(d.link().animation().dialogGotItemActive());
        assertEquals(0, d.link().animation().presentedDialogGotItem());
        assertNotEquals(0x6B, d.link().animation().romAnimationState());
        assertTrue(d.playerState().ammunition().hasToadstool());
        assertEquals(0, d.playerState().ammunition().magicPowderCount());
    }

    @Test
    void activePresentationBypassesInventoryAndPassOutDispatch() throws Exception {
        var game = game();
        var d = game.diagnostics();
        d.roomSession().rooms().loadIndoorFromSavedPosition(0x10, 0xA3, 0);
        d.link().position().setRoomEntryPixelPosition(40, 80);
        game.headlessFrame();
        use(game, "a");
        d.playerState().vitals().setHealth(0);
        game.headlessButton("start", true);
        game.headlessFrame();
        game.headlessButton("start", false);
        assertTrue(d.inventoryMenu().isFullyClosed());
        assertFalse(d.link().status().isPassingOut());
        assertEquals(0x29, d.link().animation().dialogGotItemCountdown());
    }

    @Test
    void witchAcceptsPreviouslyPresentedToadstoolAndOnlyBBranchClearsPresentation() throws Exception {
        for (boolean bSlot : new boolean[] {false, true}) {
            var game = game();
            var d = game.diagnostics();
            d.link().animation().presentToadstool();
            // This tests the ownership/presentation distinction, including the
            // ROM's A/B asymmetry at $05:4833-4847.
            d.roomSession().rooms().loadIndoorFromSavedPosition(0x0E, 0xA2, 0);
            d.link().position().setRoomEntryRomPosition(0x48, 0x56);
            d.link().animation().setDirection(linksawakening.state.Direction.UP.javaIndex());
            if (bSlot) {
                d.playerState().inventory().setItemA(0);
                d.playerState().inventory().setItemB(linksawakening.state.InventoryItem.MAGIC_POWDER.romValue());
            }
            game.headlessFrame(); // initialize the witch; got-item timer runs independently
            use(game, bSlot ? "b" : "a");
            assertFalse(d.playerState().ammunition().hasToadstool());
            assertEquals(
                    0,
                    bSlot
                            ? d.playerState().inventory().itemB()
                            : d.playerState().inventory().itemA());
            assertEquals(!bSlot, d.link().animation().dialogGotItemActive());
            for (int frame = 0; frame < 12; frame++) game.headlessFrame();
            assertTrue(d.dialogController().isActive(), "Witch proceeds to brewing Dialog009");
        }
    }

    private static void use(GameSession game, String button) {
        game.headlessButton(button, true);
        game.headlessFrame();
        game.headlessButton(button, false);
    }

    private static GameSession game() throws Exception {
        try (var rom = linksawakening.testing.TestRom.open()) {
            assertNotNull(rom);
        }
        var game = new GameSession((pcm, sampleRate) -> {});
        game.initializeHeadlessIntro();
        game.state().options().replace(AppConfig.parse("{\"debugEnabled\":true}"));
        game.onKeyEvent(GLFW_KEY_F2, GLFW_PRESS);
        var player = game.diagnostics().playerState();
        player.ammunition().setHasToadstool(true);
        player.ammunition().setMagicPowderCount(0);
        player.inventory().setItemA(linksawakening.state.InventoryItem.MAGIC_POWDER.romValue());
        player.inventory().setItemB(0);
        return game;
    }
}
