package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.*;

import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.testing.RequiresRom;
import linksawakening.testing.TestRom;
import org.junit.jupiter.api.Test;

@RequiresRom
final class BushHeartTrajectoryTest {
    @Test
    void wellHeartMatchesEveryRomHandlerPassThroughAllThreeLandings() throws Exception {
        byte[] rom;
        try (var input = TestRom.open()) {
            rom = input.readAllBytes();
        }
        var initial = new EntityRoomLoader(rom).load(EntityRoomLoader.RoomTable.OVERWORLD, 0xA0);
        var heart = initial.loadedEntities().stream()
                .filter(e -> e.type() == 0x2D)
                .findFirst()
                .orElseThrow();
        var runtime = RoomEntityRuntime.builder(initial)
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(new EntitySpriteHandlerCatalog(rom))
                .enemyCombatTables(new RomEnemyCombatTables(rom))
                .build();
        runtime.configuration().setObjectQuery(e -> new RoomEntityObjectSample(0x04, 0, 0, 0));
        int slot = heart.slot();
        runtime.tick(EntityTestFrames.frame(runtime, 0, heart.x() - 16, heart.y() - 16, () -> 0));
        runtime.tick(EntityTestFrames.frame(runtime, 1, heart.x() - 16, heart.y() - 16, () -> 0));
        assertEquals(-1, runtime.inspection().snapshot().slots().get(slot).spriteVariant());
        assertEquals(0x20, runtime.inspection().dropSpeedZ(slot));

        // $626B-$629C seeds XY=$0C, Z=$20 and returns past the handler.
        // $60B3/$6B7B integrate Q4 positions before gravity $02. $60E3-$6155
        // preserves fractions across landings, complements the arithmetic
        // half of Z speed, and halves XY. These closed-form arcs retain the
        // residual Z fractions $0E/$0B after passes 34/53 respectively.
        for (int pass = 1; pass <= 80; pass++) {
            runtime.tick(EntityTestFrames.frame(runtime, pass + 1, 0, 0, () -> 0));
            int planarQ4, zQ4, expectedSpeed;
            if (pass <= 34) {
                planarQ4 = 12 * pass;
                zQ4 = 32 * pass - pass * (pass - 1);
                expectedSpeed = pass == 34 ? 17 : (32 - 2 * pass) & 255;
            } else if (pass <= 53) {
                int n = pass - 34;
                planarQ4 = 408 + 6 * n;
                zQ4 = 14 + 17 * n - n * (n - 1);
                expectedSpeed = pass == 53 ? 10 : (17 - 2 * n) & 255;
            } else {
                int n = Math.min(pass, 65) - 53;
                planarQ4 = 522 + 3 * n;
                zQ4 = 11 + 10 * n - n * (n - 1);
                expectedSpeed = pass >= 65 ? 0 : (10 - 2 * n) & 255;
            }
            var actual = runtime.inspection().snapshot().slots().get(slot);
            assertEquals(heart.x() + planarQ4 / 16, actual.x(), "X pass " + pass);
            assertEquals(heart.y() + planarQ4 / 16, actual.y(), "Y pass " + pass);
            assertEquals(Math.max(0, Math.floorDiv(zQ4, 16)), actual.z(), "Z pass " + pass);
            if (pass <= 65) assertEquals(expectedSpeed, runtime.inspection().dropSpeedZ(slot), "speed pass " + pass);
        }
    }
}
