package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class BlooperMotionTest {

    @Test
    void stateZeroAcceleratesTowardSwimmingUpAndZeroHorizontalSpeed() {
        BlooperMotion motion = new BlooperMotion();
        motion.initialize(0);
        motion.setStateForTest(0, 0, 0x05, 0, 0xF0, 0x02);

        BlooperMotion.Movement movement = motion.move(entity(64, 64), 0, null, 0);
        BlooperMotion.Update update = motion.finish(
            movement, RoomEntityGroundInteraction.Result.unchanged(movement.entity(), 0x01),
            0, 0x80, 0x80);

        assertEquals(0xF1, motion.speedX(0));
        assertEquals(0x03, motion.speedY(0));
        assertEquals(0x05, motion.transitionCountdown(0));
        assertEquals(0, motion.state(0));
        assertEquals(0, update.entity().spriteVariant());
    }

    @Test
    void stateZeroStartsTheRomChaseOnlyWhenLinkIsAbove() {
        BlooperMotion motion = new BlooperMotion();
        motion.initialize(0);
        motion.setStateForTest(0, 0, 0, 0, 0x08, 0xF8);

        BlooperMotion.Movement movement = motion.move(entity(64, 64), 0, null, 0);
        BlooperMotion.Update update = motion.finish(
            movement, RoomEntityGroundInteraction.Result.unchanged(movement.entity(), 0x01),
            0, 48, 40);

        assertEquals(1, motion.state(0));
        assertEquals(0x25, motion.transitionCountdown(0));
        assertEquals(1, motion.direction(0));
        assertEquals(0, motion.speedX(0));
        assertEquals(0, motion.speedY(0));
        assertEquals(0, update.entity().spriteVariant());
    }

    @Test
    void stateOneChangesSpeedOnlyOnEvenFramesAndUsesHorizontalDirection() {
        BlooperMotion motion = new BlooperMotion();
        motion.initialize(0);
        motion.setStateForTest(0, 1, 0x03, 0, 0, 0x04);

        BlooperMotion.Movement evenMovement = motion.move(entity(64, 64), 2, null, 0);
        BlooperMotion.Update even = motion.finish(
            evenMovement, RoomEntityGroundInteraction.Result.unchanged(evenMovement.entity(), 1),
            2, 80, 80);
        assertEquals(1, motion.speedX(0));
        assertEquals(0x03, motion.speedY(0));
        assertEquals(1, even.entity().spriteVariant());

        BlooperMotion.Movement oddMovement = motion.move(even.entity(), 3, null, 0);
        BlooperMotion.Update odd = motion.finish(
            oddMovement, RoomEntityGroundInteraction.Result.unchanged(oddMovement.entity(), 1),
            3, 80, 80);
        assertEquals(1, motion.speedX(0));
        assertEquals(0x03, motion.speedY(0));
        assertEquals(1, odd.entity().spriteVariant());
    }

    @Test
    void stateOneReturnsToStateZeroAtTheRomCountdownBoundary() {
        BlooperMotion motion = new BlooperMotion();
        motion.initialize(0);
        motion.setStateForTest(0, 1, 0, 1, 0, 0);

        BlooperMotion.Movement movement = motion.move(entity(64, 64), 0, null, 0);
        BlooperMotion.Update update = motion.finish(
            movement, RoomEntityGroundInteraction.Result.unchanged(movement.entity(), 1),
            0, 64, 64);

        assertEquals(0, motion.state(0));
        assertEquals(0x40, motion.transitionCountdown(0));
        assertEquals(0, update.entity().spriteVariant());
    }

    @Test
    void zeroGroundStatusRestoresThePreMovePositionAndStartsTheRomCooldown() {
        BlooperMotion motion = new BlooperMotion();
        motion.initialize(0);
        motion.setStateForTest(0, 0, 1, 0, 0x10, 0);

        RoomEntity initial = entity(64, 64);
        BlooperMotion.Movement movement = motion.move(initial, 0, null, 0);
        assertEquals(65, movement.entity().x());

        BlooperMotion.Update update = motion.finish(
            movement, RoomEntityGroundInteraction.Result.unchanged(movement.entity(), 0),
            0, 64, 64);

        assertEquals(64, update.entity().x());
        assertEquals(64, update.entity().y());
        assertEquals(0x10, motion.privateCountdown3(0));
    }

    private static RoomEntity entity(int x, int y) {
        return new RoomEntity(0, 0, 0xA9, x, y, EntityStatus.ACTIVE,
            EntitySpriteDefinition.unsupported(0xA9), -1);
    }
}
