package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.EntityCombatEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class GenieFireballCycleTest {

    @Test
    void genieBodyStateOneSpawnsTheSourceSideProjectileAtItsNextDelay() {
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
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

        runtime.lifecycle().clearEntity(0);
        for (int frame = 1; frame <= 80; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x30, 0x70, () -> 0));
        }
        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(projectile.slot()).status());
    }

    @Test
    void genieBodyPrivateCountdownSpawnsAimedFireballAndAdvancesInertia() {
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
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

        assertEquals(1, new EntityRoomInspectionQueries(runtime.inspection()).entityInertia(0));
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
        assertTrue(runtime.outbox().consumePendingEntityEvents().stream()
                .anyMatch(event ->
                        event.soundChannel() == EntityCombatEvent.SoundChannel.NOISE && event.soundId() == 0x28));

        runtime.lifecycle().clearEntity(0);
        for (int frame = 1; frame <= 80; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x30, 0x70, () -> 0));
        }
        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(fireball.slot()).status());
    }

    @Test
    void genieFireballRunsItsSourceLinkCollisionBeforeMovement() {
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x5C)
                        .at(0x50, 0x50)
                        .spriteDefinition(EntitySpriteDefinition.unsupported(0x5C))
                        .build()))
                .indoorRoom(true)
                .build();
        EntityRuntimeFixture.debug(runtime).setGeniePrivateState1ForTest(0, 4);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x50, 0x50, () -> 0));

        assertFalse(runtime.outbox().consumePendingLinkFinalPositionRequests().isEmpty());
    }

    @Test
    void genieBodyFinishesEightAttackCyclesAndStartsReturningToTheJar() {
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
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
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
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
        assertTrue(runtime.outbox().consumePendingEntityEvents().stream()
                .anyMatch(event ->
                        event.soundChannel() == EntityCombatEvent.SoundChannel.JINGLE && event.soundId() == 0x1F));
    }

    @Test
    void genieDisappearanceControllerReactivatesTheOriginalJarAtCountdownEightZero() {
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
}
