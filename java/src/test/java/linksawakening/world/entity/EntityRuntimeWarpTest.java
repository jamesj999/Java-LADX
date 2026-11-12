package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.world.room.ChestContentsTable;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityRuntimeWarpTest extends EntityRuntimeTestSupport {

    @Test
    void dungeonWarpStaysHiddenUntilMinibossDefeatThenCyclesAllEightPhases() throws IOException {
        byte[] rom = loadRom();
        var catalog = new EntitySpriteHandlerCatalog(rom);
        var warp = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x61)
                .at(80, 72)
                .spriteDefinition(catalog.forEntityType(0x61, EntityRoomLoader.RoomTable.INDOORS_A, 0))
                .build();
        var runtime = RoomEntityRuntime.builder(snapshot(warp))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom)))
                .chestContentsTable(new ChestContentsTable(rom))
                .romTables(linksawakening.rom.RomTables.load(linksawakening.world.entity.EntityRomTestData.view(rom)))
                .rom(linksawakening.world.entity.EntityRomTestData.view(rom))
                .build();
        EntityRuntimeFixture.debug(runtime).setEntityMapIdForTest(0);
        runtime.configuration().setEntityRoomId(0x11);
        assertEquals(-1, runtime.inspection().snapshot().slots().get(0).spriteVariant());
        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 32, () -> 0));
        assertEquals(-1, runtime.inspection().snapshot().slots().get(0).spriteVariant());
        runtime.configuration().setDungeonMinibossReady(true);
        for (int phase = 0; phase < 8; phase++) {
            runtime.tick(EntityTestFrames.frame(runtime, phase * 8, 120, 32, () -> 0));
            assertEquals(phase, runtime.inspection().snapshot().slots().get(0).spriteVariant());
        }
    }
}
