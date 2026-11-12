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
        session.rooms().loadIndoor(0, 0x17);
        int index = RoomConstants.ROOM_OBJECTS_BASE + 0x33;
        var frame = new LinkGroundFrame(
                40, 40, false, false, false, false, linksawakening.world.room.LinkMotion.stateForRomByte(0), 0, 0, 0);
        session.terrain().tickCrackedFloor(frame, false); // seed the previous ground probe
        session.rooms().activeRoom().roomObjectsArea()[index] = 0xDF;
        var sounds = new java.util.ArrayList<GameplaySoundEvent>();
        session.entityState().setColorShellSoundSink(sounds::add);
        for (int i = 0; i < 39; i++) assertFalse(session.terrain().tickCrackedFloor(frame, false));
        assertFalse(session.terrain()
                .tickCrackedFloor(
                        new LinkGroundFrame(
                                40,
                                40,
                                false,
                                true,
                                false,
                                false,
                                linksawakening.world.room.LinkMotion.stateForRomByte(0),
                                0,
                                0,
                                0),
                        false));
        assertEquals(0, session.components.terrain().crackedFloorCounter);
        for (int i = 0; i < 39; i++) assertFalse(session.terrain().tickCrackedFloor(frame, false));
        assertFalse(session.terrain()
                .tickCrackedFloor(
                        new LinkGroundFrame(
                                40,
                                40,
                                true,
                                false,
                                false,
                                false,
                                linksawakening.world.room.LinkMotion.stateForRomByte(0),
                                0,
                                0,
                                0),
                        false));
        assertEquals(39, session.components.terrain().crackedFloorCounter); // airborne bypass retains timer
        assertTrue(sounds.isEmpty());
        assertEquals(0xDF, session.rooms().activeRoom().roomObjectsArea()[index]);
        assertTrue(session.terrain().tickCrackedFloor(frame, false));
        assertEquals(0xAE, session.rooms().activeRoom().roomObjectsArea()[index]);
        assertEquals(java.util.List.of(GameplaySoundEvent.CRACKED_FLOOR_COLLAPSE), sounds);
        // A pit does not reach Default, so its frame retains wC1C9.
        assertFalse(session.terrain().tickCrackedFloor(frame, false));
        assertEquals(40, session.components.terrain().crackedFloorCounter);
        session.rooms().activeRoom().roomObjectsArea()[index] = 0x0D;
        session.terrain().tickCrackedFloor(frame, false);
        assertEquals(0, session.components.terrain().crackedFloorCounter); // non-$DF on Default resets
    }

    @Test
    void pushedBlockPlaysRumbleOnlyWhenMovementStarts() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0x00, 0x04);
        var sounds = new java.util.ArrayList<GameplaySoundEvent>();
        session.entityState().setColorShellSoundSink(sounds::add);
        for (int tick = 0; tick < 63; tick++) {
            session.doors()
                    .tryPushIndoorBlock(
                            0x15, 0x28, linksawakening.state.Direction.fromJavaOrUnknown(Link.DIRECTION_RIGHT), 0x08);
        }
        assertTrue(sounds.isEmpty());
        session.doors()
                .tryPushIndoorBlock(
                        0x15, 0x28, linksawakening.state.Direction.fromJavaOrUnknown(Link.DIRECTION_RIGHT), 0x08);
        assertEquals(java.util.List.of(GameplaySoundEvent.BLOCK_PUSH), sounds);
        session.doors()
                .tryPushIndoorBlock(
                        0x15, 0x28, linksawakening.state.Direction.fromJavaOrUnknown(Link.DIRECTION_RIGHT), 0x08);
        assertEquals(1, sounds.size());

        RoomSession blocked = newSession();
        blocked.rooms().loadIndoor(0x00, 0x04);
        blocked.entityState().setColorShellSoundSink(sounds::add);
        blocked.rooms().activeRoom().roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + 0x33] = 0xA6;
        for (int tick = 0; tick < 64; tick++) {
            blocked.doors()
                    .tryPushIndoorBlock(
                            0x15, 0x28, linksawakening.state.Direction.fromJavaOrUnknown(Link.DIRECTION_RIGHT), 0x08);
        }
        assertEquals(1, sounds.size());
    }

    @Test
    void staticSwordCollisionBreaksOnlyIndoorSwordCrystal() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0x0A, 0xB0);
        int index = RoomConstants.ROOM_OBJECTS_BASE + 0x33;
        for (int object : new int[] {0x4E, 0x4F, 0x88, 0xA7}) {
            session.rooms().activeRoom().roomObjectsArea()[index] = object;
            assertFalse(session.terrain().breakIndoorSwordCrystal(0x33));
            assertEquals(object, session.rooms().activeRoom().roomObjectsArea()[index]);
        }
        session.rooms().activeRoom().roomObjectsArea()[index] = 0xDD;
        assertTrue(session.terrain().breakIndoorSwordCrystal(0x33));
        assertEquals(0x0D, session.rooms().activeRoom().roomObjectsArea()[index]);
        assertFalse(session.terrain().breakIndoorSwordCrystal(0x33));
        session.entities().tickEntities(0, 8, 16);
        assertTrue(session.components.entityBridge().outbox.consumeEntityEvents().stream()
                .anyMatch(event ->
                        event.soundChannel() == EntityCombatEvent.SoundChannel.NOISE && event.soundId() == 0x09));
        assertTrue(session.rooms().activeRoom().entities().slots().stream()
                .anyMatch(entity ->
                        entity.loaded() && entity.type() == 0x05 && entity.x() == 0x38 && entity.y() == 0x40));
    }
}
