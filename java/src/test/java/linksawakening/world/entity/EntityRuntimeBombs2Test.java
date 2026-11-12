package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.world.entity.event.BombExplosionEvent;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.EntityProjectileEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityRuntimeBombs2Test extends EntityRuntimeTestSupport {

    @Test
    void bouncingBombiteUsesItsRomSwordRecoilInsteadOfTakingSwordDamage() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(0x55, EntityRoomLoader.RoomTable.INDOORS_A, -1);
        assertTrue(definition.supported());
        assertEquals(0x04, definition.bank());
        assertEquals(0x7DF5, definition.address());
        assertEquals(2, definition.variantCount());

        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshotWithSlots(new RoomEntityBuilder()
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

        List<EntityCombatEvent> events = runtime.resolveCombat(new EntityVerticalContactFrame(
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
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(0x55, EntityRoomLoader.RoomTable.INDOORS_A, -1);
        assertTrue(definition.supported());

        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshotWithSlots(
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

        List<EntityCombatEvent> swordEvents = runtime.resolveCombat(new EntityVerticalContactFrame(
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
                runtime.consumePendingEntityEvents());
    }

    @Test
    void protectedEnemyBombStillDoublesLinkSpeedWithoutDamageOrHurtSound() throws IOException {
        byte[] rom = loadRom();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot())
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(new EntitySpriteHandlerCatalog(rom))
                .enemyCombatTables(new RomEnemyCombatTables(rom))
                .build();

        int bombSlot = runtime.spawnEnemyBomb(0x48, 0x50, 0x00, 0x13);
        List<EntityProjectileEvent> events = runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                0,
                0x48,
                0x50,
                () -> 0,
                null,
                new EnemyProjectileCollision.LinkState(0x48, 0x50, 0x00, 0x00, 0x03, false, 1, 0x01),
                0x10,
                0xF0));

        assertEquals(
                List.of(new EntityProjectileEvent(
                        bombSlot,
                        0x02,
                        EntityProjectileEvent.Kind.LINK_DAMAGE,
                        0x00,
                        0x00,
                        EntityProjectileEvent.SoundChannel.NONE,
                        -1,
                        false,
                        false,
                        0x00,
                        0x00,
                        0x20,
                        0xE0,
                        0x00)),
                events);
        assertEquals(0x04, runtime.enemies().swordMoblinAlertingSoundCounter());
        List<BombExplosionEvent> explosionEvents = runtime.consumeBombExplosionEvents();
        assertEquals(
                List.of(BombExplosionEvent.OBJECT_TARGET),
                explosionEvents.stream().map(BombExplosionEvent::targetSlot).toList());
        assertEquals(bombSlot, explosionEvents.getFirst().bombSlot());
    }

    @Test
    void bombExplosionUsesNonlethalRomDamageBeforeTheTargetRecoilHandler() throws IOException {
        byte[] rom = loadRom();
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        RoomEntity ghini = new RoomEntityBuilder()
                .slot(14)
                .sourceLoadOrder(14)
                .type(0x12)
                .at(0x50, 0x4F)
                .spriteDefinition(pairDefinition(0x12, 2))
                .z(0x10)
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshotWithSlots(ghini))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(null)
                .enemyCombatTables(tables)
                .build();

        int bombSlot = runtime.spawnBomb(0x40, 0x50, 0, 0);
        EntityRuntimeFixture.debug(runtime).setBombTransitionCountdownForTest(bombSlot, 0x13);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x40, 0x50, () -> 0));

        assertEquals(4, runtime.enemies().enemyHealth(14));
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(14).status());
        // The lower-slot target's handler decrements the newly written flash
        // once after the higher-slot bomb has applied it.
        assertEquals(0x17, runtime.enemies().enemyFlashCountdown(14));
        assertEquals(0x09, runtime.enemies().enemyIgnoreHitsCountdown(14));
        assertEquals(0x30, EntityRuntimeFixture.enemies(runtime).enemyRecoilSpeedXForTest(14));
        assertEquals(0x00, EntityRuntimeFixture.enemies(runtime).enemyRecoilSpeedYForTest(14));
        assertEquals(
                List.of(new EntityCombatEvent(
                        14,
                        0x12,
                        0,
                        false,
                        4,
                        -1,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        0x03,
                        EntityCombatEvent.SoundChannel.NONE,
                        -1,
                        null)),
                runtime.consumePendingEntityEvents());
    }

    @Test
    void bombExplosionObjectWindowIsInclusiveAndSilentOutsideIt() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot()).build();
        int bombSlot = runtime.spawnBomb(0x40, 0x50, 0, 0);

        EntityRuntimeFixture.debug(runtime).setBombTransitionCountdownForTest(bombSlot, 0x18);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));
        assertTrue(runtime.consumeBombExplosionEvents().isEmpty());

        EntityRuntimeFixture.debug(runtime).setBombTransitionCountdownForTest(bombSlot, 0x17);
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0, 0, () -> 0));
        assertEquals(
                List.of(0x16),
                runtime.consumeBombExplosionEvents().stream()
                        .map(BombExplosionEvent::countdown)
                        .toList());

        EntityRuntimeFixture.debug(runtime).setBombTransitionCountdownForTest(bombSlot, 0x0F);
        runtime.tick(EntityTestFrames.frame(runtime, 2, 0, 0, () -> 0));
        assertEquals(
                List.of(0x0E),
                runtime.consumeBombExplosionEvents().stream()
                        .map(BombExplosionEvent::countdown)
                        .toList());

        EntityRuntimeFixture.debug(runtime).setBombTransitionCountdownForTest(bombSlot, 0x0E);
        runtime.tick(EntityTestFrames.frame(runtime, 3, 0, 0, () -> 0));
        assertTrue(runtime.consumeBombExplosionEvents().isEmpty());
    }
}
