package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.loadRom;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshotAt;
import static linksawakening.world.entity.EntityRuntimeTestSupport.syntheticRom;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.testing.TestRoms;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.LinkFallenPoseRequest;
import linksawakening.world.entity.event.LinkFinalPositionRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.ScreenShakeRequest;
import linksawakening.world.entity.event.TransientVfxRequest;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class ArmosKnightRuntimeTest {

    @Test
    void armosKnightWakesChargesAndEnablesItsRomHitbox() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        EntitySpriteDefinition definition = catalog.forEntityType(0x88, EntityRoomLoader.RoomTable.INDOORS_B);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x88)
                        .at(0x50, 0x60)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom)))
                .build();

        assertEquals(0x84, runtime.enemies().physicsFlags(0));
        assertEquals(0xC4, runtime.execution().slotProperties().options1(0));
        assertEquals(0x80, EntityRuntimeFixture.enemies(runtime).hitboxFlagsForTest(0));

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));
        assertEquals(0, runtime.bosses().armosKnightState(0));

        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x50, 0x60, () -> 0));
        assertEquals(1, runtime.bosses().armosKnightState(0));
        assertEquals(0x30, runtime.bosses().armosKnightTransitionCountdown(0));

        for (int frame = 2; frame <= 0x31; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x50, 0x60, () -> 0));
        }
        assertEquals(2, runtime.bosses().armosKnightState(0));
        assertEquals(0x80, runtime.bosses().armosKnightTransitionCountdown(0));
        assertEquals(0x84, runtime.enemies().physicsFlags(0));
        assertEquals(0x80, EntityRuntimeFixture.enemies(runtime).hitboxFlagsForTest(0));

        for (int frame = 0x32; frame <= 0xB1; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x50, 0x60, () -> 0));
        }
        assertEquals(3, runtime.bosses().armosKnightState(0));
        assertEquals(0x50, runtime.bosses().armosKnightTransitionCountdown(0));
        assertEquals(0x04, runtime.enemies().physicsFlags(0));
        assertEquals(0x00, EntityRuntimeFixture.enemies(runtime).hitboxFlagsForTest(0));
        assertEquals(0x84, runtime.execution().slotProperties().options1(0));
    }

    @Test
    void armosKnightRequestsCopyingLinksFinalPositionOnActiveCollision() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        EntitySpriteDefinition definition = catalog.forEntityType(0x88, EntityRoomLoader.RoomTable.INDOORS_B);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x88)
                        .at(0x50, 0x60)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom)))
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x50, 0x60, () -> 0));
        for (int frame = 2; frame <= 0x31; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x50, 0x60, () -> 0));
        }
        for (int frame = 0x32; frame <= 0xB1; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x50, 0x60, () -> 0));
        }
        assertEquals(3, runtime.bosses().armosKnightState(0));

        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                0xB2,
                0x50,
                0x60,
                () -> 0,
                linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN,
                new EnemyProjectileCollision.LinkState(0x50, 0x60, 0x04, 0x00, 0, false)));

        assertEquals(
                List.of(new LinkFinalPositionRequest(0, true, true, false)),
                runtime.outbox().consumePendingLinkFinalPositionRequests());
    }

    @Test
    void armosKnightDamageThresholdSpawnsRomRubbleWithPoofAndNoise() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        EntitySpriteDefinition definition = catalog.forEntityType(0x88, EntityRoomLoader.RoomTable.INDOORS_B);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x88)
                        .at(0x50, 0x60)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom)))
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x50, 0x60, () -> 0));
        for (int frame = 2; frame <= 0x31; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x50, 0x60, () -> 0));
        }
        for (int frame = 0x32; frame <= 0xB1; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x50, 0x60, () -> 0));
        }
        assertEquals(3, runtime.bosses().armosKnightState(0));
        assertEquals(0x0C, runtime.enemies().enemyHealth(0));
        EnemyAttackContext levelTwoSword = new EnemyAttackContext(2, false, false, false, false);

        for (int hit = 0; hit < 3; hit++) {
            EntityRuntimeFixture.debug(runtime).setEnemyIgnoreHitsCountdownForTest(0, 0);
            EntityRuntimeFixture.debug(runtime).setEnemyFlashCountdownForTest(0, 0);
            RoomEntity armos = runtime.inspection().snapshot().slots().get(0);
            int frame = 0xB2 + hit;
            List<EntityCombatEvent> hitEvents = runtime.combat()
                    .resolveCombat(new EntityVerticalContactFrame(
                            frame, 0, 0, true, true, true, armos.x() + 8, 1, armos.y() + 8, 1, levelTwoSword));
            assertEquals(1, hitEvents.size());
            assertEquals(2, hitEvents.get(0).enemyDamage());
            assertEquals(0x0A - (hit * 2), runtime.enemies().enemyHealth(0));

            EntityRuntimeFixture.debug(runtime).setEnemyIgnoreHitsCountdownForTest(0, 0);
            EntityRuntimeFixture.debug(runtime).setEnemyFlashCountdownForTest(0, 0);
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x50, 0x60, () -> 0));
        }

        assertEquals(0x06, runtime.enemies().enemyHealth(0));
        assertEquals(1, runtime.bosses().armosKnightInertia(0));
        RoomEntity rubble = runtime.inspection().snapshot().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x05 && entity.sourceLoadOrder() == -1)
                .findFirst()
                .orElseThrow();
        assertEquals(0x57, rubble.x());
        assertEquals(0x50, rubble.y());
        assertEquals(0x05, rubble.spriteVariant());
        assertEquals(0xC4, runtime.enemies().physicsFlags(rubble.slot()));
        assertEquals(0x0A, runtime.execution().slotProperties().options1(rubble.slot()));
        assertEquals(
                List.of(new TransientVfxRequest(TransientVfxType.POOF, 0x57, 0x50)),
                runtime.enemies().transientVfxRequests());
        assertEquals(
                List.of(new EntityCombatEvent(
                        rubble.slot(), 0x05, 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x29)),
                runtime.outbox().consumePendingEntityEvents());
    }

    @Test
    void armosKnightRunsTheRomJumpAndBounceStates() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        EntitySpriteDefinition definition = catalog.forEntityType(0x88, EntityRoomLoader.RoomTable.INDOORS_B);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x88)
                        .at(0x50, 0x60)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom)))
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x50, 0x60, () -> 0));
        for (int frame = 2; frame <= 0x31; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x50, 0x60, () -> 0));
        }
        for (int frame = 0x32; frame <= 0xB1; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x50, 0x60, () -> 0));
        }
        assertEquals(3, runtime.bosses().armosKnightState(0));
        assertEquals(0x50, runtime.bosses().armosKnightTransitionCountdown(0));

        for (int frame = 0xB2; frame <= 0x101; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x50, 0x60, () -> 0));
        }
        assertEquals(4, runtime.bosses().armosKnightState(0));
        assertEquals(0x30, runtime.bosses().armosKnightSpeedZ(0));
        assertEquals(
                List.of(new EntityCombatEvent(0, 0x88, 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x24)),
                runtime.outbox().consumePendingEntityEvents());

        for (int frame = 0x102; frame <= 0x119; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x50, 0x60, () -> 0));
        }
        assertEquals(5, runtime.bosses().armosKnightState(0));
        assertEquals(0x10, runtime.bosses().armosKnightTransitionCountdown(0));

        for (int frame = 0x11A; frame <= 0x130; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x50, 0x60, () -> 0));
        }
        assertEquals(7, runtime.bosses().armosKnightState(0));
        assertEquals(0x30, runtime.bosses().armosKnightTransitionCountdown(0));
        assertEquals(0x40, runtime.bosses().armosKnightPrivateCountdown1(0));
        assertEquals(
                List.of(new EntityCombatEvent(0, 0x88, 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x0B)),
                runtime.outbox().consumePendingEntityEvents());
        assertEquals(
                List.of(new ScreenShakeRequest(0, 0x30, 0x04)), runtime.outbox().consumePendingScreenShakeRequests());

        for (int frame = 0x131; frame <= 0x160; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x50, 0x60, () -> 0));
        }
        assertEquals(2, runtime.bosses().armosKnightState(0));
        assertEquals(0, runtime.bosses().armosKnightTransitionCountdown(0));
        assertEquals(List.of(new LinkMotionBlockRequest(0)), runtime.outbox().consumePendingLinkMotionBlockRequests());
        assertEquals(List.of(new LinkFallenPoseRequest(0)), runtime.outbox().consumePendingLinkFallenPoseRequests());
    }

    @Test
    void armosKnightDeathUsesItsRomBossCountdownsBeforeDidKillEnemy() {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(syntheticRom()));
        RoomEntity source = new RoomEntityBuilder()
                .slot(15)
                .sourceLoadOrder(0)
                .type(0x88)
                .at(0x50, 0x60)
                .status(EntityStatus.DYING)
                .spriteDefinition(EntitySpriteDefinition.unsupported(0x88))
                .spriteVariant(-1)
                .z(0x05)
                .build();
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshotAt(source))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));
        assertEquals(0xA0, runtime.enemies().transitionCountdown(15));
        assertEquals(0xFF, new EntityEnemyInspectionQueries(runtime.enemies()).enemyFlashCountdown(15));
        assertEquals(
                EntityStatus.DYING,
                runtime.inspection().snapshot().slots().get(15).status());

        for (int frame = 1; frame <= 0xA0; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0, 0, () -> 0));
        }
        assertEquals(0xC0, runtime.enemies().transitionCountdown(15));
        assertEquals(0xFF, new EntityEnemyInspectionQueries(runtime.enemies()).enemyFlashCountdown(15));

        for (int frame = 0xA1; frame <= 0x160; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0, 0, () -> 0));
        }

        RoomEntity drop = runtime.inspection().snapshot().slots().get(14);
        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(15).status());
        assertEquals(EntityStatus.ACTIVE, drop.status());
        assertEquals(0x30, drop.type());
        assertEquals(0x03, drop.spriteVariant());
        assertEquals(0x50, drop.x());
        assertEquals(0x60, drop.y());
        assertEquals(0x05, drop.z());
        assertEquals(
                List.of(new EntityCombatEvent(15, 0x88, 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x1A)),
                runtime.outbox().consumePendingEntityEvents());
    }
}
