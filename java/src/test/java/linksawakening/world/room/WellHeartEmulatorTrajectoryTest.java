package linksawakening.world.room;

import static org.junit.jupiter.api.Assertions.*;

import linksawakening.gpu.GPU;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.testing.RequiresRom;
import linksawakening.testing.TestRom;
import linksawakening.vfx.TransientVfxSystem;
import org.junit.jupiter.api.Test;

@RequiresRom
final class WellHeartEmulatorTrajectoryTest {
    @Test
    void neighbouringBushesStopTheWellHeartAtTheEmulatorPositionOnEveryFrame() throws Exception {
        byte[] rom;
        try (var input = TestRom.open()) {
            rom = input.readAllBytes();
        }
        var session = new RoomSession(
                rom,
                new GPU(),
                new RoomLoader(rom),
                new OverworldTilesetTable(rom),
                new OverworldCollision(RomTables.loadFromRom(rom)),
                new TransientVfxSystem(16),
                null);
        session.rooms().loadInitialOverworld(0xA0);
        int slot = session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(e -> e.type() == 0x2D && e.x() == 72)
                .findFirst()
                .orElseThrow()
                .slot();
        // Replace only this bush, just as the emulator experiment does. Keep
        // the adjacent bushes/house walls in the actual ROM room buffer.
        session.components.location().activeRoom.roomObjectsArea()[0x35] = 0x04;
        session.entities().tickEntities(0, 56, 32); // Entity init.
        session.entities().tickEntities(1, 56, 32); // Reveal return; no movement yet.
        var reveal = session.rooms().activeRoom().entities().slots().get(slot);
        assertEquals(-1, reveal.spriteVariant());
        assertEquals(72, reveal.x());
        assertEquals(48, reveal.y());
        for (int pass = 1; pass <= 80; pass++) {
            session.entities().tickEntities(pass + 1, 56, 32);
            var actual = session.rooms().activeRoom().entities().slots().get(slot);
            // PyBoy, real ROM room A0: XY advances $0C/16 until blocked
            // at (77,53) on pass 8; speeds/fractions survive wall restoration.
            int travel = Math.min(5, 12 * pass / 16);
            assertEquals(72 + travel, actual.x(), "X pass " + pass);
            assertEquals(48 + travel, actual.y(), "Y pass " + pass);
            int planarSpeed = pass < 34 ? 12 : pass < 53 ? 6 : pass < 65 ? 3 : 0;
            assertEquals(
                    planarSpeed,
                    session.components.roomSessionViews().entityDropSpeedXForTest(slot),
                    "speed X pass " + pass);
            int zQ4;
            if (pass <= 34) zQ4 = 32 * pass - pass * (pass - 1);
            else if (pass <= 53) {
                int n = pass - 34;
                zQ4 = 14 + 17 * n - n * (n - 1);
            } else {
                int n = Math.min(pass, 65) - 53;
                zQ4 = 11 + 10 * n - n * (n - 1);
            }
            assertEquals(Math.max(0, Math.floorDiv(zQ4, 16)), actual.z(), "Z pass " + pass);
        }
    }
}
