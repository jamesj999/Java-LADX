package linksawakening.world.room;

import linksawakening.entity.Link;
import linksawakening.equipment.ItemRegistry;
import linksawakening.gpu.GPU;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.state.PlayerState;
import linksawakening.testing.RequiresRom;
import linksawakening.testing.TestRom;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.world.transition.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@RequiresRom
final class WarpEntranceRegressionTest {
    @Test
    void doorAndStaircaseEntryQueueOneStairsNoise() throws Exception {
        byte[] rom;
        try (var input = TestRom.open()) { rom = input.readAllBytes(); }
        var tables = RomTables.loadFromRom(rom);
        var collision = new OverworldCollision(tables);
        var session = new RoomSession(rom, new GPU(), new RoomLoader(rom),
            new OverworldTilesetTable(rom), collision, new TransientVfxSystem(16), null);
        var transition = new TransitionController();
        var sounds = new java.util.ArrayList<linksawakening.gameplay.GameplaySoundEvent>();
        var coordinator = new RoomTransitionCoordinator(session, new RoomBoundaryController(),
            transition, new ScrollController(), sounds::add);
        var input = new InputState();
        var config = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        var link = new Link(input, config, tables, collision, null,
            new PlayerState(), new ItemRegistry());
        session.loadInitialOverworld(0xA2);
        link.setRoomEntryRomPosition(0x58, 0x52);
        input.onKeyEvent(config.upKey(), 1);
        for (int tick = 0; tick < 16 && !transition.isActive(); tick++) {
            link.update();
            coordinator.handleWarpAndIndoorBoundaries(link);
        }
        assertTrue(transition.isActive());
        assertEquals(java.util.List.of(linksawakening.gameplay.GameplaySoundEvent.STAIRS), sounds);
        while (transition.isActive()) transition.tick();
        input.onKeyEvent(config.upKey(), 0);
        sounds.clear();
        session.loadIndoor(0, 1);
        link.setRoomEntryPixelPosition(0x40, 0x50);
        coordinator.handleWarpAndIndoorBoundaries(link);
        assertTrue(sounds.isEmpty(), "arrival leaves staircase inactive");
        link.setPixelPosition(0x50, 0x50);
        coordinator.handleWarpAndIndoorBoundaries(link);
        link.setPixelPosition(0x40, 0x50);
        coordinator.handleWarpAndIndoorBoundaries(link);
        assertTrue(transition.isActive());
        assertEquals(java.util.List.of(linksawakening.gameplay.GameplaySoundEvent.STAIRS), sounds);
    }

    @Test
    void houseExitsKeepTheirRomLandingCoordinatesWithoutReentering() throws Exception {
        byte[] rom;
        try (var input = TestRom.open()) { rom = input.readAllBytes(); }
        var tables = RomTables.loadFromRom(rom);
        var collision = new OverworldCollision(tables);
        var session = new RoomSession(rom, new GPU(), new RoomLoader(rom),
            new OverworldTilesetTable(rom), collision, new TransientVfxSystem(16), null);
        var transition = new TransitionController();
        var sounds = new java.util.ArrayList<linksawakening.gameplay.GameplaySoundEvent>();
        var coordinator = new RoomTransitionCoordinator(session, new RoomBoundaryController(),
            transition, new ScrollController(), sounds::add);
        var link = new Link(new InputState(), new InputConfig(1, 2, 3, 4, 5, 6, 7),
            tables, collision, null, new PlayerState(), new ItemRegistry());
        // Marin, Madam MeowMeow, kennel, witch, shop.
        for (int[] house : new int[][] {{0x10, 0xA3}, {0x10, 0xA7}, {0x12, 0xB2},
                {0x0E, 0xA2}, {0x0E, 0xA1}}) {
            session.loadIndoor(house[0], house[1]);
            Warp exit = session.activeRoom().firstWarp();
            assertEquals(Warp.CATEGORY_OVERWORLD, exit.category());
            link.setRoomEntryPixelPosition(72, 128);
            coordinator.handleWarpAndIndoorBoundaries(link);
            assertTrue(transition.isActive());
            assertEquals(java.util.List.of(linksawakening.gameplay.GameplaySoundEvent.STAIRS), sounds);
            sounds.clear();
            while (transition.isActive()) transition.tick();
            assertEquals(exit.destRoom(), session.currentRoomId());
            assertEquals(exit.destX(), link.romEntityX());
            assertEquals(exit.destY(), link.romEntityY());
            for (int frame = 0; frame < 32; frame++) {
                link.update();
                coordinator.handleWarpAndIndoorBoundaries(link);
                assertFalse(transition.isActive(), "stationary landing at " + exit.destRoom());
            }
        }
    }
}
