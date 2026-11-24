package linksawakening.world.room;

import static linksawakening.world.room.RoomNavigationFixture.loadRom;
import static linksawakening.world.room.RoomNavigationFixture.newSession;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import linksawakening.entity.Link;
import linksawakening.equipment.ItemRegistry;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.state.PlayerState;
import linksawakening.testing.TestRoms;
import linksawakening.world.entity.FollowingNpcState;
import linksawakening.world.transition.RoomBoundaryController;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class RoomTransitionFollowersTest {

    @Test
    void overworldScrollSpawnsBowWowAtTheDestinationEntryPosition() throws Exception {
        byte[] rom = loadRom();
        RomTables tables = RomTables.load(TestRoms.partial(rom));
        OverworldCollision collision = new OverworldCollision(tables);
        RoomSession session = newSession(rom, tables, collision);
        session.story().setBowWowState(1);
        session.story().setFollowingNpcState(new FollowingNpcState(false, 0, false, true, 0, 0, false), 3, 81, 0, 0, 1);
        session.rooms().loadInitialOverworld(0x35);

        Link link = new Link(
                new InputState(),
                new InputConfig(1, 2, 3, 4, 5, 6, 7),
                tables,
                collision,
                null,
                new PlayerState(),
                new ItemRegistry());
        link.position().setPixelPosition(-5, 65);
        ScrollController scroll = new ScrollController();
        session.transitions()
                .coordinator(new RoomBoundaryController(), new TransitionController(), scroll)
                .handleOverworldBoundary(link);

        assertTrue(scroll.isActive());
        assertEquals(0x34, session.rooms().currentRoomId());
        var bowWow = session.rooms().requireActiveRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x6D)
                .findFirst()
                .orElseThrow();
        assertEquals(148, bowWow.x());
        assertEquals(81, bowWow.y());
    }

    @Test
    void bowWowHideoutDirectRoomIdsMoveNorthByOneHexRow() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.load(TestRoms.partial(rom));
        OverworldCollision collision = new OverworldCollision(romTables);
        RoomSession session = newSession(rom, romTables, collision);
        session.rooms().loadIndoor(0x15, 0xF0);

        ScrollController scroll = new ScrollController();
        session.transitions().startAdjacentIndoorScroll(scroll, ScrollController.UP, 0x50, 0x00);

        assertEquals(0xE0, session.rooms().currentRoomId());
        assertEquals(
                4,
                session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x0B)
                        .count());
    }
}
