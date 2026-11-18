package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.ghiniEntity;
import static linksawakening.world.entity.EntityRuntimeTestSupport.loadRom;
import static linksawakening.world.entity.EntityRuntimeTestSupport.romWithSwordResult;
import static linksawakening.world.entity.EntityRuntimeTestSupport.sequence;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.testing.TestRoms;
import linksawakening.world.entity.event.EntityCombatEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class HiddenGiantRevealTest {

    @Test
    void collisionTypeIsExplicitPerTickAndRevealKeepsHiddenPresentationForThatTick() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));

        RoomEntityRuntime compatibilityRuntime = new RoomEntityRuntimeBuilder(
                        snapshot(ghiniEntity(catalog, 0, 0x10, EntityStatus.ACTIVE)))
                .indoorRoom(false)
                .randomByteSupplier(sequence(0x00))
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();
        compatibilityRuntime.tick(EntityTestFrames.frame(compatibilityRuntime, 0, 0x50, 0x50, sequence(0x00)));
        assertEquals(
                -1, compatibilityRuntime.inspection().snapshot().slots().get(0).spriteVariant());

        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(
                        snapshot(ghiniEntity(catalog, 0, 0x10, EntityStatus.ACTIVE)))
                .indoorRoom(false)
                .randomByteSupplier(sequence(0x00))
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();
        RoomEntity before = runtime.inspection().snapshot().slots().get(0);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x50, 0x50, 0x08, sequence(0x00)));

        RoomEntity revealTick = runtime.inspection().snapshot().slots().get(0);
        assertEquals(-1, revealTick.spriteVariant());
        assertEquals(before.x(), revealTick.x());
        assertEquals(before.y(), revealTick.y());
        assertTrue(runtime.combat()
                .resolveCombat(new EntitySwordContactFrame(1, 0x50, 0x50, false, true, true, 0x58, 1, 0x58, 1))
                .isEmpty());

        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x50, 0x50, 0, sequence(0x00)));
        RoomEntity visible = runtime.inspection().snapshot().slots().get(0);
        assertTrue(visible.spriteVariant() >= 0);
        List<EntityCombatEvent> contact = runtime.combat()
                .resolveCombat(new EntitySwordContactFrame(1, 0x50, 0x50, false, true, false, 0, 0, 0, 0));
        assertEquals(1, contact.size());
        assertEquals(0x08, contact.get(0).linkDamage());
    }

    @Test
    void visibleGiantUsesTheExistingLethalCombatStatusPath() throws IOException {
        byte[] rom = romWithSwordResult(0x11, 0x08);
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(
                        snapshot(ghiniEntity(catalog, 0, 0x11, EntityStatus.ACTIVE)))
                .indoorRoom(false)
                .randomByteSupplier(sequence(0x00))
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x50, 0x50, 0x01, sequence(0x00)));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x90, 0x90, 0, sequence(0x00)));
        List<EntityCombatEvent> events = runtime.combat()
                .resolveCombat(new EntityVerticalContactFrame(
                        0, 0x90, 0x90, false, false, true, 0x58, 1, 0x58, 1, EnemyAttackContext.standard()));

        assertEquals(1, events.size());
        assertTrue(events.get(0).swordHit());
        assertEquals(0, runtime.enemies().enemyHealth(0));
        assertEquals(
                EntityStatus.DYING,
                runtime.inspection().snapshot().slots().get(0).status());
    }
}
