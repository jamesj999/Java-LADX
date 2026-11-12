package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityRuntimeGenie1Test extends EntityRuntimeTestSupport {

    @Test
    void genieInitializesTheSourceJarCombatFields() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x5C)
                        .at(0x50, 0x68)
                        .spriteDefinition(EntitySpriteDefinition.unsupported(0x5C))
                        .build()))
                .indoorRoom(true)
                .build();

        assertEquals(0x06, runtime.enemies().enemyHealth(0));
        assertEquals(0x91, runtime.enemies().physicsFlags(0));
        assertEquals(0x8C, EntityRuntimeFixture.enemies(runtime).hitboxFlagsForTest(0));
        assertEquals(0, EntityRuntimeFixture.bosses(runtime).geniePrivateState1ForTest(0));

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x50, 0x70, () -> 0));

        assertEquals(0x20, runtime.enemies().enemyHealth(0));
        assertEquals(0x81, runtime.enemies().physicsFlags(0));
        assertEquals(0x80, EntityRuntimeFixture.enemies(runtime).hitboxFlagsForTest(0));
    }

    @Test
    void genieInitAppliesTheSourceEightPixelXShift() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x5C)
                        .at(0x48, 0x30)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(EntitySpriteDefinition.unsupported(0x5C))
                        .build()))
                .indoorRoom(true)
                .build();

        assertEquals(0x50, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(0x8C, EntityRuntimeFixture.enemies(runtime).hitboxFlagsForTest(0));
    }

    @Test
    void genieJarActivatesWhenLinkCrossesTheArenaThreshold() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x5C)
                        .at(0x50, 0x68)
                        .spriteDefinition(EntitySpriteDefinition.unsupported(0x5C))
                        .build()))
                .indoorRoom(true)
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x50, 0x70, () -> 0));
        assertEquals(0, EntityRuntimeFixture.bosses(runtime).genieActiveStateForTest(0));
        assertEquals(0, runtime.enemies().transitionCountdown(0));

        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x50, 0x6F, () -> 0));
        assertEquals(1, EntityRuntimeFixture.bosses(runtime).genieActiveStateForTest(0));
        assertEquals(0xFF, runtime.enemies().transitionCountdown(0));
        assertEquals(0x68, runtime.inspection().snapshot().slots().get(0).y());
    }

    @Test
    void activeGenieJarReachesTheCenterAndSpawnsItsAppearanceController() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x5C)
                        .at(0x50, 0x48)
                        .spriteDefinition(EntitySpriteDefinition.unsupported(0x5C))
                        .build()))
                .indoorRoom(true)
                .build();
        EntityRuntimeFixture.debug(runtime).setGenieActiveStateForTest(0, 1);
        EntityRuntimeFixture.debug(runtime).setTransitionCountdownForTest(0, 2);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x50, 0x6F, () -> 0));

        assertEquals(1, EntityRuntimeFixture.bosses(runtime).geniePrivateState3ForTest(0));
        RoomEntity jar = runtime.inspection().snapshot().slots().get(0);
        RoomEntity appearance = runtime.inspection().snapshot().loadedEntities().stream()
                .filter(entity -> entity.slot() != 0 && entity.type() == 0x5C)
                .findFirst()
                .orElseThrow();
        assertEquals(jar.x(), appearance.x());
        assertEquals((jar.y() - 0x26) & 0xFF, appearance.y());
        assertEquals(2, EntityRuntimeFixture.bosses(runtime).geniePrivateState1ForTest(appearance.slot()));
        assertEquals(0x47, runtime.enemies().transitionCountdown(appearance.slot()));
        assertTrue(runtime.consumePendingEntityEvents().stream()
                .anyMatch(event ->
                        event.soundChannel() == EntityCombatEvent.SoundChannel.JINGLE && event.soundId() == 0x06));
    }

    @Test
    void activeGenieJarFallsAndBouncesWhileMovingTowardTheCenter() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x5C)
                        .at(0x30, 0x48)
                        .spriteDefinition(EntitySpriteDefinition.unsupported(0x5C))
                        .build()))
                .indoorRoom(true)
                .build();
        EntityRuntimeFixture.debug(runtime).setGenieActiveStateForTest(0, 1);
        EntityRuntimeFixture.debug(runtime).setTransitionCountdownForTest(0, 0x20);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x50, 0x6F, () -> 0));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x50, 0x6F, () -> 0));
        assertTrue(runtime.consumePendingEntityEvents().stream()
                .anyMatch(event ->
                        event.soundChannel() == EntityCombatEvent.SoundChannel.JINGLE && event.soundId() == 0x20));

        runtime.tick(EntityTestFrames.frame(runtime, 2, 0x50, 0x6F, () -> 0));
        assertEquals(1, runtime.inspection().snapshot().slots().get(0).z());
    }

    @Test
    void genieAppearanceCountdownSixSpawnsTheFirstCycleBody() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x5C)
                        .at(0x50, 0x0A)
                        .spriteDefinition(EntitySpriteDefinition.unsupported(0x5C))
                        .build()))
                .indoorRoom(true)
                .build();
        EntityRuntimeFixture.debug(runtime).setGeniePrivateState1ForTest(0, 2);
        EntityRuntimeFixture.debug(runtime).setTransitionCountdownForTest(0, 7);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x50, 0x6F, () -> 0));

        RoomEntity body = runtime.inspection().snapshot().loadedEntities().stream()
                .filter(entity -> entity.slot() != 0 && entity.type() == 0x5C)
                .findFirst()
                .orElseThrow();
        assertEquals(0x50, body.x());
        assertEquals(0x14, body.y());
        assertEquals(1, EntityRuntimeFixture.bosses(runtime).geniePrivateState1ForTest(body.slot()));
        assertEquals(0x40, runtime.enemies().transitionCountdown(body.slot()));
        assertEquals(0x01, runtime.enemies().physicsFlags(body.slot()));
        assertEquals(0x8C, EntityRuntimeFixture.enemies(runtime).hitboxFlagsForTest(body.slot()));
        assertEquals(1, runtime.enemies().enemyIgnoreHitsCountdown(body.slot()));
    }

    @Test
    void firstGenieBodyOpensJarCycleDialogWhenTheOriginalJarStillExists() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(
                        new RoomEntityBuilder()
                                .sourceLoadOrder(0)
                                .type(0x5C)
                                .at(0x50, 0x48)
                                .spriteDefinition(EntitySpriteDefinition.unsupported(0x5C))
                                .build(),
                        new RoomEntityBuilder()
                                .slot(1)
                                .sourceLoadOrder(1)
                                .type(0x5C)
                                .at(0x50, 0x22)
                                .spriteDefinition(EntitySpriteDefinition.unsupported(0x5C))
                                .build()))
                .indoorRoom(true)
                .build();
        EntityRuntimeFixture.debug(runtime).setGeniePrivateState1ForTest(1, 1);
        EntityRuntimeFixture.debug(runtime).setTransitionCountdownForTest(1, 2);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x50, 0x70, () -> 0));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x50, 0x70, () -> 0));

        assertEquals(1, EntityRuntimeFixture.bosses(runtime).genieActiveStateForTest(1));
        assertEquals(0xFF, EntityRuntimeFixture.bosses(runtime).geniePrivateCountdown1ForTest(1));
        assertEquals(List.of(new DialogRequest(0, 0x52)), runtime.consumePendingDialogRequests());
    }

    @Test
    void firstGenieBodyEntersFinalFightWhenTheJarNoLongerExists() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x5C)
                        .at(0x50, 0x22)
                        .spriteDefinition(EntitySpriteDefinition.unsupported(0x5C))
                        .build()))
                .indoorRoom(true)
                .build();
        EntityRuntimeFixture.debug(runtime).setGeniePrivateState1ForTest(0, 1);
        EntityRuntimeFixture.debug(runtime).setTransitionCountdownForTest(0, 1);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x50, 0x70, () -> 0));

        assertEquals(4, EntityRuntimeFixture.bosses(runtime).genieActiveStateForTest(0));
        assertEquals(0x08, runtime.enemies().enemyHealth(0));
        assertEquals(0xFF, EntityRuntimeFixture.bosses(runtime).geniePrivateCountdown1ForTest(0));
        assertEquals(List.of(new DialogRequest(0, 0x53)), runtime.consumePendingDialogRequests());
    }

    @Test
    void genieBodyStateOneSpawnsTheSourceSideProjectileAtItsNextDelay() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x5C)
                        .at(0x50, 0x40)
                        .spriteDefinition(EntitySpriteDefinition.unsupported(0x5C))
                        .build()))
                .indoorRoom(true)
                .build();
        EntityRuntimeFixture.debug(runtime).setGeniePrivateState1ForTest(0, 1);
        EntityRuntimeFixture.debug(runtime).setGenieActiveStateForTest(0, 1);
        EntityRuntimeFixture.debug(runtime).setTransitionCountdownForTest(0, 1);
        EntityRuntimeFixture.debug(runtime).setGeniePrivateCountdown1ForTest(0, 0xFF);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x30, 0x70, () -> 0));

        assertEquals(0x10, runtime.enemies().transitionCountdown(0));
        RoomEntity projectile = runtime.inspection().snapshot().loadedEntities().stream()
                .filter(entity -> entity.slot() != 0 && entity.type() == 0x5C)
                .findFirst()
                .orElseThrow();
        assertEquals(3, EntityRuntimeFixture.bosses(runtime).geniePrivateState1ForTest(projectile.slot()));
        assertEquals(0x44, projectile.x());
        assertEquals(0x2C, projectile.y());
        assertEquals(0x20, EntityRuntimeFixture.bosses(runtime).genieSpeedZForTest(projectile.slot()));
        assertEquals(0x0C, EntityRuntimeFixture.bosses(runtime).genieSpeedXForTest(projectile.slot()));
        assertEquals(0x42, runtime.enemies().physicsFlags(projectile.slot()));

        runtime.clearEntity(0);
        for (int frame = 1; frame <= 80; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x30, 0x70, () -> 0));
        }
        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(projectile.slot()).status());
    }

    @Test
    void genieBodyPrivateCountdownSpawnsAimedFireballAndAdvancesInertia() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x5C)
                        .at(0x50, 0x40)
                        .spriteDefinition(EntitySpriteDefinition.unsupported(0x5C))
                        .build()))
                .indoorRoom(true)
                .build();
        EntityRuntimeFixture.debug(runtime).setGeniePrivateState1ForTest(0, 1);
        EntityRuntimeFixture.debug(runtime).setGenieActiveStateForTest(0, 1);
        EntityRuntimeFixture.debug(runtime).setTransitionCountdownForTest(0, 0x20);
        EntityRuntimeFixture.debug(runtime).setGeniePrivateCountdown1ForTest(0, 1);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x30, 0x70, () -> 0));

        assertEquals(1, runtime.inspection().entityInertia(0));
        assertEquals(0x30, EntityRuntimeFixture.bosses(runtime).geniePrivateCountdown1ForTest(0));
        assertEquals(0x10, EntityRuntimeFixture.bosses(runtime).geniePrivateCountdown2ForTest(0));
        RoomEntity fireball = runtime.inspection().snapshot().loadedEntities().stream()
                .filter(entity -> entity.slot() != 0 && entity.type() == 0x5C)
                .findFirst()
                .orElseThrow();
        assertEquals(4, EntityRuntimeFixture.bosses(runtime).geniePrivateState1ForTest(fireball.slot()));
        assertEquals(0x44, fireball.x());
        assertEquals(0x40, fireball.y());
        assertEquals(0x24, EntityRuntimeFixture.bosses(runtime).genieSpeedZForTest(fireball.slot()));
        assertEquals(0x12, runtime.enemies().physicsFlags(fireball.slot()));
        assertTrue(runtime.consumePendingEntityEvents().stream()
                .anyMatch(event ->
                        event.soundChannel() == EntityCombatEvent.SoundChannel.NOISE && event.soundId() == 0x28));

        runtime.clearEntity(0);
        for (int frame = 1; frame <= 80; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x30, 0x70, () -> 0));
        }
        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(fireball.slot()).status());
    }

    @Test
    void genieFireballRunsItsSourceLinkCollisionBeforeMovement() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x5C)
                        .at(0x50, 0x50)
                        .spriteDefinition(EntitySpriteDefinition.unsupported(0x5C))
                        .build()))
                .indoorRoom(true)
                .build();
        EntityRuntimeFixture.debug(runtime).setGeniePrivateState1ForTest(0, 4);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x50, 0x50, () -> 0));

        assertFalse(runtime.consumePendingLinkFinalPositionRequests().isEmpty());
    }

    @Test
    void genieBodyFinishesEightAttackCyclesAndStartsReturningToTheJar() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x5C)
                        .at(0x50, 0x40)
                        .spriteDefinition(EntitySpriteDefinition.unsupported(0x5C))
                        .build()))
                .indoorRoom(true)
                .build();
        EntityRuntimeFixture.debug(runtime).setGeniePrivateState1ForTest(0, 1);
        EntityRuntimeFixture.debug(runtime).setGenieActiveStateForTest(0, 1);
        EntityRuntimeFixture.debug(runtime).setTransitionCountdownForTest(0, 1);
        EntityRuntimeFixture.debug(runtime).setGeniePrivateCountdown1ForTest(0, 0x20);
        EntityRuntimeFixture.debug(runtime).setEntityInertiaForTest(0, 8);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x30, 0x70, () -> 0));

        assertEquals(2, EntityRuntimeFixture.bosses(runtime).genieActiveStateForTest(0));
        assertEquals(0x30, runtime.enemies().transitionCountdown(0));
        assertEquals(1, runtime.inspection().snapshot().loadedEntities().size());
    }

    @Test
    void returningGenieBodyHomesToTheJarAndStartsItsDisappearDelay() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(
                        new RoomEntityBuilder()
                                .sourceLoadOrder(0)
                                .type(0x5C)
                                .at(0x50, 0x48)
                                .spriteDefinition(EntitySpriteDefinition.unsupported(0x5C))
                                .build(),
                        new RoomEntityBuilder()
                                .slot(1)
                                .sourceLoadOrder(1)
                                .type(0x5C)
                                .at(0x50, 0x28)
                                .spriteDefinition(EntitySpriteDefinition.unsupported(0x5C))
                                .build()))
                .indoorRoom(true)
                .build();
        EntityRuntimeFixture.debug(runtime).setGeniePrivateState1ForTest(1, 1);
        EntityRuntimeFixture.debug(runtime).setTransitionCountdownForTest(1, 0x20);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x30, 0x70, () -> 0));
        EntityRuntimeFixture.debug(runtime).setGenieActiveStateForTest(1, 2);

        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x30, 0x70, () -> 0));

        assertEquals(3, EntityRuntimeFixture.bosses(runtime).genieActiveStateForTest(1));
        assertEquals(0x10, runtime.enemies().transitionCountdown(1));
    }

    @Test
    void disappearingGenieBodySpawnsTheHighBitJarControllerAtCountdownFour() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x5C)
                        .at(0x50, 0x28)
                        .spriteDefinition(EntitySpriteDefinition.unsupported(0x5C))
                        .build()))
                .indoorRoom(true)
                .build();
        EntityRuntimeFixture.debug(runtime).setGeniePrivateState1ForTest(0, 1);
        EntityRuntimeFixture.debug(runtime).setGenieActiveStateForTest(0, 3);
        EntityRuntimeFixture.debug(runtime).setTransitionCountdownForTest(0, 5);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x30, 0x70, () -> 0));

        RoomEntity controller = runtime.inspection().snapshot().loadedEntities().stream()
                .filter(entity -> entity.slot() != 0 && entity.type() == 0x5C)
                .findFirst()
                .orElseThrow();
        assertEquals(2, EntityRuntimeFixture.bosses(runtime).geniePrivateState1ForTest(controller.slot()));
        assertEquals(0xC7, runtime.enemies().transitionCountdown(controller.slot()));
        assertEquals(0x50, controller.x());
        assertEquals(0x28, controller.y());
        assertTrue(runtime.consumePendingEntityEvents().stream()
                .anyMatch(event ->
                        event.soundChannel() == EntityCombatEvent.SoundChannel.JINGLE && event.soundId() == 0x1F));
    }

    @Test
    void vulnerableGenieBodyHitFlashStartsTheMirroredDashPhase() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x5C)
                        .at(0x40, 0x40)
                        .spriteDefinition(EntitySpriteDefinition.unsupported(0x5C))
                        .build()))
                .indoorRoom(true)
                .build();
        EntityRuntimeFixture.debug(runtime).setGeniePrivateState1ForTest(0, 1);
        EntityRuntimeFixture.debug(runtime).setGenieActiveStateForTest(0, 4);
        EntityRuntimeFixture.debug(runtime).setEnemyFlashCountdownForTest(0, 3);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x70, 0x70, () -> 0));

        assertEquals(5, EntityRuntimeFixture.bosses(runtime).genieActiveStateForTest(0));
        assertEquals(0x80, runtime.enemies().transitionCountdown(0));
        assertEquals(0x0C, EntityRuntimeFixture.enemies(runtime).hitboxFlagsForTest(0));
        assertEquals(0x81, runtime.options1(0));
    }

    @Test
    void genieDashStoresThePositionMirroredAcrossTheArenaCenter() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x5C)
                        .at(0x40, 0x40)
                        .spriteDefinition(EntitySpriteDefinition.unsupported(0x5C))
                        .build()))
                .indoorRoom(true)
                .build();
        EntityRuntimeFixture.debug(runtime).setGeniePrivateState1ForTest(0, 1);
        EntityRuntimeFixture.debug(runtime).setGenieActiveStateForTest(0, 5);
        EntityRuntimeFixture.debug(runtime).setTransitionCountdownForTest(0, 0x20);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x70, 0x70, () -> 0));

        RoomEntity moved = runtime.inspection().snapshot().slots().get(0);
        assertEquals(
                (0x90 - moved.y()) & 0xFF, EntityRuntimeFixture.bosses(runtime).geniePrivateState3ForTest(0));
        assertEquals(
                (0xA0 - moved.x()) & 0xFF, EntityRuntimeFixture.bosses(runtime).geniePrivateState4ForTest(0));
        assertEquals(0x41, runtime.enemies().physicsFlags(0));
    }

    @Test
    void genieDashCountdownZeroTeleportsToMirrorAndLaunchesAnotherFireball() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x5C)
                        .at(0x40, 0x40)
                        .spriteDefinition(EntitySpriteDefinition.unsupported(0x5C))
                        .build()))
                .indoorRoom(true)
                .build();
        EntityRuntimeFixture.debug(runtime).setGeniePrivateState1ForTest(0, 1);
        EntityRuntimeFixture.debug(runtime).setGenieActiveStateForTest(0, 5);
        EntityRuntimeFixture.debug(runtime).setTransitionCountdownForTest(0, 1);
        EntityRuntimeFixture.debug(runtime).setGeniePrivateState3ForTest(0, 0x60);
        EntityRuntimeFixture.debug(runtime).setGeniePrivateState4ForTest(0, 0x70);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x20, 0x20, () -> 0));

        RoomEntity body = runtime.inspection().snapshot().slots().get(0);
        assertEquals(4, EntityRuntimeFixture.bosses(runtime).genieActiveStateForTest(0));
        assertEquals(0x70, body.x());
        assertEquals(0x60, body.y());
        assertEquals(0x01, runtime.enemies().physicsFlags(0));
        assertEquals(0, runtime.enemies().enemyIgnoreHitsCountdown(0));
        RoomEntity fireball = runtime.inspection().snapshot().loadedEntities().stream()
                .filter(entity -> entity.slot() != 0 && entity.type() == 0x5C)
                .findFirst()
                .orElseThrow();
        assertEquals(4, EntityRuntimeFixture.bosses(runtime).geniePrivateState1ForTest(fireball.slot()));
        assertTrue(runtime.consumePendingEntityEvents().stream()
                .anyMatch(event ->
                        event.soundChannel() == EntityCombatEvent.SoundChannel.NOISE && event.soundId() == 0x28));
    }

    @Test
    void defeatedGenieRunsBossExplosionTimersAndDropsTheHeartContainer() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x5C)
                        .at(0x90, 0x10)
                        .status(EntityStatus.DYING)
                        .spriteDefinition(EntitySpriteDefinition.unsupported(0x5C))
                        .build()))
                .indoorRoom(true)
                .build();
        EntityRuntimeFixture.debug(runtime).setGeniePrivateState1ForTest(0, 1);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x50, 0x50, () -> 0));
        assertEquals(1, EntityRuntimeFixture.bosses(runtime).genieActiveStateForTest(0));
        assertEquals(0x40, runtime.enemies().transitionCountdown(0));

        EntityRuntimeFixture.debug(runtime).setTransitionCountdownForTest(0, 1);
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x50, 0x50, () -> 0));
        assertEquals(2, EntityRuntimeFixture.bosses(runtime).genieActiveStateForTest(0));
        assertEquals(0xA0, runtime.enemies().transitionCountdown(0));

        EntityRuntimeFixture.debug(runtime).setTransitionCountdownForTest(0, 1);
        runtime.tick(EntityTestFrames.frame(runtime, 2, 0x50, 0x50, () -> 0));

        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(0).status());
        RoomEntity heart = runtime.inspection().snapshot().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x36)
                .findFirst()
                .orElseThrow();
        assertEquals(0x88, heart.x());
        assertEquals(0x20, heart.y());
        assertTrue(runtime.consumePendingEntityEvents().stream()
                .anyMatch(event ->
                        event.soundChannel() == EntityCombatEvent.SoundChannel.NOISE && event.soundId() == 0x1A));
    }

    @Test
    void defeatedGenieExplosionTickCreatesAPoofAndDestroyedNoise() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x5C)
                        .at(0x50, 0x40)
                        .status(EntityStatus.DYING)
                        .spriteDefinition(EntitySpriteDefinition.unsupported(0x5C))
                        .build()))
                .indoorRoom(true)
                .build();
        EntityRuntimeFixture.debug(runtime).setGeniePrivateState1ForTest(0, 1);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x50, 0x50, () -> 0));
        EntityRuntimeFixture.debug(runtime).setGenieActiveStateForTest(0, 2);
        EntityRuntimeFixture.debug(runtime).setTransitionCountdownForTest(0, 9);

        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x50, 0x50, () -> 0));

        assertFalse(runtime.enemies().transientVfxRequests().isEmpty());
        assertTrue(runtime.consumePendingEntityEvents().stream()
                .anyMatch(event ->
                        event.soundChannel() == EntityCombatEvent.SoundChannel.NOISE && event.soundId() == 0x13));
    }

    @Test
    void defeatedGeniePlaysTheGlobalBossDeathCryAfterThreeMoreTicks() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x5C)
                        .at(0x50, 0x40)
                        .status(EntityStatus.DYING)
                        .spriteDefinition(EntitySpriteDefinition.unsupported(0x5C))
                        .build()))
                .indoorRoom(true)
                .build();
        EntityRuntimeFixture.debug(runtime).setGeniePrivateState1ForTest(0, 1);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x50, 0x50, () -> 0));
        runtime.consumePendingEntityEvents();
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x50, 0x50, () -> 0));
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());
        runtime.tick(EntityTestFrames.frame(runtime, 2, 0x50, 0x50, () -> 0));
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());
        runtime.tick(EntityTestFrames.frame(runtime, 3, 0x50, 0x50, () -> 0));

        assertTrue(runtime.consumePendingEntityEvents().stream()
                .anyMatch(event ->
                        event.soundChannel() == EntityCombatEvent.SoundChannel.WAVE && event.soundId() == 0x10));
    }

    @Test
    void genieDisappearanceControllerReactivatesTheOriginalJarAtCountdownEightZero() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(
                        new RoomEntityBuilder()
                                .sourceLoadOrder(0)
                                .type(0x5C)
                                .at(0x50, 0x48)
                                .spriteDefinition(EntitySpriteDefinition.unsupported(0x5C))
                                .build(),
                        new RoomEntityBuilder()
                                .slot(1)
                                .sourceLoadOrder(1)
                                .type(0x5C)
                                .at(0x50, 0x22)
                                .spriteDefinition(EntitySpriteDefinition.unsupported(0x5C))
                                .build()))
                .indoorRoom(true)
                .build();
        EntityRuntimeFixture.debug(runtime).setGeniePrivateState1ForTest(1, 2);
        EntityRuntimeFixture.debug(runtime).setTransitionCountdownForTest(1, 0x82);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x50, 0x70, () -> 0));
        assertEquals(0x81, runtime.enemies().transitionCountdown(1));
        assertEquals(0, EntityRuntimeFixture.bosses(runtime).genieActiveStateForTest(0));

        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x50, 0x70, () -> 0));

        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(1).status());
        assertEquals(3, EntityRuntimeFixture.bosses(runtime).genieActiveStateForTest(0));
    }

    @Test
    void genieStateZeroHonorsTheSourceInteractiveGateAndSkipsGenericFallthrough() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x5C)
                        .at(0x50, 0x68)
                        .spriteDefinition(EntitySpriteDefinition.unsupported(0x5C))
                        .build()))
                .indoorRoom(true)
                .build();
        AtomicInteger groundCalls = new AtomicInteger();
        runtime.configuration().setGroundInteraction((entity, frame, previousStatus, speedZ, sideScrolling) -> {
            groundCalls.incrementAndGet();
            return RoomEntityGroundInteraction.Result.unloaded(entity, previousStatus, true);
        });
        runtime.configuration().setDialogActive(true);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x50, 0x68, () -> 0));

        assertEquals(0x06, runtime.enemies().enemyHealth(0));
        assertEquals(0, groundCalls.get());
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(List.of(), runtime.consumePendingLinkFinalPositionRequests());

        runtime.configuration().setDialogActive(false);
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x50, 0x68, () -> 0));
        assertEquals(0x20, runtime.enemies().enemyHealth(0));
        assertEquals(0, groundCalls.get());

        EntityRuntimeFixture.debug(runtime).setGeniePrivateState4ForTest(0, 2);
        runtime.clearEntity(0);
        assertEquals(0, EntityRuntimeFixture.bosses(runtime).geniePrivateState1ForTest(0));
        assertEquals(0, EntityRuntimeFixture.bosses(runtime).geniePrivateState4ForTest(0));
    }

    @Test
    void genieStartsBossMusicAndMapOneDialogAfterTheIntroDelay() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x5C)
                        .at(0x50, 0x68)
                        .spriteDefinition(EntitySpriteDefinition.unsupported(0x5C))
                        .build()))
                .indoorRoom(true)
                .build();
        EntityRuntimeFixture.debug(runtime).setEntityMapIdForTest(0x01);
        EntityRuntimeFixture.debug(runtime).setTransitionSequenceCounterForTest(0x04);

        for (int frame = 0; frame < 0x20; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x50, 0x70, () -> 0));
            assertEquals(-1, runtime.consumePendingMusicTrack());
            assertEquals(List.of(), runtime.consumePendingDialogRequests());
        }

        runtime.tick(EntityTestFrames.frame(runtime, 0x20, 0x50, 0x70, () -> 0));
        assertEquals(0x19, runtime.consumePendingMusicTrack());
        assertEquals(List.of(new DialogRequest(0, 0xB4)), runtime.consumePendingDialogRequests());

        runtime.tick(EntityTestFrames.frame(runtime, 0x21, 0x50, 0x70, () -> 0));
        assertEquals(-1, runtime.consumePendingMusicTrack());
        assertEquals(List.of(), runtime.consumePendingDialogRequests());
    }
}
