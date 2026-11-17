package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.pairDefinition;
import static linksawakening.world.entity.EntityRuntimeTestSupport.sequence;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.LikeLikeEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class LikeLikeCaptureTest {

    @Test
    void likeLikeRunsTheRomInitDirectionChoiceAndFixedPointWalk() {
        EntitySpriteDefinition definition = pairDefinition(0x23, 2);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x23)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0xFF)));
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(1, new EntityRoomInspectionQueries(runtime.inspection()).likeLikeWalkState(0));

        // LikeLikeGibdoSpeeds[1] is $08 (+8 pixels per 16 frames).
        runtime.tick(EntityTestFrames.frame(runtime, 1, 120, 120, sequence(0x00, 0x01)));
        assertEquals(0, new EntityRoomInspectionQueries(runtime.inspection()).likeLikeWalkState(0));
        assertEquals(0x08, new EntityRoomInspectionQueries(runtime.inspection()).likeLikeWalkSpeedY(0));
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).y());

        runtime.tick(EntityTestFrames.frame(runtime, 2, 120, 120, sequence(0xFF)));
        runtime.tick(EntityTestFrames.frame(runtime, 3, 120, 120, sequence(0xFF)));
        assertEquals(65, runtime.inspection().snapshot().slots().get(0).y());
    }

    @Test
    void likeLikeCapturesLinkOnTheRomCollisionCadenceWithoutContactDamage() {
        EntitySpriteDefinition definition = pairDefinition(0x23, 2);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x23)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 64, 64, sequence(0xFF)));
        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                1,
                64,
                64,
                sequence(0x00, 0x00),
                linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN,
                new EnemyProjectileCollision.LinkState(64, 64, 0, 0, 0, false, 1, 0)));
        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                2,
                64,
                64,
                sequence(0x00, 0x00),
                linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN,
                new EnemyProjectileCollision.LinkState(64, 64, 0, 0, 0, false, 1, 0)));

        List<LikeLikeEvent> events = runtime.outbox().consumePendingLikeLikeEvents();
        assertEquals(1, events.size());
        assertEquals(LikeLikeEvent.Kind.CAPTURE, events.getFirst().kind());
        assertEquals(-1, events.getFirst().stolenInventorySlot());
        assertEquals(1, new EntityRoomInspectionQueries(runtime.inspection()).likeLikeState(0));
        assertTrue(runtime.outbox().consumePendingEntityEvents().isEmpty());

        assertTrue(runtime.combat()
                .resolveCombat(new EntitySwordContactFrame(3, 64, 64, false, true, true, 72, 1, 72, 1))
                .isEmpty());
        assertEquals(2, runtime.enemies().enemyHealth(0));
    }

    @Test
    void likeLikeStealsTheBShieldFirstAndRefusesToStealTheLevelTwoShield() {
        EntitySpriteDefinition definition = pairDefinition(0x23, 2);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x23)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .build();
        EntityRuntimeFixture.debug(runtime).setLikeLikeLinkInventoryForTest(0x04, 0x04);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 64, 64, sequence(0xFF)));
        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                1,
                64,
                64,
                sequence(0x00),
                linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN,
                new EnemyProjectileCollision.LinkState(64, 64, 0, 0, 0, false, 1, 0)));
        runtime.outbox().consumePendingLikeLikeEvents();

        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                2,
                64,
                64,
                sequence(0x00),
                linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN,
                new EnemyProjectileCollision.LinkState(64, 64, 0, 0, 0, false, 1, 0)));
        LikeLikeEvent stolen = runtime.outbox().consumePendingLikeLikeEvents().getFirst();
        assertEquals(LikeLikeEvent.Kind.CAPTURE, stolen.kind());
        assertEquals(0, stolen.stolenInventorySlot());
        assertEquals(1, stolen.stolenShieldLevel());
        assertEquals(1, new EntityRoomInspectionQueries(runtime.inspection()).likeLikePrivateState1(0));

        RoomEntityRuntime levelTwo = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x23)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .build();
        EntityRuntimeFixture.debug(levelTwo).setLikeLikeLinkInventoryForTest(0x00, 0x04);
        levelTwo.tick(EntityTestFrames.frame(levelTwo, 0, 64, 64, sequence(0xFF)));
        levelTwo.tick(EntityTestFrames.projectileFrame(
                levelTwo,
                1,
                64,
                64,
                sequence(0x00),
                linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN,
                new EnemyProjectileCollision.LinkState(64, 64, 0, 0, 0, false, 2, 0)));
        levelTwo.outbox().consumePendingLikeLikeEvents();
        levelTwo.tick(EntityTestFrames.projectileFrame(
                levelTwo,
                2,
                64,
                64,
                sequence(0x00),
                linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN,
                new EnemyProjectileCollision.LinkState(64, 64, 0, 0, 0, false, 2, 0)));
        LikeLikeEvent protectedShield =
                levelTwo.outbox().consumePendingLikeLikeEvents().getFirst();
        assertEquals(-1, protectedShield.stolenInventorySlot());
        assertEquals(0, new EntityRoomInspectionQueries(levelTwo.inspection()).likeLikePrivateState1(0));
    }

    @Test
    void likeLikeDeliversShieldTheftWhileLinksMotionStatePreventsPositionWrites() {
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x23)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(pairDefinition(0x23, 2))
                        .build()))
                .build();
        EntityRuntimeFixture.debug(runtime).setLikeLikeLinkInventoryForTest(0x00, 0x04);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 64, 64, sequence(0xFF)));
        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                1,
                64,
                64,
                sequence(0x00),
                linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN,
                new EnemyProjectileCollision.LinkState(64, 64, 0, 0, 0, false, 1, 0)));
        runtime.outbox().consumePendingLikeLikeEvents();

        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                2,
                80,
                80,
                sequence(0x00),
                linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN,
                new EnemyProjectileCollision.LinkState(80, 80, 0, 1, 0, false, 1, 0)));
        var events = runtime.outbox().consumePendingLikeLikeEvents();
        assertEquals(1, events.size());
        assertEquals(LikeLikeEvent.Kind.CAPTURE, events.getFirst().kind());
        assertFalse(events.getFirst().pinLinkPosition());
        assertEquals(0, events.getFirst().stolenInventorySlot());
        assertEquals(1, new EntityRoomInspectionQueries(runtime.inspection()).likeLikePrivateState1(0));
    }

    @Test
    void likeLikeReleasesLinkAfterEightHeldActionFramesAndStartsTheRomSlowTimer() {
        EntitySpriteDefinition definition = pairDefinition(0x23, 2);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x23)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .build();
        runtime.tick(EntityTestFrames.frame(runtime, 0, 64, 64, sequence(0xFF)));
        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                1,
                64,
                64,
                sequence(0x00),
                linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN,
                new EnemyProjectileCollision.LinkState(64, 64, 0, 0, 0, false, 1, 0)));
        runtime.outbox().consumePendingLikeLikeEvents();

        runtime.configuration().setActionButtonsHeld(true);
        for (int frame = 2; frame <= 8; frame++) {
            runtime.tick(EntityTestFrames.projectileFrame(
                    runtime,
                    frame,
                    64,
                    64,
                    sequence(0x00),
                    linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN,
                    new EnemyProjectileCollision.LinkState(64, 64, 0, 0, 0, false, 1, 0)));
            runtime.outbox().consumePendingLikeLikeEvents();
        }
        assertEquals(1, new EntityRoomInspectionQueries(runtime.inspection()).likeLikeState(0));
        assertEquals(7, new EntityRoomInspectionQueries(runtime.inspection()).likeLikeInertia(0));

        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                9,
                64,
                64,
                sequence(0x00),
                linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN,
                new EnemyProjectileCollision.LinkState(64, 64, 0, 0, 0, false, 1, 0)));
        LikeLikeEvent release = runtime.outbox().consumePendingLikeLikeEvents().getFirst();
        assertEquals(LikeLikeEvent.Kind.RELEASE, release.kind());
        assertEquals(0, new EntityRoomInspectionQueries(runtime.inspection()).likeLikeState(0));
        assertEquals(0x15, new EntityRoomInspectionQueries(runtime.inspection()).slowTransitionCountdown(0));
    }
}
