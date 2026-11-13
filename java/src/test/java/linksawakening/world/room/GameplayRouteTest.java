package linksawakening.world.room;

import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class GameplayRouteTest {

    @Test
    void completeGameplayRouteUsesPhysicalTraversalAndRealRoomMechanics() throws Exception {
        var route = new GameplayRouteState();
        GameplayRouteScenario.initializeBeachAndForest(route);
        GameplayRouteScenario.unlockTailCave(route);
        GameplayRouteScenario.completeTailCavePuzzles(route);
        GameplayRouteScenario.performInstrumentWarp(route);
        GameplayRouteScenario.enterBottleGrotto(route);
        GameplayRouteScenario.collectBottleGrottoKeys(route);
        GameplayRouteScenario.clearBottleGrottoEnemies(route);
        GameplayRouteScenario.solveBottleGrottoSwitches(route);
        GameplayRouteScenario.fightHinox(route);
        GameplayRouteScenario.clearBooBuddies(route);
        GameplayRouteScenario.completeKillOrderChest(route);
        GameplayRouteScenario.rideMovingPlatform(route);
    }
}
