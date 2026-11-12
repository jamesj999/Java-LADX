package linksawakening.world.entity.motion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.RoomEntity;
import org.junit.jupiter.api.Test;

final class CuccoMotionTest {

    @Test
    void choosesTheRomRandomFlightVectorAndCountdown() {
        CuccoMotion motion = new CuccoMotion();
        RoomEntity entity = entity(0x40, 0x50, 0);
        AtomicInteger randomIndex = new AtomicInteger();
        int[] random = {0x02, 0x01, 0x01};

        CuccoMotion.Update update = motion.advance(
                entity,
                0,
                0,
                0,
                0x40,
                0x50,
                0,
                true,
                false,
                false,
                () -> random[randomIndex.getAndIncrement()],
                null,
                false,
                false,
                0x04);

        assertEquals(1, motion.state(0));
        assertEquals(0x06, motion.speedX(0));
        assertEquals(0x04, motion.speedY(0));
        assertEquals(0x00, motion.direction(0));
        assertEquals(0x31, update.transitionCountdown());
        assertEquals(2, update.entity().spriteVariant());
        assertFalse(update.liftRequested());
    }

    @Test
    void expiredWalkReturnsToIdleInsteadOfFleeing() {
        CuccoMotion motion = new CuccoMotion();
        linksawakening.world.entity.motion.MotionTestFixture.setStateForTest(motion, 0, 1, 0, 0, 0, 0, 0x80);

        CuccoMotion.Update update = motion.advance(
                entity(0x40, 0x50, 0), 0, 0, 0, 0x40, 0x50, 0, true, false, false, () -> 0, null, false, false, 0x04);

        assertEquals(0, motion.state(0));
        assertEquals(0x30, update.transitionCountdown());
        assertEquals(0, update.entity().z());
        assertEquals(2, update.entity().spriteVariant());
    }

    @Test
    void homingStateUsesTheRomVectorAndUpdatesDirection() {
        CuccoMotion motion = new CuccoMotion();
        linksawakening.world.entity.motion.MotionTestFixture.setStateForTest(motion, 0, 2, 0, 0, 0, 0, 0);

        CuccoMotion.Update update = motion.advance(
                entity(0x40, 0x50, 0), 0, 0, 0, 0x30, 0x50, 0, true, false, false, () -> 0, null, false, false, 0x04);

        assertEquals(0x0C, motion.speedX(0));
        assertEquals(0x00, motion.speedY(0));
        assertEquals(0x00, motion.direction(0));
        assertEquals(0x02, update.entity().spriteVariant());
    }

    @Test
    void powerBraceletCollisionRequestsTheCuccoLiftBeforeItsStateHandler() {
        CuccoMotion motion = new CuccoMotion();
        linksawakening.world.entity.motion.MotionTestFixture.setStateForTest(motion, 0, 0, 0x20, 0, 0, 0, 0);

        CuccoMotion.Update update = motion.advance(
                entity(0x40, 0x50, 0),
                0,
                0x20,
                0,
                0x40,
                0x50,
                0,
                true,
                true,
                false,
                () -> {
                    throw new AssertionError("Lift must return before random flight selection");
                },
                null,
                false,
                false,
                0x04);

        assertTrue(update.liftRequested());
        assertEquals(0, motion.state(0));
        assertEquals(0x20, update.transitionCountdown());
    }

    @Test
    void angryStateMovesOutwardAndUnloadsAtTheRomBoundary() {
        CuccoMotion motion = new CuccoMotion();
        motion.initializeAngry(0, 0x10, 0);

        CuccoMotion.Update update = motion.advance(
                entity(0xA8, 0x50, 0), 0, 0, 0, 0x40, 0x50, 0, true, false, false, () -> 0, null, false, false, 0x04);

        assertTrue(update.unloaded());
        assertTrue(update.boomerangSound());
        assertEquals(0x02, update.entity().spriteVariant());
    }

