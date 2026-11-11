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
    void houseExitsKeepTheirRomLandingCoordinatesWithoutReentering() throws Exception {
        byte[] rom;
        try (var input = TestRom.open()) { rom = input.readAllBytes(); }
        var tables = RomTables.loadFromRom(rom);
        var collision = new OverworldCollision(tables);
        var session = new RoomSession(rom, new GPU(), new RoomLoader(rom),
            new OverworldTilesetTable(rom), collision, new TransientVfxSystem(16), null);
        var transition = new TransitionController();
        var coordinator = new RoomTransitionCoordinator(session, new RoomBoundaryController(),
            transition, new ScrollController());
        var link = new Link(new InputState(), new InputConfig(1, 2, 3, 4, 5, 6, 7),
            tables, collision, null, new PlayerState(), new ItemRegistry());
        // Marin, kennel, telephone, witch, shop.
        for (int[] house : new int[][] {{0x10, 0xA3}, {0x10, 0xA7}, {0x12, 0xB2},
                {0x0E, 0xA2}, {0x0E, 0xA1}}) {
            session.loadIndoor(house[0], house[1]);
            Warp exit = session.activeRoom().firstWarp();
            assertEquals(Warp.CATEGORY_OVERWORLD, exit.category());
            link.setRoomEntryPixelPosition(72, 128);
            coordinator.handleWarpAndIndoorBoundaries(link);
            assertTrue(transition.isActive());
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
