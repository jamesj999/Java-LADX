package linksawakening.world.entity.motion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.EntityBackgroundCollisionResult;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.entity.RoomEntityBuilder;
import org.junit.jupiter.api.Test;

final class RoamingEnemyMotionTest {

    @Test
    void pausedEnemyDoesNotProbeWallsOrStopAgainAfterChoosingDirection() {
        var motion = new RoamingEnemyMotion();
        var source = entity(0, 0x0B, 0x40, 0x40);
        MotionTestFixture.setStateForTest(motion, 0, 1, 3, 0, 1, 0);
        motion.speedX()[0] = 0;
        for (int frame = 0; frame < 2; frame++) {
            motion.advance(source, 0x50, 0x40, () -> 0, (e, d, x, y) -> {
                throw new AssertionError("zero-speed wall probe");
            });
            assertEquals(0, motion.collisionFlags(0));
        }
        motion.advance(source, 0x50, 0x40, () -> 0, linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN);
        assertEquals(0, motion.state(0));
        motion.advance(source, 0x50, 0x40, () -> 0, linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN);
        assertEquals(0, motion.state(0));
    }

    @Test
    void postRecoilPauseChecksTheRetainedSpeedAtTheCurrentPosition() {
        var motion = new RoamingEnemyMotion();
        var source = entity(1, 0x09, 0x5B, 0x17);
        MotionTestFixture.setStateForTest(motion, 1, 1, 0x40, 0, 0, 2);
        var update = motion.advance(source, 0x40, 0x18, () -> 0, (e, direction, x, y) -> {
            assertEquals(2, direction);
            assertEquals(0x5B, x);
            assertEquals(0x17, y);
            return true;
        });
        assertEquals(source, update);
        assertEquals(0x3F, motion.transitionCountdown(1));
        assertEquals(4, motion.collisionFlags(1));
    }

    @Test
    void octorokLaunchesAtThePostDecrementCountdownTenWithoutConsumingRandomness() {
        RoamingEnemyMotion motion = new RoamingEnemyMotion();
        RoomEntity source = entity(0, 0x09, 0x40, 0x40);
        MotionTestFixture.setStateForTest(motion, 0, 1, 0x0B, 0, 0, 0);
        AtomicInteger randomCalls = new AtomicInteger();

        RoamingEnemyMotion.Update update = motion.advance(
                source,
                0x50,
                0x40,
                () -> {
                    randomCalls.incrementAndGet();
                    return 0xFF;
                },
                linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN,
                false);

        assertEquals(source, update.entity());
        assertEquals(0x0A, motion.transitionCountdown(0));
        assertNotNull(update.launchRequest());
        assertEquals(0, update.launchRequest().sourceSlot());
        assertEquals(0x09, update.launchRequest().sourceType());
        assertEquals(0x0A, update.launchRequest().projectileType());
        assertEquals(0, randomCalls.get());
    }

    @Test
    void moblinUsesTheTieBreakingDirectionAndRequestsAnArrow() {
        RoamingEnemyMotion motion = new RoamingEnemyMotion();
        RoomEntity source = entity(0, 0x0B, 0x40, 0x40);
        MotionTestFixture.setStateForTest(motion, 0, 1, 0x0B, 0, 0, 3);

        RoamingEnemyMotion.Update update = motion.advance(
                source, 0x48, 0x48, () -> 0, linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN, false);

        assertNotNull(update.launchRequest());
        assertEquals(0x0B, update.launchRequest().sourceType());
        assertEquals(0x0C, update.launchRequest().projectileType());
    }

    @Test
    void launchRequiresTheExactCountdownAfterTheRomDecrement() {
        RoamingEnemyMotion motion = new RoamingEnemyMotion();
        RoomEntity source = entity(0, 0x09, 0x40, 0x40);
        MotionTestFixture.setStateForTest(motion, 0, 1, 0x0A, 0, 0, 0);

        RoamingEnemyMotion.Update update = motion.advance(
                source, 0x50, 0x40, () -> 0, linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN, false);

        assertNull(update.launchRequest());
        assertEquals(0x09, motion.transitionCountdown(0));
    }

    @Test
    void directionCycleDoesNotSuppressShooting() {
        for (int cycle = 0; cycle < 4; cycle++) {
            RoamingEnemyMotion motion = new RoamingEnemyMotion();
            MotionTestFixture.setStateForTest(motion, 0, 1, 0x0B, 0, cycle, 0);
            assertNotNull(motion.advance(
                            entity(0, 0x09, 0x40, 0x40),
                            0x50,
                            0x40,
                            () -> 0,
                            linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN,
                            false)
                    .launchRequest());
        }
    }

    @Test
    void launchRequiresPrivateCountdownOneToBeZero() {
        RoamingEnemyMotion motion = new RoamingEnemyMotion();
        RoomEntity source = entity(0, 0x09, 0x40, 0x40);
        MotionTestFixture.setStateForTest(motion, 0, 1, 0x0B, 0, 1, 0);

        MotionTestFixture.setPrivateCountdown1ForTest(motion, 0, 2);

        RoamingEnemyMotion.Update update = motion.advance(
                source, 0x50, 0x40, () -> 0, linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN, false);

        assertNull(update.launchRequest());
    }

    @Test
    void launchRequiresDirectionToLinkToMatchTheStoredDirection() {
        RoamingEnemyMotion motion = new RoamingEnemyMotion();
        RoomEntity source = entity(0, 0x09, 0x40, 0x40);
        MotionTestFixture.setStateForTest(motion, 0, 1, 0x0B, 0, 0, 1);

        RoamingEnemyMotion.Update update = motion.advance(
                source, 0x50, 0x40, () -> 0, linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN, false);

        assertNull(update.launchRequest());
    }

