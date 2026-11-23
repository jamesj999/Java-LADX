package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.pairDefinition;
import static linksawakening.world.entity.EntityRuntimeTestSupport.sequence;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.LinkFinalPositionRequest;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class ArmosActivationTest {

    @Test
    void armosWakesOnLinkCollisionThenChargesForTheRomCountdown() {
        EntitySpriteDefinition definition = pairDefinition(0x0F, 2);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x0F)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 64, 64, sequence(0x00)));
        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).armosState(0));
        assertEquals(0x30, new EntityEnemyInspectionQueries(runtime.enemies()).armosTransitionCountdown(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).armosSpeedX(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).armosSpeedY(0));

        runtime.tick(EntityTestFrames.frame(runtime, 1, 120, 120, sequence(0x00)));
        assertEquals(0x2F, new EntityEnemyInspectionQueries(runtime.enemies()).armosTransitionCountdown(0));
        assertEquals(0xF8, new EntityEnemyInspectionQueries(runtime.enemies()).armosSpeedX(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).armosSpeedY(0));

        for (int frame = 2; frame <= 0x30; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, sequence(0x00)));
        }
        assertEquals(2, new EntityEnemyInspectionQueries(runtime.enemies()).armosState(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).armosTransitionCountdown(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).armosSpeedX(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).armosSpeedY(0));
        assertEquals(1, runtime.inspection().snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void armosStatueRequestsLinkFinalPositionWithoutTeleportingTheStatue() {
        EntitySpriteDefinition definition = pairDefinition(0x0F, 2);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x0F)
                        .at(56, 64)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                0,
                64,
                64,
                sequence(0x00),
                linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN,
                // The statue uses CheckLinkCollisionWithEnemy.collisionEvenInTheAir;
                // Link's Z must not suppress this collision.
                new EnemyProjectileCollision.LinkState(64, 64, 0x10, 0, 0, false)));

        RoomEntity statue = runtime.inspection().snapshot().slots().get(0);
        assertEquals(56, statue.x());
        assertEquals(64, statue.y());
        assertEquals(
                List.of(new LinkFinalPositionRequest(0)), runtime.outbox().consumePendingLinkFinalPositionRequests());
    }

    @Test
    void armosStatueRunsItsPostMovementRomBackgroundCollisionPass() {
        EntitySpriteDefinition definition = pairDefinition(0x0F, 2);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
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
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
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
                linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN,
                new EnemyProjectileCollision.LinkState(64, 64, 0, 0x02, 0, false)));

        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).armosState(0));
        assertTrue(runtime.outbox().consumePendingLinkFinalPositionRequests().isEmpty());
    }

    @Test
    void armosUsesRomActivationFlagsAndOnlyTheActiveStateJoinsCombat() {
        EntitySpriteDefinition definition = pairDefinition(0x0F, 2);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x0F)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        assertEquals(0x92, runtime.enemies().physicsFlags(0));
        assertTrue(runtime.combat()
                .resolveCombat(new EntitySwordContactFrame(1, 64, 64, false, true, true, 72, 1, 72, 1))
                .isEmpty());

        runtime.tick(EntityTestFrames.frame(runtime, 0, 64, 64, sequence(0x00)));
        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).armosState(0));
        assertEquals(0x18, new EntityEnemyInspectionQueries(runtime.enemies()).enemyFlashCountdown(0));
        assertEquals(0x92, runtime.enemies().physicsFlags(0));
        assertTrue(runtime.combat()
                .resolveCombat(new EntitySwordContactFrame(1, 64, 64, false, true, true, 72, 1, 72, 1))
                .isEmpty());

        for (int frame = 1; frame <= 0x30; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, sequence(0x00)));
        }

        assertEquals(2, new EntityEnemyInspectionQueries(runtime.enemies()).armosState(0));
        assertEquals(0x12, runtime.enemies().physicsFlags(0));

        RoomEntity active = runtime.inspection().snapshot().slots().get(0);
        List<EntityCombatEvent> contact = runtime.combat()
                .resolveCombat(new EntitySwordContactFrame(1, active.x(), active.y(), false, true, false, 0, 0, 0, 0));
        assertEquals(1, contact.size());
        assertEquals(0x10, contact.get(0).linkDamage());

        List<EntityCombatEvent> sword = runtime.combat()
                .resolveCombat(
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
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();

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
}
