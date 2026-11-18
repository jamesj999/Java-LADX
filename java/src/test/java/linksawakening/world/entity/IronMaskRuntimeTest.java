package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.loadRom;
import static linksawakening.world.entity.EntityRuntimeTestSupport.sequence;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.function.IntSupplier;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.testing.TestRoms;
import linksawakening.world.entity.event.EntityCombatEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class IronMaskRuntimeTest {

    @Test
    void maskedIronMaskRejectsARearSwordHitWithTheRomPokeResponse() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        EntitySpriteDefinition definition = catalog.forEntityType(0x24, EntityRoomLoader.RoomTable.INDOORS_A, -1);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x24)
                        .at(0x50, 0x50)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        // Java direction 1 is ROM UP, while the freshly initialized Iron Mask
        // is facing ROM RIGHT (direction 0).
        runtime.tick(EntityTestFrames.frame(
                runtime,
                0,
                0xC0,
                0xC0,
                () -> 0,
                linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN,
                null,
                0,
                1,
                0));
        List<EntityCombatEvent> events = runtime.combat()
                .resolveCombat(new EntitySwordContactFrame(0, 0xC0, 0xC0, false, true, true, 0x58, 1, 0x58, 1));

        assertEquals(1, events.size());
        EntityCombatEvent event = events.get(0);
        assertTrue(event.swordHit());
        assertEquals(0, event.enemyDamage());
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, event.soundChannel());
        assertEquals(0x07, event.soundId());
        assertEquals(new EntityCombatEvent.SwordPokeVfx(0x50, 0x50), event.swordPokeVfx());
        assertEquals(2, runtime.enemies().enemyHealth(0));
        assertEquals(0x10, runtime.enemies().enemyIgnoreHitsCountdown(0));
        assertEquals(0xF0, EntityRuntimeFixture.enemies(runtime).enemyRecoilSpeedXForTest(0));
        assertEquals(0xF0, EntityRuntimeFixture.enemies(runtime).enemyRecoilSpeedYForTest(0));
    }

    @Test
    void ironMaskUsesItsRomRoamingHandlerSpeedAndMaskedDisplay() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        EntitySpriteDefinition definition = catalog.forEntityType(0x24, EntityRoomLoader.RoomTable.INDOORS_A, -1);

        assertTrue(definition.supported());
        assertEquals(0x03, definition.bank());
        assertEquals(0x4FCB, definition.address());
        assertEquals(8, definition.variantCount());

        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x24)
                        .at(0x50, 0x50)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();
        IntSupplier randomBytes = sequence(0x00, 0x00, 0x03);

        assertEquals(2, runtime.enemies().enemyHealth(0));
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0xC0, 0xC0, randomBytes));

        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).ironMaskState(0));
        assertEquals(0x10, new EntityEnemyInspectionQueries(runtime.enemies()).ironMaskTransitionCountdown(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).ironMaskSpeedY(0));
        assertEquals(
                0x4FCB,
                runtime.inspection()
                        .snapshot()
                        .slots()
                        .get(0)
                        .spriteDefinition()
                        .address());

        for (int frame = 1; frame <= 0x10; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0xC0, 0xC0, randomBytes));
        }

        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).ironMaskState(0));
        assertEquals(3, new EntityEnemyInspectionQueries(runtime.enemies()).ironMaskDirection(0));
        assertEquals(0x0C, new EntityEnemyInspectionQueries(runtime.enemies()).ironMaskSpeedY(0));
        assertEquals(0x20, new EntityEnemyInspectionQueries(runtime.enemies()).ironMaskTransitionCountdown(0));
        assertEquals(
                0x4FCB,
                runtime.inspection()
                        .snapshot()
                        .slots()
                        .get(0)
                        .spriteDefinition()
                        .address());

        runtime.tick(EntityTestFrames.frame(runtime, 0x11, 0xC0, 0xC0, randomBytes));
        runtime.tick(EntityTestFrames.frame(runtime, 0x12, 0xC0, 0xC0, randomBytes));
        assertEquals(0x51, runtime.inspection().snapshot().slots().get(0).y());
    }
}
