package linksawakening.world.room;

import static linksawakening.world.room.RoomTransitionTestSupport.*;
import static linksawakening.world.room.RoomTransitionTestSupport2.*;

import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class GameplayRouteTest {

    @Test
    void completeGameplayRouteUsesPhysicalTraversalAndRealRoomMechanics() throws Exception {
        var route = new GameplayRouteState();
        GameplayRouteStage1.run(route);
        GameplayRouteStage2.run(route);
        GameplayRouteStage3.run(route);
        GameplayRouteStage4.run(route);
        GameplayRouteStage5.run(route);
        GameplayRouteStage6.run(route);
        GameplayRouteStage7.run(route);
        GameplayRouteStage8.run(route);
        GameplayRouteStage9.run(route);
        GameplayRouteStage10.run(route);
        GameplayRouteStage11.run(route);
        GameplayRouteStage12.run(route);
    }
}
