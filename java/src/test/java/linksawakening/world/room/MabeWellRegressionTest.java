package linksawakening.world.room;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import linksawakening.entity.Link;
import linksawakening.equipment.ItemRegistry;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gpu.GPU;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.Rom;
import linksawakening.rom.RomTables;
import linksawakening.state.PlayerState;
import linksawakening.testing.RequiresRom;
import linksawakening.testing.TestRom;
import linksawakening.testing.TestRoms;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.world.transition.*;
import org.junit.jupiter.api.Test;

@RequiresRom
final class MabeWellRegressionTest {
    @Test
    void jumpingOffTheWellLedgeFallsWithSoundAndWarpsWithoutDamage() throws Exception {
        byte[] rom;
        try (var stream = TestRom.open()) {
            rom = stream.readAllBytes();
        }
        var tables = RomTables.load(TestRoms.partial(rom));
        var collision = new OverworldCollision(tables);
        var room = new RoomSession(new RoomSessionConfig(
                (rom == null ? (Rom) null : TestRoms.partial(rom)),
                new GPU(),
                new RoomLoader((rom == null ? (Rom) null : TestRoms.partial(rom))),
                new OverworldTilesetTable((rom == null ? (Rom) null : TestRoms.partial(rom))),
                collision,
                new RoomSessionConfig.Presentation(new TransientVfxSystem(16), null, null),
                new linksawakening.world.entity.RomRandomByteSource()));
        room.rooms().loadInitialOverworld(0xA0);
        var warp = room.rooms().activeRoom().firstWarp();
        assertEquals(0x61, collision.objectIdAtPoint(88, 88));
        assertTrue(collision.pointBlocked(88, 88), "well remains solid to ordinary walking");
        var input = new InputState();
        var config = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        var player = new PlayerState();
        int health = player.vitals().health();
        var sounds = new ArrayList<GameplaySoundEvent>();
        var link = new Link(input, config, tables, collision, null, player, new ItemRegistry(), sounds::add);
        var transition = new TransitionController();
        var coordinator = new RoomTransitionCoordinator(
                room, new RoomBoundaryController(), transition, new ScrollController(), sounds::add);
        link.position().setRoomEntryPixelPosition(80, 32);
        input.onKeyEvent(config.downKey(), 1);
        for (int frame = 0; frame < 160 && !link.airborne().isFallingIntoPit(); frame++) {
            link.motion().update();
            coordinator.handleWarpAndIndoorBoundaries(link);
            assertFalse(transition.isActive(), "fall animation precedes the warp");
        }
        input.onKeyEvent(config.downKey(), 0);
        assertTrue(
                link.airborne().isFallingIntoPit(),
                "well fall at " + link.position().pixelX() + ","
                        + link.position().pixelY());
        assertEquals(
                1, sounds.stream().filter(s -> s == GameplaySoundEvent.PIT_FALL).count());
        for (int frame = 0; frame < 96 && !transition.isActive(); frame++) {
            link.motion().update();
            coordinator.handleWarpAndIndoorBoundaries(link);
        }
        assertTrue(transition.isActive());
        while (transition.isActive()) transition.tick();
        assertEquals(warp.category(), room.rooms().mapCategory());
        assertEquals(warp.destMap(), room.rooms().activeRoom().mapId());
        assertEquals(warp.destRoom(), room.rooms().currentRoomId());
        assertEquals(0x70, link.animation().romEntityZ());
        assertEquals(health, player.vitals().health());
        assertEquals(0, player.vitals().subtractHealthBuffer());
        assertFalse(sounds.contains(GameplaySoundEvent.STAIRS), "pit fade is silent after falling");
    }
}
