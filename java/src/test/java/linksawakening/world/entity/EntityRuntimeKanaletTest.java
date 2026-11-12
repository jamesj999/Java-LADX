package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityRuntimeKanaletTest extends EntityRuntimeTestSupport {

    @Test
    void kanaletKnightAndMadBomberUseNonPersistentGoldenLeafDrops() {
        RoomEntityRuntime knightRuntime = RoomEntityRuntime.builder(
                        snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                                .sourceLoadOrder(3)
                                .type(0x51)
                                .at(0x40, 0x50)
                                .spriteDefinition(EntitySpriteDefinition.unsupported(0x51))
                                .build()))
                .indoorRoom(true)
                .build();
        EntityRuntimeFixture.debug(knightRuntime).setEntityMapIdForTest(0x14);
        knightRuntime.tick(EntityTestFrames.frame(knightRuntime, 0, 0, 0, () -> 0));
        assertEquals(0xFF, knightRuntime.inspection().snapshot().slots().get(0).sourceLoadOrder());
        assertEquals(0x3C, EntityRuntimeFixture.inspection(knightRuntime).droppedItemForTest(0));

        RoomEntityRuntime bomberRuntime = RoomEntityRuntime.builder(
                        snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                                .sourceLoadOrder(2)
                                .type(0x93)
                                .at(0x40, 0x50)
                                .spriteDefinition(EntitySpriteDefinition.unsupported(0x93))
                                .build()))
                .indoorRoom(false)
                .build();
        bomberRuntime.tick(EntityTestFrames.frame(bomberRuntime, 0, 0, 0, () -> 0));
        assertEquals(0xFF, bomberRuntime.inspection().snapshot().slots().get(0).sourceLoadOrder());
        assertEquals(0x3C, EntityRuntimeFixture.inspection(bomberRuntime).droppedItemForTest(0));
    }

    @Test
    void kanaletRoomEventDropsGoldenLeafInsteadOfDungeonKey() {
        RoomEntityRuntime runtime =
                RoomEntityRuntime.builder(snapshot()).indoorRoom(true).build();
        EntityRuntimeFixture.debug(runtime).setEntityMapIdForTest(0x14);

        int slot = runtime.spawnRoomEventKeyDrop();

        RoomEntity leaf = runtime.inspection().snapshot().slots().get(slot);
        assertEquals(0x3C, leaf.type());
        assertEquals(0x28, leaf.x());
        assertEquals(0x3C, leaf.y());
        assertEquals(0x70, leaf.z());
        assertEquals(-1, leaf.sourceLoadOrder());
    }

    @Test
    void kanaletKnightTerminalDeathActuallySpawnsTheGoldenLeaf() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        RoomEntity knight = new linksawakening.world.entity.RoomEntityBuilder()
                .slot(15)
                .sourceLoadOrder(3)
                .type(0x51)
                .at(0x44, 0x58)
                .status(EntityStatus.DYING)
                .spriteDefinition(catalog.forEntityType(0x51, EntityRoomLoader.RoomTable.INDOORS_B, -1))
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshotAt(knight))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();
        runtime.configuration().setEnemyDropResolver(new EnemyDropResolver(rom));
        EntityRuntimeFixture.debug(runtime).setEntityMapIdForTest(0x14);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));

        RoomEntity leaf = runtime.inspection().snapshot().slots().stream()
                .filter(entity -> entity.loaded() && entity.type() == 0x3C)
                .findFirst()
                .orElseThrow();
        assertEquals(0x44, leaf.x());
        assertEquals(0x58, leaf.y());
        assertEquals(0, runtime.consumePendingClearedEntityMask());
    }

    @Test
    void bothKanaletBombableWallsSpawnSwordMoblinsCarryingGoldenLeaves() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(
                        new linksawakening.world.entity.RoomEntityBuilder()
                                .sourceLoadOrder(0)
                                .type(0x94)
                                .at(0x28, 0x40)
                                .spriteDefinition(EntitySpriteDefinition.unsupported(0x94))
                                .spriteVariant(-1)
                                .build(),
                        new linksawakening.world.entity.RoomEntityBuilder()
                                .slot(1)
                                .sourceLoadOrder(1)
                                .type(0x94)
                                .at(0x78, 0x40)
                                .spriteDefinition(EntitySpriteDefinition.unsupported(0x94))
                                .spriteVariant(-1)
                                .build()))
                .indoorRoom(true)
                .build();
        EntityRuntimeFixture.debug(runtime).setEntityMapIdForTest(0x14);
        EntityRuntimeFixture.debug(runtime).setEnemyIgnoreHitsCountdownForTest(0, 0x0A);
        EntityRuntimeFixture.debug(runtime).setEnemyIgnoreHitsCountdownForTest(1, 0x0A);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));

        List<RoomEntity> moblins = runtime.inspection().snapshot().slots().stream()
                .filter(entity -> entity.loaded() && entity.type() == 0x14)
                .toList();
        assertEquals(2, moblins.size());
        assertTrue(moblins.stream().anyMatch(entity -> entity.x() == 0x28 && entity.y() == 0x48));
        assertTrue(moblins.stream().anyMatch(entity -> entity.x() == 0x78 && entity.y() == 0x48));
        for (RoomEntity moblin : moblins) {
            assertEquals(0xFF, moblin.sourceLoadOrder());
            assertEquals(0x3C, EntityRuntimeFixture.inspection(runtime).droppedItemForTest(moblin.slot()));
        }
        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(1).status());
    }

    @Test
    void completedKanaletWallRoomUnloadsWallsWithoutSpawningMoblins() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(
                        snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                                .sourceLoadOrder(0)
                                .type(0x94)
                                .at(0x28, 0x40)
                                .spriteDefinition(EntitySpriteDefinition.unsupported(0x94))
                                .spriteVariant(-1)
                                .build()))
                .indoorRoom(true)
                .build();
        EntityRuntimeFixture.debug(runtime).setEntityMapIdForTest(0x14);
        EntityRuntimeFixture.debug(runtime).setEntityRoomStatusForTest(0x10);
        EntityRuntimeFixture.debug(runtime).setEnemyIgnoreHitsCountdownForTest(0, 0x0A);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));

        assertFalse(runtime.inspection().snapshot().slots().stream()
                .anyMatch(entity -> entity.loaded() && entity.type() == 0x14));
        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(0).status());
    }

    @Test
    void kanaletWallWaitsForAFreeSlotBeforeBreaking() {
        List<RoomEntity> entities = new ArrayList<>();
        entities.add(new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x94)
                .at(0x28, 0x40)
                .spriteDefinition(EntitySpriteDefinition.unsupported(0x94))
                .spriteVariant(-1)
                .build());
        for (int slot = 1; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            entities.add(new linksawakening.world.entity.RoomEntityBuilder()
                    .slot(slot)
                    .sourceLoadOrder(slot)
                    .type(0xFF)
                    .at(0x20, 0x20)
                    .spriteDefinition(EntitySpriteDefinition.unsupported(0xFF))
                    .spriteVariant(-1)
                    .build());
        }
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(new RoomEntitySnapshot(entities))
                .indoorRoom(true)
                .build();
        EntityRuntimeFixture.debug(runtime).setEntityMapIdForTest(0x14);
        EntityRuntimeFixture.debug(runtime).setEnemyIgnoreHitsCountdownForTest(0, 0x0A);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));

        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());
        assertFalse(runtime.inspection().snapshot().slots().stream()
                .anyMatch(entity -> entity.loaded() && entity.type() == 0x14));
    }

    @Test
    void bombExplosionTriggersTheKanaletWallMoblinHandler() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        RoomEntity wall = new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x94)
                .at(0x48, 0x4F)
                .spriteDefinition(catalog.forEntityType(0x94, EntityRoomLoader.RoomTable.INDOORS_B, 0x14))
                .spriteVariant(-1)
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(wall))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();
        EntityRuntimeFixture.debug(runtime).setEntityMapIdForTest(0x14);
        int bombSlot = runtime.spawnBomb(0x40, 0x50, 0, 0);
        EntityRuntimeFixture.debug(runtime).setBombTransitionCountdownForTest(bombSlot, 0x13);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));

        RoomEntity moblin = runtime.inspection().snapshot().slots().stream()
                .filter(entity -> entity.loaded() && entity.type() == 0x14)
                .findFirst()
                .orElseThrow();
        assertEquals(0x48, moblin.x());
        assertEquals(0x57, moblin.y());
        assertEquals(0x3C, EntityRuntimeFixture.inspection(runtime).droppedItemForTest(moblin.slot()));
    }
}
