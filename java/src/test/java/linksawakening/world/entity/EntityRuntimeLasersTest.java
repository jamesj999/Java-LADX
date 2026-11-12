package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.EntityProjectileEvent;
import linksawakening.world.entity.event.TransientVfxRequest;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityRuntimeLasersTest extends EntityRuntimeTestSupport {

    @Test
    void laserParentRotatesEveryEightFramesAndSpawnsAnInvisibleRomSensor() {
        EntitySpriteDefinition definition = pairDefinition(0x2A, 8);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x2A)
                        .at(0x40, 0x50)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        for (int frame = 0; frame < 8; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x10, 0x10, () -> 0));
        }

        RoomEntity parent = runtime.inspection().snapshot().slots().get(0);
        RoomEntity sensor = runtime.inspection().snapshot().slots().get(15);
        assertEquals(1, runtime.enemies().laserDirection(0));
        assertEquals(0, parent.spriteVariant());
        assertEquals(EntityStatus.ACTIVE, sensor.status());
        assertEquals(0x2A, sensor.type());
        assertEquals(-1, sensor.sourceLoadOrder());
        assertEquals(-1, sensor.spriteVariant());
        assertEquals(0x40, sensor.x());
        assertEquals(0x50, sensor.y());
    }

    @Test
    void laserParentSpawnsBeamAtRomCountdownTenWithCopiedSpeed() {
        EntitySpriteDefinition definition = pairDefinition(0x2A, 8);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x2A)
                        .at(0x40, 0x50)
                        .spriteDefinition(definition)
                        .build()))
                .build();
        EntityRuntimeFixture.debug(runtime).setLaserParentForTest(0, 0x10, 0x20, 0xF0);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x10, 0x10, () -> 0));

        RoomEntity beam = runtime.inspection().snapshot().slots().get(15);
        assertEquals(0x0F, runtime.enemies().laserParentTransitionCountdown(0));
        assertEquals(EntityStatus.ACTIVE, beam.status());
        assertEquals(0x2B, beam.type());
        assertEquals(-1, beam.sourceLoadOrder());
        assertEquals(-1, beam.spriteVariant());
        assertEquals(0x40, beam.x());
        assertEquals(0x50, beam.y());
        assertEquals(0x20, runtime.enemies().laserSpeedX(15));
        assertEquals(0xF0, runtime.enemies().laserSpeedY(15));
        List<EntityCombatEvent> events = runtime.consumePendingEntityEvents();
        assertEquals(1, events.size());
        assertEquals(EntityCombatEvent.SoundChannel.NOISE, events.getFirst().soundChannel());
        assertEquals(0x08, events.getFirst().soundId());
    }

    @Test
    void laserParentDoesNotEmitFiringNoiseWhenTheBeamSlotIsUnavailable() {
        List<RoomEntity> entities = new ArrayList<>();
        entities.add(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x2A)
                .at(0x40, 0x50)
                .spriteDefinition(pairDefinition(0x2A, 8))
                .build());
        for (int slot = 1; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            entities.add(new RoomEntityBuilder()
                    .slot(slot)
                    .sourceLoadOrder(slot)
                    .type(0xFF)
                    .spriteDefinition(EntitySpriteDefinition.unsupported(0xFF))
                    .spriteVariant(-1)
                    .build());
        }
        RoomEntityRuntime runtime =
                RoomEntityRuntime.builder(new RoomEntitySnapshot(entities)).build();
        EntityRuntimeFixture.debug(runtime).setLaserParentForTest(0, 0x10, 0x20, 0xF0);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x10, 0x10, () -> 0));

        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(15).status());
        assertEquals(0xFF, runtime.inspection().snapshot().slots().get(15).type());
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());
    }

    @Test
    void laserSensorClearsAndArmsItsParentWhenLinkEntersTheRomWindow() {
        EntitySpriteDefinition definition = pairDefinition(0x2A, 8);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x2A)
                        .at(0x40, 0x50)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        for (int frame = 0; frame < 8; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x10, 0x10, () -> 0));
        }

        EnemyProjectileCollision.LinkState link =
                new EnemyProjectileCollision.LinkState(0x40, 0x50, 0, 0, 0, false, 0, 0);
        runtime.tick(EntityTestFrames.projectileFrame(runtime, 8, 0x40, 0x50, () -> 0, null, link));

        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(15).status());
        assertEquals(0x1F, runtime.enemies().laserParentTransitionCountdown(0));
        assertEquals(0x40, runtime.enemies().laserSpeedX(0));
        assertEquals(0x40, runtime.enemies().laserSpeedY(0));
        assertEquals(0x0F, runtime.enemies().enemyFlashCountdown(0));
        assertEquals(0x10, runtime.inspection().snapshot().slots().get(0).entityFlipAttribute());
    }

    @Test
    void laserBeamUsesRomFixedPointMovementAndEmitsTheTransientBeamVfx() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .type(0x2B)
                        .at(0x20, 0x30)
                        .spriteDefinition(EntitySpriteDefinition.unsupported(0x2B))
                        .spriteVariant(-1)
                        .build()))
                .build();
        EntityRuntimeFixture.debug(runtime).setLaserBeamForTest(0, 0x10, 0, 0);

        runtime.tick(EntityTestFrames.projectileFrame(
                runtime, 0, 0, 0, () -> 0, null, EnemyProjectileCollision.LinkState.nonInteractive()));

        RoomEntity beam = runtime.inspection().snapshot().slots().get(0);
        assertEquals(0x21, beam.x());
        assertEquals(0x30, beam.y());
        assertEquals(1, runtime.enemies().transientVfxRequests().size());
        TransientVfxRequest request = runtime.enemies().transientVfxRequests().get(0);
        assertEquals(TransientVfxType.LASER_BEAM, request.type());
        assertEquals(0x25, request.worldX());
        assertEquals(0x30, request.worldY());
    }

    @Test
    void laserBeamMirrorShieldReflectionReversesTheRomAxisBeforeMovement() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .type(0x2B)
                        .at(0x40, 0x50)
                        .spriteDefinition(EntitySpriteDefinition.unsupported(0x2B))
                        .spriteVariant(-1)
                        .build()))
                .build();
        EntityRuntimeFixture.debug(runtime).setLaserBeamForTest(0, 0x10, 0, 2);
        EnemyProjectileCollision.LinkState link =
                new EnemyProjectileCollision.LinkState(0x40, 0x50, 0, 0, 0, true, 2, 0);

        List<EntityProjectileEvent> events =
                runtime.tick(EntityTestFrames.projectileFrame(runtime, 0, 0x40, 0x50, () -> 0, null, link));

        assertEquals(1, events.size());
        assertEquals(EntityProjectileEvent.Kind.SHIELD_BLOCK, events.get(0).kind());
        assertEquals(0x02, events.get(0).collisionValue());
        assertFalse(events.get(0).remove());
        assertEquals(0x41, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(0x50, runtime.inspection().snapshot().slots().get(0).y());
        assertEquals(0xF0, runtime.enemies().laserSpeedX(0));
        assertEquals(1, runtime.enemies().transientVfxRequests().size());
        assertEquals(
                TransientVfxType.SWORD_POKE,
                runtime.enemies().transientVfxRequests().get(0).type());
        assertEquals(0x41, runtime.enemies().transientVfxRequests().get(0).worldX());
        assertEquals(0x50, runtime.enemies().transientVfxRequests().get(0).worldY());
    }
}
