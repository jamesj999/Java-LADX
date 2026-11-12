package linksawakening.app.gameplay;

import static org.junit.jupiter.api.Assertions.*;

import linksawakening.app.GameSession;
import linksawakening.save.SaveRamStore;
import linksawakening.testing.RequiresRom;
import linksawakening.world.transition.Warp;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@RequiresRom
class GameOverOutcomeSessionTest {
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void choicesSaveOrRetainLiveProgressAndResumeAtSpawn(int option) throws Exception {
        var game = new GameSession();
        game.initializeHeadlessNewGame();
        for (int i = 0; i < 520; i++) game.headlessFrame();
        for (int i = 0; i < 600 && game.diagnostics().dialogController().isActive(); i++) {
            game.headlessButton("a", (i & 1) == 0);
            game.headlessFrame();
        }
        game.headlessButton("a", false);
        game.headlessButton("right", true);
        for (int i = 0; i < 32; i++) game.headlessFrame();
        game.headlessButton("right", false);
        var storeField = linksawakening.app.SessionTestAccess.field(game, "saveRamStore");
        storeField.setAccessible(true);
        var store = (SaveRamStore) storeField.get(linksawakening.app.SessionTestAccess.owner(game, "saveRamStore"));
        store.createNewGame(0, new int[] {1, 0, 0, 0, 0});
        var slot = linksawakening.app.SessionTestAccess.field(game, "currentSaveSlot");
        slot.setAccessible(true);
        slot.setInt(linksawakening.app.SessionTestAccess.owner(game, "currentSaveSlot"), 0);
        var room = game.diagnostics().roomSession();
        room.loadWarpDestination(new Warp(0, 0, 0x92, 0x50, 0x60, -1));
        room.loadOverworld(0x93); // Scrolling must not replace wSpawnLocationData.
        var link = game.diagnostics().link();
        link.setPixelPosition(80, 64);
        var player = game.diagnostics().playerState();
        player.setMaxHearts(6);
        player.setRupees(17);
        player.setDeathCount(8);
        player.setActivePowerUp(1);
        player.applyRomEnemyDamage(1);
        player.clearResourceBuffersForDebug();
        player.setHealth(0);
        for (int i = 0; i < 208; i++) game.headlessFrame();
        assertEquals(4, link.gameOverStage());
        assertEquals(9, player.deathCount());
        assertEquals(0, player.activePowerUp());
        assertEquals(0, player.powerUpHits());
        game.headlessFrame(); // First interactive handler draws cursor.
        for (int i = 0; i < option; i++) {
            game.headlessButton("down", true);
            game.headlessFrame();
            game.headlessButton("down", false);
            game.headlessFrame();
        }
        game.headlessButton(option == 0 ? "a" : option == 1 ? "b" : "start", true);
        game.headlessFrame();
        assertEquals(40, player.health()); // MaxHeartsToStartingHealthTable[6]: five hearts.
        assertEquals(17, player.rupees());
        assertEquals(9, player.deathCount());
        assertEquals(0, link.romMotionState());
        assertEquals(0, link.romPhysicsModifier());
        assertNotEquals(255, link.romAnimationState());
        assertEquals(0, player.activePowerUp());
        assertEquals(0, player.powerUpHits());
        if (option == 1) {
            var screen = linksawakening.app.SessionTestAccess.field(game, "currentScreen");
            screen.setAccessible(true);
            assertInstanceOf(
                    linksawakening.app.screen.FileMenuScreen.class,
                    screen.get(linksawakening.app.SessionTestAccess.owner(game, "currentScreen")));
        } else {
            assertEquals(0x92, room.currentRoomId());
            assertEquals(0, room.mapCategory());
            assertEquals(0x50 - 8, link.pixelX());
            assertEquals(0x60 - 16, link.pixelY());
            assertEquals(128, player.invincibilityCounter());
        }
        assertEquals(option == 2 ? 0 : 9, store.readSlot(0).deathCountBcd());
        if (option != 2) {
            assertEquals(0x92, store.readSlot(0).spawnMapRoom());
            assertEquals(0x50, store.readSlot(0).spawnPositionX());
            assertEquals(40, store.readSlot(0).health());
        }
        assertFalse(link.gameOverScreenPending());
    }
}
