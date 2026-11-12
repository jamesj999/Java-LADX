package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionTestSupport.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import linksawakening.entity.Link;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.world.entity.event.EntityCombatEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class RoomSessionDoors3Test {

    @Test
    void crackedFloorCollapsesAfterFortyFramesAndResetsOnMovementOrBlockedInteraction() {
        RoomSession session = newSession();
        session.loadIndoor(0, 0x17);
        int index = RoomConstants.ROOM_OBJECTS_BASE + 0x33;
        var frame = new RoomSession.LinkGroundFrame(40, 40, false, false, false, false, 0, 0, 0);
        session.tickCrackedFloor(frame, false); // seed the previous ground probe
        session.activeRoom().roomObjectsArea()[index] = 0xDF;
        var sounds = new java.util.ArrayList<GameplaySoundEvent>();
        session.setColorShellSoundSink(sounds::add);
        for (int i = 0; i < 39; i++) assertFalse(session.tickCrackedFloor(frame, false));
        assertFalse(session.tickCrackedFloor(
                new RoomSession.LinkGroundFrame(40, 40, false, true, false, false, 0, 0, 0), false));
        assertEquals(0, session.context.crackedFloorCounter);
        for (int i = 0; i < 39; i++) assertFalse(session.tickCrackedFloor(frame, false));
        assertFalse(session.tickCrackedFloor(
                new RoomSession.LinkGroundFrame(40, 40, true, false, false, false, 0, 0, 0), false));
        assertEquals(39, session.context.crackedFloorCounter); // airborne bypass retains timer
        assertTrue(sounds.isEmpty());
        assertEquals(0xDF, session.activeRoom().roomObjectsArea()[index]);
        assertTrue(session.tickCrackedFloor(frame, false));
        assertEquals(0xAE, session.activeRoom().roomObjectsArea()[index]);
        assertEquals(java.util.List.of(GameplaySoundEvent.CRACKED_FLOOR_COLLAPSE), sounds);
        // A pit does not reach Default, so its frame retains wC1C9.
        assertFalse(session.tickCrackedFloor(frame, false));
        assertEquals(40, session.context.crackedFloorCounter);
        session.activeRoom().roomObjectsArea()[index] = 0x0D;
        session.tickCrackedFloor(frame, false);
        assertEquals(0, session.context.crackedFloorCounter); // non-$DF on Default resets
    }

    @Test
    void pushedBlockPlaysRumbleOnlyWhenMovementStarts() {
        RoomSession session = newSession();
        session.loadIndoor(0x00, 0x04);
        var sounds = new java.util.ArrayList<GameplaySoundEvent>();
        session.setColorShellSoundSink(sounds::add);
        for (int tick = 0; tick < 63; tick++) {
            session.tryPushIndoorBlock(0x15, 0x28, Link.DIRECTION_RIGHT, 0x08);
        }
        assertTrue(sounds.isEmpty());
        session.tryPushIndoorBlock(0x15, 0x28, Link.DIRECTION_RIGHT, 0x08);
        assertEquals(java.util.List.of(GameplaySoundEvent.BLOCK_PUSH), sounds);
        session.tryPushIndoorBlock(0x15, 0x28, Link.DIRECTION_RIGHT, 0x08);
        assertEquals(1, sounds.size());

        RoomSession blocked = newSession();
        blocked.loadIndoor(0x00, 0x04);
        blocked.setColorShellSoundSink(sounds::add);
        blocked.activeRoom().roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + 0x33] = 0xA6;
        for (int tick = 0; tick < 64; tick++) {
            blocked.tryPushIndoorBlock(0x15, 0x28, Link.DIRECTION_RIGHT, 0x08);
        }
        assertEquals(1, sounds.size());
    }

    @Test
    void staticSwordCollisionBreaksOnlyIndoorSwordCrystal() {
        RoomSession session = newSession();
        session.loadIndoor(0x0A, 0xB0);
        int index = RoomConstants.ROOM_OBJECTS_BASE + 0x33;
        for (int object : new int[] {0x4E, 0x4F, 0x88, 0xA7}) {
            session.activeRoom().roomObjectsArea()[index] = object;
            assertFalse(session.breakIndoorSwordCrystal(0x33));
            assertEquals(object, session.activeRoom().roomObjectsArea()[index]);
        }
        session.activeRoom().roomObjectsArea()[index] = 0xDD;
        assertTrue(session.breakIndoorSwordCrystal(0x33));
        assertEquals(0x0D, session.activeRoom().roomObjectsArea()[index]);
        assertFalse(session.breakIndoorSwordCrystal(0x33));
        session.tickEntities(0, 8, 16);
        assertTrue(session.consumeEntityEvents().stream()
                .anyMatch(event ->
                        event.soundChannel() == EntityCombatEvent.SoundChannel.NOISE && event.soundId() == 0x09));
        assertTrue(session.activeRoom().entities().slots().stream()
                .anyMatch(entity ->
                        entity.loaded() && entity.type() == 0x05 && entity.x() == 0x38 && entity.y() == 0x40));
    }
}
