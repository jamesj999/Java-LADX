package linksawakening.world.room;

import static linksawakening.world.room.RoomConstants.ROOM_PIXEL_WIDTH;
import static linksawakening.world.room.RoomTransitionTestSupport.*;
import static linksawakening.world.room.RoomTransitionTestSupport2.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

import linksawakening.entity.Link;
import linksawakening.equipment.ItemRegistry;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.state.PlayerState;
import linksawakening.world.transition.RoomBoundaryController;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;
import linksawakening.world.transition.Warp;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class RoomTransitionDoorsTest {

    @Test
    void warpClearsOnlyActivePowerUpAcrossIndoorBoundary() throws Exception {
        byte[] rom = loadRom();
        RomTables tables = RomTables.loadFromRom(rom);
        RoomSession session = newSession(rom, tables, new OverworldCollision(tables));
        PlayerState player = new PlayerState();
        session.onMapBoundaryWarp(player::clearActivePowerUpForWarp);
        session.loadInitialOverworld(0x82);
        player.setActivePowerUp(PlayerState.ACTIVE_POWER_UP_PIECE_OF_POWER);
        player.applyRomEnemyDamage(1);
        session.startAdjacentOverworldScroll(new ScrollController(), ScrollController.RIGHT, 144, 64);
        assertEquals(PlayerState.ACTIVE_POWER_UP_PIECE_OF_POWER, player.activePowerUp());
        session.loadWarpDestination(new Warp(Warp.CATEGORY_OVERWORLD, 0, 0x82, 80, 80, 0));
        assertEquals(PlayerState.ACTIVE_POWER_UP_PIECE_OF_POWER, player.activePowerUp());
        session.loadWarpDestination(new Warp(Warp.CATEGORY_INDOOR, 1, 0, 80, 80, 0));
        assertEquals(0, player.activePowerUp());
        assertEquals(1, player.powerUpHits());
        player.setActivePowerUp(PlayerState.ACTIVE_POWER_UP_GUARDIAN_ACORN);
        session.loadWarpDestination(new Warp(Warp.CATEGORY_INDOOR, 1, 0, 80, 80, 0));
        assertEquals(PlayerState.ACTIVE_POWER_UP_GUARDIAN_ACORN, player.activePowerUp());
        session.loadWarpDestination(new Warp(Warp.CATEGORY_OVERWORLD, 0, 0x82, 80, 80, 0));
        assertEquals(0, player.activePowerUp());
    }

    @Test
    void blockedDoorMovementPreservesProbePositionInEveryDirectionForOnlyThatFrame() throws Exception {
        byte[] rom = loadRom();
        RomTables tables = RomTables.loadFromRom(rom);
        OverworldCollision collision = new OverworldCollision(tables);
        RoomSession session = newSession(rom, tables, collision);
        session.loadIndoor(0x02, 0x56);
        InputConfig config = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        int[][] cases = {
            {0x40, 0x0A, Link.DIRECTION_UP, config.upKey(), 0x01, 0, -1},
            {0x40, 0x60, Link.DIRECTION_DOWN, config.downKey(), 0x02, 0, 1},
            {0x0C, 0x30, Link.DIRECTION_LEFT, config.leftKey(), 0x04, -1, 0},
            {0x84, 0x30, Link.DIRECTION_RIGHT, config.rightKey(), 0x08, 1, 0}
        };
        for (int[] example : cases) {
            InputState input = new InputState();
            Link link = new Link(input, config, tables, collision, null, new PlayerState(), new ItemRegistry());
            link.setPixelPosition(example[0], example[1]);
            input.onKeyEvent(example[3], GLFW_PRESS);
            link.update();
            assertEquals(example[4], link.romCollisionType());
            assertEquals(example[0], link.pixelX());
            assertEquals(example[1], link.pixelY());
            assertEquals(example[0] + example[5], link.romCollisionProbePixelX());
            assertEquals(example[1] + example[6], link.romCollisionProbePixelY());
            link.setDirection(example[2] ^ 1);
            assertEquals(link.pixelX(), link.romCollisionProbePixelX());
            assertEquals(link.pixelY(), link.romCollisionProbePixelY());
            link.setDirection(example[2]);
            input.onKeyEvent(example[3], GLFW_RELEASE);
            link.update();
            assertEquals(0, link.romCollisionType());
            assertEquals(link.pixelX(), link.romCollisionProbePixelX());
            assertEquals(link.pixelY(), link.romCollisionProbePixelY());
        }
    }

    @Test
    void bottleGrottoTwoTorchTriggerOpensTheRoom31EastShutter() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        OverworldCollision collision = new OverworldCollision(romTables);
        RoomSession session = newSession(rom, romTables, collision);
        session.loadIndoor(0x01, 0x31);

        assertEquals(0x25, session.activeRoomEventForTest());
        assertEquals(0xAB, objectAt(session, 0x34));
        assertEquals(0xAB, objectAt(session, 0x35));
        assertEquals(0x08, session.activeRoom().shutterDoorMask() & 0x08);
        for (int tick = 0; tick < 9; tick++) tickInteractiveEntities(session, tick, 0x40, 0x50);
        int closedEastDoorObject = objectAt(session, 0x39);
        int[] closedEastDoorTiles = roomObjectTiles(session, 0x39);
        assertEquals(0x3B, closedEastDoorObject);
        assertEquals(0x3C, objectAt(session, 0x49));

        int frame = 0;
        assertTrue(session.sprinkleMagicPowder(0x33, 0x39, 0, 0));
        for (int tick = 0; tick < 16; tick++) {
            tickInteractiveEntities(session, frame++, 0x40, 0x50);
        }
        assertTrue(session.sprinkleMagicPowder(0x43, 0x39, 0, 0));
        for (int tick = 0; tick < 16; tick++) {
            tickInteractiveEntities(session, frame++, 0x50, 0x50);
        }

        assertEquals(2, session.roomTriggerCountForTest());
        assertEquals(0, session.activeRoomEventForTest());
        assertEquals(0, session.activeRoom().shutterDoorMask() & 0x08);
        assertEquals(0x0B, objectAt(session, 0x39));
        assertEquals(0x0C, objectAt(session, 0x49));
        assertFalse(java.util.Arrays.equals(closedEastDoorTiles, roomObjectTiles(session, 0x39)));
    }

    @Test
    void indoorBoundaryScrollWithoutWarpsUpdatesRoomEntryPosition() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        OverworldCollision collision = new OverworldCollision(romTables);
        RoomSession session = newSession(rom, romTables, collision);
        session.loadInitialOverworld(0x92);
        session.loadIndoor(MAP_WITH_NO_WARP_ROOM, INDOOR_ROOM_WITH_NO_WARPS);
        assertFalse(session.activeRoom().hasWarps());

        ScrollController scrollController = new ScrollController();
        RoomTransitionCoordinator coordinator = new RoomTransitionCoordinator(
                session, new RoomBoundaryController(), new TransitionController(), scrollController);
        Link link = new Link(
                new InputState(),
                new InputConfig(1, 2, 3, 4, 5, 6, 7),
                romTables,
                collision,
                null,
                new PlayerState(),
                new ItemRegistry());

        link.setRoomEntryPixelPosition(0x30, 0x30);
        link.setPixelPosition(ROOM_PIXEL_WIDTH - Link.SPRITE_SIZE + 1, 0x28);

        coordinator.handleWarpAndIndoorBoundaries(link);

        assertTrue(scrollController.isActive());
        assertEquals(ScrollController.RIGHT, scrollController.direction());
        assertEquals(0x01, session.currentRoomId());
        assertEquals(0x00, link.pixelX());
        assertEquals(0x28, link.pixelY());

        collision.setRoom(roomWithPitAt(0, 3));
        collision.setGbcOverlay(null);
        collision.setPhysicsTable(RomTables.PHYSICS_TABLE_OVERWORLD);
        runUntilPitFallStarts(link);
        runUntilPitFallFinishes(link);

        assertEquals(0x00, link.pixelX());
        assertEquals(0x28, link.pixelY());
    }

    @Test
    void newGameHouseSouthDoorReturnsLinkToTheRomOverworldDestination() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        OverworldCollision collision = new OverworldCollision(romTables);
        RoomSession session = newSession(rom, romTables, collision);
        session.loadIndoor(0x10, 0xA3);
        assertTrue(session.activeRoom().indoorHasSouthEntrance());
        assertEquals(Warp.CATEGORY_OVERWORLD, session.activeRoom().firstWarp().category());

        TransitionController transition = new TransitionController();
        RoomTransitionCoordinator coordinator = new RoomTransitionCoordinator(
                session, new RoomBoundaryController(), transition, new ScrollController());
        Link link = new Link(
                new InputState(),
                new InputConfig(1, 2, 3, 4, 5, 6, 7),
                romTables,
                collision,
                null,
                new PlayerState(),
                new ItemRegistry());
        link.setPixelPosition(0x48, RoomConstants.ROOM_PIXEL_HEIGHT);

        coordinator.handleWarpAndIndoorBoundaries(link);
        for (int frame = 0; frame < 16; frame++) transition.tick();

        assertEquals(Warp.CATEGORY_OVERWORLD, session.mapCategory());
        assertEquals(0xA2, session.currentRoomId());
        assertEquals(0x50, link.pixelX());
        assertEquals(0x42, link.pixelY());
    }

    @Test
    void tailCaveSideScrollTopEdgeReturnsThroughRomWarpZero() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        OverworldCollision collision = new OverworldCollision(romTables);
        RoomSession session = newSession(rom, romTables, collision);
        session.loadInitialOverworld(0x92);
        session.loadIndoor(0x00, 0x19, Warp.CATEGORY_SIDESCROLL);
        assertEquals(Warp.CATEGORY_INDOOR, session.activeRoom().firstWarp().category());
        assertEquals(0x03, session.activeRoom().firstWarp().destRoom());

        TransitionController transition = new TransitionController();
        ScrollController scroll = new ScrollController();
        RoomTransitionCoordinator coordinator =
                new RoomTransitionCoordinator(session, new RoomBoundaryController(), transition, scroll);
        Link link = new Link(
                new InputState(),
                new InputConfig(1, 2, 3, 4, 5, 6, 7),
                romTables,
                collision,
                null,
                new PlayerState(),
                new ItemRegistry());
        link.setPixelPosition(0x70, -5);

        coordinator.handleWarpAndIndoorBoundaries(link);

        assertTrue(transition.isActive());
        assertFalse(scroll.isActive());
        while (transition.isActive()) transition.tick();
        assertEquals(Warp.CATEGORY_INDOOR, session.mapCategory());
        assertEquals(0x03, session.currentRoomId());
        assertEquals(0x80, link.pixelX());
        assertEquals(0x10, link.pixelY());
    }

    @Test
    void tailCaveSideScrollPassageRoutesRoom19Through18ToRoom1() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        OverworldCollision collision = new OverworldCollision(romTables);
        RoomSession session = newSession(rom, romTables, collision);
        session.loadInitialOverworld(0x92);
        session.loadIndoor(0x00, 0x19, Warp.CATEGORY_SIDESCROLL);

        TransitionController transition = new TransitionController();
        ScrollController scroll = new ScrollController();
        RoomTransitionCoordinator coordinator =
                new RoomTransitionCoordinator(session, new RoomBoundaryController(), transition, scroll);
        Link link = new Link(
                new InputState(),
                new InputConfig(1, 2, 3, 4, 5, 6, 7),
                romTables,
                collision,
                null,
                new PlayerState(),
                new ItemRegistry());
        link.setPixelPosition(-5, 0x30);

        coordinator.handleWarpAndIndoorBoundaries(link);
        assertTrue(scroll.isActive());
        assertEquals(0x18, session.currentRoomId());
        while (scroll.isActive()) scroll.tick(8);

        link.setPixelPosition(0x20, -5);
        coordinator.handleWarpAndIndoorBoundaries(link);
        assertTrue(transition.isActive());
        assertFalse(scroll.isActive());
        while (transition.isActive()) transition.tick();
        assertEquals(Warp.CATEGORY_INDOOR, session.mapCategory());
        assertEquals(0x01, session.currentRoomId());
        assertEquals(0x40, link.pixelX());
        assertEquals(0x50, link.pixelY());
    }
}
