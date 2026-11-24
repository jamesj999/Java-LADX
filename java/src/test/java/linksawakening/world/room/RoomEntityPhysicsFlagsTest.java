package linksawakening.world.room;

import static linksawakening.world.room.RoomNavigationFixture.loadRom;
import static linksawakening.world.room.RoomNavigationFixture.newSession;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.testing.RequiresRom;
import linksawakening.testing.TestRoms;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.Warp;
import org.junit.jupiter.api.Test;

@RequiresRom
final class RoomEntityPhysicsFlagsTest {
    @Test
    void overworldScrollInitializesIncomingOrdinaryEnemiesFromTheRom() throws Exception {
        byte[] rom = loadRom();
        var tables = RomTables.load(TestRoms.partial(rom));
        var session = newSession(rom, tables, new OverworldCollision(tables));
        session.rooms().loadInitialOverworld(0xF0);
        session.transitions().startAdjacentOverworldScroll(new ScrollController(), ScrollController.RIGHT, 161, 40);

        RoomRuntime room = RoomSessionInspection.loadedRoom(session);
        assertEquals(0xF1, session.rooms().currentRoomId());
        var enemies = room.entities().inspection().view().loadedEntities();
        assertEquals(
                List.of(0x09, 0x0E, 0x0E),
                enemies.stream().map(entity -> entity.type()).toList());
        for (var entity : enemies) {
            assertEquals(0x12, room.entities().enemies().physicsFlags(entity.slot()));
        }
    }

    @Test
    void roomPhysicsUsesRomDefaultsAndKeepsExplicitInitOverrides() throws Exception {
        byte[] rom = loadRom();
        var tables = RomTables.load(TestRoms.partial(rom));
        var session = newSession(rom, tables, new OverworldCollision(tables));
        session.rooms().loadInitialOverworld(0x70);

        RoomRuntime room = RoomSessionInspection.loadedRoom(session);
        var moblin = room.entities().inspection().view().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x0B)
                .findFirst()
                .orElseThrow();
        assertEquals(0x12, room.entities().enemies().physicsFlags(moblin.slot()));
        assertEquals(0, tables.entityPropertyTables().entityPhysicsFlags(0xFB));

        session.rooms().loadIndoor(0x02, 0x3A, Warp.CATEGORY_SIDESCROLL);
        RoomRuntime passage = RoomSessionInspection.loadedRoom(session);
        var pot = passage.entities().inspection().view().loadedEntities().stream()
                .filter(entity -> entity.type() == 0xD6)
                .findFirst()
                .orElseThrow();
        assertEquals(0x82, tables.entityPropertyTables().entityPhysicsFlags(0xD6));
        assertEquals(0x20, passage.entities().enemies().physicsFlags(pot.slot()));
    }
}
