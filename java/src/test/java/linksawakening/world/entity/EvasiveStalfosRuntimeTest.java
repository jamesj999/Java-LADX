package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.pairDefinition;
import static linksawakening.world.entity.EntityRuntimeTestSupport.sequence;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.TransientVfxRequest;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EvasiveStalfosRuntimeTest {

    @Test
    void evasiveStalfosUsesTheRomRandomWalkAndNormalAnimation() {
        EntitySpriteDefinition definition = pairDefinition(0x1E, 3);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x1E)
                .at(64, 64)
                .status(EntityStatus.INIT)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x01)));
        for (int frame = 1; frame <= 4; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, sequence(0x01)));
        }

        assertEquals(65, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).y());

        runtime.tick(EntityTestFrames.frame(runtime, 8, 120, 120, sequence(0x01)));
        assertEquals(1, runtime.inspection().snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void evasiveStalfosJumpsAwayFromHeldActionButtonsAndLandsWithRomState() {
        EntitySpriteDefinition definition = pairDefinition(0x1E, 3);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x1E)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        runtime.configuration().setActionButtonsHeld(true);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 80, 64, sequence(0x00)));

        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).evasiveInertia(0));
        assertEquals(0x08, runtime.enemies().transitionCountdown(0));
        assertEquals(0x15, new EntityEnemyInspectionQueries(runtime.enemies()).evasiveSpeedZ(0));
        assertEquals(0xEE, new EntityEnemyInspectionQueries(runtime.enemies()).evasiveSpeedX(0));
        assertEquals(0x00, new EntityEnemyInspectionQueries(runtime.enemies()).evasiveSpeedY(0));
        assertEquals(2, runtime.inspection().snapshot().slots().get(0).spriteVariant());
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).y());

        runtime.configuration().setActionButtonsHeld(false);
        runtime.tick(EntityTestFrames.frame(runtime, 1, 80, 64, sequence(0x00)));
        assertEquals(62, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(1, runtime.inspection().snapshot().slots().get(0).z());
        assertEquals(0x14, new EntityEnemyInspectionQueries(runtime.enemies()).evasiveSpeedZ(0));

        int frame = 2;
        while (new EntityEnemyInspectionQueries(runtime.enemies()).evasiveInertia(0) != 0 && frame < 100) {
            runtime.tick(EntityTestFrames.frame(runtime, frame++, 80, 64, sequence(0x00)));
        }

        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).evasiveInertia(0));
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).z());
        assertEquals(0x08, new EntityEnemyInspectionQueries(runtime.enemies()).evasiveSpeedX(0));
        assertEquals(0x08, new EntityEnemyInspectionQueries(runtime.enemies()).evasiveSpeedY(0));
        assertEquals(0x10, new EntityEnemyInspectionQueries(runtime.enemies()).evasivePrivateCountdown1(0));
    }

    @Test
    void evasiveStalfosCanBeHitInMidairAfterItsEightFrameJumpGracePeriod() {
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x1E)
                        .at(64, 64)
                        .spriteDefinition(pairDefinition(0x1E, 3))
                        .build()))
                .build();
        runtime.configuration().setActionButtonsHeld(true);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 80, 64, sequence(0)));
        runtime.configuration().setActionButtonsHeld(false);
        for (int frame = 0; frame < 8; frame++) {
            if (frame != 0) runtime.tick(EntityTestFrames.frame(runtime, frame, 80, 64, sequence(0)));
            RoomEntity entity = runtime.inspection().snapshot().slots().get(0);
            assertTrue(runtime.combat()
                    .resolveCombat(new EntityAttackContactFrame(
                            frame, 0, 0, false, 0, true, true, entity.x() - 8, 16, entity.y() - entity.z() - 8, 16))
                    .isEmpty());
        }
        runtime.tick(EntityTestFrames.frame(runtime, 8, 80, 64, sequence(0)));
        RoomEntity airborne = runtime.inspection().snapshot().slots().get(0);
        assertTrue(airborne.z() > 0);
        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).evasiveInertia(0));
        assertEquals(0, runtime.enemies().transitionCountdown(0));
        var hits = runtime.combat()
                .resolveCombat(new EntityAttackContactFrame(
                        8, 0, 0, false, 0, true, true, airborne.x() - 8, 16, airborne.y() - airborne.z() - 8, 16));
        assertTrue(hits.stream().anyMatch(hit -> hit.swordHit() && hit.enemyDamage() > 0));
    }

    @Test
    void evasiveStalfosClonesInAnglersTunnelWithRomAttributesAndVector() {
        EntitySpriteDefinition definition = pairDefinition(0x1E, 3);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x1E)
                        .at(0x40, 0x50)
                        .spriteDefinition(definition)
                        .z(0x03)
                        .build()))
                .build();
        EntityRuntimeFixture.debug(runtime).setEntityMapIdForTest(0x03);
        // AnimateEntities decrements private countdowns before dispatching the
        // handler; raw value two therefore enters the source's ==1 branch.
        EntityRuntimeFixture.debug(runtime).setEvasivePrivateCountdown1ForTest(0, 0x02);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x70, 0x50, sequence(0x01)));

        RoomEntity clone = runtime.inspection().snapshot().slots().get(15);
        assertEquals(EntityStatus.ACTIVE, clone.status());
        assertEquals(-1, clone.sourceLoadOrder());
        assertEquals(0x1E, clone.type());
        assertEquals(0x40, clone.x());
        assertEquals(0x50, clone.y());
        assertEquals(0x03, clone.z());
        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).evasivePrivateState1(15));
        assertEquals(0x52, runtime.enemies().physicsFlags(15));
        assertEquals(0x1A, runtime.execution().slotProperties().options1(15));
        assertEquals(0x00, runtime.enemies().enemyIgnoreHitsCountdown(15));
        // GetVectorTowardsLink($18) sees dx=$30 and dy=$03 (source Z is
        // included in the Y distance), producing X=$18, Y=$01.
        assertEquals(0x18, new EntityEnemyInspectionQueries(runtime.enemies()).evasiveSpeedX(15));
        assertEquals(0x01, new EntityEnemyInspectionQueries(runtime.enemies()).evasiveSpeedY(15));
        List<EntityCombatEvent> events = runtime.outbox().consumePendingEntityEvents();
        assertEquals(1, events.size());
        assertEquals(EntityCombatEvent.SoundChannel.NOISE, events.getFirst().soundChannel());
        assertEquals(0x0A, events.getFirst().soundId());
    }

    @Test
    void evasiveStalfosCloneSkipsGenericGroundInteractionButKeepsFleeingMotion() {
        EntitySpriteDefinition definition = pairDefinition(0x1E, 3);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x1E)
                        .at(0x40, 0x50)
                        .spriteDefinition(definition)
                        .build()))
                .build();
        EntityRuntimeFixture.debug(runtime).setEntityMapIdForTest(0x03);
        EntityRuntimeFixture.debug(runtime).setEvasivePrivateCountdown1ForTest(0, 0x02);
        List<Integer> groundInteractionSlots = new ArrayList<>();
        runtime.configuration().setGroundInteraction((entity, frame, previousStatus, speedZ, sideScrolling) -> {
            groundInteractionSlots.add(entity.slot());
            return RoomEntityGroundInteraction.Result.unchanged(entity, previousStatus);
        });

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x70, 0x50, sequence(0x01)));
        groundInteractionSlots.clear();
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x70, 0x50, sequence(0x01)));

        assertFalse(groundInteractionSlots.contains(15));
    }

    @Test
    void evasiveStalfosCloneIsGatedBelowAnglersTunnelAndDoesNotWhoosh() {
        EntitySpriteDefinition definition = pairDefinition(0x1E, 3);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x1E)
                        .at(0x40, 0x50)
                        .spriteDefinition(definition)
                        .build()))
                .build();
        EntityRuntimeFixture.debug(runtime).setEntityMapIdForTest(0x02);
        EntityRuntimeFixture.debug(runtime).setEvasivePrivateCountdown1ForTest(0, 0x02);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x70, 0x50, sequence(0x01)));

        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(15).status());
        assertEquals(0x01, new EntityEnemyInspectionQueries(runtime.enemies()).evasivePrivateCountdown1(0));
        assertTrue(runtime.outbox().consumePendingEntityEvents().isEmpty());
    }

    @Test
    void evasiveStalfosCloneDoesNotEmitWhooshWhenAllSlotsAreOccupied() {
        EntitySpriteDefinition definition = pairDefinition(0x1E, 3);
        List<RoomEntity> entities = new ArrayList<>();
        entities.add(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x1E)
                .at(0x40, 0x50)
                .spriteDefinition(definition)
                .build());
        for (int slot = 1; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            entities.add(new RoomEntityBuilder()
                    .slot(slot)
                    .type(0xFF)
                    .spriteDefinition(EntitySpriteDefinition.unsupported(0xFF))
                    .spriteVariant(-1)
                    .build());
        }
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(new RoomEntitySnapshot(entities)).build();
        EntityRuntimeFixture.debug(runtime).setEntityMapIdForTest(0x03);
        EntityRuntimeFixture.debug(runtime).setEvasivePrivateCountdown1ForTest(0, 0x02);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x70, 0x50, sequence(0x01)));

        assertEquals(0x1E, runtime.inspection().snapshot().slots().get(0).type());
        assertTrue(runtime.outbox().consumePendingEntityEvents().isEmpty());
    }

    @Test
    void fleeingEvasiveStalfosPokesAndClearsOnBackgroundCollision() {
        EntitySpriteDefinition definition = pairDefinition(0x1E, 2);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x1E)
                        .at(0x40, 0x50)
                        .spriteDefinition(definition)
                        .z(0x02)
                        .build()))
                .build();
        EntityRuntimeFixture.debug(runtime).setEvasiveFleeingForTest(0, 0x10, 0);
        RoomEntityBackgroundCollision wall = (entity, direction, nextX, nextY) -> direction == 0;

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x70, 0x50, sequence(0x00), wall));

        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(0).status());
        List<EntityCombatEvent> events = runtime.outbox().consumePendingEntityEvents();
        assertEquals(1, events.size());
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, events.getFirst().soundChannel());
        assertEquals(0x07, events.getFirst().soundId());
        assertEquals(
                List.of(new TransientVfxRequest(TransientVfxType.SWORD_POKE, 0x40, 0x4E)),
                runtime.enemies().transientVfxRequests());
    }

    @Test
    void fleeingEvasiveStalfosClearsAtTheRomScreenEdgeWithoutSwordPoke() {
        EntitySpriteDefinition definition = pairDefinition(0x1E, 2);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x1E)
                        .at(0xA7, 0x50)
                        .spriteDefinition(definition)
                        .build()))
                .build();
        EntityRuntimeFixture.debug(runtime).setEvasiveFleeingForTest(0, 0x10, 0);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x70, 0x50, sequence(0x00)));

        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(0).status());
        assertTrue(runtime.outbox().consumePendingEntityEvents().isEmpty());
        assertTrue(runtime.enemies().transientVfxRequests().isEmpty());
    }
}
