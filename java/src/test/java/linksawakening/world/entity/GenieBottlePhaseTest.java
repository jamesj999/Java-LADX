package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class GenieBottlePhaseTest {

    @Test
    void genieInitializesTheSourceJarCombatFields() {
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
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
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
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
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
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
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
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
        assertTrue(runtime.outbox().consumePendingEntityEvents().stream()
                .anyMatch(event ->
                        event.soundChannel() == EntityCombatEvent.SoundChannel.JINGLE && event.soundId() == 0x06));
    }

    @Test
    void activeGenieJarFallsAndBouncesWhileMovingTowardTheCenter() {
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
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
        assertTrue(runtime.outbox().consumePendingEntityEvents().stream()
                .anyMatch(event ->
                        event.soundChannel() == EntityCombatEvent.SoundChannel.JINGLE && event.soundId() == 0x20));

        runtime.tick(EntityTestFrames.frame(runtime, 2, 0x50, 0x6F, () -> 0));
        assertEquals(1, runtime.inspection().snapshot().slots().get(0).z());
    }

    @Test
    void genieAppearanceCountdownSixSpawnsTheFirstCycleBody() {
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
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
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(
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
        assertEquals(List.of(new DialogRequest(0, 0x52)), runtime.outbox().consumePendingDialogRequests());
    }

    @Test
    void genieStateZeroHonorsTheSourceInteractiveGateAndSkipsGenericFallthrough() {
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
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
        assertEquals(List.of(), runtime.outbox().consumePendingLinkFinalPositionRequests());

        runtime.configuration().setDialogActive(false);
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x50, 0x68, () -> 0));
        assertEquals(0x20, runtime.enemies().enemyHealth(0));
        assertEquals(0, groundCalls.get());

        EntityRuntimeFixture.debug(runtime).setGeniePrivateState4ForTest(0, 2);
        runtime.lifecycle().clearEntity(0);
        assertEquals(0, EntityRuntimeFixture.bosses(runtime).geniePrivateState1ForTest(0));
        assertEquals(0, EntityRuntimeFixture.bosses(runtime).geniePrivateState4ForTest(0));
    }

    @Test
    void genieStartsBossMusicAndMapOneDialogAfterTheIntroDelay() {
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
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
            assertEquals(-1, runtime.outbox().consumePendingMusicTrack());
            assertEquals(List.of(), runtime.outbox().consumePendingDialogRequests());
        }

        runtime.tick(EntityTestFrames.frame(runtime, 0x20, 0x50, 0x70, () -> 0));
        assertEquals(0x19, runtime.outbox().consumePendingMusicTrack());
        assertEquals(List.of(new DialogRequest(0, 0xB4)), runtime.outbox().consumePendingDialogRequests());

        runtime.tick(EntityTestFrames.frame(runtime, 0x21, 0x50, 0x70, () -> 0));
        assertEquals(-1, runtime.outbox().consumePendingMusicTrack());
        assertEquals(List.of(), runtime.outbox().consumePendingDialogRequests());
    }

    @Test
    void genieBossIntroDelayPausesForInventoryAndRoomTransitions() {
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x5C)
                        .at(0x50, 0x68)
                        .spriteDefinition(EntitySpriteDefinition.unsupported(0x5C))
                        .build()))
                .indoorRoom(true)
                .build();
        EntityRuntimeFixture.debug(runtime).setEntityMapIdForTest(0x01);
        EntityRuntimeFixture.debug(runtime).setTransitionSequenceCounterForTest(0x04);
        runtime.configuration().setTalkState(true, 0, 0);

        for (int frame = 0; frame < 0x20; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x50, 0x70, () -> 0));
        }
        runtime.configuration().setTalkState(false, 0, 0);
        EntityRuntimeFixture.debug(runtime).setRoomTransitionStateForTest(true);
        for (int frame = 0x20; frame < 0x40; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x50, 0x70, () -> 0));
        }
        assertEquals(-1, runtime.outbox().consumePendingMusicTrack());

        EntityRuntimeFixture.debug(runtime).setRoomTransitionStateForTest(false);
        for (int frame = 0x40; frame < 0x60; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x50, 0x70, () -> 0));
            assertEquals(-1, runtime.outbox().consumePendingMusicTrack());
        }
        runtime.tick(EntityTestFrames.frame(runtime, 0x60, 0x50, 0x70, () -> 0));
        assertEquals(0x19, runtime.outbox().consumePendingMusicTrack());
    }
}