    @Test
    void ironMaskNeverRequestsAProjectileFromTheSharedHandler() {
        RoamingEnemyMotion motion = new RoamingEnemyMotion();
        RoomEntity source = entity(0, 0x24, 0x40, 0x40);
        MotionTestFixture.setStateForTest(motion, 0, 1, 0x0B, 0, 0, 0);

        RoamingEnemyMotion.Update update = motion.advance(
                source, 0x50, 0x40, () -> 0, linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN, false);

        assertNull(update.launchRequest());
    }

    @Test
    void creditsSuppressOctorokRocksButNotMoblinArrows() {
        RoamingEnemyMotion motion = new RoamingEnemyMotion();
        RoomEntity octorok = entity(0, 0x09, 0x40, 0x40);
        MotionTestFixture.setStateForTest(motion, 0, 1, 0x0B, 0, 0, 0);
        assertNull(motion.advance(
                        octorok,
                        0x50,
                        0x40,
                        () -> 0,
                        linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN,
                        true)
                .launchRequest());

        RoomEntity moblin = entity(0, 0x0B, 0x40, 0x40);
        MotionTestFixture.setStateForTest(motion, 0, 1, 0x0B, 0, 0, 0);
        assertNotNull(motion.advance(
                        moblin,
                        0x50,
                        0x40,
                        () -> 0,
                        linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN,
                        true)
                .launchRequest());
    }

    @Test
    void fractionalStepRecordsTheRomDirectionBitAndIsConsumedOnTheNextDispatch() {
        RoamingEnemyMotion motion = new RoamingEnemyMotion();
        RoomEntity source = entity(0, 0x09, 0x40, 0x40);
        MotionTestFixture.setStateForTest(motion, 0, 0, 0x08, 0, 0, 0);
        AtomicReference<EntityBackgroundCollisionResult> probe = new AtomicReference<>();

        RoomEntityBackgroundInteraction interaction = (entity, direction, nextX, nextY) -> {
            EntityBackgroundCollisionResult result =
                    EntityBackgroundCollisionResult.blocked(direction, 0x22, 0x01, nextX, nextY);
            probe.set(result);
            return result;
        };

        RoamingEnemyMotion.Update blocked = motion.advanceWithInteraction(
                source,
                0x50,
                0x40,
                () -> {
                    throw new AssertionError("wall detection must defer the state transition");
                },
                interaction,
                false);

        assertEquals(source.x(), blocked.entity().x());
        assertEquals(0x01, motion.collisionFlags(0));
        assertEquals(0x22, motion.horizontallyCollidedObject(0));
        assertEquals(0x08, motion.speedX(0));
        assertEquals(0, motion.state(0));
        assertEquals(0x01, probe.get().collisionFlag());
        assertEquals(source.x(), probe.get().sampleX());

        RoamingEnemyMotion.Update consumed =
                motion.advanceWithInteraction(source, 0x50, 0x40, () -> 0x0A, interaction, false);

        assertEquals(1, motion.state(0));
        assertEquals(0x1A, motion.transitionCountdown(0));
        assertEquals(0, motion.collisionFlags(0));
        assertEquals(0, motion.speedX(0));
        assertEquals(source.x(), consumed.entity().x());
        assertEquals(source.y(), consumed.entity().y());
    }

    @Test
    void stateAwareRichCollisionReceivesTheRoamingIgnoreHitsCountdown() {
        RoamingEnemyMotion motion = new RoamingEnemyMotion();
        RoomEntity source = entity(0, 0x09, 0x40, 0x40);
        MotionTestFixture.setStateForTest(motion, 0, 0, 0x08, 0, 0, 0);
        AtomicInteger observedIgnoreHits = new AtomicInteger(-1);
        AtomicInteger observedFrame = new AtomicInteger(-1);

        RoomEntityBackgroundInteraction interaction = new RoomEntityBackgroundInteraction() {
            @Override
            public EntityBackgroundCollisionResult probe(RoomEntity entity, int direction, int nextX, int nextY) {
                return EntityBackgroundCollisionResult.passable(direction, 0, nextX, nextY);
            }

            @Override
            public EntityBackgroundCollisionResult probe(
                    RoomEntity entity, int direction, int nextX, int nextY, int ignoreHitsCountdown) {
                observedIgnoreHits.set(ignoreHitsCountdown);
                return EntityBackgroundCollisionResult.blocked(direction, 0x22, 0x01, nextX, nextY);
            }

            @Override
            public EntityBackgroundCollisionResult probe(
                    RoomEntity entity, int direction, int nextX, int nextY, int ignoreHitsCountdown, int frameCounter) {
                observedIgnoreHits.set(ignoreHitsCountdown);
                observedFrame.set(frameCounter);
                return EntityBackgroundCollisionResult.blocked(direction, 0x22, 0x01, nextX, nextY);
            }
        };

        RoamingEnemyMotion.Update blocked =
                motion.advanceWithInteraction(source, 0x50, 0x40, () -> 0x0A, interaction, 0x0A, 0x15, false);

        assertEquals(0x0A, observedIgnoreHits.get());
        assertEquals(0x15, observedFrame.get());
        assertEquals(source.x(), blocked.entity().x());
        assertEquals(0x01, motion.collisionFlags(0));
    }

    private static RoomEntity entity(int slot, int type, int x, int y) {
        return new RoomEntityBuilder()
                .slot(slot)
                .sourceLoadOrder(0)
                .type(type)
                .at(x, y)
                .spriteDefinition(EntitySpriteDefinition.unsupported(type))
                .spriteVariant(-1)
                .build();
    }
}
