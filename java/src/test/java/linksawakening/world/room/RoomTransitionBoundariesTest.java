package linksawakening.world.room;

import static linksawakening.world.room.RoomConstants.ROOM_OBJECTS_BASE;
import static linksawakening.world.room.RoomConstants.ROOM_OBJECT_ROW_STRIDE;
import static linksawakening.world.room.RoomTransitionTestSupport.*;
import static linksawakening.world.room.RoomTransitionTestSupport2.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import linksawakening.entity.Link;
import linksawakening.equipment.ItemRegistry;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.state.PlayerState;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.transition.RoomBoundaryController;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;
import linksawakening.world.transition.Warp;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class RoomTransitionBoundariesTest {

    @Test
    void downwardScrollAfterFlipAutomaticallyDropsClearOfDestinationWall() throws Exception {
        byte[] rom = loadRom();
        RomTables tables = RomTables.loadFromRom(rom);
        OverworldCollision collision = new OverworldCollision(tables);
        RoomSession session = newSession(rom, tables, collision);
        session.loadInitialOverworld(0x82);
        ScrollController scroll = new ScrollController();
        RoomTransitionCoordinator coordinator = new RoomTransitionCoordinator(
                session, new RoomBoundaryController(), new TransitionController(), scroll);
        Link link = new Link(
                new InputState(),
                new InputConfig(1, 2, 3, 4, 5, 6, 7),
                tables,
                collision,
                null,
                new PlayerState(),
                new ItemRegistry());
        // Finish a flip over the south boundary; the scroll waits for landing.
        int[] floor = new int[0x100];
        java.util.Arrays.fill(floor, 0x09);
        collision.setRoom(floor);
        link.setPixelPosition(0x40, 0x74);
        link.useRocsFeather();
        coordinator.handleOverworldBoundary(link);
        assertFalse(scroll.isActive());
        for (int frame = 0; frame < 64 && !scroll.isActive(); frame++) {
            link.update();
            coordinator.handleOverworldBoundary(link);
        }
        assertTrue(scroll.isActive());
        assertEquals(0x92, session.currentRoomId());
        // Model a continuous cliff at the destination's north edge using
        // the ROM's full-solid object physics, then open ground below it.
        int solidObject = 0x00;
        assertEquals(0x01, tables.objectPhysicsFlag(RomTables.PHYSICS_TABLE_OVERWORLD, solidObject));
        int[] destination = floor.clone();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 10; column++) {
                destination[ROOM_OBJECTS_BASE + row * ROOM_OBJECT_ROW_STRIDE + column] = solidObject;
            }
        }
        collision.setRoom(destination);
        assertFalse(link.isAirborne(), "recovery must wait for scroll completion");
        while (scroll.isActive()) scroll.tick(4);
        assertTrue(link.isAirborne());
        assertEquals(0x02, link.romMotionState());
        assertEquals(0, link.roomEntryPixelY(), "retain the recorded entry before recovery");
        for (int frame = 0; frame < 64 && link.isAirborne(); frame++) link.update();
        assertFalse(link.isAirborne());
        assertEquals(Link.LINK_MOTION_DEFAULT, link.romMotionState());
        assertTrue(link.pixelY() >= 0x28, "must clear the cliff without directional input");
        assertFalse(collision.pointBlocked(link.pixelX() + 6, link.pixelY() + 16));
        coordinator.handleOverworldBoundary(link);
        assertFalse(scroll.isActive(), "must not scroll back into the previous room");
    }

    @Test
    void linkRoomPhysicsDefaultReinitializesAcrossRoomAndSaveLikeReloads() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        OverworldCollision collision = new OverworldCollision(romTables);
        RoomSession session = newSession(rom, romTables, collision);
        ScrollController scroll = new ScrollController();
        RoomTransitionCoordinator coordinator = new RoomTransitionCoordinator(
                session, new RoomBoundaryController(), new TransitionController(), scroll);
        Link link = new Link(
                new InputState(),
                new InputConfig(1, 2, 3, 4, 5, 6, 7),
                romTables,
                collision,
                null,
                new PlayerState(),
                new ItemRegistry());

        session.loadIndoor(0x00, 0x19, Warp.CATEGORY_SIDESCROLL);
        link.setPixelPosition(0x50, 0x40);
        coordinator.handleWarpAndIndoorBoundaries(link);
        assertEquals(0x01, link.romPhysicsModifier());

        link.setRomPhysicsModifier(0x02);
        session.loadIndoor(0x00, 0x19, Warp.CATEGORY_INDOOR);
        coordinator.handleWarpAndIndoorBoundaries(link);
        assertEquals(0x00, link.romPhysicsModifier(), "top-down reload resets a prior side-view modifier");

        session.loadIndoor(0x00, 0x19, Warp.CATEGORY_SIDESCROLL);
        coordinator.handleWarpAndIndoorBoundaries(link);
        assertEquals(0x01, link.romPhysicsModifier(), "same-room side-view reload restores the source entry default");

        link.setRomPhysicsModifier(0x02);
        RoomSession restoredSession = newSession(rom, romTables, collision);
        restoredSession.loadIndoor(0x00, 0x19, Warp.CATEGORY_SIDESCROLL);
        RoomTransitionCoordinator restoredCoordinator = new RoomTransitionCoordinator(
                restoredSession, new RoomBoundaryController(), new TransitionController(), new ScrollController());
        restoredCoordinator.handleWarpAndIndoorBoundaries(link);
        assertEquals(0x01, link.romPhysicsModifier(), "a new session reload restores the source entry default");
    }

    @Test
    void warpPitFallsThenUsesWarpZeroWithoutDamage() throws Exception {
        byte[] rom = loadRom();
        RomTables tables = RomTables.loadFromRom(rom);
        for (boolean sideView : new boolean[] {false, true}) {
            OverworldCollision collision = new OverworldCollision(tables);
            RoomSession session = newSession(rom, tables, collision);
            session.loadIndoor(sideView ? 0 : 0x10, sideView ? 6 : 0xA3);
            Warp warp = session.activeRoom().firstWarp();
            assertEquals(sideView, warp.category() == Warp.CATEGORY_SIDESCROLL);
            if (!sideView) {
                int[] objects = roomWithPitAt(1, 1);
                objects[0x11 + 0x10 + 1] = 0x1C;
                collision.setRoom(objects);
            }
            assertEquals(0x51, collision.objectPhysicsFlagAtPoint(24, 28));
            assertFalse(collision.pointBlockedForLink(24, 28, false));
            PlayerState player = new PlayerState();
            int health = player.health();
            Link link = new Link(
                    new InputState(),
                    new InputConfig(1, 2, 3, 4, 5, 6, 7),
                    tables,
                    collision,
                    null,
                    player,
                    new ItemRegistry());
            link.setRoomEntryPixelPosition(64, 64);
            link.setPixelPosition(16, 16);
            TransitionController transition = new TransitionController();
            RoomTransitionCoordinator coordinator = new RoomTransitionCoordinator(
                    session, new RoomBoundaryController(), transition, new ScrollController());
            runUntilPitFallStarts(link);
            coordinator.handleWarpAndIndoorBoundaries(link);
            assertFalse(transition.isActive());
            runUntilPitFallFinishes(link);
            int sourceX = link.romEntityX();
            int sourceY = link.romEntityY();
            assertTrue(link.pitWarpPending());
            assertEquals(health, player.health());
            assertEquals(0, player.subtractHealthBuffer());
            coordinator.handleWarpAndIndoorBoundaries(link);
            assertTrue(transition.isActive());
            while (transition.isActive()) transition.tick();
            assertEquals(warp.destRoom(), session.currentRoomId());
            assertEquals(warp.category(), session.mapCategory());
            assertEquals(sideView ? sourceY : (sourceX & 0xF0) + 8, link.romEntityX());
            assertEquals(sideView ? warp.destY() : sourceY & 0xF0, link.romEntityY());
            assertEquals(sideView ? 0 : 0x70, link.romEntityZ());
            assertEquals(!sideView, link.isAirborne());
            assertEquals(Link.LINK_MOTION_DEFAULT, link.romMotionState());
            assertEquals(health, player.health());
        }
    }

    @Test
    void mysteriousWoodsUpBoundaryRoutesToRoom63AndQueuesTheLostJingle() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        OverworldCollision collision = new OverworldCollision(romTables);
        RoomSession session = newSession(rom, romTables, collision);
        session.loadInitialOverworld(0x51);
        session.tickEntities(0, 0x78, 0x60, 0, 1);
        session.tickEntities(1, 0x78, 0x1F, 0, 1);
        assertTrue(session.shouldGetLostInMysteriousWoods());

        ScrollController scroll = new ScrollController();
        RoomTransitionCoordinator coordinator = new RoomTransitionCoordinator(
                session, new RoomBoundaryController(), new TransitionController(), scroll);
        Link link = new Link(
                new InputState(),
                new InputConfig(1, 2, 3, 4, 5, 6, 7),
                romTables,
                collision,
                null,
                new PlayerState(),
                new ItemRegistry());
        link.setPixelPosition(0x78, -5);

        coordinator.handleOverworldBoundary(link);

        assertEquals(0x63, session.currentRoomId());
        assertTrue(scroll.isActive());
        assertTrue(session.context.entityBridge.outbox.consumeEntityEvents().stream()
                .anyMatch(event ->
                        event.soundChannel() == EntityCombatEvent.SoundChannel.JINGLE && event.soundId() == 0x1E));
    }

    @Test
    void completedMamboPlaybackUsesTheRomDestinationAfterTheNonInteractiveEffect() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        OverworldCollision collision = new OverworldCollision(romTables);
        RoomSession session = newSession(rom, romTables, collision);
        session.loadIndoor(0x00, 0x25);
        assertTrue(session.startOcarina(0x01, 0x02, 0x01));
        session.tickEntities(0);

        TransitionController transitionController = new TransitionController();
        RoomTransitionCoordinator coordinator = new RoomTransitionCoordinator(
                session, new RoomBoundaryController(), transitionController, new ScrollController());
        Link link = new Link(
                new InputState(),
                new InputConfig(1, 2, 3, 4, 5, 6, 7),
                romTables,
                collision,
                null,
                new PlayerState(),
                new ItemRegistry());

        coordinator.handlePendingManboTransition(link);
        assertEquals(TransitionController.State.MANBO_IN, transitionController.state());

        for (int frame = 0; frame < TransitionController.MANBO_TRANSITION_FRAMES; frame++) {
            transitionController.tick();
        }

        assertEquals(TransitionController.State.MANBO_OUT, transitionController.state());
        assertEquals(0x17, session.currentRoomId());
        assertEquals(0x48, link.pixelX());
        assertEquals(0x6C, link.pixelY());

        for (int frame = TransitionController.MANBO_OUT_INITIAL_FRAME;
                frame < TransitionController.MANBO_TRANSITION_FRAMES;
                frame++) {
            transitionController.tick();
        }
        assertEquals(TransitionController.State.IDLE, transitionController.state());
    }
}
