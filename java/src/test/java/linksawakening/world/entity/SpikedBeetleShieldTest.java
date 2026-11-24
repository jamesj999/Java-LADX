package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.loadRom;
import static linksawakening.world.entity.EntityRuntimeTestSupport.sequence;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.testing.TestRoms;
import linksawakening.world.entity.event.EntityCombatEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class SpikedBeetleShieldTest {

    @Test
    void spikedBeetleUsesTheRomRestWalkAndAnimationStateMachine() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        EntitySpriteDefinition definition = catalog.forEntityType(0x2C, EntityRoomLoader.RoomTable.INDOORS_A, 0x00);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x2C)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 80, sequence(0x00)));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 120, 80, sequence(0x00, 0x00)));

        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).spikedBeetleState(0));
        assertEquals(0x30, new EntityEnemyInspectionQueries(runtime.enemies()).spikedBeetleTransitionCountdown(0));
        assertEquals(0x06, new EntityEnemyInspectionQueries(runtime.enemies()).spikedBeetleSpeedX(0));
        assertEquals(0x00, new EntityEnemyInspectionQueries(runtime.enemies()).spikedBeetleSpeedY(0));
        assertEquals(0x12, runtime.enemies().physicsFlags(0));
        assertEquals(0x48, runtime.execution().slotProperties().options1(0));
        assertEquals(0x80, EntityRuntimeFixture.enemies(runtime).hitboxFlagsForTest(0));

        for (int frame = 2; frame <= 0x31; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 80, sequence(0xFF)));
        }
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).spikedBeetleState(0));
        assertEquals(0x18, new EntityEnemyInspectionQueries(runtime.enemies()).spikedBeetleTransitionCountdown(0));
        assertEquals(0x06, new EntityEnemyInspectionQueries(runtime.enemies()).spikedBeetleSpeedX(0));
        assertEquals(0x00, new EntityEnemyInspectionQueries(runtime.enemies()).spikedBeetleSpeedY(0));

        runtime.tick(EntityTestFrames.frame(runtime, 0x40, 120, 64, sequence(0xFF)));
        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).spikedBeetleState(0));
        assertEquals(0xFF, new EntityEnemyInspectionQueries(runtime.enemies()).spikedBeetleTransitionCountdown(0));
        runtime.tick(EntityTestFrames.frame(runtime, 0x41, 120, 64, sequence(0xFF)));
        assertEquals(2, new EntityEnemyInspectionQueries(runtime.enemies()).spikedBeetleState(0));
        assertEquals(0xFF, new EntityEnemyInspectionQueries(runtime.enemies()).spikedBeetleTransitionCountdown(0));
        assertEquals(0x00, new EntityEnemyInspectionQueries(runtime.enemies()).spikedBeetleSpeedX(0));
        assertEquals(0x00, new EntityEnemyInspectionQueries(runtime.enemies()).spikedBeetleSpeedY(0));
    }

    @Test
    void spikedBeetleRestingKeepsTheLastHandlerSelectedAnimation() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        EntitySpriteDefinition definition = catalog.forEntityType(0x2C, EntityRoomLoader.RoomTable.INDOORS_A, 0x00);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x2C)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 80, sequence(0x00)));
        runtime.tick(EntityTestFrames.frame(runtime, 0xE1, 120, 80, sequence(0x00, 0x00)));
        for (int frame = 0xE2; frame != 0x12; frame = (frame + 1) & 0xFF) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 80, sequence(0xFF)));
        }

        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).spikedBeetleState(0));
        assertEquals(1, runtime.inspection().snapshot().slots().get(0).spriteVariant());

        runtime.tick(EntityTestFrames.frame(runtime, 0x20, 120, 80, sequence(0xFF)));
        runtime.tick(EntityTestFrames.frame(runtime, 0x21, 120, 80, sequence(0xFF)));
        assertEquals(1, runtime.inspection().snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void spikedBeetleFlipsOnShieldContactAndDoesNotTakeDamage() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        EntitySpriteDefinition definition = catalog.forEntityType(0x2C, EntityRoomLoader.RoomTable.INDOORS_A, 0x00);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x2C)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x00)));
        assertEquals(0x48, runtime.execution().slotProperties().options1(0));

        List<EntityCombatEvent> events = runtime.combat()
                .resolveCombat(new EnemyCombatFrame(
                        0,
                        56,
                        64,
                        false,
                        0,
                        true,
                        false,
                        0,
                        0,
                        0,
                        0,
                        new EnemyAttackContext(1, false, false, false, false, true, 0, 0)));

        assertEquals(1, events.size());
        assertFalse(events.get(0).swordHit());
        assertEquals(0, events.get(0).linkDamage());
        assertEquals(0x0C, events.get(0).linkCollisionResponse().ignoreCollisionCountdown());
        assertEquals(0, events.get(0).enemyDamage());
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, events.get(0).soundChannel());
        assertEquals(0x09, events.get(0).soundId());
        assertEquals(3, new EntityEnemyInspectionQueries(runtime.enemies()).spikedBeetleState(0));
        assertEquals(0xFF, new EntityEnemyInspectionQueries(runtime.enemies()).spikedBeetleTransitionCountdown(0));
        assertEquals(0x10, new EntityEnemyInspectionQueries(runtime.enemies()).spikedBeetleSpeedX(0));
        assertEquals(0x00, new EntityEnemyInspectionQueries(runtime.enemies()).spikedBeetleSpeedY(0));
        assertEquals(0x02, runtime.enemies().enemyHealth(0));

        runtime.tick(EntityTestFrames.frame(runtime, 1, 120, 120, sequence(0x00)));
        assertEquals(0xFF, runtime.enemies().transitionCountdown(0));
        assertEquals(0x48, runtime.execution().slotProperties().options1(0));
        assertEquals(0x80, EntityRuntimeFixture.enemies(runtime).hitboxFlagsForTest(0));

        runtime.tick(EntityTestFrames.frame(runtime, 2, 120, 120, sequence(0x00)));
        assertEquals(0xFE, runtime.enemies().transitionCountdown(0));
        assertEquals(0x08, runtime.execution().slotProperties().options1(0));
        assertEquals(0x00, EntityRuntimeFixture.enemies(runtime).hitboxFlagsForTest(0));
    }

    @Test
    void spikedBeetleShieldChecksAllFourDirections() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        int[] linkX = {56, 72, 64, 64};
        int[] linkY = {64, 64, 72, 59};
        int[] flipX = {0x10, 0xF0, 0, 0};
        int[] flipY = {0, 0, 0xF0, 0x10};
        for (int direction = 0; direction < 4; direction++) {
            RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                            .sourceLoadOrder(0)
                            .type(0x2C)
                            .at(64, 64)
                            .spriteDefinition(catalog.forEntityType(0x2C, EntityRoomLoader.RoomTable.INDOORS_A, 0))
                            .build()))
                    .indoorRoom(true)
                    .randomByteSupplier(null)
                    .spriteHandlers(catalog)
                    .enemyCombatTables(tables)
                    .build();
            runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0)));
            var away = new EnemyAttackContext(1, false, false, false, false, true, direction ^ 1, 0);
            var misses = runtime.combat()
                    .resolveCombat(new EnemyCombatFrame(
                            1, linkX[direction], linkY[direction], false, 0, true, false, 0, 0, 0, 0, away));
            assertFalse(misses.stream().anyMatch(e -> e.soundId() == 0x09));
            assertNotEquals(3, new EntityEnemyInspectionQueries(runtime.enemies()).spikedBeetleState(0));
            var shield = new EnemyAttackContext(1, false, false, false, false, true, direction, 0);
            var hits = runtime.combat()
                    .resolveCombat(new EnemyCombatFrame(
                            1, linkX[direction], linkY[direction], false, 0, true, false, 0, 0, 0, 0, shield));
            assertTrue(hits.stream().anyMatch(e -> e.soundId() == 0x09));
            assertEquals(3, new EntityEnemyInspectionQueries(runtime.enemies()).spikedBeetleState(0));
            assertEquals(flipX[direction], new EntityEnemyInspectionQueries(runtime.enemies()).spikedBeetleSpeedX(0));
            assertEquals(flipY[direction], new EntityEnemyInspectionQueries(runtime.enemies()).spikedBeetleSpeedY(0));
        }
    }

    @Test
    void spikedBeetleNormalHandlerUsesTheRomSwordPokeOptions() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        EntitySpriteDefinition definition = catalog.forEntityType(0x2C, EntityRoomLoader.RoomTable.INDOORS_A, 0x00);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x2C)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 80, sequence(0x00)));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 120, 80, sequence(0x00, 0x00)));
        List<EntityCombatEvent> events = runtime.combat()
                .resolveCombat(new EntitySwordContactFrame(2, 120, 120, false, true, true, 72, 1, 72, 1));

        assertEquals(1, events.size());
        assertTrue(events.get(0).swordHit());
        assertEquals(0, events.get(0).enemyDamage());
        assertEquals(new EntityCombatEvent.SwordPokeVfx(64, 64), events.get(0).swordPokeVfx());
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, events.get(0).soundChannel());
        assertEquals(0x07, events.get(0).soundId());
        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).spikedBeetleState(0));
        assertEquals(0x02, runtime.enemies().enemyHealth(0));

        runtime.tick(EntityTestFrames.frame(runtime, 2, 120, 80, sequence(0xFF)));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).spikedBeetleSpeedX(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).spikedBeetleSpeedY(0));
    }

    @Test
    void spikedBeetleUsesTheRomHealthAndContactDamageValues() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        EntitySpriteDefinition definition = catalog.forEntityType(0x2C, EntityRoomLoader.RoomTable.INDOORS_A, 0x00);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x2C)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        List<EntityCombatEvent> events =
                runtime.combat().resolveCombat(new EntitySwordContactFrame(1, 72, 72, false, true, false, 0, 0, 0, 0));

        assertEquals(1, events.size());
        assertEquals(0x04, events.get(0).linkDamage());
    }
}
