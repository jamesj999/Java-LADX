package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionTestSupport.newSession;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

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
        next.contents.puzzles.tailCaveFinalMotionBlockPending = true;
        org.junit.jupiter.api.Assertions.assertTrue(session.doors().tailCaveKeyholeSequenceActive());
        session.doors().tickTailCaveKeyholeSequence();
        org.junit.jupiter.api.Assertions.assertFalse(session.doors().tailCaveKeyholeSequenceActive());
        assertEquals(0, next.contents.puzzles.indoorPotPullFrames);
        assertEquals(0, next.contents.overrides.bombedBlockTileOverrides.size());
        // ResetRoomVariables deliberately retains wC1C9 and Link room-position registers.
        assertEquals(17, session.components.terrain().crackedFloorCounter);
        assertEquals(23, session.components.terrain().linkGroundRoomPosition);
        assertEquals(2, session.components.terrain().switchBlocksState);
        assertEquals(77, session.components.puzzles().dungeonTitleMessageCountdown);
    }

    @Test
    void reentrantLoadListenerPublishesBeforeNotificationAndOuterWorkUsesItsReplacement() {
        RoomSession[] holder = new RoomSession[1];
        var observed = new java.util.ArrayList<Integer>();
        holder[0] = newSession(room -> {
            org.junit.jupiter.api.Assertions.assertSame(room, holder[0].rooms().activeRoom());
            observed.add(room.roomId());
            if (room.roomId() == 0xA2) holder[0].rooms().loadOverworld(0xA3);
        });
        var session = holder[0];
        session.rooms().loadInitialOverworld(0xA2);
        assertEquals(java.util.List.of(0xA2, 0xA3), observed);
        assertEquals(0xA3, session.rooms().currentRoomId());
        var active = session.rooms().activeRoom();
        assertEquals(
                active.roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE],
                session.components.overworldCollision().objectIdAtPoint(0, 0));
        assertEquals(
                active.roomId(),
                session.loadedRoomOrNull()
                        .entities
                        .inspection()
                        .view()
                        .spriteSelection()
                        .roomId());
    }
}
