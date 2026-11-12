package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.world.entity.event.BombExplosionEvent;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.EntityProjectileEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityRuntimeBombs1Test extends EntityRuntimeTestSupport {

    @Test
    void spawnBombUsesTheRomSlotStateAndDoesNotSkipItsFirstTick() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(
                        new RoomEntitySnapshot(new ArrayList<>(snapshot().slots())))
                .indoorRoom(false)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .build();

        int slot = runtime.spawnBomb(0x40, 0x50, 0x07, 3);

        assertEquals(EntityRoomLoader.MAX_ENTITIES - 1, slot);
        RoomEntity bomb = runtime.inspection().snapshot().slots().get(slot);
        assertEquals(0x02, bomb.type());
        assertEquals(-1, bomb.sourceLoadOrder());
        assertEquals(EntityStatus.ACTIVE, bomb.status());
        assertEquals(0x40, bomb.x());
        assertEquals(0x54, bomb.y());
        assertEquals(0x08, bomb.z());
        assertEquals(
                EntitySpriteDefinition.Shape.SINGLE, bomb.spriteDefinition().shape());
        assertEquals(0x03, bomb.spriteDefinition().bank());
        assertEquals(0x652E, bomb.spriteDefinition().address());
        assertEquals(0, bomb.spriteVariant());
        assertEquals(0xA0, runtime.enemies().transitionCountdown(slot));
        assertEquals(0xD2, runtime.enemies().physicsFlags(slot));
        assertEquals(0x0A, runtime.options1(slot));
        assertEquals(3, runtime.inspection().bombDirection(slot));
        assertTrue(runtime.inspection().bombActive());
        assertTrue(runtime.inspection().lastBombPlacementPlayedBump());

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x40, 0x50, () -> 0));

        RoomEntity afterFirstTick = runtime.inspection().snapshot().slots().get(slot);
        assertEquals(slot, afterFirstTick.slot());
        assertEquals(0x02, afterFirstTick.type());
        assertEquals(0x9F, runtime.enemies().transitionCountdown(slot));
        assertEquals(1, runtime.inspection().snapshot().loadedEntities().size());
    }

    @Test
    void sideScrollBombPlacementSkipsTheTopViewBumpJingle() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(
                        new RoomEntitySnapshot(new ArrayList<>(snapshot().slots())).withSideScrolling(true))
                .indoorRoom(false)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .build();

        assertTrue(runtime.spawnBomb(0x40, 0x50, 0x07, 3) >= 0);

        assertFalse(runtime.inspection().lastBombPlacementPlayedBump());
        assertEquals(0, runtime.inspection().snapshot().loadedEntities().get(0).z());
    }

    @Test
    void bombSpawnRejectsASecondLoadedBombAndReusesTheSlotAfterClear() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot()).build();

        int firstSlot = runtime.spawnBomb(0x40, 0x50, 0, 0);

        assertEquals(EntityRoomLoader.MAX_ENTITIES - 1, firstSlot);
        assertEquals(-1, runtime.spawnBomb(0x42, 0x52, 0, 1));

        runtime.clearEntity(firstSlot);

        assertFalse(runtime.inspection().bombActive());
        assertEquals(EntityRoomLoader.MAX_ENTITIES - 1, runtime.spawnBomb(0x42, 0x52, 0, 1));
    }

    @Test
    void bombSpawnRejectsAFullEntityTableWithoutChangingItsSlots() {
        List<RoomEntity> slots = new ArrayList<>();
        EntitySpriteDefinition unsupported = EntitySpriteDefinition.unsupported(0xFF);
        for (int slot = 0; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            slots.add(new RoomEntityBuilder()
                    .slot(slot)
                    .sourceLoadOrder(slot)
                    .type(0xFF)
                    .at(0x20, 0x20)
                    .spriteDefinition(unsupported)
                    .spriteVariant(-1)
                    .build());
        }
        RoomEntityRuntime runtime =
                RoomEntityRuntime.builder(new RoomEntitySnapshot(slots)).build();

        assertEquals(-1, runtime.spawnBomb(0x40, 0x50, 0, 0));
        assertFalse(runtime.inspection().bombActive());
        assertEquals(0xFF, runtime.inspection().snapshot().slots().get(15).type());
    }

    @Test
    void bombKeepsOneSlotAcrossWarningExplosionSoundAndUnload() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(
                        new RoomEntitySnapshot(new ArrayList<>(snapshot().slots())))
                .indoorRoom(false)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .build();
        int slot = runtime.spawnBomb(0x40, 0x50, 0, 0);

        EntityRuntimeFixture.debug(runtime).setBombTransitionCountdownForTest(slot, 0x22);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x40, 0x50, () -> 0));
        RoomEntity warning = runtime.inspection().snapshot().slots().get(slot);
        assertEquals(0x02, warning.type());
        assertEquals(0x5484, warning.spriteDefinition().address());
        assertEquals(0, warning.spriteVariant());
        assertEquals(0x21, runtime.enemies().transitionCountdown(slot));
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());

        // UpdateEntityTimers decrements before BombEntityHandler sees $18.
        EntityRuntimeFixture.debug(runtime).setBombTransitionCountdownForTest(slot, 0x19);
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x40, 0x50, () -> 0));
        RoomEntity warningAtSound = runtime.inspection().snapshot().slots().get(slot);
        assertEquals(slot, warningAtSound.slot());
        assertEquals(0x02, warningAtSound.type());
        assertEquals(0x5484, warningAtSound.spriteDefinition().address());
        assertEquals(0x17, runtime.enemies().transitionCountdown(slot));
        assertEquals(
                List.of(new EntityCombatEvent(slot, 0x02, 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x0C)),
                runtime.consumePendingEntityEvents());

        runtime.tick(EntityTestFrames.frame(runtime, 2, 0x40, 0x50, () -> 0));
        RoomEntity explosionVariantThree =
                runtime.inspection().snapshot().slots().get(slot);
        assertEquals(slot, explosionVariantThree.slot());
        assertEquals(0x02, explosionVariantThree.type());
        assertEquals(0x6530, explosionVariantThree.spriteDefinition().address());
        assertEquals(3, explosionVariantThree.spriteVariant());
        assertEquals(0xD8, runtime.enemies().physicsFlags(slot));
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());

        EntityRuntimeFixture.debug(runtime).setBombTransitionCountdownForTest(slot, 0x14);
        runtime.tick(EntityTestFrames.frame(runtime, 3, 0x40, 0x50, () -> 0));
        assertEquals(
                0x6530,
                runtime.inspection()
                        .snapshot()
                        .slots()
                        .get(slot)
                        .spriteDefinition()
                        .address());
        assertEquals(2, runtime.inspection().snapshot().slots().get(slot).spriteVariant());

        EntityRuntimeFixture.debug(runtime).setBombTransitionCountdownForTest(slot, 0x10);
        runtime.tick(EntityTestFrames.frame(runtime, 4, 0x40, 0x50, () -> 0));
        assertEquals(1, runtime.inspection().snapshot().slots().get(slot).spriteVariant());

        EntityRuntimeFixture.debug(runtime).setBombTransitionCountdownForTest(slot, 0x08);
        runtime.tick(EntityTestFrames.frame(runtime, 5, 0x40, 0x50, () -> 0));
        assertEquals(0, runtime.inspection().snapshot().slots().get(slot).spriteVariant());
        assertTrue(runtime.inspection().bombActive());

        EntityRuntimeFixture.debug(runtime).setBombTransitionCountdownForTest(slot, 0x01);
        runtime.tick(EntityTestFrames.frame(runtime, 6, 0x40, 0x50, () -> 0));

        RoomEntity finalExplosion = runtime.inspection().snapshot().slots().get(slot);
        assertEquals(slot, finalExplosion.slot());
        assertEquals(0x02, finalExplosion.type());
        assertEquals(EntityStatus.ACTIVE, finalExplosion.status());
        assertEquals(0x6530, finalExplosion.spriteDefinition().address());
        assertEquals(0, finalExplosion.spriteVariant());
        assertEquals(0xD8, runtime.enemies().physicsFlags(slot));
        assertFalse(runtime.inspection().bombActive());
        assertEquals(1, runtime.inspection().snapshot().loadedEntities().size());

        runtime.tick(EntityTestFrames.frame(runtime, 7, 0x40, 0x50, () -> 0));

        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(slot).status());
        assertFalse(runtime.inspection().bombActive());
        assertEquals(0, runtime.enemies().physicsFlags(slot));
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());
    }

    @Test
    void spawnBombFinalizesPendingFinalPresentationBeforeReusingItsSlot() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(
                        new RoomEntitySnapshot(new ArrayList<>(snapshot().slots())))
                .indoorRoom(false)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .build();
        int slot = runtime.spawnBomb(0x40, 0x50, 0, 0);

        EntityRuntimeFixture.debug(runtime).setBombTransitionCountdownForTest(slot, 0x01);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x40, 0x50, () -> 0));

        assertFalse(runtime.inspection().bombActive());
        assertEquals(slot, runtime.spawnBomb(0x44, 0x54, 0, 1));
        RoomEntity replacement = runtime.inspection().snapshot().slots().get(slot);
        assertEquals(EntityStatus.ACTIVE, replacement.status());
        assertEquals(0x02, replacement.type());
        assertEquals(0x652E, replacement.spriteDefinition().address());
        assertEquals(0xA0, runtime.enemies().transitionCountdown(slot));
        assertTrue(runtime.inspection().bombActive());
        assertEquals(1, runtime.inspection().snapshot().loadedEntities().size());
    }

    @Test
    void alternateBombDisableClearsPendingPresentationAndDirectionBeforeReuse() throws Exception {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(
                        new RoomEntitySnapshot(new ArrayList<>(snapshot().slots())))
                .indoorRoom(false)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .build();
        int slot = runtime.spawnBomb(0x40, 0x50, 0, 3);

        EntityRuntimeFixture.debug(runtime).setBombTransitionCountdownForTest(slot, 0x01);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x40, 0x50, () -> 0));

        assertTrue(runtime.bombInteractions.bombFinalPresentationPending(slot));

        Method disable = RoomEntityRuntime.class.getDeclaredMethod("disableEntityWithoutPersistence", int.class);
        disable.setAccessible(true);
        disable.invoke(runtime, slot);

        assertFalse(runtime.bombInteractions.bombFinalPresentationPending(slot));
        assertEquals(0xFF, runtime.inspection().bombDirection(slot));
        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(slot).status());
        assertEquals(slot, runtime.spawnBomb(0x44, 0x54, 0, 1));
        assertEquals(1, runtime.inspection().bombDirection(slot));
        assertFalse(runtime.bombInteractions.bombFinalPresentationPending(slot));
    }

    @Test
    void bombExplosionPublishesOnlyTargetsInsideTheInclusiveCountdownWindow() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshotWithSlots(
                        new RoomEntityBuilder()
                                .slot(11)
                                .sourceLoadOrder(11)
                                .type(0x4D)
                                .at(0x40, 0x67)
                                .spriteDefinition(pairDefinition(0x4D, 1))
                                .build(),
                        new RoomEntityBuilder()
                                .slot(12)
                                .sourceLoadOrder(12)
                                .type(0x4D)
                                .at(0x58, 0x4F)
                                .spriteDefinition(pairDefinition(0x4D, 1))
                                .build(),
                        new RoomEntityBuilder()
                                .slot(13)
                                .sourceLoadOrder(13)
                                .type(0x4D)
                                .at(0x57, 0x66)
                                .spriteDefinition(pairDefinition(0x4D, 1))
                                .build(),
                        new RoomEntityBuilder()
                                .slot(14)
                                .sourceLoadOrder(14)
                                .type(0x4D)
                                .at(0x28, 0x4F)
                                .spriteDefinition(pairDefinition(0x4D, 1))
                                .build()))
                .build();
        int bombSlot = runtime.spawnBomb(0x40, 0x50, 0, 0);
        EntityRuntimeFixture.debug(runtime).setBombTransitionCountdownForTest(bombSlot, 0x13);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));

        List<BombExplosionEvent> events = runtime.consumeBombExplosionEvents();
        assertEquals(
                List.of(13, 12),
                events.stream()
                        .filter(event -> !event.targetsRoomObjects())
                        .map(BombExplosionEvent::targetSlot)
                        .toList());
        assertEquals(0x04, new EntityEnemyInspectionQueries(runtime.enemies()).swordMoblinAlertingSoundCounter());
        for (BombExplosionEvent event : events) {
            assertEquals(bombSlot, event.bombSlot());
            assertEquals(0x48, event.bombX());
            assertEquals(0x4F, event.bombVisualY());
            assertEquals(0x12, event.countdown());
            assertEquals(0x07, event.damageType());
        }
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());

        runtime.tick(EntityTestFrames.frame(runtime, 1, 0, 0, () -> 0));
        List<BombExplosionEvent> nextFrame = runtime.consumeBombExplosionEvents();
        assertEquals(
                List.of(BombExplosionEvent.OBJECT_TARGET),
                nextFrame.stream().map(BombExplosionEvent::targetSlot).toList());
    }

    @Test
    void bombExplosionFiltersNonInteractiveTargetsAtTheExactInteractionTick() throws Exception {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshotWithSlots(
                        new RoomEntityBuilder()
                                .slot(10)
                                .sourceLoadOrder(10)
                                .type(0x4D)
                                .at(0x40, 0x4F)
                                .spriteDefinition(pairDefinition(0x4D, 1))
                                .build(),
                        new RoomEntityBuilder()
                                .slot(11)
                                .sourceLoadOrder(11)
                                .type(0x4D)
                                .at(0x40, 0x4F)
                                .spriteDefinition(pairDefinition(0x4D, 1))
                                .build(),
                        new RoomEntityBuilder()
                                .slot(12)
                                .sourceLoadOrder(12)
                                .type(0x4D)
                                .at(0x40, 0x4F)
                                .spriteDefinition(pairDefinition(0x4D, 1))
                                .build(),
                        new RoomEntityBuilder()
                                .slot(13)
                                .sourceLoadOrder(13)
                                .type(0x05)
                                .at(0x40, 0x4F)
                                .spriteDefinition(pairDefinition(0x05, 1))
                                .build(),
                        new RoomEntityBuilder()
                                .slot(14)
                                .sourceLoadOrder(14)
                                .type(0x4D)
                                .at(0x40, 0x4F)
                                .spriteDefinition(pairDefinition(0x4D, 1))
                                .build()))
                .build();
        setPhysicsFlags(runtime, 14, 0x40);
        EntityRuntimeFixture.debug(runtime).setHitboxFlagsForTest(12, 0x80);
        EntityRuntimeFixture.debug(runtime).setEnemyIgnoreHitsCountdownForTest(11, 0x02);
        int bombSlot = runtime.spawnBomb(0x40, 0x50, 0, 0);
        EntityRuntimeFixture.debug(runtime).setBombTransitionCountdownForTest(bombSlot, 0x13);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));

        List<BombExplosionEvent> events = runtime.consumeBombExplosionEvents();
        List<BombExplosionEvent> targetEvents =
                events.stream().filter(event -> !event.targetsRoomObjects()).toList();
        assertEquals(2, targetEvents.size());
        assertEquals(
                List.of(11, 10),
                targetEvents.stream().map(BombExplosionEvent::targetSlot).toList());
        assertEquals(0x12, targetEvents.getFirst().countdown());
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());
    }

    @Test
    void bombExplosionAppliesRomDamageAndFinalSourceRelativeRecoil() throws IOException {
        byte[] rom = loadRom();
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        RoomEntity octorok = new RoomEntityBuilder()
                .slot(14)
                .sourceLoadOrder(14)
                .type(0x09)
                .at(0x40, 0x4F)
                .spriteDefinition(pairDefinition(0x09, 8))
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshotWithSlots(octorok))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(null)
                .enemyCombatTables(tables)
                .build();

        int bombSlot = runtime.spawnBomb(0x40, 0x50, 0, 0);
        EntityRuntimeFixture.debug(runtime).setBombTransitionCountdownForTest(bombSlot, 0x13);

        // The bomb candidate is placed at ($48,$50,$01), so the target's
        // visual Y is aligned with the explosion while its X is eight pixels
        // to the left. The ROM's final length-$30 vector must therefore be
        // -$30 on X and zero on Y.
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x40, 0x50, () -> 0));

        assertEquals(0, runtime.enemies().enemyHealth(14));
        assertEquals(
                EntityStatus.DYING,
                runtime.inspection().snapshot().slots().get(14).status());
        // CheckExplosionInteractionWithEntities does not pre-arm the normal
        // sword hit flash/ignore timers; a lethal ApplySwordDamagesToEnemy
        // path reaches DYING with both values still clear.
        assertEquals(0x00, new EntityEnemyInspectionQueries(runtime.enemies()).enemyFlashCountdown(14));
        assertEquals(0x00, runtime.enemies().enemyIgnoreHitsCountdown(14));
        assertEquals(0xD0, EntityRuntimeFixture.enemies(runtime).enemyRecoilSpeedXForTest(14));
        assertEquals(0x00, EntityRuntimeFixture.enemies(runtime).enemyRecoilSpeedYForTest(14));
        assertEquals(
                List.of(
                        linksawakening.world.entity.event.EntityCombatEvent.builder(14, 0x09)
                                .linkDamage(0)
                                .swordHit(false)
                                .enemyDamage(1)
                                .enemySpecialAction(-1)
                                .soundChannel(EntityCombatEvent.SoundChannel.JINGLE)
                                .soundId(0x03)
                                .secondarySoundChannel(EntityCombatEvent.SoundChannel.NONE)
                                .secondarySoundId(-1)
                                .swordPokeVfx(null)
                                .build(),
                        new EntityCombatEvent(14, 0x09, 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x13)),
                runtime.consumePendingEntityEvents());

        assertEquals(
                List.of(14),
                runtime.consumeBombExplosionEvents().stream()
                        .filter(event -> !event.targetsRoomObjects())
                        .map(BombExplosionEvent::targetSlot)
                        .toList());
    }

    @Test
    void enemyBombExplosionUsesTheSourceLinkCollisionBranch() throws IOException {
        byte[] rom = loadRom();
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot())
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        int bombSlot = runtime.spawnEnemyBomb(0x48, 0x50, 0x00, 0x13);
        List<EntityProjectileEvent> events = runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                0,
                0x48,
                0x50,
                () -> 0,
                null,
                new EnemyProjectileCollision.LinkState(0x48, 0x50, 0x00, 0x00, 0x03, false, 1, 0x00),
                0x10,
                0xF0));

        assertEquals(
                List.of(new EntityProjectileEvent(
                        bombSlot,
                        0x02,
                        EntityProjectileEvent.Kind.LINK_DAMAGE,
                        0x00,
                        0x08,
                        EntityProjectileEvent.SoundChannel.WAVE,
                        0x03,
                        false,
                        false,
                        0x00,
                        0x00,
                        0x28,
                        0x28,
                        0x10)),
                events);
        assertEquals(0x04, new EntityEnemyInspectionQueries(runtime.enemies()).swordMoblinAlertingSoundCounter());
        assertEquals(
                List.of(BombExplosionEvent.OBJECT_TARGET),
                runtime.consumeBombExplosionEvents().stream()
                        .map(BombExplosionEvent::targetSlot)
                        .toList());
    }

    @Test
    void enemyBombUsesRawEntityYInsteadOfExplosionVisualYForLinkCollision() throws IOException {
        byte[] rom = loadRom();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot())
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(new EntitySpriteHandlerCatalog(rom))
                .enemyCombatTables(new RomEnemyCombatTables(rom))
                .build();

        int bombSlot = runtime.spawnEnemyBomb(0x48, 0x68, 0x18, 0x13);
        List<EntityProjectileEvent> events = runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                0,
                0x48,
                0x50,
                () -> 0,
                null,
                new EnemyProjectileCollision.LinkState(0x48, 0x50, 0x00, 0x00, 0x03, false, 1, 0x00),
                0x10,
                0xF0));

        assertEquals(List.of(), events);
        assertEquals(0x04, new EntityEnemyInspectionQueries(runtime.enemies()).swordMoblinAlertingSoundCounter());
        List<BombExplosionEvent> explosionEvents = runtime.consumeBombExplosionEvents();
        assertEquals(
                List.of(BombExplosionEvent.OBJECT_TARGET),
                explosionEvents.stream().map(BombExplosionEvent::targetSlot).toList());
        assertEquals(bombSlot, explosionEvents.getFirst().bombSlot());
    }

    @Test
    void bomberUsesItsSourceHandlerToSpawnAnEnemyBomb() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition bomberDefinition = catalog.forEntityType(0xBA, EntityRoomLoader.RoomTable.OVERWORLD, -1);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshotWithSlots(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0xBA)
                        .at(0x50, 0x50)
                        .spriteDefinition(bomberDefinition)
                        .z(0x00)
                        .build()))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x50, 0x50, () -> 0));

        RoomEntity bomb = runtime.inspection().snapshot().slots().get(15);
        assertTrue(bomb.loaded());
        assertEquals(EntityStatus.ACTIVE, bomb.status());
        assertEquals(0x02, bomb.type());
        assertEquals(0x50, bomb.x());
        assertEquals(0x50, bomb.y());
        assertEquals(0x10, bomb.z());
        assertEquals(0x40, runtime.enemies().transitionCountdown(15));
        assertEquals(0x01, runtime.enemies().enemyIgnoreHitsCountdown(15));
        assertEquals(
                List.of(new EntityCombatEvent(15, 0x02, 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x08)),
                runtime.consumePendingEntityEvents());
    }

    @Test
    void bomberEnemyBombCarriesTheSourceVectorIntoTheNextBombHandlerFrame() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshotWithSlots(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0xBA)
                        .at(0x50, 0x50)
                        .spriteDefinition(catalog.forEntityType(0xBA, EntityRoomLoader.RoomTable.OVERWORLD, -1))
                        .z(0x00)
                        .build()))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(new RomEnemyCombatTables(rom))
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x70, 0x50, () -> 0));
        runtime.consumePendingEntityEvents();
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x70, 0x50, () -> 0));

        RoomEntity bomb = runtime.inspection().snapshot().slots().get(15);
        assertEquals(0x51, bomb.x());
        assertEquals(0x50, bomb.y());
        assertEquals(0x10, bomb.z());
        assertEquals(0x3F, runtime.enemies().transitionCountdown(15));
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());
    }

    @Test
    void madBomberUsesItsHoleCycleToSpawnTheSourceEnemyBomb() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(0x93, EntityRoomLoader.RoomTable.OVERWORLD, -1);
        assertTrue(definition.supported());
        assertEquals(0x06, definition.bank());
        assertEquals(0x4126, definition.address());
        assertEquals(5, definition.variantCount());

        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshotWithSlots(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x93)
                        .at(0x50, 0x50)
                        .spriteDefinition(definition)
                        .z(0x00)
                        .build()))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        for (int frame = 0; frame <= 136; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0xC0, 0xC0, () -> 0));
        }

        RoomEntity bomb = runtime.inspection().snapshot().slots().get(15);
        assertTrue(bomb.loaded());
        assertEquals(EntityStatus.ACTIVE, bomb.status());
        assertEquals(0x02, bomb.type());
        assertEquals(0x28, bomb.x());
        assertEquals(0x40, bomb.y());
        assertEquals(0x04, bomb.z());
        assertEquals(0x40, runtime.enemies().transitionCountdown(15));
        assertEquals(0x01, runtime.enemies().enemyIgnoreHitsCountdown(15));
        assertEquals(
                List.of(new EntityCombatEvent(15, 0x02, 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x08)),
                runtime.consumePendingEntityEvents());
    }

    @Test
    void timerBombiteUsesItsRomFuseToProduceAnEnemyBomb() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(0x56, EntityRoomLoader.RoomTable.INDOORS_A, -1);
        assertTrue(definition.supported());
        assertEquals(0x04, definition.bank());
        assertEquals(0x7CEF, definition.address());
        assertEquals(6, definition.variantCount());

        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshotWithSlots(new RoomEntityBuilder()
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
            explosionSound |= runtime.consumePendingEntityEvents().stream()
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
}
