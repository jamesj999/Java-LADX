package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.testing.TestRoms;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityRuntimeLifecycle3Test extends EntityRuntimeTestSupport {

    @Test
    void terminalEnemyDeathSpawnsTopDownDropInReverseFreeSlotAndPreservesItUntilNextTick() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        int sourceX = 0x44;
        int sourceY = 0x58;
        int sourceZ = 0x07;
        RoomEntity source = new RoomEntityBuilder()
                .slot(15)
                .sourceLoadOrder(0)
                .type(0x09)
                .at(sourceX, sourceY)
                .status(EntityStatus.DYING)
                .spriteDefinition(catalog.forEntityType(0x09, EntityRoomLoader.RoomTable.OVERWORLD, -1))
                .z(sourceZ)
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshotAt(source))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();
        runtime.configuration()
                .setEnemyDropResolver(new EnemyDropResolver(linksawakening.world.entity.EntityRomTestData.view(rom)));
        EntityRuntimeFixture.debug(runtime).setDroppedItemForTest(15, 0x2E);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));

        RoomEntity drop = runtime.inspection().snapshot().slots().get(14);
        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(15).status());
        assertEquals(EntityStatus.ACTIVE, drop.status());
        assertEquals(-1, drop.sourceLoadOrder());
        assertEquals(0x2E, drop.type());
        assertEquals(sourceX, drop.x());
        assertEquals(sourceY, drop.y());
        assertEquals(sourceZ, drop.z());
        assertTrue(drop.spriteDefinition().supported());
        assertEquals(drop.spriteDefinition().initialVariant(), drop.spriteVariant());
        assertEquals(0x80, new EntityRoomInspectionQueries(runtime.inspection()).slowTransitionCountdown(14));
        assertEquals(0x18, new EntityRoomInspectionQueries(runtime.inspection()).dropPrivateCountdown1(14));
        assertEquals(0x03, new EntityRoomInspectionQueries(runtime.inspection()).dropPrivateCountdown3(14));
        assertEquals(0x18, runtime.inspection().dropSpeedZ(14));
        assertEquals(0x00, new EntityRoomInspectionQueries(runtime.inspection()).dropSpeedY(14));
        assertEquals(0, EntityRuntimeFixture.inspection(runtime).droppedItemForTest(15));

        runtime.tick(EntityTestFrames.frame(runtime, 1, 0, 0, () -> 0));

        assertEquals(0x17, new EntityRoomInspectionQueries(runtime.inspection()).dropPrivateCountdown1(14));
        assertEquals(0x02, new EntityRoomInspectionQueries(runtime.inspection()).dropPrivateCountdown3(14));
        assertEquals(
                (sourceZ + 1) & 0xFF,
                runtime.inspection().snapshot().slots().get(14).z());
        assertEquals(0x16, runtime.inspection().dropSpeedZ(14));
        assertNull(runtime.collectIfNeeded(1, sourceX, sourceY + 2, false, true));

        for (int frame = 2; frame <= 24; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0, 0, () -> 0));
        }
        assertEquals(0, new EntityRoomInspectionQueries(runtime.inspection()).dropPrivateCountdown1(14));
        runtime.tick(EntityTestFrames.frame(runtime, 25, 0, 0, () -> 0));
        EntityPickupEvent pickup = runtime.collectIfNeeded(25, sourceX, sourceY + 2, false, true);
        assertNotNull(pickup);
        assertEquals(14, pickup.slot());
        assertEquals(0x2E, pickup.type());
        assertEquals(0, pickup.persistentClearMask());
        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(14).status());
        assertEquals(0, new EntityRoomInspectionQueries(runtime.inspection()).dropPrivateCountdown1(14));
        assertEquals(0, new EntityRoomInspectionQueries(runtime.inspection()).dropPrivateCountdown3(14));
        assertEquals(0, runtime.inspection().dropSpeedZ(14));
        assertEquals(0, new EntityRoomInspectionQueries(runtime.inspection()).dropSpeedY(14));
    }

    @Test
    void terminalEnemyDeathSpawnsSideScrollHeartWithRomDropConfiguration() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        RoomEntity source = new RoomEntityBuilder()
                .slot(15)
                .sourceLoadOrder(0)
                .type(0x0B)
                .at(0x50, 0x60)
                .status(EntityStatus.DYING)
                .spriteDefinition(catalog.forEntityType(0x0B, EntityRoomLoader.RoomTable.OVERWORLD, -1))
                .z(0x09)
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshotAt(source).withSideScrolling(true))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();
        runtime.configuration()
                .setEnemyDropResolver(new EnemyDropResolver(linksawakening.world.entity.EntityRomTestData.view(rom)));
        EntityRuntimeFixture.debug(runtime).setDroppedItemForTest(15, 0x2D);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));

        RoomEntity drop = runtime.inspection().snapshot().slots().get(14);
        assertEquals(EntityStatus.ACTIVE, drop.status());
        assertEquals(-1, drop.sourceLoadOrder());
        assertEquals(0x2D, drop.type());
        assertEquals(0x50, drop.x());
        assertEquals(0x60, drop.y());
        assertEquals(0x09, drop.z());
        assertTrue(drop.spriteDefinition().supported());
        assertEquals(drop.spriteDefinition().initialVariant(), drop.spriteVariant());
        assertEquals(0x80, new EntityRoomInspectionQueries(runtime.inspection()).slowTransitionCountdown(14));
        assertEquals(0x18, new EntityRoomInspectionQueries(runtime.inspection()).dropPrivateCountdown1(14));
        assertEquals(0x03, new EntityRoomInspectionQueries(runtime.inspection()).dropPrivateCountdown3(14));
        assertEquals(0xEC, new EntityRoomInspectionQueries(runtime.inspection()).dropSpeedY(14));
        assertEquals(0x00, runtime.inspection().dropSpeedZ(14));
    }

    @Test
    void fullEntitySlotsStillUnloadSourceWhenDropCannotBeSpawned() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        List<RoomEntity> entities = new ArrayList<>();
        for (int slot = 0; slot < 15; slot++) {
            entities.add(new RoomEntityBuilder()
                    .slot(slot)
                    .sourceLoadOrder(slot + 1)
                    .type(0xFF)
                    .at(0x20, 0x20)
                    .spriteDefinition(EntitySpriteDefinition.unsupported(0xFF))
                    .spriteVariant(-1)
                    .build());
        }
        RoomEntity source = new RoomEntityBuilder()
                .slot(15)
                .sourceLoadOrder(0)
                .type(0x09)
                .at(0x44, 0x58)
                .status(EntityStatus.DYING)
                .spriteDefinition(catalog.forEntityType(0x09, EntityRoomLoader.RoomTable.OVERWORLD, -1))
                .build();
        entities.add(source);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(new RoomEntitySnapshot(entities))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();
        runtime.configuration()
                .setEnemyDropResolver(new EnemyDropResolver(linksawakening.world.entity.EntityRomTestData.view(rom)));
        EntityRuntimeFixture.debug(runtime).setDroppedItemForTest(15, 0x2E);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));

        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(15).status());
        assertFalse(runtime.inspection().snapshot().slots().stream()
                .anyMatch(entity -> entity.loaded() && (entity.type() == 0x2D || entity.type() == 0x2E)));
        assertEquals(1, new EntityRoomInspectionQueries(runtime.inspection()).killCount());
        assertEquals(1, runtime.consumePendingClearedEntityMask());
    }
}
