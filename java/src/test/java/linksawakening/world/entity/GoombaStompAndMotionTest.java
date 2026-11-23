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
final class GoombaStompAndMotionTest {

    @Test
    void goombaUsesTheRomNormalRoomRandomWalkAndAnimation() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        EntitySpriteDefinition definition = catalog.forEntityType(0x9F, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x9F)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        // The room init is a noop for Goomba; its first active handler pass
        // chooses the random-walk interval and direction.
        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x00)));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 120, 120, sequence(0x00, 0x00)));

        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).goombaState(0));
        assertEquals(0x30, new EntityEnemyInspectionQueries(runtime.enemies()).goombaTransitionCountdown(0));
        assertEquals(0x08, new EntityEnemyInspectionQueries(runtime.enemies()).goombaSpeedX(0));
        assertEquals(0x00, new EntityEnemyInspectionQueries(runtime.enemies()).goombaSpeedY(0));
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).spriteVariant());

        runtime.tick(EntityTestFrames.frame(runtime, 2, 120, 120, sequence(0xFF)));
        assertEquals(0x2F, new EntityEnemyInspectionQueries(runtime.enemies()).goombaTransitionCountdown(0));
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(0x02, new EntityEnemyInspectionQueries(runtime.enemies()).goombaSpeedY(0));

        runtime.tick(EntityTestFrames.frame(runtime, 0x10, 120, 120, sequence(0xFF)));
        assertEquals(1, runtime.inspection().snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void goombaUsesRomHealthGroupZeroContactAndSwordValues() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        EntitySpriteDefinition definition = catalog.forEntityType(0x9F, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x9F)
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

    @Test
    void goombaStompUsesTheRomDescendingVelocityBranchAndSharedDeathHandler() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        EntitySpriteDefinition definition = catalog.forEntityType(0x9F, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x9F)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        List<EntityCombatEvent> events = runtime.combat()
                .resolveCombat(new EntityAttackContactFrame(1, 72, 72, true, 0xF0, true, false, 0, 0, 0, 0));
        assertEquals(1, events.size());
        assertEquals(
                EntityCombatEvent.LinkAction.GOOMBA_BOUNCE_TOP_DOWN,
                events.get(0).linkAction());
        assertEquals(EntityCombatEvent.SoundChannel.WAVE, events.get(0).soundChannel());
        assertEquals(0x0E, events.get(0).soundId());
        assertEquals(2, new EntityEnemyInspectionQueries(runtime.enemies()).goombaState(0));
        assertEquals(0x30, new EntityEnemyInspectionQueries(runtime.enemies()).goombaTransitionCountdown(0));
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());

        for (int frame = 0; frame < 0x30; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, sequence(0x00)));
        }
        assertEquals(
                EntityStatus.DYING,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(0x2D, EntityRuntimeFixture.inspection(runtime).droppedItemForTest(0));
        assertEquals(0x0C, new EntityEnemyInspectionQueries(runtime.enemies()).dyingCountdown(0));
        assertEquals(0x04, runtime.enemies().physicsFlags(0));
        assertEquals(2, runtime.inspection().snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void goombaDoesNotStompWhileLinkIsAscending() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        EntitySpriteDefinition definition = catalog.forEntityType(0x9F, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x9F)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        List<EntityCombatEvent> events = runtime.combat()
                .resolveCombat(new EntityAttackContactFrame(1, 72, 72, true, 0x10, true, false, 0, 0, 0, 0));
        assertTrue(events.isEmpty());
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).goombaState(0));
    }

    @Test
    void goombaSideScrollStompUsesTheRomHorizontalBounceAction() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        EntitySpriteDefinition definition = catalog.forEntityType(0x9F, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x9F)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build())
                .withSideScrolling(true);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial)
                .indoorRoom(true)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        List<EntityCombatEvent> events = runtime.combat()
                .resolveCombat(new EntityAttackContactFrame(1, 72, 72, true, 0x10, true, false, 0, 0, 0, 0));
        assertEquals(1, events.size());
        assertEquals(
                EntityCombatEvent.LinkAction.GOOMBA_BOUNCE_SIDE_SCROLLING,
                events.get(0).linkAction());
        assertEquals(2, new EntityEnemyInspectionQueries(runtime.enemies()).goombaState(0));

        for (int frame = 0; frame < 0x30; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, sequence(0x00)));
        }
        assertEquals(
                EntityStatus.DYING,
                runtime.inspection().snapshot().slots().get(0).status());
    }
}
