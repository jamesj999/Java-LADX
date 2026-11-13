package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionTestSupport.newSession;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import linksawakening.testing.RequiresRom;
import org.junit.jupiter.api.Test;

@RequiresRom
class RoomLifecycleLoadTest {
    @Test
    void reloadOwnsFreshRoomStateWhileSourceGroundRegistersSurvive() {
        var session = newSession();
        session.rooms().loadInitialOverworld(0xA2);
        var first = session.loadedRoomOrNull();
        first.contents.puzzles.indoorPotPullFrames = 7;
        first.contents.overrides.bombedBlockTileOverrides.add(1);
        session.components.terrain().crackedFloorCounter = 17;
        session.components.terrain().linkGroundRoomPosition = 23;
        session.components.terrain().switchBlocksState = 2;
        session.components.puzzles().dungeonTitleMessageCountdown = 77;
        session.rooms().loadOverworld(0xA2);
        var next = session.loadedRoomOrNull();
        assertNotSame(first, next);
        assertNotSame(first.contents.puzzles, next.contents.puzzles);
        assertNotSame(first.contents.overrides, next.contents.overrides);
        assertNotSame(first.collisions, next.collisions);
        assertSame(next.contents.puzzles, session.components.puzzles().current);
        assertEquals(0, next.contents.puzzles.indoorPotPullFrames);
        assertEquals(0, next.contents.overrides.bombedBlockTileOverrides.size());
        // ResetRoomVariables deliberately retains wC1C9 and Link room-position registers.
        assertEquals(17, session.components.terrain().crackedFloorCounter);
        assertEquals(23, session.components.terrain().linkGroundRoomPosition);
        assertEquals(2, session.components.terrain().switchBlocksState);
        assertEquals(77, session.components.puzzles().dungeonTitleMessageCountdown);
    }
}
