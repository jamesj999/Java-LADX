package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.HinoxMotion;
import linksawakening.world.entity.motion.MotionTestAccess;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityRuntimeHinoxTest extends EntityRuntimeTestSupport {

    @Test
    void hinoxSkipsItsInteractiveHandlerAndTimersDuringNonInteractiveFrames() {
        RoomEntity hinox = new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(HinoxMotion.ENTITY_TYPE)
                .at(0x40, 0x40)
                .spriteDefinition(EntitySpriteDefinition.unsupported(HinoxMotion.ENTITY_TYPE))
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(hinox)).build();
        EntityRuntimeFixture.debug(runtime).setTransitionCountdownForTest(0, 0x12);

        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                0,
                0x40,
                0x40,
                () -> 0,
                null,
                null,
                0,
                0,
                0,
                EnemyProjectileCollision.LinkState.nonInteractive()));

        assertEquals(0x12, runtime.enemies().transitionCountdown(0));
        assertEquals(
                MotionTestAccess.HinoxMotion_STATE_INITIAL,
                EntityRuntimeFixture.inspection(runtime).hinoxStateForTest(0));
    }

    @Test
    void hinoxSkipsItsHandlerAndTimersOutsideTheRomTransitionSequence() {
        RoomEntity hinox = new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(HinoxMotion.ENTITY_TYPE)
                .at(0x40, 0x40)
                .spriteDefinition(EntitySpriteDefinition.unsupported(HinoxMotion.ENTITY_TYPE))
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(hinox)).build();
        EntityRuntimeFixture.debug(runtime).setTransitionSequenceCounterForTest(0x03);
        EntityRuntimeFixture.debug(runtime).setTransitionCountdownForTest(0, 0x12);

        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                0,
                0x40,
                0x40,
                () -> 0,
                null,
                null,
                0,
                0,
                0,
                new EnemyProjectileCollision.LinkState(0x40, 0x40, 0, 0, 3, false)));

        assertEquals(0x12, runtime.enemies().transitionCountdown(0));
        assertEquals(
                MotionTestAccess.HinoxMotion_STATE_INITIAL,
                EntityRuntimeFixture.inspection(runtime).hinoxStateForTest(0));
    }

    @Test
    void hinoxInactiveDeathUsesSharedArmosCountdownsAndRoomStatus() {
        RoomEntity hinox = new linksawakening.world.entity.RoomEntityBuilder()
                .slot(15)
                .sourceLoadOrder(0)
                .type(HinoxMotion.ENTITY_TYPE)
                .at(0x40, 0x40)
                .status(EntityStatus.DYING)
                .spriteDefinition(EntitySpriteDefinition.unsupported(HinoxMotion.ENTITY_TYPE))
                .spriteVariant(-1)
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshotAt(hinox))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .build();
        EntityRuntimeFixture.debug(runtime).setEntityMapIdForTest(0x05);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x40, 0x40, () -> 0));
        assertEquals(0xA0, runtime.enemies().transitionCountdown(15));
        assertEquals(0xFF, runtime.enemies().enemyFlashCountdown(15));

        for (int frame = 1; frame <= 0xA0; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x40, 0x40, () -> 0));
        }
        assertEquals(0xC0, runtime.enemies().transitionCountdown(15));
        assertEquals(0xFF, runtime.enemies().enemyFlashCountdown(15));

        for (int frame = 0xA1; frame <= 0x160; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x40, 0x40, () -> 0));
        }

        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(15).status());
        assertEquals(0x20, runtime.consumePendingRoomStatusMask());
        assertEquals(
                List.of(new EntityCombatEvent(
                        15, HinoxMotion.ENTITY_TYPE, 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x1A)),
                runtime.consumePendingEntityEvents());
    }

    @Test
    void hinoxRunsBossIntroWhenInteractive() {
        RoomEntity hinox = new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(HinoxMotion.ENTITY_TYPE)
                .at(0x40, 0x40)
                .spriteDefinition(EntitySpriteDefinition.unsupported(HinoxMotion.ENTITY_TYPE))
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(hinox)).build();
        EnemyProjectileCollision.LinkState interactive =
                new EnemyProjectileCollision.LinkState(0x40, 0x40, 0, 0, 3, false);

        for (int frame = 0; frame <= 0x20; frame++) {
            runtime.tick(EntityTestFrames.projectileFrame(
                    runtime, frame, 0x40, 0x40, () -> 0, null, null, 0, 0, 0, interactive));
        }
        assertEquals(0x50, runtime.consumePendingMusicTrack());
    }

    @Test
    void hinoxStateFourSkipsGenericSwordAndContactCombat() {
        RoomEntity hinox = new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(HinoxMotion.ENTITY_TYPE)
                .at(0x40, 0x40)
                .spriteDefinition(EntitySpriteDefinition.unsupported(HinoxMotion.ENTITY_TYPE))
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(hinox)).build();
        EntityRuntimeFixture.debug(runtime).setHinoxStateForTest(0, HinoxMotion.STATE_GRAB);
        EntityRuntimeFixture.debug(runtime).setEnemyHealthForTest(0, 1);

        assertTrue(runtime.resolveCombat(0, 0x40, 0x40, false, true, false, 0, 0, 0, 0)
                .isEmpty());
        assertTrue(runtime.resolveCombat(1, 0x40, 0x40, false, true, true, 0x40, 0x10, 0x40, 0x10)
                .isEmpty());
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(1, EntityRuntimeFixture.inspection(runtime).enemyHealthForTest(0));
    }

    @Test
    void hinoxUsesBooleanBackgroundCollisionWhenRichProbeIsAbsent() {
        RoomEntity hinox = new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(HinoxMotion.ENTITY_TYPE)
                .at(0x40, 0x40)
                .spriteDefinition(EntitySpriteDefinition.unsupported(HinoxMotion.ENTITY_TYPE))
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(hinox)).build();
        EntityRuntimeFixture.debug(runtime).setHinoxStateForTest(0, MotionTestAccess.HinoxMotion_STATE_WANDER);
        EntityRuntimeFixture.debug(runtime).setHinoxSpeedForTest(0, 0x08, 0);
        EntityRuntimeFixture.debug(runtime).setTransitionCountdownForTest(0, 0x20);

        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                0,
                0x40,
                0x40,
                () -> 0,
                (entity, direction, nextX, nextY) -> true,
                new EnemyProjectileCollision.LinkState(0x40, 0x40, 0, 0, 3, false)));

        assertEquals(0x40, runtime.inspection().snapshot().slots().get(0).x());
    }

    @Test
    void hinoxGrowlAndBounceUseTheirRomAudioChannels() {
        RoomEntity hinox = new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(HinoxMotion.ENTITY_TYPE)
                .at(0x50, 0x40)
                .spriteDefinition(EntitySpriteDefinition.unsupported(HinoxMotion.ENTITY_TYPE))
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(hinox)).build();
        EntityRuntimeFixture.debug(runtime).setHinoxStateForTest(0, MotionTestAccess.HinoxMotion_STATE_CHARGE);
        EntityRuntimeFixture.debug(runtime).setTransitionCountdownForTest(0, 0);

        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                0,
                0x30,
                0x30,
                () -> 0,
                null,
                new EnemyProjectileCollision.LinkState(0x30, 0x30, 0, 0, 3, false)));

        List<EntityCombatEvent> events = runtime.consumePendingEntityEvents();
        assertTrue(events.contains(new EntityCombatEvent(
                0, HinoxMotion.ENTITY_TYPE, 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x20)));
        assertTrue(events.contains(new EntityCombatEvent(
                0, HinoxMotion.ENTITY_TYPE, 0, false, EntityCombatEvent.SoundChannel.WAVE, 0x16)));
    }
}
