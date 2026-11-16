package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.loadRom;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshotWithSlots;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.testing.TestRoms;
import linksawakening.world.entity.event.EntityCombatEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class BomberSpawnTest {

    @Test
    void bomberUsesItsSourceHandlerToSpawnAnEnemyBomb() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        EntitySpriteDefinition bomberDefinition = catalog.forEntityType(0xBA, EntityRoomLoader.RoomTable.OVERWORLD, -1);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshotWithSlots(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0xBA)
                        .at(0x50, 0x50)
                        .spriteDefinition(bomberDefinition)
                        .z(0x00)
                        .build()))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x50, 0x50, () -> 0));

        RoomEntity bomb = runtime.inspection().snapshot().slots().get(15);
        assertTrue(bomb.loaded());
        assertEquals(EntityStatus.ACTIVE, bomb.status());
        assertEquals(0x02, bomb.type());
        assertEquals(0x50, bomb.x());
        assertEquals(0x50, bomb.y());
        assertEquals(0x10, bomb.z());
        assertEquals(0x40, runtime.enemies().transitionCountdown(15));
        assertEquals(0x01, runtime.enemies().enemyIgnoreHitsCountdown(15));
        assertEquals(
                List.of(new EntityCombatEvent(15, 0x02, 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x08)),
                runtime.outbox().consumePendingEntityEvents());
    }

    @Test
    void bomberEnemyBombCarriesTheSourceVectorIntoTheNextBombHandlerFrame() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshotWithSlots(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0xBA)
                        .at(0x50, 0x50)
                        .spriteDefinition(catalog.forEntityType(0xBA, EntityRoomLoader.RoomTable.OVERWORLD, -1))
                        .z(0x00)
                        .build()))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom)))
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x70, 0x50, () -> 0));
        runtime.outbox().consumePendingEntityEvents();
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x70, 0x50, () -> 0));

        RoomEntity bomb = runtime.inspection().snapshot().slots().get(15);
        assertEquals(0x51, bomb.x());
        assertEquals(0x50, bomb.y());
        assertEquals(0x10, bomb.z());
        assertEquals(0x3F, runtime.enemies().transitionCountdown(15));
        assertTrue(runtime.outbox().consumePendingEntityEvents().isEmpty());
    }

    @Test
    void madBomberUsesItsHoleCycleToSpawnTheSourceEnemyBomb() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        EntitySpriteDefinition definition = catalog.forEntityType(0x93, EntityRoomLoader.RoomTable.OVERWORLD, -1);
        assertTrue(definition.supported());
        assertEquals(0x06, definition.bank());
        assertEquals(0x4126, definition.address());
        assertEquals(5, definition.variantCount());

        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshotWithSlots(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x93)
                        .at(0x50, 0x50)
                        .spriteDefinition(definition)
                        .z(0x00)
                        .build()))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        for (int frame = 0; frame <= 136; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0xC0, 0xC0, () -> 0));
        }

        RoomEntity bomb = runtime.inspection().snapshot().slots().get(15);
        assertTrue(bomb.loaded());
        assertEquals(EntityStatus.ACTIVE, bomb.status());
        assertEquals(0x02, bomb.type());
        assertEquals(0x28, bomb.x());
        assertEquals(0x40, bomb.y());
        assertEquals(0x04, bomb.z());
        assertEquals(0x40, runtime.enemies().transitionCountdown(15));
        assertEquals(0x01, runtime.enemies().enemyIgnoreHitsCountdown(15));
        assertEquals(
                List.of(new EntityCombatEvent(15, 0x02, 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x08)),
                runtime.outbox().consumePendingEntityEvents());
    }
}
