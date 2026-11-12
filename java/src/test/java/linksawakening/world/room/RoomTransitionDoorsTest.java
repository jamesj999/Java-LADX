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
import linksawakening.state.Direction;
import linksawakening.state.PlayerState;
import linksawakening.state.PowerUp;
import linksawakening.testing.TestRoms;
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
        RomTables tables = RomTables.load(TestRoms.partial(rom));
        RoomSession session = newSession(rom, tables, new OverworldCollision(tables));
        PlayerState player = new PlayerState();
        session.progress().onMapBoundaryWarp(player.equipmentLevels()::clearActivePowerUpForWarp);
        session.rooms().loadInitialOverworld(0x82);
        player.equipmentLevels().setActivePowerUp(PowerUp.PIECE_OF_POWER.romValue());
        player.vitals().applyRomEnemyDamage(1);
        session.transitions().startAdjacentOverworldScroll(new ScrollController(), ScrollController.RIGHT, 144, 64);
        assertEquals(PowerUp.PIECE_OF_POWER.romValue(), player.equipmentLevels().activePowerUp());
        session.transitions().loadWarpDestination(new Warp(Warp.CATEGORY_OVERWORLD, 0, 0x82, 80, 80, 0));
        assertEquals(PowerUp.PIECE_OF_POWER.romValue(), player.equipmentLevels().activePowerUp());
        session.transitions().loadWarpDestination(new Warp(Warp.CATEGORY_INDOOR, 1, 0, 80, 80, 0));
        assertEquals(0, player.equipmentLevels().activePowerUp());
        assertEquals(1, player.equipmentLevels().powerUpHits());
        player.equipmentLevels().setActivePowerUp(PowerUp.GUARDIAN_ACORN.romValue());
        session.transitions().loadWarpDestination(new Warp(Warp.CATEGORY_INDOOR, 1, 0, 80, 80, 0));
        assertEquals(PowerUp.GUARDIAN_ACORN.romValue(), player.equipmentLevels().activePowerUp());
        session.transitions().loadWarpDestination(new Warp(Warp.CATEGORY_OVERWORLD, 0, 0x82, 80, 80, 0));
        assertEquals(0, player.equipmentLevels().activePowerUp());
    }

    @Test
    void blockedDoorMovementPreservesProbePositionInEveryDirectionForOnlyThatFrame() throws Exception {
        byte[] rom = loadRom();
        RomTables tables = RomTables.load(TestRoms.partial(rom));
        OverworldCollision collision = new OverworldCollision(tables);
        RoomSession session = newSession(rom, tables, collision);
        session.rooms().loadIndoor(0x02, 0x56);
        InputConfig config = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        int[][] cases = {
            {0x40, 0x0A, Direction.UP.javaIndex(), config.upKey(), 0x01, 0, -1},
            {0x40, 0x60, Direction.DOWN.javaIndex(), config.downKey(), 0x02, 0, 1},
            {0x0C, 0x30, Direction.LEFT.javaIndex(), config.leftKey(), 0x04, -1, 0},
            {0x84, 0x30, Direction.RIGHT.javaIndex(), config.rightKey(), 0x08, 1, 0}
        };
        for (int[] example : cases) {
            InputState input = new InputState();
            Link link = new Link(input, config, tables, collision, null, new PlayerState(), new ItemRegistry());
            link.position().setPixelPosition(example[0], example[1]);
            input.onKeyEvent(example[3], GLFW_PRESS);
            link.motion().update();
            assertEquals(example[4], link.position().romCollisionType());
            assertEquals(example[0], link.position().pixelX());
            assertEquals(example[1], link.position().pixelY());
            assertEquals(example[0] + example[5], link.position().romCollisionProbePixelX());
            assertEquals(example[1] + example[6], link.position().romCollisionProbePixelY());
            link.animation().setDirection(example[2] ^ 1);
            assertEquals(link.position().pixelX(), link.position().romCollisionProbePixelX());
            assertEquals(link.position().pixelY(), link.position().romCollisionProbePixelY());
            link.animation().setDirection(example[2]);
            input.onKeyEvent(example[3], GLFW_RELEASE);
            link.motion().update();
            assertEquals(0, link.position().romCollisionType());
            assertEquals(link.position().pixelX(), link.position().romCollisionProbePixelX());
            assertEquals(link.position().pixelY(), link.position().romCollisionProbePixelY());
        }
    }

    @Test
    void bottleGrottoTwoTorchTriggerOpensTheRoom31EastShutter() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.load(TestRoms.partial(rom));
        OverworldCollision collision = new OverworldCollision(romTables);
        RoomSession session = newSession(rom, romTables, collision);
        session.rooms().loadIndoor(0x01, 0x31);

        assertEquals(0x25, RoomDebugFixture.of(session).activeRoomEventForTest());
        assertEquals(0xAB, objectAt(session, 0x34));
        assertEquals(0xAB, objectAt(session, 0x35));
        assertEquals(0x08, session.rooms().activeRoom().shutterDoorMask() & 0x08);
        for (int tick = 0; tick < 9; tick++) tickInteractiveEntities(session, tick, 0x40, 0x50);
        int closedEastDoorObject = objectAt(session, 0x39);
        int[] closedEastDoorTiles = roomObjectTiles(session, 0x39);
        assertEquals(0x3B, closedEastDoorObject);
        assertEquals(0x3C, objectAt(session, 0x49));

        int frame = 0;
        assertTrue(session.playerItems().sprinkleMagicPowder(0x33, 0x39, 0, 0));
        for (int tick = 0; tick < 16; tick++) {
            tickInteractiveEntities(session, frame++, 0x40, 0x50);
        }
        assertTrue(session.playerItems().sprinkleMagicPowder(0x43, 0x39, 0, 0));
        for (int tick = 0; tick < 16; tick++) {
            tickInteractiveEntities(session, frame++, 0x50, 0x50);
        }

        assertEquals(2, RoomDebugFixture.of(session).roomTriggerCountForTest());
        assertEquals(0, RoomDebugFixture.of(session).activeRoomEventForTest());
        assertEquals(0, session.rooms().activeRoom().shutterDoorMask() & 0x08);
        assertEquals(0x0B, objectAt(session, 0x39));
        assertEquals(0x0C, objectAt(session, 0x49));
        assertFalse(java.util.Arrays.equals(closedEastDoorTiles, roomObjectTiles(session, 0x39)));
    }

    @Test
    void indoorBoundaryScrollWithoutWarpsUpdatesRoomEntryPosition() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.load(TestRoms.partial(rom));
        OverworldCollision collision = new OverworldCollision(romTables);
        RoomSession session = newSession(rom, romTables, collision);
        session.rooms().loadInitialOverworld(0x92);
        session.rooms().loadIndoor(MAP_WITH_NO_WARP_ROOM, INDOOR_ROOM_WITH_NO_WARPS);
        assertFalse(session.rooms().activeRoom().hasWarps());

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

        link.position().setRoomEntryPixelPosition(0x30, 0x30);
        link.position().setPixelPosition(ROOM_PIXEL_WIDTH - Link.SPRITE_SIZE + 1, 0x28);

        coordinator.handleWarpAndIndoorBoundaries(link);

        assertTrue(scrollController.isActive());
        assertEquals(ScrollController.RIGHT, scrollController.direction());
        assertEquals(0x01, session.rooms().currentRoomId());
        assertEquals(0x00, link.position().pixelX());
        assertEquals(0x28, link.position().pixelY());

        collision.setRoom(roomWithPitAt(0, 3));
        collision.setGbcOverlay(null);
        collision.setPhysicsTable(RomTables.PHYSICS_TABLE_OVERWORLD);
        runUntilPitFallStarts(link);
        runUntilPitFallFinishes(link);

        assertEquals(0x00, link.position().pixelX());
        assertEquals(0x28, link.position().pixelY());
    }

    @Test
    void newGameHouseSouthDoorReturnsLinkToTheRomOverworldDestination() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.load(TestRoms.partial(rom));
        OverworldCollision collision = new OverworldCollision(romTables);
        RoomSession session = newSession(rom, romTables, collision);
        session.rooms().loadIndoor(0x10, 0xA3);
        assertTrue(session.rooms().activeRoom().indoorHasSouthEntrance());
        assertEquals(
                Warp.CATEGORY_OVERWORLD,
                session.rooms().activeRoom().firstWarp().category());

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
        link.position().setPixelPosition(0x48, RoomConstants.ROOM_PIXEL_HEIGHT);

        coordinator.handleWarpAndIndoorBoundaries(link);
        for (int frame = 0; frame < 16; frame++) transition.tick();

        assertEquals(Warp.CATEGORY_OVERWORLD, session.rooms().mapCategory());
        assertEquals(0xA2, session.rooms().currentRoomId());
        assertEquals(0x50, link.position().pixelX());
        assertEquals(0x42, link.position().pixelY());
    }

    @Test
    void tailCaveSideScrollTopEdgeReturnsThroughRomWarpZero() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.load(TestRoms.partial(rom));
        OverworldCollision collision = new OverworldCollision(romTables);
        RoomSession session = newSession(rom, romTables, collision);
        session.rooms().loadInitialOverworld(0x92);
        session.rooms().loadIndoor(0x00, 0x19, Warp.CATEGORY_SIDESCROLL);
        assertEquals(
                Warp.CATEGORY_INDOOR, session.rooms().activeRoom().firstWarp().category());
        assertEquals(0x03, session.rooms().activeRoom().firstWarp().destRoom());

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
        link.position().setPixelPosition(0x70, -5);

        coordinator.handleWarpAndIndoorBoundaries(link);

        assertTrue(transition.isActive());
        assertFalse(scroll.isActive());
        while (transition.isActive()) transition.tick();
        assertEquals(Warp.CATEGORY_INDOOR, session.rooms().mapCategory());
        assertEquals(0x03, session.rooms().currentRoomId());
        assertEquals(0x80, link.position().pixelX());
        assertEquals(0x10, link.position().pixelY());
    }

    @Test
    void tailCaveSideScrollPassageRoutesRoom19Through18ToRoom1() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.load(TestRoms.partial(rom));
        OverworldCollision collision = new OverworldCollision(romTables);
        RoomSession session = newSession(rom, romTables, collision);
        session.rooms().loadInitialOverworld(0x92);
        session.rooms().loadIndoor(0x00, 0x19, Warp.CATEGORY_SIDESCROLL);

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
        link.position().setPixelPosition(-5, 0x30);

        coordinator.handleWarpAndIndoorBoundaries(link);
        assertTrue(scroll.isActive());
        assertEquals(0x18, session.rooms().currentRoomId());
        while (scroll.isActive()) scroll.tick(8);

        link.position().setPixelPosition(0x20, -5);
        coordinator.handleWarpAndIndoorBoundaries(link);
        assertTrue(transition.isActive());
        assertFalse(scroll.isActive());
        while (transition.isActive()) transition.tick();
        assertEquals(Warp.CATEGORY_INDOOR, session.rooms().mapCategory());
        assertEquals(0x01, session.rooms().currentRoomId());
        assertEquals(0x40, link.position().pixelX());
        assertEquals(0x50, link.position().pixelY());
    }
}
