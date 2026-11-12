package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.EntityProjectileEvent;
import linksawakening.world.entity.event.TransientVfxRequest;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityRuntimeMagicTest extends EntityRuntimeTestSupport {

    @Test
    void pairoddUsesItsRomNormalEnemyHealthAndContactDamage() {
        EntitySpriteDefinition definition = pairDefinition(0x57, 8);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x57)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();

        assertTrue(RoomEntityCombatRules.supportsEnemyCollision(0x57));
        assertEquals(2, runtime.enemies().enemyHealth(0));

        List<EntityCombatEvent> contact =
                runtime.resolveCombat(new EntitySwordContactFrame(1, 64, 64, false, true, false, 0, 0, 0, 0));
        assertEquals(1, contact.size());
        assertEquals(4, contact.get(0).linkDamage());

        List<EntityCombatEvent> firstHit =
                runtime.resolveCombat(new EntitySwordContactFrame(0, 120, 120, false, true, true, 72, 1, 72, 1));
        assertEquals(1, firstHit.size());
        assertTrue(firstHit.get(0).swordHit());
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(1, runtime.enemies().enemyHealth(0));

        for (int frame = 1; frame <= 0x18; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, sequence(0x00)));
        }
        RoomEntity afterFirstHit = runtime.inspection().snapshot().slots().get(0);
        List<EntityCombatEvent> secondHit = runtime.resolveCombat(new EntitySwordContactFrame(
                0x19, 120, 120, false, true, true, afterFirstHit.x() + 8, 1, afterFirstHit.y() + 8, 1));
        assertEquals(1, secondHit.size());
        assertEquals(
                EntityStatus.DYING,
                runtime.inspection().snapshot().slots().get(0).status());

        assertFalse(RoomEntityCombatRules.supportsEnemyCollision(0x58));
        assertEquals(0, RoomEntityCombatRules.basicSwordDamage(0x58));
    }

    @Test
    void armosKnightStartsTheRomMinibossMusicAfterTheBossIntroDelay() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x88)
                        .at(0x50, 0x60)
                        .spriteDefinition(EntitySpriteDefinition.unsupported(0x88))
                        .build()))
                .indoorRoom(true)
                .build();

        for (int frame = 0; frame < 0x20; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x50, 0x60, () -> 0));
            assertEquals(-1, runtime.consumePendingMusicTrack());
        }

        runtime.tick(EntityTestFrames.frame(runtime, 0x20, 0x50, 0x60, () -> 0));
        assertEquals(0x50, runtime.consumePendingMusicTrack());

        runtime.tick(EntityTestFrames.frame(runtime, 0x21, 0x50, 0x60, () -> 0));
        assertEquals(-1, runtime.consumePendingMusicTrack());
    }

    @Test
    void polsVoiceVectorBranchMatchesTheRomZeroDistanceResult() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(0x18, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x18)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 64, 64, sequence(0x00)));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 64, 64, sequence(0x00, 0x06)));

        // GetVectorTowardsLink's repeated-remainder loop increments both
        // components when both source distances are zero.
        assertEquals(0x0A, runtime.enemies().polsVoiceSpeedX(0));
        assertEquals(0x0A, runtime.enemies().polsVoiceSpeedY(0));
    }

    @Test
    void ocarinaAnimationSpawnsAndAdvancesTheRomMusicalNoteEntity() {
        byte[] rom = syntheticRom();
        write(rom, 0x05, 0x7EF8, 0x0E, 0x13);
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot())
                .indoorRoom(true)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .build();
        EntityRuntimeFixture.debug(runtime).setOcarinaPlaybackForTest(0x20, 0x01, 0x00);
        EntityRuntimeFixture.debug(runtime).setOcarinaAnimationForTest(0x14, 0x00);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x40, 0x50, sequence(0x00)));

        RoomEntity note = runtime.inspection().snapshot().loadedEntities().stream()
                .filter(entity -> entity.type() == 0xC9)
                .findFirst()
                .orElseThrow();
        assertEquals(0x48, note.x());
        assertEquals(0x48, note.y());
        assertEquals(0x05, note.spriteDefinition().bank());
        assertEquals(0x7EF8, note.spriteDefinition().address());
        assertEquals(0x40, EntityRuntimeFixture.enemies(runtime).musicalNoteInertiaForTest(note.slot()));

        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x40, 0x50, sequence(0x00)));

        RoomEntity movedNote = runtime.inspection().snapshot().slots().get(note.slot());
        assertEquals(0x4E, movedNote.x());
        assertEquals(0x44, movedNote.y());
        assertEquals(0x3F, EntityRuntimeFixture.enemies(runtime).musicalNoteInertiaForTest(note.slot()));
    }

    @Test
    void pairoddRuntimeConsumesItsRandomDirectionInitializerAndStartsTeleportState() {
        EntitySpriteDefinition definition = pairDefinition(0x57, 8);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x57)
                .at(64, 80)
                .status(EntityStatus.INIT)
                .spriteDefinition(definition)
                .build());
        AtomicInteger randomCalls = new AtomicInteger();
        IntSupplier randomBytes = () -> {
            randomCalls.incrementAndGet();
            return 0x03;
        };
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial)
                .indoorRoom(false)
                .randomByteSupplier(randomBytes)
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x00, 0x00, randomBytes));
        assertEquals(1, randomCalls.get());
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(0, runtime.flyingEnemies().pairoddState(0));

        runtime.tick(EntityTestFrames.frame(runtime, 1, 64, 80, randomBytes));

        assertEquals(1, runtime.flyingEnemies().pairoddState(0));
        assertEquals(0x20, runtime.flyingEnemies().pairoddTransitionCountdown(0));
        assertEquals(3, runtime.flyingEnemies().pairoddDirection(0));
        assertEquals(
                List.of(new EntityCombatEvent(0, 0x57, 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x3C)),
                runtime.consumePendingEntityEvents());
    }

    @Test
    void pairoddUsesTheBankFourRecoilStepBeforeItsHandler() {
        EntitySpriteDefinition definition = pairDefinition(0x57, 8);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x57)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        EntityRuntimeFixture.debug(runtime).configureEnemyRecoilForTest(0, 80, 80, 0x30);
        EntityRuntimeFixture.debug(runtime).setEnemyIgnoreHitsCountdownForTest(0, 0x0A);

        runtime.tick(EntityTestFrames.frame(runtime, 1, 120, 120, sequence(0x00)));

        RoomEntity afterRecoil = runtime.inspection().snapshot().slots().get(0);
        assertEquals(61, afterRecoil.x());
        assertEquals(61, afterRecoil.y());
        assertEquals(0x09, runtime.enemies().enemyIgnoreHitsCountdown(0));
        assertTrue(runtime.enemies().enemyRecoilActive(0));
    }

    @Test
    void pairoddSpawnsAReverseSlotProjectileWithRomDefinitionPositionAndVector() {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(syntheticRom());
        EntitySpriteDefinition definition = catalog.forEntityType(0x57, EntityRoomLoader.RoomTable.OVERWORLD, -1);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x57)
                .at(64, 80)
                .spriteDefinition(definition)
                .z(3)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial)
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .build();

        // Initialize the active snapshot, enter the teleport, and let the
        // disassembly's countdown reach the mirrored resting position.
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 64, 80, () -> 0));
        int frame = 2;
        while (runtime.flyingEnemies().pairoddState(0) == 1) {
            runtime.tick(EntityTestFrames.frame(runtime, frame++, 0, 0, () -> 0));
        }
        while (runtime.flyingEnemies().pairoddState(0) == 2) {
            runtime.tick(EntityTestFrames.frame(runtime, frame++, 0, 0, () -> 0));
        }
        assertEquals(0, runtime.flyingEnemies().pairoddState(0));
        assertEquals(0x30, runtime.flyingEnemies().pairoddTransitionCountdown(0));
        assertEquals(0x60, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(0x40, runtime.inspection().snapshot().slots().get(0).y());

        while (runtime.flyingEnemies().pairoddTransitionCountdown(0) > 0x19) {
            runtime.tick(EntityTestFrames.frame(runtime, frame++, 0, 0, () -> 0));
        }
        runtime.tick(EntityTestFrames.frame(runtime, frame, 0x70, 0x58, () -> 0));

        RoomEntity projectile = runtime.inspection().snapshot().slots().get(15);
        assertEquals(EntityStatus.ACTIVE, projectile.status());
        assertEquals(-1, projectile.sourceLoadOrder());
        assertEquals(0x58, projectile.type());
        assertEquals(0x60, projectile.x());
        assertEquals(0x40, projectile.y());
        assertEquals(3, projectile.z());
        assertTrue(projectile.spriteDefinition().supported());
        assertEquals(0x04, projectile.spriteDefinition().bank());
        assertEquals(0x5EF4, projectile.spriteDefinition().address());
        // ApplyVectorTowardsLink includes the source Z in its Y displacement:
        // dx=$10, dy=$1B, length=$18 -> ($0E, $18).
        assertEquals(0x0E, runtime.flyingEnemies().pairoddProjectileSpeedX(15));
        assertEquals(0x18, runtime.flyingEnemies().pairoddProjectileSpeedY(15));
        assertEquals(0, runtime.flyingEnemies().pairoddProjectileDirection(15));

        assertEquals(0, runtime.clearEntity(15));
        assertEquals(0, runtime.flyingEnemies().pairoddProjectileSpeedX(15));
        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(15).status());
    }

    @Test
    void pairoddProjectileIsRemovedAfterRoomObjectIntersectionAndQueuesRomVfx() {
        EntitySpriteDefinition definition = pairDefinition(0x58, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .type(0x58)
                        .at(0x40, 0x40)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        List<EntityProjectileEvent> events = runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                0,
                0,
                0,
                () -> 0,
                null,
                entity -> true,
                EnemyProjectileCollision.LinkState.nonInteractive(),
                false,
                0,
                0,
                0,
                0));

        assertTrue(events.isEmpty());
        assertFalse(runtime.inspection().snapshot().slots().get(0).loaded());
        assertEquals(1, runtime.enemies().transientVfxRequests().size());
        TransientVfxRequest request = runtime.enemies().transientVfxRequests().get(0);
        assertEquals(TransientVfxType.SWORD_POKE, request.type());
        assertEquals(0x40, request.worldX());
        assertEquals(0x40, request.worldY());
    }

    @Test
    void pairoddProjectileKeepsItsLinkCollisionPathWhenTheRoomObjectPasses() {
        EntitySpriteDefinition definition = pairDefinition(0x58, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .type(0x58)
                        .at(0x40, 0x40)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        List<EntityProjectileEvent> events = runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                0,
                0x40,
                0x40,
                () -> 0,
                null,
                entity -> false,
                new EnemyProjectileCollision.LinkState(0x40, 0x40, 0, 0, 0, false, 1, 0),
                false,
                0,
                0,
                0,
                0));

        assertEquals(1, events.size());
        assertEquals(EntityProjectileEvent.Kind.LINK_DAMAGE, events.get(0).kind());
        assertEquals(0x08, events.get(0).linkDamage());
        assertFalse(runtime.inspection().snapshot().slots().get(0).loaded());
    }

    @Test
    void desertLanmolaDeathUsesTheRomKeyProducerCountdownAndSpawnFields() {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(syntheticRom());
        RoomEntity source = new RoomEntityBuilder()
                .slot(15)
                .sourceLoadOrder(0)
                .type(0x87)
                .at(0x44, 0x58)
                .status(EntityStatus.DYING)
                .spriteDefinition(EntitySpriteDefinition.unsupported(0x87))
                .spriteVariant(-1)
                .z(0x07)
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshotAt(source))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));
        assertEquals(0x60, runtime.enemies().transitionCountdown(15));
        assertEquals(
                EntityStatus.DYING,
                runtime.inspection().snapshot().slots().get(15).status());

        for (int frame = 1; frame <= 0x60; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0, 0, () -> 0));
        }
        assertEquals(0xCF, runtime.enemies().transitionCountdown(15));

        for (int frame = 0x61; frame <= 0x12F; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0, 0, () -> 0));
        }

        RoomEntity drop = runtime.inspection().snapshot().slots().get(14);
        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(15).status());
        assertEquals(EntityStatus.ACTIVE, drop.status());
        assertEquals(0x30, drop.type());
        assertEquals(0x02, drop.spriteVariant());
        assertEquals(0x44, drop.x());
        assertEquals(0x58, drop.y());
        assertEquals(0x07, drop.z());
        assertEquals(0x10, runtime.inspection().dropPrivateCountdown1(14));
        assertEquals(0x10, runtime.inspection().dropSpeedZ(14));
        assertEquals(
                List.of(new EntityCombatEvent(15, 0x87, 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x13)),
                runtime.consumePendingEntityEvents());
    }

    @Test
    void masterStalfosDeathUsesTheRomKeyProducerCountdownAndSpawnFields() {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(syntheticRom());
        RoomEntity source = new RoomEntityBuilder()
                .slot(15)
                .sourceLoadOrder(0)
                .type(0x5F)
                .at(0x4C, 0x5A)
                .status(EntityStatus.DYING)
                .spriteDefinition(EntitySpriteDefinition.unsupported(0x5F))
                .spriteVariant(-1)
                .z(0x09)
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshotAt(source))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));
        assertEquals(0xA0, runtime.enemies().transitionCountdown(15));

        for (int frame = 1; frame <= 0xA0; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0, 0, () -> 0));
        }
        assertEquals(0xC0, runtime.enemies().transitionCountdown(15));

        for (int frame = 0xA1; frame <= 0x160; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0, 0, () -> 0));
        }

        RoomEntity drop = runtime.inspection().snapshot().slots().get(14);
        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(15).status());
        assertEquals(EntityStatus.ACTIVE, drop.status());
        assertEquals(0x30, drop.type());
        assertEquals(0x00, drop.spriteVariant());
        assertEquals(0x4C, drop.x());
        assertEquals(0x5A, drop.y());
        assertEquals(0x00, drop.z());
        assertEquals(0x20, runtime.inspection().dropPrivateCountdown1(14));
        assertEquals(0x18, runtime.inspection().dropSpeedZ(14));
        assertEquals(
                List.of(new EntityCombatEvent(15, 0x5F, 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x1A)),
                runtime.consumePendingEntityEvents());
    }
}
