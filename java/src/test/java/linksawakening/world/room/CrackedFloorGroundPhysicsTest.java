package linksawakening.world.room;

import linksawakening.gpu.GPU;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.testing.RequiresRom;
import linksawakening.testing.TestRom;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.world.transition.Warp;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@RequiresRom
class CrackedFloorGroundPhysicsTest {
    private static RoomSession session() throws Exception {
        byte[] rom;
        try (var input = TestRom.open()) { rom = input.readAllBytes(); }
        var session = new RoomSession(rom, new GPU(), new RoomLoader(rom),
            new OverworldTilesetTable(rom), new OverworldCollision(RomTables.loadFromRom(rom)),
            new TransientVfxSystem(16), null);
        session.loadIndoor(0, 0x17);
        return session;
    }

    private static RoomSession.LinkGroundFrame frame(int x, int y, boolean airborne,
                                                     boolean blocked, boolean dialog, boolean transition) {
        return new RoomSession.LinkGroundFrame(x, y, airborne, blocked, dialog, transition, 0, 0, 0);
    }

    private static RoomSession.LinkGroundFrame standing() {
        return frame(40, 40, false, false, false, false);
    }

    private static void object(RoomSession session, int location, int object) {
        session.activeRoom().roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + location] = object;
    }

    private static void count(RoomSession session, int frames) {
        for (int i = 0; i < frames; i++) assertFalse(session.tickCrackedFloor(standing(), false));
    }

    private static void seedCrack(RoomSession session, int frames) {
        object(session, 0x33, 0x0D);
        session.tickCrackedFloor(standing(), false); // establish the preceding probe
        object(session, 0x33, 0xDF);
        count(session, frames);
        assertEquals(frames, session.context.crackedFloorCounter);
    }

    @Test void sideScrollingIsNotExcludedByTheIndoorGateAt77F7() throws Exception {
        var session = session();
        session.loadIndoor(0, 0x19, Warp.CATEGORY_SIDESCROLL);
        seedCrack(session, 39);
        assertTrue(session.tickCrackedFloor(standing(), false));
        assertEquals(0xAE, session.objectAtRoomLocation(0x33));
    }

    @Test void overworldDfIsSolidBut77FBRetainsRatherThanResetsTheCounter() throws Exception {
        var session = session();
        seedCrack(session, 17);
        session.loadInitialOverworld(0x40);
        object(session, 0x33, 0xDF);
        count(session, 50);
        assertEquals(0x01, session.context.linkGroundObjectPhysics); // ROM overworld $DF = SOLID
        assertEquals(17, session.context.crackedFloorCounter); // wIsIndoor=0: $02:77F7-77FB
        assertEquals(0xDF, session.objectAtRoomLocation(0x33));
    }

    @Test void dialog75C4Through76C0And7757ResetsWithCachedZeroPhysics() throws Exception {
        var session = session();
        seedCrack(session, 17);
        // $02:75C4 -> 76C0 -> 7757 -> Default -> $7858/$786C.
        // Even different coordinates must not cause a fresh GetObjectUnderLink probe.
        session.tickCrackedFloor(frame(56, 56, false, false, true, false), false);
        assertEquals(0, session.context.crackedFloorCounter);
        assertEquals(0x33, session.context.linkGroundRoomPosition);
        assertEquals(0x33, session.context.linkGroundFinalRoomPosition);
        assertEquals(0xDF, session.context.linkGroundObjectUnderEntity);
        assertEquals(0, session.context.linkGroundObjectPhysics);
        count(session, 39);
        assertTrue(session.tickCrackedFloor(standing(), false));
    }

    @Test void transition7802RetainsEvenWithDialogGotItemAndBlockedInteraction() throws Exception {
        var session = session();
        seedCrack(session, 39);
        // $02:75C4 -> 76C0 -> 7757 -> Default returns at $02:7802,
        // before position/$DF/dialog/interaction/got-item reset conditions.
        for (int i = 0; i < 50; i++) {
            assertFalse(session.tickCrackedFloor(frame(56, 56, false, true, true, true), true));
        }
        assertEquals(39, session.context.crackedFloorCounter);
        assertEquals(0x33, session.context.linkGroundRoomPosition);
        assertEquals(0, session.context.linkGroundObjectPhysics);
        assertTrue(session.tickCrackedFloor(standing(), false));
    }

    @Test void dialogOnCachedWaterReturnsBeforeDefaultAndRetainsCounter() throws Exception {
        var session = session();
        seedCrack(session, 17);
        int water = objectWithPhysics(session, 0x07);
        object(session, 0x33, water);
        count(session, 1); // $02:76EC -> water handler returns before Default
        // $02:75C4 -> 76C0 -> 76EC, so $7858 is not reached.
        session.tickCrackedFloor(frame(56, 56, false, true, true, false), true);
        assertEquals(17, session.context.crackedFloorCounter);
        assertEquals(water, session.context.linkGroundObjectUnderEntity);
    }

    @Test void gotItemBlockedInteractionAndNonDfOnDefaultResetTheCounter() throws Exception {
        var session = session();
        seedCrack(session, 17);
        session.tickCrackedFloor(standing(), true); // $02:7854/785C -> $786C
        assertEquals(0, session.context.crackedFloorCounter);
        count(session, 17);
        session.tickCrackedFloor(frame(40, 40, false, true, false, false), false);
        assertEquals(0, session.context.crackedFloorCounter);
        count(session, 17);
        object(session, 0x33, 0x0D);
        session.tickCrackedFloor(standing(), false);
        assertEquals(0, session.context.crackedFloorCounter);
    }

    @Test void previousLocationIsTheLastProbeEvenAcrossEntityMovesAndAirborneFrames() throws Exception {
        var session = session();
        seedCrack(session, 17);
        object(session, 0x34, 0xDF);
        // An entity can move Link before this update. Comparing this frame's
        // pre/post XY (both 56,40) would incorrectly preserve the count.
        session.tickCrackedFloor(frame(56, 40, false, false, false, false), false);
        assertEquals(0x33, session.context.linkGroundFinalRoomPosition); // $02:7589 copy
        assertEquals(0x34, session.context.linkGroundRoomPosition); // $02:7525 probe
        assertEquals(0, session.context.crackedFloorCounter);
        session.tickCrackedFloor(frame(57, 41, false, false, false, false), false);
        assertEquals(1, session.context.crackedFloorCounter); // movement inside a cell does not reset
        for (int i = 0; i < 3; i++) {
            session.tickCrackedFloor(frame(40, 40, true, false, false, false), false);
        }
        assertEquals(0x34, session.context.linkGroundRoomPosition); // no new airborne probe
        assertEquals(0x34, session.context.linkGroundFinalRoomPosition);
        assertEquals(1, session.context.crackedFloorCounter);
        session.tickCrackedFloor(standing(), false);
        assertEquals(0x34, session.context.linkGroundFinalRoomPosition); // older than the last frame's XY
        assertEquals(0x33, session.context.linkGroundRoomPosition);
        assertEquals(0, session.context.crackedFloorCounter);
    }

    @Test void landingProbesBeforeMotionThen7589CopiesThatSampleForTheSecondProbe() throws Exception {
        var session = session();
        seedCrack(session, 17);
        object(session, 0x34, 0xDF);
        // $02:4588 has no $7589 copy; its comparison still uses the saved final sample.
        assertFalse(session.tickCrackedFloorOnLanding(standing(), false));
        assertEquals(18, session.context.crackedFloorCounter);
        assertFalse(session.tickCrackedFloor(frame(56, 40, false, false, false, false), false));
        assertEquals(0x33, session.context.linkGroundFinalRoomPosition);
        assertEquals(0x34, session.context.linkGroundRoomPosition);
        assertEquals(0, session.context.crackedFloorCounter);
    }

    @Test void hookshot755BProbesWithout7589CopyOrCounterUpdate() throws Exception {
        var session = session();
        seedCrack(session, 17);
        assertTrue(session.fireHookshot(48, 56, 0, 0, false, false));
        assertTrue(session.hookshotActive());
        object(session, 0x34, 0xDF);
        session.tickCrackedFloor(frame(56, 40, false, false, false, false), false);
        assertEquals(0x33, session.context.linkGroundFinalRoomPosition);
        assertEquals(0x34, session.context.linkGroundRoomPosition);
        assertEquals(17, session.context.crackedFloorCounter);
    }

    @Test void groundProbeMatches7512To7525IncludingOffsetsAndByteWrap() {
        assertEquals(0x00, RoomObjectMutations.linkGroundLocation(7, 3));
        assertEquals(0x01, RoomObjectMutations.linkGroundLocation(8, 3));
        assertEquals(0x10, RoomObjectMutations.linkGroundLocation(7, 4));
        assertEquals(0x11, RoomObjectMutations.linkGroundLocation(8, 4));
        assertEquals(0x79, RoomObjectMutations.linkGroundLocation(144, 112));
        assertEquals(0xFF, RoomObjectMutations.linkGroundLocation(-9, -13));
        assertEquals(0x00, RoomObjectMutations.linkGroundLocation(248, 244));
    }

    @Test void defaultPathSelectionMatches75EE763A76D276E17757And779F() {
        for (int physics : new int[] {0, 1, 2, 3, 4, 5, 8, 9, 0xFF}) {
            assertTrue(RoomObjectMutations.reachesDefaultGroundPhysics(physics, false, 0, 0),
                "physics=" + physics);
        }
        for (int physics : new int[] {6, 7, 0x0B, 0x50, 0x51, 0xE0, 0xF0, 0xF3}) {
            assertFalse(RoomObjectMutations.reachesDefaultGroundPhysics(physics, false, 0, 0),
                "physics=" + physics);
        }
        assertTrue(RoomObjectMutations.reachesDefaultGroundPhysics(7, false, 0, 0x80)); // raft $76F9
        assertFalse(RoomObjectMutations.reachesDefaultGroundPhysics(7, false, 1, 0x80)); // $76F1 first
        // $75C4 bypasses the pit/spikes/conveyor dispatch and uses cached physics at $76C0.
        assertTrue(RoomObjectMutations.reachesDefaultGroundPhysics(0x50, true, 0, 0));
        assertTrue(RoomObjectMutations.reachesDefaultGroundPhysics(0xE0, true, 0, 0));
        assertTrue(RoomObjectMutations.reachesDefaultGroundPhysics(0xF0, true, 0, 0));
    }

    private static int objectWithPhysics(RoomSession session, int physics) {
        for (int object = 0; object < 256; object++) {
            object(session, 0x33, object);
            if (session.context.overworldCollision.objectPhysicsFlagAtPoint(48, 52) == physics) return object;
        }
        throw new AssertionError("No ROM object for physics=" + physics);
    }
}
