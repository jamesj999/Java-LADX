package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.loadRom;
import static linksawakening.world.entity.EntityRuntimeTestSupport.pairDefinition;
import static linksawakening.world.entity.EntityRuntimeTestSupport.romWithSwordResult;
import static linksawakening.world.entity.EntityRuntimeTestSupport.sequence;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import linksawakening.world.entity.event.EntityCombatEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class RomCombatDamageTest {

    @Test
    void romBurningStatusUsesTheSharedSpecialDamageValueAndExpiresIntoDeath() throws IOException {
        byte[] rom = romWithSwordResult(0x09, 0xFE);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x09)
                        .at(64, 64)
                        .spriteDefinition(pairDefinition(0x09, 2))
                        .build()))
                .indoorRoom(false)
                .randomByteSupplier(null)
                .spriteHandlers(null)
                .enemyCombatTables(tables)
                .build();

        List<EntityCombatEvent> events = runtime.combat()
                .resolveCombat(new EntitySwordContactFrame(0, 120, 120, false, true, true, 72, 1, 72, 1));

        assertEquals(1, events.size());
        assertEquals(0xFE, events.get(0).enemySpecialAction());
        assertEquals(0, events.get(0).enemyDamage());
        assertEquals(
                EntityStatus.BURNING,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(1, runtime.enemies().enemyHealth(0));
        assertEquals(0x60, runtime.enemies().transitionCountdown(0));
        assertEquals(0x0A, runtime.enemies().enemyIgnoreHitsCountdown(0));
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, events.get(0).soundChannel());
        assertEquals(0x03, events.get(0).soundId());
        assertEquals(EntityCombatEvent.SoundChannel.NOISE, events.get(0).secondarySoundChannel());
        assertEquals(0x12, events.get(0).secondarySoundId());
        assertTrue(runtime.combat()
                .resolveCombat(new EntitySwordContactFrame(1, 120, 120, false, true, true, 72, 1, 72, 1))
                .isEmpty());

        for (int frame = 0; frame < 0x60; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, sequence(0x00)));
        }
        assertEquals(
                EntityStatus.DYING,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(0x1F, new EntityEnemyInspectionQueries(runtime.enemies()).dyingCountdown(0));
        assertEquals(3, runtime.inspection().snapshot().slots().get(0).deathSpriteVariant());
        assertFalse(runtime.inspection().snapshot().slots().get(0).powerRecoilDeath());
        assertEquals(0x04, runtime.enemies().physicsFlags(0));
        List<EntityCombatEvent> expiryEvents = runtime.outbox().consumePendingEntityEvents();
        assertEquals(1, expiryEvents.size());
        assertEquals(
                EntityCombatEvent.SoundChannel.NOISE, expiryEvents.getFirst().soundChannel());
        assertEquals(0x13, expiryEvents.getFirst().soundId());
    }

    @Test
    void unknownRomSpecialDamageRemainsEventDataWithoutChangingHealthOrStatus() throws IOException {
        RomEnemyCombatTables tables = new RomEnemyCombatTables(
                linksawakening.world.entity.EntityRomTestData.view(romWithSwordResult(0x09, 0xFD)));
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x09)
                        .at(64, 64)
                        .spriteDefinition(pairDefinition(0x09, 2))
                        .build()))
                .indoorRoom(false)
                .randomByteSupplier(null)
                .spriteHandlers(null)
                .enemyCombatTables(tables)
                .build();

        List<EntityCombatEvent> events = runtime.combat()
                .resolveCombat(new EntitySwordContactFrame(0, 120, 120, false, true, true, 72, 1, 72, 1));

        assertEquals(1, events.size());
        assertEquals(0xFD, events.get(0).enemySpecialAction());
        assertEquals(0, events.get(0).enemyDamage());
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(1, runtime.enemies().enemyHealth(0));
        assertEquals(EntityCombatEvent.SoundChannel.NONE, events.get(0).secondarySoundChannel());
        assertEquals(-1, events.get(0).secondarySoundId());
    }

    @Test
    void romCombatTablesDriveInitialHealthAndContactDamage() throws IOException {
        RomEnemyCombatTables tables =
                new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(loadRom()));
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(
                        new RoomEntityBuilder()
                                .sourceLoadOrder(0)
                                .type(0x09)
                                .at(64, 64)
                                .spriteDefinition(pairDefinition(0x09, 2))
                                .build(),
                        new RoomEntityBuilder()
                                .slot(1)
                                .sourceLoadOrder(1)
                                .type(0x0B)
                                .at(96, 64)
                                .spriteDefinition(pairDefinition(0x0B, 2))
                                .build()))
                .indoorRoom(false)
                .randomByteSupplier(null)
                .spriteHandlers(null)
                .enemyCombatTables(tables)
                .build();

        assertEquals(1, runtime.enemies().enemyHealth(0));
        assertEquals(2, runtime.enemies().enemyHealth(1));

        List<EntityCombatEvent> contact =
                runtime.combat().resolveCombat(new EntitySwordContactFrame(1, 64, 64, false, true, false, 0, 0, 0, 0));
        assertEquals(1, contact.size());
        assertEquals(4, contact.get(0).linkDamage());
    }

    @Test
    void attackContextUsesRomSwordUpgradeDamageInTheRuntime() throws IOException {
        RomEnemyCombatTables tables =
                new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(loadRom()));
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x0B)
                        .at(64, 64)
                        .spriteDefinition(pairDefinition(0x0B, 2))
                        .build()))
                .indoorRoom(false)
                .randomByteSupplier(null)
                .spriteHandlers(null)
                .enemyCombatTables(tables)
                .build();

        List<EntityCombatEvent> events = runtime.combat()
                .resolveCombat(new EntityVerticalContactFrame(
                        0,
                        120,
                        120,
                        false,
                        true,
                        true,
                        72,
                        1,
                        72,
                        1,
                        new EnemyAttackContext(2, false, false, false, false)));

        assertEquals(1, events.size());
        assertEquals(0, runtime.enemies().enemyHealth(0));
        assertEquals(
                EntityStatus.DYING,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(2, events.get(0).enemyDamage());
        assertEquals(-1, events.get(0).enemySpecialAction());
    }

    @Test
    void zeroRomSwordResultKeepsHealthAndReportsOnlyTheBump() throws IOException {
        RomEnemyCombatTables tables =
                new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(loadRom()));
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x15)
                        .at(64, 64)
                        .spriteDefinition(pairDefinition(0x15, 2))
                        .build()))
                .indoorRoom(false)
                .randomByteSupplier(null)
                .spriteHandlers(null)
                .enemyCombatTables(tables)
                .build();

        List<EntityCombatEvent> events = runtime.combat()
                .resolveCombat(new EntitySwordContactFrame(0, 120, 120, false, true, true, 72, 1, 72, 1));

        assertEquals(1, events.size());
        assertEquals(4, runtime.enemies().enemyHealth(0));
        assertEquals(0, events.get(0).enemyDamage());
        assertEquals(-1, events.get(0).enemySpecialAction());
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, events.get(0).soundChannel());
        assertEquals(0x09, events.get(0).soundId());
    }
}
