package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class GenieFinalFightTest {

    @Test
    void firstGenieBodyEntersFinalFightWhenTheJarNoLongerExists() {
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
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
        assertEquals(List.of(new DialogRequest(0, 0x53)), runtime.outbox().consumePendingDialogRequests());
    }

    @Test
    void vulnerableGenieBodyHitFlashStartsTheMirroredDashPhase() {
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
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
        assertEquals(0x81, runtime.execution().slotProperties().options1(0));
    }

    @Test
    void genieDashStoresThePositionMirroredAcrossTheArenaCenter() {
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
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
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
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
        assertTrue(runtime.outbox().consumePendingEntityEvents().stream()
                .anyMatch(event ->
                        event.soundChannel() == EntityCombatEvent.SoundChannel.NOISE && event.soundId() == 0x28));
    }

    @Test
    void defeatedGenieRunsBossExplosionTimersAndDropsTheHeartContainer() {
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
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
        assertTrue(runtime.outbox().consumePendingEntityEvents().stream()
                .anyMatch(event ->
                        event.soundChannel() == EntityCombatEvent.SoundChannel.NOISE && event.soundId() == 0x1A));
    }

    @Test
    void defeatedGenieExplosionTickCreatesAPoofAndDestroyedNoise() {
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
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
        assertTrue(runtime.outbox().consumePendingEntityEvents().stream()
                .anyMatch(event ->
                        event.soundChannel() == EntityCombatEvent.SoundChannel.NOISE && event.soundId() == 0x13));
    }

    @Test
    void defeatedGeniePlaysTheGlobalBossDeathCryAfterThreeMoreTicks() {
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
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
        runtime.outbox().consumePendingEntityEvents();
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x50, 0x50, () -> 0));
        assertTrue(runtime.outbox().consumePendingEntityEvents().isEmpty());
        runtime.tick(EntityTestFrames.frame(runtime, 2, 0x50, 0x50, () -> 0));
        assertTrue(runtime.outbox().consumePendingEntityEvents().isEmpty());
        runtime.tick(EntityTestFrames.frame(runtime, 3, 0x50, 0x50, () -> 0));

        assertTrue(runtime.outbox().consumePendingEntityEvents().stream()
                .anyMatch(event ->
                        event.soundChannel() == EntityCombatEvent.SoundChannel.WAVE && event.soundId() == 0x10));
    }
}
