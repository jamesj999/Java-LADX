package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.EntityCombatEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityRuntimeGenie2Test extends EntityRuntimeTestSupport {

    @Test
    void genieBossIntroDelayPausesForInventoryAndRoomTransitions() {
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
        runtime.configuration().setTalkState(true, 0, 0);

        for (int frame = 0; frame < 0x20; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x50, 0x70, () -> 0));
        }
        runtime.configuration().setTalkState(false, 0, 0);
        EntityRuntimeFixture.debug(runtime).setRoomTransitionStateForTest(true);
        for (int frame = 0x20; frame < 0x40; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x50, 0x70, () -> 0));
        }
        assertEquals(-1, runtime.consumePendingMusicTrack());

        EntityRuntimeFixture.debug(runtime).setRoomTransitionStateForTest(false);
        for (int frame = 0x40; frame < 0x60; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x50, 0x70, () -> 0));
            assertEquals(-1, runtime.consumePendingMusicTrack());
        }
        runtime.tick(EntityTestFrames.frame(runtime, 0x60, 0x50, 0x70, () -> 0));
        assertEquals(0x19, runtime.consumePendingMusicTrack());
    }

    @Test
    void genieJarThresholdSpawnsBodyAndRockThenUnloadsTheJar() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x5C)
                        .at(0x50, 0x68)
                        .spriteDefinition(EntitySpriteDefinition.unsupported(0x5C))
                        .build()))
                .indoorRoom(true)
                .build();
        EntityRuntimeFixture.debug(runtime).setGeniePrivateState4ForTest(0, 0x03);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x50, 0x70, () -> 0));

        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(0).status());
        RoomEntity body = runtime.inspection().snapshot().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x5C)
                .findFirst()
                .orElseThrow();
        assertEquals(0x50, body.x());
        assertEquals(0x50, body.y());
        assertEquals(0x02, EntityRuntimeFixture.bosses(runtime).geniePrivateState1ForTest(body.slot()));
        assertEquals(0x27, runtime.enemies().transitionCountdown(body.slot()));
        assertEquals(0x08, runtime.enemies().enemyHealth(body.slot()));

        RoomEntity rock = runtime.inspection().snapshot().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x05)
                .findFirst()
                .orElseThrow();
        assertEquals(0x50, rock.x());
        assertEquals(0x68, rock.y());
        assertEquals(0xC4, runtime.enemies().physicsFlags(rock.slot()));
        assertTrue(runtime.consumePendingEntityEvents().stream()
                .anyMatch(event ->
                        event.soundChannel() == EntityCombatEvent.SoundChannel.NOISE && event.soundId() == 0x29));
    }

    @Test
    void thrownGenieJarCollisionCountsAnImpactAndEmitsBossHurtWave() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x5C)
                        .at(0x50, 0x68)
                        .status(EntityStatus.THROWN)
                        .spriteDefinition(EntitySpriteDefinition.unsupported(0x5C))
                        .build()))
                .indoorRoom(true)
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0, (entity, direction, nextX, nextY) -> true));

        assertEquals(1, EntityRuntimeFixture.bosses(runtime).geniePrivateState4ForTest(0));
        assertEquals(0x20, runtime.enemies().enemyFlashCountdown(0));
        assertEquals(
                EntityStatus.THROWN,
                runtime.inspection().snapshot().slots().get(0).status());
        assertTrue(runtime.consumePendingEntityEvents().stream()
                .anyMatch(event ->
                        event.soundChannel() == EntityCombatEvent.SoundChannel.WAVE && event.soundId() == 0x07));
    }

    @Test
    void thirdThrownGenieJarCollisionRestoresActiveThresholdState() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x5C)
                        .at(0x50, 0x68)
                        .status(EntityStatus.THROWN)
                        .spriteDefinition(EntitySpriteDefinition.unsupported(0x5C))
                        .build()))
                .indoorRoom(true)
                .build();
        EntityRuntimeFixture.debug(runtime).setGeniePrivateState4ForTest(0, 2);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0, (entity, direction, nextX, nextY) -> true));

        assertEquals(3, EntityRuntimeFixture.bosses(runtime).geniePrivateState4ForTest(0));
        assertEquals(1, EntityRuntimeFixture.bosses(runtime).genieActiveStateForTest(0));
        assertEquals(0, EntityRuntimeFixture.bosses(runtime).geniePrivateState3ForTest(0));
        assertEquals(0x80, runtime.enemies().transitionCountdown(0));
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());

        runtime.configuration().setDialogActive(true);
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0, 0, () -> 0));
        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(0).status());
        assertTrue(runtime.inspection().snapshot().loadedEntities().stream().anyMatch(entity -> entity.type() == 0x5C));
        assertTrue(runtime.inspection().snapshot().loadedEntities().stream().anyMatch(entity -> entity.type() == 0x05));
    }

    @Test
    void thrownGenieJarTargetOverlapDoesNotCountBeforeTheBackgroundCollisionCheck() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(
                        new RoomEntityBuilder()
                                .sourceLoadOrder(0)
                                .type(0x5C)
                                .at(0x50, 0x68)
                                .status(EntityStatus.THROWN)
                                .spriteDefinition(EntitySpriteDefinition.unsupported(0x5C))
                                .build(),
                        new RoomEntityBuilder()
                                .slot(1)
                                .sourceLoadOrder(1)
                                .type(0x88)
                                .at(0x50, 0x68)
                                .spriteDefinition(EntitySpriteDefinition.unsupported(0x88))
                                .build()))
                .indoorRoom(true)
                .build();
        EntityRuntimeFixture.debug(runtime).setGeniePrivateState4ForTest(0, 2);
        EntityRuntimeFixture.debug(runtime).setHitboxFlagsForTest(1, 0x80);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0, (entity, direction, nextX, nextY) -> false));

        assertEquals(2, EntityRuntimeFixture.bosses(runtime).geniePrivateState4ForTest(0));
        assertEquals(
                EntityStatus.THROWN,
                runtime.inspection().snapshot().slots().get(0).status());
    }
}
