package linksawakening.app;

import static org.junit.jupiter.api.Assertions.*;

import linksawakening.testing.RequiresRom;
import org.junit.jupiter.api.Test;

@RequiresRom
final class HouseNpcReentryTest {
    @Test
    void swordVisitNeverPresentsMarinOrUntransformedTarinEvenBeforeFirstFrame() {
        var game = new GameSession((pcm, sampleRate) -> {});
        game.initializeHeadlessNewGame();
        var player = game.diagnostics().playerState();
        var room = game.diagnostics().roomSession();
        player.equipmentLevels().setSwordLevel(1);
        player.equipmentLevels().setShieldLevel(1);
        room.story().setChestPlayerLevels(1, 1, 0);
        room.story().setTarinFlag(0);
        room.rooms().loadIndoorFromSavedPosition(0x10, 0xA3, 0);
        assertHouseNpcsHidden(game);
        for (int frame = 0; frame < 64; frame++) {
            game.headlessFrame();
            assertHouseNpcsHidden(game);
        }
        room.story().setTarinFlag(1);
        room.rooms().loadIndoorFromSavedPosition(0x10, 0xA3, 0);
        assertTrue(
                room.rooms().activeRoom().entities().loadedEntities().stream()
                        .anyMatch(entity -> entity.type() == 0x3F && entity.spriteVariant() >= 0),
                "Tarin returns home after the raccoon transformation");
    }

    private static void assertHouseNpcsHidden(GameSession game) {
        assertFalse(game.diagnostics().roomSession().rooms().activeRoom().entities().loadedEntities().stream()
                .anyMatch(entity -> (entity.type() == 0x3E || entity.type() == 0x3F) && entity.spriteVariant() >= 0));
    }
}
