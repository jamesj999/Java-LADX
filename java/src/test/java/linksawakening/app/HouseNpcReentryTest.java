package linksawakening.app;

import linksawakening.testing.RequiresRom;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@RequiresRom
final class HouseNpcReentryTest {
    @Test
    void swordVisitNeverPresentsMarinOrUntransformedTarinEvenBeforeFirstFrame() {
        var game = new GameSession((pcm, sampleRate) -> {});
        game.initializeHeadlessNewGame();
        var player = game.diagnostics().playerState();
        var room = game.diagnostics().roomSession();
        player.setSwordLevel(1);
        player.setShieldLevel(1);
        room.setChestPlayerLevels(1, 1, 0);
        room.setTarinFlag(0);
        room.loadIndoorFromSavedPosition(0x10, 0xA3, 0);
        assertHouseNpcsHidden(game);
        for (int frame = 0; frame < 64; frame++) {
            game.headlessFrame();
            assertHouseNpcsHidden(game);
        }
        room.setTarinFlag(1);
        room.loadIndoorFromSavedPosition(0x10, 0xA3, 0);
        assertTrue(room.activeRoom().entities().loadedEntities().stream()
            .anyMatch(entity -> entity.type() == 0x3F && entity.spriteVariant() >= 0),
            "Tarin returns home after the raccoon transformation");
    }

    private static void assertHouseNpcsHidden(GameSession game) {
        assertFalse(game.diagnostics().roomSession().activeRoom().entities().loadedEntities().stream()
            .anyMatch(entity -> (entity.type() == 0x3E || entity.type() == 0x3F)
                && entity.spriteVariant() >= 0));
    }
}
