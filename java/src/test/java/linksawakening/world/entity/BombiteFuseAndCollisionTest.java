package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.loadRom;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshotWithSlots;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.testing.TestRoms;
import linksawakening.world.entity.event.EntityCombatEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class BombiteFuseAndCollisionTest {

    @Test
    void timerBombiteUsesItsRomFuseToProduceAnEnemyBomb() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        EntitySpriteDefinition definition = catalog.forEntityType(0x56, EntityRoomLoader.RoomTable.INDOORS_A, -1);
        assertTrue(definition.supported());
        assertEquals(0x04, definition.bank());
        assertEquals(0x7CEF, definition.address());
        assertEquals(6, definition.variantCount());

        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshotWithSlots(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x56)
                        .at(0x50, 0x50)
                        .spriteDefinition(definition)
                        .z(0x00)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();
        EntityRuntimeFixture.debug(runtime).setEnemyIgnoreHitsCountdownForTest(0, 0x09);

        boolean explosionSound = false;
        boolean sourceCleared = false;
        for (int frame = 0; frame < 520; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0xC0, 0xC0, () -> 0));
            explosionSound |= runtime.outbox().consumePendingEntityEvents().stream()
                    .anyMatch(event ->
                            event.soundChannel() == EntityCombatEvent.SoundChannel.NOISE && event.soundId() == 0x0C);
            sourceCleared = !runtime.inspection().snapshot().slots().get(0).loaded();
            if (sourceCleared) {
                break;
            }
        }

        assertTrue(explosionSound);
        assertTrue(sourceCleared);
        RoomEntity bomb = runtime.inspection().snapshot().slots().get(15);
        assertTrue(bomb.loaded());
        assertEquals(EntityStatus.ACTIVE, bomb.status());
        assertEquals(0x02, bomb.type());
        assertEquals(0x00, bomb.z());
        assertEquals(0x17, runtime.enemies().transitionCountdown(15));
        assertEquals(0x01, runtime.enemies().enemyIgnoreHitsCountdown(15));
    }

    @Test
    void bouncingBombiteUsesItsRomSwordRecoilInsteadOfTakingSwordDamage() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        EntitySpriteDefinition definition = catalog.forEntityType(0x55, EntityRoomLoader.RoomTable.INDOORS_A, -1);
        assertTrue(definition.supported());
        assertEquals(0x04, definition.bank());
        assertEquals(0x7DF5, definition.address());
        assertEquals(2, definition.variantCount());

        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshotWithSlots(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x55)
                        .at(0x50, 0x50)
                        .spriteDefinition(definition)
                        .z(0x00)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        List<EntityCombatEvent> events = runtime.combat()
                .resolveCombat(new EntityVerticalContactFrame(
                        0, 0xC0, 0xC0, false, true, true, 0x58, 1, 0x58, 1, EnemyAttackContext.standard()));

        assertEquals(
                List.of(new EntityCombatEvent(0, 0x55, 0, true, 0, -1, EntityCombatEvent.SoundChannel.NONE, -1)),
                events);
        assertEquals(4, runtime.enemies().enemyHealth(0));
        assertEquals(2, runtime.pickupState().bombiteState(0));
        assertEquals(0x40, runtime.enemies().transitionCountdown(0));
        assertEquals(0x08, runtime.pickupState().bombitePrivateCountdown1(0));
        assertEquals(0, runtime.enemies().enemyIgnoreHitsCountdown(0));
    }

    @Test
    void litBouncingBombiteCollidesWithAnotherBombiteUsingTheRomEntityPass() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        EntitySpriteDefinition definition = catalog.forEntityType(0x55, EntityRoomLoader.RoomTable.INDOORS_A, -1);
        assertTrue(definition.supported());

        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshotWithSlots(
                        new RoomEntityBuilder()
                                .sourceLoadOrder(0)
                                .type(0x55)
                                .at(0x50, 0x50)
                                .spriteDefinition(definition)
                                .z(0x00)
                                .build(),
                        new RoomEntityBuilder()
                                .slot(1)
                                .sourceLoadOrder(1)
                                .type(0x55)
                                .at(0x58, 0x58)
                                .spriteDefinition(definition)
                                .z(0x00)
                                .build()))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        List<EntityCombatEvent> swordEvents = runtime.combat()
                .resolveCombat(new EntityVerticalContactFrame(
                        0, 0xC0, 0xC0, false, true, true, 0x58, 1, 0x58, 1, EnemyAttackContext.standard()));
        assertEquals(
                List.of(new EntityCombatEvent(0, 0x55, 0, true, 0, -1, EntityCombatEvent.SoundChannel.NONE, -1)),
                swordEvents);
        int sourceSpeedX = runtime.pickupState().bombiteSpeedX(0);
        int sourceSpeedY = runtime.pickupState().bombiteSpeedY(0);

        // Frame 1 selects target slot 1 in func_003_75A2's alternating pass.
        // The source moves three pixels toward the upper-left first, remaining
        // inside the ROM's strict twelve-pixel entity-collision windows.
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0xC0, 0xC0, () -> 0));

        assertFalse(runtime.inspection().snapshot().slots().get(0).loaded());
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(1).status());
        assertEquals(2, runtime.pickupState().bombiteState(1));
        assertEquals(0x40, runtime.enemies().transitionCountdown(1));
        assertEquals(0x08, runtime.pickupState().bombitePrivateCountdown1(1));
        assertEquals(sourceSpeedX, runtime.pickupState().bombiteSpeedX(1));
        assertEquals(sourceSpeedY, runtime.pickupState().bombiteSpeedY(1));

        RoomEntity bomb = runtime.inspection().snapshot().slots().get(15);
        assertTrue(bomb.loaded());
        assertEquals(EntityStatus.ACTIVE, bomb.status());
        assertEquals(0x02, bomb.type());
        assertEquals(0x00, bomb.z());
        assertEquals(0x17, runtime.enemies().transitionCountdown(15));
        assertEquals(0x01, runtime.enemies().enemyIgnoreHitsCountdown(15));
        assertEquals(
                List.of(new EntityCombatEvent(15, 0x02, 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x0C)),
                runtime.outbox().consumePendingEntityEvents());
    }
}
