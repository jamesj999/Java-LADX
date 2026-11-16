package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.loadRom;
import static linksawakening.world.entity.EntityRuntimeTestSupport.pairDefinition;
import static linksawakening.world.entity.EntityRuntimeTestSupport.romWithSwordResult;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityDeathAndSlotResetTest {

    @Test
    void clearingAnEntityResetsTheRomHitboxIgnoreFlag() {
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot()).build();
        EntityRuntimeFixture.debug(runtime).setHitboxFlagsForTest(0, 0x80);

        runtime.lifecycle().clearEntity(0);

        assertEquals(0, EntityRuntimeFixture.enemies(runtime).hitboxFlagsForTest(0));
    }

    @Test
    void clearingBurningOrStunnedEntitiesResetsTheirStatusCountdowns() throws IOException {
        RomEnemyCombatTables burningTables = new RomEnemyCombatTables(
                linksawakening.world.entity.EntityRomTestData.view(romWithSwordResult(0x09, 0xFE)));
        RoomEntityRuntime burningRuntime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x09)
                        .at(64, 64)
                        .spriteDefinition(pairDefinition(0x09, 2))
                        .build()))
                .indoorRoom(false)
                .randomByteSupplier(null)
                .spriteHandlers(null)
                .enemyCombatTables(burningTables)
                .build();
        burningRuntime
                .combat()
                .resolveCombat(new EntitySwordContactFrame(0, 120, 120, false, true, true, 72, 1, 72, 1));
        burningRuntime.lifecycle().clearEntity(0);

        assertEquals(
                EntityStatus.DISABLED,
                burningRuntime.inspection().snapshot().slots().get(0).status());
        assertEquals(0, burningRuntime.enemies().transitionCountdown(0));
        assertEquals(0, new EntityEnemyInspectionQueries(burningRuntime.enemies()).stunnedCountdown(0));

        RomEnemyCombatTables stunnedTables =
                new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(loadRom()));
        RoomEntityRuntime stunnedRuntime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0xE9)
                        .at(64, 64)
                        .spriteDefinition(pairDefinition(0xE9, 2))
                        .build()))
                .indoorRoom(false)
                .randomByteSupplier(null)
                .spriteHandlers(null)
                .enemyCombatTables(stunnedTables)
                .build();
        stunnedRuntime
                .combat()
                .resolveCombat(new EntitySwordContactFrame(0, 120, 120, false, true, true, 72, 1, 72, 1));
        stunnedRuntime.lifecycle().clearEntity(0);

        assertEquals(0, stunnedRuntime.enemies().transitionCountdown(0));
        assertEquals(0, new EntityEnemyInspectionQueries(stunnedRuntime.enemies()).stunnedCountdown(0));
    }

    @Test
    void onlyTheFirstEightLoadOrdersContributeToThePersistentClearMask() {
        EntitySpriteDefinition definition = pairDefinition(0x33, 2);
        RoomEntitySnapshot initial = snapshot(
                new RoomEntityBuilder()
                        .sourceLoadOrder(3)
                        .type(0x33)
                        .at(24, 32)
                        .spriteDefinition(definition)
                        .build(),
                new RoomEntityBuilder()
                        .slot(1)
                        .sourceLoadOrder(8)
                        .type(0x33)
                        .at(40, 32)
                        .spriteDefinition(definition)
                        .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();

        assertEquals(1 << 3, runtime.lifecycle().clearEntity(0));
        assertEquals(0, runtime.lifecycle().clearEntity(1));
        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(1).status());
    }
}
