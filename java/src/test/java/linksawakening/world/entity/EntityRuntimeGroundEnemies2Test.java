package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.LinkFallenPoseRequest;
import linksawakening.world.entity.event.LinkFinalPositionRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.ScreenShakeRequest;
import linksawakening.world.entity.event.TransientVfxRequest;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityRuntimeGroundEnemies2Test extends EntityRuntimeTestSupport {

    @Test
    void armosStatueRunsItsPostMovementRomBackgroundCollisionPass() {
        EntitySpriteDefinition definition = pairDefinition(0x0F, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x0F)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 64, 64, sequence(0x00)));
        List<Integer> directions = new ArrayList<>();
        runtime.configuration().setBackgroundInteraction((entity, direction, nextX, nextY) -> {
            directions.add(direction);
            return EntityBackgroundCollisionResult.blocked(direction, 0x01, 0x00, nextX, nextY);
        });

        runtime.tick(EntityTestFrames.frame(runtime, 1, 120, 120, sequence(0x00)));
        int beforeBlockedStep = runtime.inspection().snapshot().slots().get(0).x();
        runtime.tick(EntityTestFrames.frame(runtime, 2, 120, 120, sequence(0x00)));

        assertEquals(List.of(EntityBackgroundCollisionResult.LEFT), directions);
        assertEquals(
                beforeBlockedStep,
                runtime.inspection().snapshot().slots().get(0).x());
    }

    @Test
    void armosStatueDoesNotWakeForNonInteractiveLiveLinkMotion() {
        EntitySpriteDefinition definition = pairDefinition(0x0F, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x0F)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                0,
                64,
                64,
                sequence(0x00),
                null,
                new EnemyProjectileCollision.LinkState(64, 64, 0, 0x02, 0, false)));

        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).armosState(0));
        assertTrue(runtime.consumePendingLinkFinalPositionRequests().isEmpty());
    }

    @Test
    void armosUsesRomActivationFlagsAndOnlyTheActiveStateJoinsCombat() {
        EntitySpriteDefinition definition = pairDefinition(0x0F, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x0F)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        assertEquals(0x92, runtime.enemies().physicsFlags(0));
        assertTrue(runtime.resolveCombat(new EntitySwordContactFrame(1, 64, 64, false, true, true, 72, 1, 72, 1))
                .isEmpty());

        runtime.tick(EntityTestFrames.frame(runtime, 0, 64, 64, sequence(0x00)));
        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).armosState(0));
        assertEquals(0x18, new EntityEnemyInspectionQueries(runtime.enemies()).enemyFlashCountdown(0));
        assertEquals(0x92, runtime.enemies().physicsFlags(0));
        assertTrue(runtime.resolveCombat(new EntitySwordContactFrame(1, 64, 64, false, true, true, 72, 1, 72, 1))
                .isEmpty());

        for (int frame = 1; frame <= 0x30; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, sequence(0x00)));
        }

        assertEquals(2, new EntityEnemyInspectionQueries(runtime.enemies()).armosState(0));
        assertEquals(0x12, runtime.enemies().physicsFlags(0));

        RoomEntity active = runtime.inspection().snapshot().slots().get(0);
        List<EntityCombatEvent> contact = runtime.resolveCombat(
                new EntitySwordContactFrame(1, active.x(), active.y(), false, true, false, 0, 0, 0, 0));
        assertEquals(1, contact.size());
        assertEquals(0x10, contact.get(0).linkDamage());

        List<EntityCombatEvent> sword = runtime.resolveCombat(
                new EntitySwordContactFrame(0, 0, 0, true, true, true, active.x() + 8, 1, active.y() + 8, 1));
        assertEquals(1, sword.size());
        assertTrue(sword.get(0).swordHit());
        assertEquals(1, sword.get(0).enemyDamage());
        assertEquals(3, runtime.enemies().enemyHealth(0));
        assertEquals(0x0A, runtime.enemies().enemyIgnoreHitsCountdown(0));
        assertTrue(new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilActive(0));

        runtime.tick(EntityTestFrames.frame(runtime, 0x31, 0, 0, sequence(0x00)));
        assertEquals(0x09, runtime.enemies().enemyIgnoreHitsCountdown(0));
        assertTrue(new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilActive(0));
    }

    @Test
    void armosStateTwoLoadsItsContiguousRomSpeedTables() {
        EntitySpriteDefinition definition = pairDefinition(0x0F, 2);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x0F)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 64, 64, sequence(0x00)));
        for (int frame = 1; frame <= 0x30; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, sequence(0x00)));
        }
        runtime.tick(EntityTestFrames.frame(runtime, 0x31, 120, 120, sequence(0x04)));

        assertEquals(2, new EntityEnemyInspectionQueries(runtime.enemies()).armosState(0));
        assertEquals(0x24, new EntityEnemyInspectionQueries(runtime.enemies()).armosTransitionCountdown(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).armosSpeedX(0));
        assertEquals(0x08, new EntityEnemyInspectionQueries(runtime.enemies()).armosSpeedY(0));
    }

    @Test
    void armosKnightWakesChargesAndEnablesItsRomHitbox() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(0x88, EntityRoomLoader.RoomTable.INDOORS_B);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x88)
                        .at(0x50, 0x60)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(new RomEnemyCombatTables(rom))
                .build();

        assertEquals(0x84, runtime.enemies().physicsFlags(0));
        assertEquals(0xC4, runtime.options1(0));
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
        assertEquals(0x84, runtime.options1(0));
    }

    @Test
    void armosKnightRequestsCopyingLinksFinalPositionOnActiveCollision() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(0x88, EntityRoomLoader.RoomTable.INDOORS_B);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x88)
                        .at(0x50, 0x60)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(new RomEnemyCombatTables(rom))
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
                null,
                new EnemyProjectileCollision.LinkState(0x50, 0x60, 0x04, 0x00, 0, false)));

        assertEquals(
                List.of(new LinkFinalPositionRequest(0, true, true, false)),
                runtime.consumePendingLinkFinalPositionRequests());
    }

    @Test
    void armosKnightDamageThresholdSpawnsRomRubbleWithPoofAndNoise() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(0x88, EntityRoomLoader.RoomTable.INDOORS_B);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x88)
                        .at(0x50, 0x60)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(new RomEnemyCombatTables(rom))
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
            List<EntityCombatEvent> hitEvents = runtime.resolveCombat(new EntityVerticalContactFrame(
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
        assertEquals(0x0A, runtime.options1(rubble.slot()));
        assertEquals(
                List.of(new TransientVfxRequest(TransientVfxType.POOF, 0x57, 0x50)),
                runtime.enemies().transientVfxRequests());
        assertEquals(
                List.of(new EntityCombatEvent(
                        rubble.slot(), 0x05, 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x29)),
                runtime.consumePendingEntityEvents());
    }

    @Test
    void armosKnightRunsTheRomJumpAndBounceStates() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(0x88, EntityRoomLoader.RoomTable.INDOORS_B);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x88)
                        .at(0x50, 0x60)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(new RomEnemyCombatTables(rom))
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
                runtime.consumePendingEntityEvents());

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
                runtime.consumePendingEntityEvents());
        assertEquals(List.of(new ScreenShakeRequest(0, 0x30, 0x04)), runtime.consumePendingScreenShakeRequests());

        for (int frame = 0x131; frame <= 0x160; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x50, 0x60, () -> 0));
        }
        assertEquals(2, runtime.bosses().armosKnightState(0));
        assertEquals(0, runtime.bosses().armosKnightTransitionCountdown(0));
        assertEquals(List.of(new LinkMotionBlockRequest(0)), runtime.consumePendingLinkMotionBlockRequests());
        assertEquals(List.of(new LinkFallenPoseRequest(0)), runtime.consumePendingLinkFallenPoseRequests());
    }

    @Test
    void tektiteLandsWithTheRomCountdownThenStartsItsNextJump() {
        EntitySpriteDefinition definition = pairDefinition(0x0D, 2);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x0D)
                .at(64, 64)
                .spriteDefinition(definition)
                .z(0x80)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();
        IntSupplier randomBytes = sequence(0x00, 0x00, 0x00, 0x00);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, randomBytes));
        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).tektiteState(0));
        assertEquals(0x10, new EntityEnemyInspectionQueries(runtime.enemies()).tektiteTransitionCountdown(0));
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).z());
        assertEquals(1, runtime.inspection().snapshot().slots().get(0).spriteVariant());

        for (int frame = 1; frame <= 16; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, randomBytes));
        }
        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).tektiteState(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).tektiteTransitionCountdown(0));

        for (int frame = 17; frame <= 31; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, randomBytes));
        }
        runtime.tick(EntityTestFrames.frame(runtime, 32, 120, 120, randomBytes));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).tektiteState(0));
        assertEquals(0x10, new EntityEnemyInspectionQueries(runtime.enemies()).tektiteSpeedZ(0));
        assertEquals(0x10, new EntityEnemyInspectionQueries(runtime.enemies()).tektiteSpeedX(0));
        assertEquals(0x10, new EntityEnemyInspectionQueries(runtime.enemies()).tektiteSpeedY(0));
        assertEquals(1, runtime.inspection().snapshot().slots().get(0).z());
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void tektiteReversesAndHalvesSpeedXAfterHorizontalWallCollision() {
        EntitySpriteDefinition definition = pairDefinition(0x0D, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x0D)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .z(0x80)
                        .build()))
                .build();
        IntSupplier randomBytes = sequence(0x00, 0x00, 0x00, 0x00);

        for (int frame = 0; frame <= 32; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, randomBytes));
        }
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).tektiteState(0));
        assertEquals(0x10, new EntityEnemyInspectionQueries(runtime.enemies()).tektiteSpeedX(0));
        assertEquals(0x10, new EntityEnemyInspectionQueries(runtime.enemies()).tektiteSpeedY(0));

        RoomEntityBackgroundCollision rightWall = (entity, direction, nextX, nextY) -> direction == 0;
        runtime.tick(EntityTestFrames.frame(runtime, 33, 120, 120, randomBytes, rightWall));

        assertEquals(64, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(65, runtime.inspection().snapshot().slots().get(0).y());
        assertEquals(0xF8, new EntityEnemyInspectionQueries(runtime.enemies()).tektiteSpeedX(0));
        assertEquals(0x10, new EntityEnemyInspectionQueries(runtime.enemies()).tektiteSpeedY(0));
    }

    @Test
    void tektiteVerticalWallCollisionUsesTheRomXSpeedHelper() {
        EntitySpriteDefinition definition = pairDefinition(0x0D, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x0D)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .z(0x80)
                        .build()))
                .build();
        IntSupplier randomBytes = sequence(0x00, 0x00, 0x00, 0x00);

        for (int frame = 0; frame <= 32; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, randomBytes));
        }
        RoomEntityBackgroundCollision downWall = (entity, direction, nextX, nextY) -> direction == 3;
        runtime.tick(EntityTestFrames.frame(runtime, 33, 120, 120, randomBytes, downWall));

        assertEquals(65, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).y());
        assertEquals(0xF8, new EntityEnemyInspectionQueries(runtime.enemies()).tektiteSpeedX(0));
        assertEquals(0x10, new EntityEnemyInspectionQueries(runtime.enemies()).tektiteSpeedY(0));
    }

    @Test
    void tektiteAddsTheRomLandingTimerBaseAfterMaskingRandomness() {
        EntitySpriteDefinition definition = pairDefinition(0x0D, 2);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x0D)
                .at(64, 64)
                .spriteDefinition(definition)
                .z(0x80)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x1F)));

        assertEquals(0x2F, new EntityEnemyInspectionQueries(runtime.enemies()).tektiteTransitionCountdown(0));
    }

    @Test
    void tektiteCanReplaceItsRandomJumpDirectionWithTheRomLinkVector() {
        EntitySpriteDefinition definition = pairDefinition(0x0D, 2);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x0D)
                .at(64, 64)
                .spriteDefinition(definition)
                .z(0x80)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();

        IntSupplier randomBytes = sequence(0x00, 0x00, 0x00, 0x01);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, randomBytes));
        for (int frame = 1; frame <= 32; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 80, 72, randomBytes));
        }

        assertEquals(0x14, new EntityEnemyInspectionQueries(runtime.enemies()).tektiteSpeedX(0));
        // GetEntityYDistanceToLink includes the post-launch Z position, so
        // dy is 9 here and the ROM divide loop yields 11.
        assertEquals(0x0B, new EntityEnemyInspectionQueries(runtime.enemies()).tektiteSpeedY(0));
    }

    @Test
    void tektiteUsesTheHealthGroupOneContactAndSwordValues() {
        EntitySpriteDefinition definition = pairDefinition(0x0D, 2);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x0D)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();

        List<EntityCombatEvent> contact =
                runtime.resolveCombat(new EntitySwordContactFrame(1, 64, 64, false, true, false, 0, 0, 0, 0));
        assertEquals(1, contact.size());
        assertEquals(0x04, contact.get(0).linkDamage());

        List<EntityCombatEvent> firstHit =
                runtime.resolveCombat(new EntitySwordContactFrame(0, 120, 120, false, true, true, 72, 1, 72, 1));
        assertEquals(1, firstHit.size());
        assertEquals(0x01, runtime.enemies().enemyHealth(0));
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());
    }

    @Test
    void leeverUsesTheRomHideEmergeAndChaseStateBoundaries() {
        EntitySpriteDefinition definition = pairDefinition(0x0E, 4);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x0E)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();
        IntSupplier randomBytes = sequence(0x00);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, randomBytes));
        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).leeverState(0));
        assertEquals(0x1F, new EntityEnemyInspectionQueries(runtime.enemies()).leeverTransitionCountdown(0));
        assertEquals(-1, runtime.inspection().snapshot().slots().get(0).spriteVariant());

        for (int frame = 1; frame <= 30; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, randomBytes));
        }
        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).leeverState(0));
        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).leeverTransitionCountdown(0));
        assertEquals(1, runtime.inspection().snapshot().slots().get(0).spriteVariant());

        runtime.tick(EntityTestFrames.frame(runtime, 31, 120, 120, randomBytes));
        assertEquals(2, new EntityEnemyInspectionQueries(runtime.enemies()).leeverState(0));
        assertEquals(0x70, new EntityEnemyInspectionQueries(runtime.enemies()).leeverTransitionCountdown(0));

        runtime.tick(EntityTestFrames.frame(runtime, 32, 80, 64, randomBytes));
        assertEquals(0x08, new EntityEnemyInspectionQueries(runtime.enemies()).leeverSpeedX(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).leeverSpeedY(0));
        assertEquals(2, runtime.inspection().snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void leeverInitializationAppliesTheRomHiddenSpriteVariantBeforeItsFirstHandlerPass() {
        EntitySpriteDefinition definition = pairDefinition(0x0E, 4);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x0E)
                .at(64, 64)
                .status(EntityStatus.INIT)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x00)));

        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(-1, runtime.inspection().snapshot().slots().get(0).spriteVariant());
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).leeverState(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).leeverTransitionCountdown(0));
    }

    @Test
    void leeverBurrowsAfterItsChaseTimerAndStartsTheRomHidingWindow() {
        EntitySpriteDefinition definition = pairDefinition(0x0E, 4);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x0E)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();
        IntSupplier randomBytes = sequence(0x00);

        for (int frame = 0; frame <= 143; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 64, randomBytes));
        }
        assertEquals(3, new EntityEnemyInspectionQueries(runtime.enemies()).leeverState(0));
        assertEquals(0x1F, new EntityEnemyInspectionQueries(runtime.enemies()).leeverTransitionCountdown(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).leeverSpeedX(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).leeverSpeedY(0));

        for (int frame = 144; frame <= 173; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 64, randomBytes));
        }
        runtime.tick(EntityTestFrames.frame(runtime, 174, 120, 64, randomBytes));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).leeverState(0));
        assertEquals(0x30, new EntityEnemyInspectionQueries(runtime.enemies()).leeverTransitionCountdown(0));
        assertEquals(0x08, new EntityEnemyInspectionQueries(runtime.enemies()).leeverSpeedX(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).leeverSpeedY(0));

        runtime.tick(EntityTestFrames.frame(runtime, 175, 120, 64, randomBytes));
        assertEquals(-1, runtime.inspection().snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void leeverOnlyUsesItsHealthGroupOneCombatValuesWhileChasing() {
        EntitySpriteDefinition definition = pairDefinition(0x0E, 4);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x0E)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();
        IntSupplier randomBytes = sequence(0x00);

        List<EntityCombatEvent> hiddenContact =
                runtime.resolveCombat(new EntitySwordContactFrame(1, 64, 64, false, true, false, 0, 0, 0, 0));
        assertTrue(hiddenContact.isEmpty());

        for (int frame = 0; frame <= 31; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, randomBytes));
        }
        List<EntityCombatEvent> contact =
                runtime.resolveCombat(new EntitySwordContactFrame(1, 64, 64, false, true, false, 0, 0, 0, 0));
        assertEquals(1, contact.size());
        assertEquals(0x04, contact.get(0).linkDamage());

        List<EntityCombatEvent> sword =
                runtime.resolveCombat(new EntitySwordContactFrame(0, 120, 120, false, true, true, 72, 1, 72, 1));
        assertEquals(1, sword.size());
        assertEquals(1, runtime.enemies().enemyHealth(0));
    }

    @Test
    void leeverSwordHitConfiguresBankFourRecoilBeforeItsStateMovement() {
        EntitySpriteDefinition definition = pairDefinition(0x0E, 4);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x0E)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        for (int frame = 0; frame <= 31; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, sequence(0x00)));
        }
        assertEquals(2, new EntityEnemyInspectionQueries(runtime.enemies()).leeverState(0));

        List<EntityCombatEvent> events =
                runtime.resolveCombat(new EntitySwordContactFrame(0, 120, 120, false, true, true, 72, 1, 72, 1));

        assertEquals(1, events.size());
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, events.get(0).soundChannel());
        assertEquals(0x03, events.get(0).soundId());
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(1, runtime.enemies().enemyHealth(0));
        assertEquals(0x0A, runtime.enemies().enemyIgnoreHitsCountdown(0));
        assertTrue(new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilActive(0));
        assertEquals(0xD0, new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilSpeedX(0));
        assertEquals(0xD0, new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilSpeedY(0));

        runtime.tick(EntityTestFrames.frame(runtime, 1, 120, 120, sequence(0x00)));

        RoomEntity afterRecoil = runtime.inspection().snapshot().slots().get(0);
        assertEquals(61, afterRecoil.x());
        assertEquals(61, afterRecoil.y());
        assertEquals(0x09, runtime.enemies().enemyIgnoreHitsCountdown(0));
    }
}
