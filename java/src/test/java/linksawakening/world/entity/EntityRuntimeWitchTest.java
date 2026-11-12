package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.testing.TestRoms;
import linksawakening.world.entity.event.EntityCombatEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityRuntimeWitchTest extends EntityRuntimeTestSupport {

    @Test
    void witchOverridesTheNpcHitboxWithHerSourceCauldronFootprint() {
        RoomEntity witch = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x40)
                .at(0x40, 0x40)
                .spriteDefinition(EntitySpriteDefinition.unsupported(0x40))
                .build();

        assertTrue(RoomEntityCombatRules.overlapsLink(witch, 0x40, 0x55));
    }

    @Test
    void crystalSwitchHitFlashesThenRequestsTheRomSwitchAnimation() throws Exception {
        byte[] rom = loadRom();
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        EntitySpriteDefinition definition = catalog.forEntityType(0x66, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x66)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        List<EntityCombatEvent> hit =
                runtime.resolveCombat(new EntitySwordContactFrame(0, 120, 120, false, true, true, 72, 1, 72, 1));

        assertEquals(1, hit.size());
        assertTrue(hit.get(0).swordHit());
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, hit.get(0).soundChannel());
        assertEquals(0x03, hit.get(0).soundId());
        assertEquals(0x18, new EntityEnemyInspectionQueries(runtime.enemies()).enemyFlashCountdown(0));
        assertEquals(0x0A, runtime.enemies().enemyIgnoreHitsCountdown(0));
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x00)));

        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).enemyFlashCountdown(0));
        assertEquals(0x18, runtime.enemies().transitionCountdown(0));
        assertTrue(runtime.consumePendingSwitchBlockAnimationRequest());
        assertEquals(
                List.of(new EntityCombatEvent(0, 0x66, 0, false, EntityCombatEvent.SoundChannel.WAVE, 0x0E)),
                runtime.consumePendingEntityEvents());
        assertFalse(runtime.consumePendingSwitchBlockAnimationRequest());
    }

    @Test
    void crystalSwitchDoesNotRequestAnotherAnimationWhileSwitchBlocksAreActive() throws Exception {
        byte[] rom = loadRom();
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x66)
                        .at(64, 64)
                        .spriteDefinition(catalog.forEntityType(0x66, EntityRoomLoader.RoomTable.INDOORS_A))
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        EntityRuntimeFixture.debug(runtime).setSwitchBlockAnimationActiveForTest(true);
        EntityRuntimeFixture.debug(runtime).setEnemyFlashCountdownForTest(0, 0x02);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x00)));

        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).enemyFlashCountdown(0));
        assertFalse(runtime.consumePendingSwitchBlockAnimationRequest());
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());
    }
}
