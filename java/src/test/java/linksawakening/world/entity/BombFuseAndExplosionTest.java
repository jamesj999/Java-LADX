package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.loadRom;
import static linksawakening.world.entity.EntityRuntimeTestSupport.pairDefinition;
import static linksawakening.world.entity.EntityRuntimeTestSupport.setPhysicsFlags;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshotWithSlots;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.testing.TestRoms;
import linksawakening.world.entity.event.BombExplosionEvent;
import linksawakening.world.entity.event.EntityCombatEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class BombFuseAndExplosionTest {

    @Test
    void bombKeepsOneSlotAcrossWarningExplosionSoundAndUnload() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(
                        new RoomEntitySnapshot(new ArrayList<>(snapshot().slots())))
                .indoorRoom(false)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .build();
        int slot = runtime.spawns().spawnBomb(0x40, 0x50, 0, 0);

        EntityRuntimeFixture.debug(runtime).setBombTransitionCountdownForTest(slot, 0x22);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x40, 0x50, () -> 0));
        RoomEntity warning = runtime.inspection().snapshot().slots().get(slot);
        assertEquals(0x02, warning.type());
        assertEquals(0x5484, warning.spriteDefinition().address());
        assertEquals(0, warning.spriteVariant());
        assertEquals(0x21, runtime.enemies().transitionCountdown(slot));
        assertTrue(runtime.outbox().consumePendingEntityEvents().isEmpty());

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
                runtime.outbox().consumePendingEntityEvents());

        runtime.tick(EntityTestFrames.frame(runtime, 2, 0x40, 0x50, () -> 0));
        RoomEntity explosionVariantThree =
                runtime.inspection().snapshot().slots().get(slot);
        assertEquals(slot, explosionVariantThree.slot());
        assertEquals(0x02, explosionVariantThree.type());
        assertEquals(0x6530, explosionVariantThree.spriteDefinition().address());
        assertEquals(3, explosionVariantThree.spriteVariant());
        assertEquals(0xD8, runtime.enemies().physicsFlags(slot));
        assertTrue(runtime.outbox().consumePendingEntityEvents().isEmpty());

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
        assertTrue(runtime.outbox().consumePendingEntityEvents().isEmpty());
    }

    @Test
    void bombExplosionPublishesOnlyTargetsInsideTheInclusiveCountdownWindow() {
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshotWithSlots(
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
        int bombSlot = runtime.spawns().spawnBomb(0x40, 0x50, 0, 0);
        EntityRuntimeFixture.debug(runtime).setBombTransitionCountdownForTest(bombSlot, 0x13);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));

        List<BombExplosionEvent> events = runtime.outbox().consumeBombExplosionEvents();
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
        assertTrue(runtime.outbox().consumePendingEntityEvents().isEmpty());

        runtime.tick(EntityTestFrames.frame(runtime, 1, 0, 0, () -> 0));
        List<BombExplosionEvent> nextFrame = runtime.outbox().consumeBombExplosionEvents();
        assertEquals(
                List.of(BombExplosionEvent.OBJECT_TARGET),
                nextFrame.stream().map(BombExplosionEvent::targetSlot).toList());
    }

    @Test
    void bombExplosionFiltersNonInteractiveTargetsAtTheExactInteractionTick() throws Exception {
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshotWithSlots(
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
        int bombSlot = runtime.spawns().spawnBomb(0x40, 0x50, 0, 0);
        EntityRuntimeFixture.debug(runtime).setBombTransitionCountdownForTest(bombSlot, 0x13);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));

        List<BombExplosionEvent> events = runtime.outbox().consumeBombExplosionEvents();
        List<BombExplosionEvent> targetEvents =
                events.stream().filter(event -> !event.targetsRoomObjects()).toList();
        assertEquals(2, targetEvents.size());
        assertEquals(
                List.of(11, 10),
                targetEvents.stream().map(BombExplosionEvent::targetSlot).toList());
        assertEquals(0x12, targetEvents.getFirst().countdown());
        assertTrue(runtime.outbox().consumePendingEntityEvents().isEmpty());
    }

    @Test
    void bombExplosionAppliesRomDamageAndFinalSourceRelativeRecoil() throws IOException {
        byte[] rom = loadRom();
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        RoomEntity octorok = new RoomEntityBuilder()
                .slot(14)
                .sourceLoadOrder(14)
                .type(0x09)
                .at(0x40, 0x4F)
                .spriteDefinition(pairDefinition(0x09, 8))
                .build();
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshotWithSlots(octorok))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(null)
                .enemyCombatTables(tables)
                .build();

        int bombSlot = runtime.spawns().spawnBomb(0x40, 0x50, 0, 0);
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
                runtime.outbox().consumePendingEntityEvents());

        assertEquals(
                List.of(14),
                runtime.outbox().consumeBombExplosionEvents().stream()
                        .filter(event -> !event.targetsRoomObjects())
                        .map(BombExplosionEvent::targetSlot)
                        .toList());
    }

    @Test
    void bombExplosionUsesNonlethalRomDamageBeforeTheTargetRecoilHandler() throws IOException {
        byte[] rom = loadRom();
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        RoomEntity ghini = new RoomEntityBuilder()
                .slot(14)
                .sourceLoadOrder(14)
                .type(0x12)
                .at(0x50, 0x4F)
                .spriteDefinition(pairDefinition(0x12, 2))
                .z(0x10)
                .build();
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshotWithSlots(ghini))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(null)
                .enemyCombatTables(tables)
                .build();

        int bombSlot = runtime.spawns().spawnBomb(0x40, 0x50, 0, 0);
        EntityRuntimeFixture.debug(runtime).setBombTransitionCountdownForTest(bombSlot, 0x13);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x40, 0x50, () -> 0));

        assertEquals(4, runtime.enemies().enemyHealth(14));
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(14).status());
        // The lower-slot target's handler decrements the newly written flash
        // once after the higher-slot bomb has applied it.
        assertEquals(0x17, new EntityEnemyInspectionQueries(runtime.enemies()).enemyFlashCountdown(14));
        assertEquals(0x09, runtime.enemies().enemyIgnoreHitsCountdown(14));
        assertEquals(0x30, EntityRuntimeFixture.enemies(runtime).enemyRecoilSpeedXForTest(14));
        assertEquals(0x00, EntityRuntimeFixture.enemies(runtime).enemyRecoilSpeedYForTest(14));
        assertEquals(
                List.of(linksawakening.world.entity.event.EntityCombatEvent.builder(14, 0x12)
                        .linkDamage(0)
                        .swordHit(false)
                        .enemyDamage(4)
                        .enemySpecialAction(-1)
                        .soundChannel(EntityCombatEvent.SoundChannel.JINGLE)
                        .soundId(0x03)
                        .secondarySoundChannel(EntityCombatEvent.SoundChannel.NONE)
                        .secondarySoundId(-1)
                        .swordPokeVfx(null)
                        .build()),
                runtime.outbox().consumePendingEntityEvents());
    }

    @Test
    void bombExplosionObjectWindowIsInclusiveAndSilentOutsideIt() {
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot()).build();
        int bombSlot = runtime.spawns().spawnBomb(0x40, 0x50, 0, 0);

        EntityRuntimeFixture.debug(runtime).setBombTransitionCountdownForTest(bombSlot, 0x18);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));
        assertTrue(runtime.outbox().consumeBombExplosionEvents().isEmpty());

        EntityRuntimeFixture.debug(runtime).setBombTransitionCountdownForTest(bombSlot, 0x17);
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0, 0, () -> 0));
        assertEquals(
                List.of(0x16),
                runtime.outbox().consumeBombExplosionEvents().stream()
                        .map(BombExplosionEvent::countdown)
                        .toList());

        EntityRuntimeFixture.debug(runtime).setBombTransitionCountdownForTest(bombSlot, 0x0F);
        runtime.tick(EntityTestFrames.frame(runtime, 2, 0, 0, () -> 0));
        assertEquals(
                List.of(0x0E),
                runtime.outbox().consumeBombExplosionEvents().stream()
                        .map(BombExplosionEvent::countdown)
                        .toList());

        EntityRuntimeFixture.debug(runtime).setBombTransitionCountdownForTest(bombSlot, 0x0E);
        runtime.tick(EntityTestFrames.frame(runtime, 3, 0, 0, () -> 0));
        assertTrue(runtime.outbox().consumeBombExplosionEvents().isEmpty());
    }
}
