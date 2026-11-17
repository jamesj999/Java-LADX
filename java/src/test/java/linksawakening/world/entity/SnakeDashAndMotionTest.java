package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.loadRom;
import static linksawakening.world.entity.EntityRuntimeTestSupport.sequence;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
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
final class SnakeDashAndMotionTest {

    @Test
    void snakeUsesTheRomInitCountdownRandomWalkAndAnimation() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        EntitySpriteDefinition definition = catalog.forEntityType(0xA1, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0xA1)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x00)));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 120, 120, sequence(0x00)));

        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).snakeState(0));
        assertEquals(0x30, new EntityEnemyInspectionQueries(runtime.enemies()).snakeTransitionCountdown(0));
        assertEquals(0x2F, new EntityEnemyInspectionQueries(runtime.enemies()).snakePrivateCountdown1(0));
        assertEquals(0x08, new EntityEnemyInspectionQueries(runtime.enemies()).snakeSpeedX(0));
        assertEquals(0x00, new EntityEnemyInspectionQueries(runtime.enemies()).snakeSpeedY(0));
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).spriteVariant());

        runtime.tick(EntityTestFrames.frame(runtime, 2, 120, 120, sequence(0xFF)));
        assertEquals(0x2F, new EntityEnemyInspectionQueries(runtime.enemies()).snakeTransitionCountdown(0));
        assertEquals(0x2E, new EntityEnemyInspectionQueries(runtime.enemies()).snakePrivateCountdown1(0));
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).x());

        runtime.tick(EntityTestFrames.frame(runtime, 8, 120, 120, sequence(0xFF)));
        assertEquals(2, runtime.inspection().snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void snakeStartsTheRomAxisDashWhenItsPrivateTimerExpires() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        EntitySpriteDefinition definition = catalog.forEntityType(0xA1, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0xA1)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x00)));
        for (int frame = 1; frame <= 48; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 64, 120, sequence(0x02)));
        }

        assertEquals(2, new EntityEnemyInspectionQueries(runtime.enemies()).snakeState(0));
        assertEquals(0x30, new EntityEnemyInspectionQueries(runtime.enemies()).snakeTransitionCountdown(0));
        assertEquals(0x00, new EntityEnemyInspectionQueries(runtime.enemies()).snakeSpeedX(0));
        assertEquals(0x10, new EntityEnemyInspectionQueries(runtime.enemies()).snakeSpeedY(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).snakePrivateCountdown1(0));
    }

    @Test
    void snakeBackgroundCollisionResetsTheRomStateAndPrivateRecoveryWindow() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        EntitySpriteDefinition definition = catalog.forEntityType(0xA1, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0xA1)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x00)));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 120, 120, sequence(0x00)));
        runtime.tick(EntityTestFrames.frame(runtime, 2, 120, 120, sequence(0x00)));
        runtime.tick(EntityTestFrames.frame(
                runtime, 3, 120, 120, sequence(0x00), (entity, direction, nextX, nextY) -> true));

        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).snakeState(0));
        assertEquals(0x08, new EntityEnemyInspectionQueries(runtime.enemies()).snakeTransitionCountdown(0));
        assertEquals(0x20, new EntityEnemyInspectionQueries(runtime.enemies()).snakePrivateCountdown1(0));
        assertEquals(0x00, new EntityEnemyInspectionQueries(runtime.enemies()).snakeSpeedX(0));
        assertEquals(0x00, new EntityEnemyInspectionQueries(runtime.enemies()).snakeSpeedY(0));
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).x());
    }

    @Test
    void snakeUsesHealthGroupZeroContactAndSwordValues() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        EntitySpriteDefinition definition = catalog.forEntityType(0xA1, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0xA1)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        List<EntityCombatEvent> contact =
                runtime.combat().resolveCombat(new EntitySwordContactFrame(1, 72, 72, false, true, false, 0, 0, 0, 0));
        assertEquals(1, contact.size());
        assertEquals(0x04, contact.get(0).linkDamage());

        List<EntityCombatEvent> sword = runtime.combat()
                .resolveCombat(new EntitySwordContactFrame(2, 120, 120, false, true, true, 72, 1, 72, 1));
        assertEquals(1, sword.size());
        assertTrue(sword.get(0).swordHit());
        assertEquals(0, runtime.enemies().enemyHealth(0));
        assertEquals(
                EntityStatus.DYING,
                runtime.inspection().snapshot().slots().get(0).status());
    }
}
