package linksawakening.world.room;

import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class GameplayRouteTest {

    @Test
    void completeGameplayRouteUsesPhysicalTraversalAndRealRoomMechanics() throws Exception {
        var route = new GameplayRouteFixture();
        GameplayRouteScenario.collectSwordAndTailKey(route);
        GameplayRouteScenario.unlockTailCaveAndRevealItsStairs(route);
        GameplayRouteScenario.collectFeatherAndCompleteTailCave(route);
        GameplayRouteScenario.returnFromTailCaveAndRescueBowWow(route);
        GameplayRouteScenario.openBottleGrottoAndDropItsSecondKey(route);
        GameplayRouteScenario.collectBottleGrottoKeysAndOpenTheWestGate(route);
        GameplayRouteScenario.claimTheStoneBeakAndLowerTheCrystalRing(route);
        GameplayRouteScenario.solveTheSwitchRouteAndReachHinox(route);
        GameplayRouteScenario.defeatHinoxAndReachTheBooBuddies(route);
        GameplayRouteScenario.collectTheBraceletAndStartTheKillOrderPuzzle(route);
        GameplayRouteScenario.completeTheKillOrderAndBoardTheMovingPlatform(route);
        GameplayRouteScenario.rideThePlatformsAndVerifyTheGenieArenaExit(route);
    }
}
