package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.testing.TestRoms;
import linksawakening.world.entity.event.EntityCombatEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityRuntimeMimicTest extends EntityRuntimeTestSupport {

    @Test
    void mimicUsesItsRomInputHandlerAndCombatTables() throws Exception {
        byte[] rom = loadRom();
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        EntitySpriteDefinition definition =
                catalog.forEntityType(EntitySpriteHandlerCatalog.ENTITY_MIMIC, EntityRoomLoader.RoomTable.INDOORS_B);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(EntitySpriteHandlerCatalog.ENTITY_MIMIC)
                        .at(0x40, 0x50)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        runtime.configuration().setPressedButtonsMask(0x01);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));
        assertEquals(0x40, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(4, runtime.inspection().snapshot().slots().get(0).spriteVariant());

        runtime.tick(EntityTestFrames.frame(runtime, 1, 0, 0, () -> 0));
        assertEquals(0x3F, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(4, runtime.inspection().snapshot().slots().get(0).spriteVariant());
        assertEquals(0x04, runtime.enemies().enemyHealth(0));
        assertEquals(0x12, runtime.enemies().physicsFlags(0));
        assertEquals(0x08, runtime.options1(0));

        List<EntityCombatEvent> contact =
                runtime.resolveCombat(new EntitySwordContactFrame(1, 0x40, 0x50, false, true, false, 0, 0, 0, 0));
        assertEquals(1, contact.size());
        assertEquals(0x18, contact.get(0).linkDamage());
    }

    @Test
    void maskedMimicUsesItsRomInputHandlerAndCombatTables() throws Exception {
        byte[] rom = loadRom();
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        EntitySpriteDefinition definition = catalog.forEntityType(
                EntitySpriteHandlerCatalog.ENTITY_MASKED_MIMIC_GORIYA, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(EntitySpriteHandlerCatalog.ENTITY_MASKED_MIMIC_GORIYA)
                        .at(0x40, 0x50)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        runtime.configuration().setPressedButtonsMask(0x01);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x50, 0x50, () -> 0, null, null, 0, 3, 0));
        assertEquals(0x3F, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(0x02, runtime.inspection().snapshot().slots().get(0).spriteVariant());
        assertEquals(0x02, runtime.enemies().enemyHealth(0));
        assertEquals(0x12, runtime.enemies().physicsFlags(0));
        assertEquals(0x48, runtime.options1(0));

        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x50, 0x50, () -> 0, null, null, 0, 3, 0));
        assertEquals(0x3E, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(0x08, runtime.options1(0));

        List<EntityCombatEvent> contact =
                runtime.resolveCombat(new EntitySwordContactFrame(1, 0x3E, 0x50, false, true, false, 0, 0, 0, 0));
        assertEquals(1, contact.size());
        assertEquals(0x04, contact.get(0).linkDamage());
    }

    @Test
    void maskedMimicShieldedSideUsesDynamicSwordClinkOption() throws Exception {
        byte[] rom = loadRom();
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        EntitySpriteDefinition definition = catalog.forEntityType(
                EntitySpriteHandlerCatalog.ENTITY_MASKED_MIMIC_GORIYA, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(EntitySpriteHandlerCatalog.ENTITY_MASKED_MIMIC_GORIYA)
                        .at(0x40, 0x50)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        runtime.configuration().setPressedButtonsMask(0x01);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x50, 0x50, () -> 0, null, null, 0, 3, 0));
        assertEquals(0x48, runtime.options1(0));

        List<EntityCombatEvent> swordHit = runtime.resolveCombat(
                new EntitySwordContactFrame(1, 0x50, 0x50, false, true, true, 0x40, 0x10, 0x50, 0x10));

        assertEquals(1, swordHit.size());
        assertEquals(0, swordHit.get(0).enemyDamage());
        assertEquals(0x02, runtime.enemies().enemyHealth(0));
        assertNotNull(swordHit.get(0).swordPokeVfx());
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, swordHit.get(0).soundChannel());
        assertEquals(0x07, swordHit.get(0).soundId());
    }

    @Test
    void maskedMimicVulnerableSideUsesDynamicDamageOption() throws Exception {
        byte[] rom = loadRom();
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        EntitySpriteDefinition definition = catalog.forEntityType(
                EntitySpriteHandlerCatalog.ENTITY_MASKED_MIMIC_GORIYA, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(EntitySpriteHandlerCatalog.ENTITY_MASKED_MIMIC_GORIYA)
                        .at(0x40, 0x50)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        runtime.configuration().setPressedButtonsMask(0x08);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x40, 0x70, () -> 0, null, null, 0, 0, 0));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x40, 0x70, () -> 0, null, null, 0, 0, 0));
        assertEquals(0x08, runtime.options1(0));

        RoomEntity maskedMimic = runtime.inspection().snapshot().slots().get(0);
        List<EntityCombatEvent> swordHit = runtime.resolveCombat(new EntitySwordContactFrame(
                2, 0x40, 0x70, false, true, true, maskedMimic.x() + 0x08, 0x08, maskedMimic.y() + 0x08, 0x08));

        assertEquals(1, swordHit.size());
        assertEquals(1, swordHit.get(0).enemyDamage());
        assertEquals(0x01, runtime.enemies().enemyHealth(0));
        assertNull(swordHit.get(0).swordPokeVfx());
    }
}