    @Test
    void marinFlashPathRaisesDialogAndCanRequestAnAngrySpawn() {
        CuccoMotion motion = new CuccoMotion();
        linksawakening.world.entity.motion.MotionTestFixture.setPrivateStateForTest(motion, 0, 0x22);
        CuccoMotion.Update dialog = motion.advance(
                entity(0x40, 0x50, 0), 1, 0, 8, 0x40, 0x50, 0, true, false, false, () -> 0, null, false, true, 0x04);

        assertEquals(0x23, motion.privateState1(0));
        assertEquals(2, motion.state(0));
        assertEquals(2, dialog.dialogTableId());
        assertEquals(0x76, dialog.dialogLowId());

        CuccoMotion spawnMotion = new CuccoMotion();
        linksawakening.world.entity.motion.MotionTestFixture.setStateForTest(spawnMotion, 0, 2, 0, 0x23, 0, 0, 0);
        CuccoMotion.Update spawn = spawnMotion.advance(
                entity(0x40, 0x50, 0), 0, 1, 0, 0x60, 0x50, 0, true, false, false, () -> 0, null, false, false, 0x04);

        assertNotNull(spawn.spawnRequest());
        assertEquals(0x28, spawn.spawnRequest().x());
        assertEquals(0x00, spawn.spawnRequest().y());
        assertEquals(0x10, spawn.spawnRequest().z());
        assertEquals(0x13, spawn.waveSoundId());
    }

    @Test
    void attackStepWindowBlocksPowerBraceletLift() {
        CuccoMotion motion = new CuccoMotion();
        linksawakening.world.entity.motion.MotionTestFixture.setStateForTest(motion, 0, 0, 0x20, 0, 0, 0, 0);

        CuccoMotion.Update update = motion.advance(
                entity(0x40, 0x50, 0),
                0,
                0x20,
                0,
                0x40,
                0x50,
                0,
                true,
                true,
                true,
                () -> {
                    throw new AssertionError("Attack-step guard must return before random flight selection");
                },
                null,
                false,
                false,
                0x04);

        assertFalse(update.liftRequested());
        assertEquals(0, motion.state(0));
    }

    @Test
    void walkingFacingFollowsVelocityWithoutStoringTheRenderFlip() {
        for (int randomX : new int[] {2, 6}) {
            CuccoMotion motion = new CuccoMotion();
            RoomEntity chicken = entity(0x40, 0x50, 0);
            motion.advance(chicken, 0, 0, 0, 0, 0, 0, true, false, false, () -> randomX, null, false, false, 4);
            for (int frame = 1; frame <= 16; frame++) {
                chicken = motion.advance(
                                chicken,
                                frame,
                                20,
                                0,
                                0,
                                0,
                                0,
                                true,
                                false,
                                false,
                                () -> {
                                    throw new AssertionError("No RNG during walk");
                                },
                                null,
                                false,
                                false,
                                4)
                        .entity();
                assertEquals(randomX == 2 ? 2 : 0, chicken.spriteVariant() & 2);
            }
            assertTrue(randomX == 2 ? chicken.x() > 0x40 : chicken.x() < 0x40);
        }
    }

    @Test
    void fullAngrySpawnRangeConsumesNoRandomByte() {
        CuccoMotion motion = new CuccoMotion();
        linksawakening.world.entity.motion.MotionTestFixture.setStateForTest(motion, 0, 2, 0, 0x23, 0, 0, 0);
        var result = motion.advance(
                entity(0x40, 0x50, 0),
                0,
                1,
                0,
                0x60,
                0x50,
                0,
                true,
                false,
                false,
                () -> {
                    throw new AssertionError("Spawn failure precedes GetRandomByte at $05:472B");
                },
                null,
                false,
                false,
                4,
                false);
        org.junit.jupiter.api.Assertions.assertNull(result.spawnRequest());
    }

    private static RoomEntity entity(int x, int y, int variant) {
        return new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(CuccoMotion.ENTITY_TYPE)
                .at(x, y)
                .spriteDefinition(definition())
                .spriteVariant(variant)
                .build();
    }

    private static EntitySpriteDefinition definition() {
        EntitySpriteDefinition.OamAttribute first = new EntitySpriteDefinition.OamAttribute(0xFF, 0);
        EntitySpriteDefinition.OamAttribute second = new EntitySpriteDefinition.OamAttribute(0xFF, 0);
        return new EntitySpriteDefinition(
                CuccoMotion.ENTITY_TYPE,
                0x05,
                0x4514,
                EntitySpriteDefinition.Shape.PAIR,
                0,
                List.of(
                        new EntitySpriteDefinition.Variant(first, second),
                        new EntitySpriteDefinition.Variant(first, second),
                        new EntitySpriteDefinition.Variant(first, second),
                        new EntitySpriteDefinition.Variant(first, second)));
    }
}
