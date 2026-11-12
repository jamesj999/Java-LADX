package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.testing.TestRoms;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.EntityProjectileEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityRuntimeWizrobeTest extends EntityRuntimeTestSupport {

    @Test
    void wizrobeRunsTheRomRevealStateMachineAndLaunchesItsProjectile() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        EntitySpriteDefinition definition = catalog.forEntityType(0x21, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x21)
                        .at(0x40, 0x50)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x80, 0x50, sequence(0x00)));
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(0x80, runtime.enemies().transitionCountdown(0));
        assertEquals(-1, runtime.inspection().snapshot().slots().get(0).spriteVariant());
        assertEquals(0x02, runtime.enemies().physicsFlags(0));

        for (int frame = 1; frame <= 0x80; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x80, 0x50, sequence(0x00)));
        }
        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).wizrobeState(0));
        assertEquals(0x20, new EntityEnemyInspectionQueries(runtime.enemies()).wizrobePrivateCountdown1(0));
        assertEquals(0x01, new EntityEnemyInspectionQueries(runtime.enemies()).wizrobePrivateState1(0));
        assertEquals(-1, runtime.inspection().snapshot().slots().get(0).spriteVariant());

        EntityRuntimeFixture.debug(runtime).setWizrobeStateForTest(0, 2, 0, 1, 2, 0);
        runtime.tick(EntityTestFrames.frame(runtime, 0x81, 0x80, 0x50, sequence(0x00)));
        assertEquals(3, new EntityEnemyInspectionQueries(runtime.enemies()).wizrobeState(0));
        assertEquals(0x01, new EntityEnemyInspectionQueries(runtime.enemies()).wizrobePrivateState1(0));
        assertEquals(0x01, new EntityEnemyInspectionQueries(runtime.enemies()).wizrobePrivateCountdown1(0));

        runtime.tick(EntityTestFrames.frame(runtime, 0x82, 0x80, 0x50, sequence(0x00)));
        assertEquals(0x40, new EntityEnemyInspectionQueries(runtime.enemies()).wizrobePrivateCountdown1(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).wizrobeDirection(0));
        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).wizrobeNextSpriteVariant(0));

        EntityRuntimeFixture.debug(runtime).setWizrobeStateForTest(0, 3, 0, 0xFF, 0x29, 0);
        runtime.tick(EntityTestFrames.frame(runtime, 0x83, 0x80, 0x50, sequence(0x00)));
        assertEquals(0x28, new EntityEnemyInspectionQueries(runtime.enemies()).wizrobePrivateCountdown1(0));
        RoomEntity projectile = runtime.inspection().snapshot().slots().get(15);
        assertEquals(EntityStatus.ACTIVE, projectile.status());
        assertEquals(-1, projectile.sourceLoadOrder());
        assertEquals(0x22, projectile.type());
        assertEquals(0x48, projectile.x());
        assertEquals(0x50, projectile.y());
        assertEquals(0, projectile.spriteVariant());
        assertEquals(0x42, runtime.enemies().physicsFlags(15));
        assertEquals(0x02, runtime.enemies().physicsFlags(0));
        assertEquals(0x20, new EntityEnemyInspectionQueries(runtime.enemies()).wizrobeProjectileSpeedX(15));
        assertEquals(0x00, new EntityEnemyInspectionQueries(runtime.enemies()).wizrobeProjectileSpeedY(15));
    }

    @Test
    void wizrobeProjectileUsesRomPaletteFlipMovementAndCollisionLifecycle() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        EntitySpriteDefinition definition = catalog.forEntityType(0x22, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .type(0x22)
                        .at(0x3F, 0x50)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();
        EntityRuntimeFixture.debug(runtime).setWizrobeProjectileForTest(0, 0x20, 0x00, 0);

        List<EntityProjectileEvent> events = runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                4,
                0x40,
                0x50,
                sequence(0x00),
                null,
                new EnemyProjectileCollision.LinkState(0x40, 0x50, 0, 0x00, 0, false)));

        assertEquals(1, events.size());
        assertEquals(EntityProjectileEvent.Kind.LINK_DAMAGE, events.getFirst().kind());
        assertEquals(0x08, events.getFirst().linkDamage());
        assertTrue(events.getFirst().remove());
        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(0).status());
    }

    @Test
    void wizrobeUsesHealthGroupTwelveContactAndSwordValues() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        EntitySpriteDefinition definition = catalog.forEntityType(0x21, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x21)
                        .at(0x40, 0x50)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();
        EntityRuntimeFixture.debug(runtime).setWizrobeStateForTest(0, 3, 0, 0xFF, 0x28, 0);

        List<EntityCombatEvent> contact =
                runtime.resolveCombat(new EntitySwordContactFrame(1, 0x48, 0x58, false, true, false, 0, 0, 0, 0));
        assertEquals(1, contact.size());
        assertEquals(0x08, contact.get(0).linkDamage());

        List<EntityCombatEvent> sword =
                runtime.resolveCombat(new EntitySwordContactFrame(2, 0x80, 0x80, false, true, true, 0x48, 1, 0x58, 1));
        assertEquals(1, sword.size());
        assertTrue(sword.get(0).swordHit());
        // Health group $0C's sword rows are zero in Data_003_43EC; Wizrobe
        // contact damage is $08, but a normal sword hit is ignored.
        assertEquals(4, runtime.enemies().enemyHealth(0));
        assertEquals(0, sword.get(0).enemyDamage());
        assertEquals(0, RoomEntityCombatRules.basicSwordDamage(0x21));
    }
}
