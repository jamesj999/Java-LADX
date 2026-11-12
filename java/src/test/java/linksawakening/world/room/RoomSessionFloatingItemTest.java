package linksawakening.world.room;

import static org.junit.jupiter.api.Assertions.assertTrue;

import linksawakening.gpu.GPU;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.Rom;
import linksawakening.rom.RomTables;
import linksawakening.testing.TestRoms;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.world.transition.Warp;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class RoomSessionFloatingItemTest {

    @Test
    void activeSideScrollRoomMarksEntityRenderSnapshotForMixedSpriteYRules() {
        byte[] rom = loadRom();
        RomTables tables = RomTables.load(TestRoms.partial(rom));
        RoomSession session = new RoomSession(new RoomSessionConfig(
                (rom == null ? (Rom) null : TestRoms.partial(rom)),
                new GPU(),
                new RoomLoader((rom == null ? (Rom) null : TestRoms.partial(rom))),
                new OverworldTilesetTable((rom == null ? (Rom) null : TestRoms.partial(rom))),
                new OverworldCollision(tables),
                new RoomSessionConfig.Presentation(new TransientVfxSystem(16), null, null),
                new linksawakening.world.entity.RomRandomByteSource()));

        session.rooms().loadInitialOverworld(0x92);
        session.rooms().loadIndoor(0x00, 0x00, Warp.CATEGORY_SIDESCROLL);

        assertTrue(session.rooms().activeRoom().entities().sideScrolling());
        assertTrue(session.rooms().renderSnapshot().entities().sideScrolling());
    }

    private static byte[] loadRom() {
        try (var stream = linksawakening.testing.TestRom.open()) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return stream.readAllBytes();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load ROM", e);
        }
    }
}
