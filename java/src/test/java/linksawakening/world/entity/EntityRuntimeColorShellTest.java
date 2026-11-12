package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.world.entity.event.EntityCombatEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityRuntimeColorShellTest extends EntityRuntimeTestSupport {

    @Test
    void colorShellDyingRefreshPreservesDeathPresentationMetadata() throws IOException {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());
        EntitySpriteDefinition body = pairDefinition(0xE9, 2);
        RoomEntity initialEntity = new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0xE9)
                .at(64, 64)
                .spriteDefinition(body)
                .spriteVariant(1)
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(initialEntity))
                .indoorRoom(false)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .build();

        runtime.resolveCombat(
                0, 120, 120, false, true, true, 72, 1, 72, 1, new EnemyAttackContext(1, false, true, false, false));
        assertEquals(
                EntityStatus.DYING,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(-1, runtime.inspection().snapshot().slots().get(0).deathSpriteVariant());
        assertTrue(runtime.inspection().snapshot().slots().get(0).powerRecoilDeath());

        for (int frame = 1; frame <= 0x30; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, () -> 0));
        }

        RoomEntity rebuilt = runtime.inspection().snapshot().slots().get(0);
        assertEquals(EntityStatus.DYING, rebuilt.status());
        assertEquals(0x0F, runtime.enemies().dyingCountdown(0));
        assertEquals(1, rebuilt.deathSpriteVariant());
        assertTrue(rebuilt.powerRecoilDeath());
        assertEquals(
                EntitySpriteDefinition.Shape.RECTANGLE,
                rebuilt.spriteDefinition().shape());
    }

    @Test
    void colorShellsAreAdmittedAsRomCombatTargets() throws IOException {
        RomEnemyCombatTables tables = new RomEnemyCombatTables(loadRom());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(
                        new linksawakening.world.entity.RoomEntityBuilder()
                                .sourceLoadOrder(0)
                                .type(0xE9)
                                .at(64, 64)
                                .spriteDefinition(pairDefinition(0xE9, 2))
                                .build(),
                        new linksawakening.world.entity.RoomEntityBuilder()
                                .slot(1)
                                .sourceLoadOrder(1)
                                .type(0xEA)
                                .at(96, 64)
                                .spriteDefinition(pairDefinition(0xEA, 2))
                                .build(),
                        new linksawakening.world.entity.RoomEntityBuilder()
                                .slot(2)
                                .sourceLoadOrder(2)
                                .type(0xEB)
                                .at(128, 64)
                                .spriteDefinition(pairDefinition(0xEB, 2))
                                .build()))
                .indoorRoom(false)
                .randomByteSupplier(null)
                .spriteHandlers(null)
                .enemyCombatTables(tables)
                .build();

        assertTrue(RoomEntityCombatRules.supportsEnemyCollision(0xE9));
        assertTrue(RoomEntityCombatRules.supportsEnemyCollision(0xEA));
        assertTrue(RoomEntityCombatRules.supportsEnemyCollision(0xEB));
        assertEquals(tables.initialHealth(0xE9), runtime.enemies().enemyHealth(0));
        assertEquals(tables.initialHealth(0xEA), runtime.enemies().enemyHealth(1));
        assertEquals(tables.initialHealth(0xEB), runtime.enemies().enemyHealth(2));
    }

    @Test
    void colorShellRuntimeDispatchesRomStateAndWorldSideEffects() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RuntimeColorShellWorld world = new RuntimeColorShellWorld();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(
                        snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                                .sourceLoadOrder(0)
                                .type(0xE9)
                                .at(0x40, 0x40)
                                .spriteDefinition(EntitySpriteDefinition.unsupported(0xE9))
                                .spriteVariant(-1)
                                .build()))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0x00)
                .spriteHandlers(catalog)
                .build();
        runtime.configuration().setColorShellWorld(world);
        EntityRuntimeFixture.debug(runtime).setColorShellStateForTest(0, 0x08, 1, 0, 0, 0);
        world.objectValue = 0x5E;

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x40, 0x40, () -> 0x00, null));

        RoomEntity shell = runtime.inspection().snapshot().slots().get(0);
        assertEquals(0x0C, runtime.pickupState().colorShellState(0));
        assertEquals(0xF0, runtime.pickupState().colorShellPhysicsFlags(0));
        assertEquals(0x67A8, shell.spriteDefinition().address());
        assertEquals(List.of(0x67), world.objectWrites);
        assertEquals(List.of(0x04), world.noises);
    }

    @Test
    void colorShellCompletionUnloadsAndPublishesItsRoomPersistenceMask() {
        RuntimeColorShellWorld world = new RuntimeColorShellWorld();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(
                        snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                                .sourceLoadOrder(0)
                                .type(0xE9)
                                .at(0x40, 0x40)
                                .spriteDefinition(EntitySpriteDefinition.unsupported(0xE9))
                                .spriteVariant(-1)
                                .build()))
                .build();
        runtime.configuration().setColorShellWorld(world);
        EntityRuntimeFixture.debug(runtime).setColorShellStateForTest(0, 0x0D, 1, 0, 0, 0);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x40, 0x40, () -> 0x00, null));

        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(1, runtime.consumePendingClearedEntityMask());
        assertEquals(List.of(0x5F), world.objectWrites);
        assertEquals(List.of(List.of(0x40, 0x40)), world.poofs);
    }

    @Test
    void romStunStatusUsesColorShellsRawSpecialDamageValueAndRecovers() throws IOException {
        RomEnemyCombatTables tables = new RomEnemyCombatTables(loadRom());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(
                        snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                                .sourceLoadOrder(0)
                                .type(0xE9)
                                .at(64, 64)
                                .spriteDefinition(pairDefinition(0xE9, 2))
                                .build()))
                .indoorRoom(false)
                .randomByteSupplier(null)
                .spriteHandlers(null)
                .enemyCombatTables(tables)
                .build();

        List<EntityCombatEvent> events = runtime.resolveCombat(0, 120, 120, false, true, true, 72, 1, 72, 1);

        assertEquals(1, events.size());
        assertEquals(0xFF, events.get(0).enemySpecialAction());
        assertEquals(0, events.get(0).enemyDamage());
        assertEquals(
                EntityStatus.STUNNED,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(tables.initialHealth(0xE9), runtime.enemies().enemyHealth(0));
        assertEquals(0xFF, runtime.enemies().stunnedCountdown(0));
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, events.get(0).soundChannel());
        assertEquals(0x03, events.get(0).soundId());
        assertEquals(EntityCombatEvent.SoundChannel.NONE, events.get(0).secondarySoundChannel());
        assertEquals(-1, events.get(0).secondarySoundId());
        assertTrue(runtime.resolveCombat(1, 120, 120, false, true, true, 72, 1, 72, 1)
                .isEmpty());

        for (int frame = 0; frame < 0xFF; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, sequence(0x00)));
        }
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(0, runtime.enemies().stunnedCountdown(0));
    }
}
