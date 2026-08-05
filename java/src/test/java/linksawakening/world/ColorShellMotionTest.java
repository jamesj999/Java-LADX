package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ColorShellMotionTest {

    @Test
    void stateZeroConsumesRomRandomDirectionAndStartsTheWalkingCountdown() {
        ColorShellMotion motion = new ColorShellMotion();
        AtomicInteger randomCalls = new AtomicInteger();
        RoomEntity entity = entity(0x40, 0x40);
        motion.initialize(0);

        ColorShellMotion.Update update = motion.advance(entity, 0, 0x00, 0x00,
            () -> {
                randomCalls.incrementAndGet();
                return 0x06;
            }, null);

        assertEquals(1, randomCalls.get());
        assertEquals(1, motion.state(0));
        assertEquals(3, motion.direction(0));
        assertEquals(0x40, motion.transitionCountdown(0));
        assertEquals(0x40, update.entity().x());
        assertEquals(0x40, update.entity().y());
    }

    @Test
    void stateOneUsesRomDirectionSpeedsAndSignedFixedPointAccumulation() {
        ColorShellMotion motion = new ColorShellMotion();
        RoomEntity entity = entity(0x40, 0x40);
        motion.setStateForTest(0, 1, 0x20, 0, 0, 0);

        ColorShellMotion.Update update = new ColorShellMotion.Update(entity);
        for (int frame = 0; frame < 6; frame++) {
            update = motion.advance(update.entity(), frame, 0x00, 0x00, () -> 0, null);
        }

        assertEquals(0x03, motion.speedX(0));
        assertEquals(0x00, motion.speedY(0));
        assertEquals(0x41, update.entity().x());
        assertEquals(0x40, update.entity().y());
        assertEquals(0x1A, motion.transitionCountdown(0));
    }

    @Test
    void stateThreeClampsBothAxesToTheBankThirtySixBounds() {
        ColorShellMotion motion = new ColorShellMotion();
        RoomEntity entity = entity(0x16, 0x1E);
        motion.setStateForTest(0, 3, 0x20, 0, 0xF0, 0xF0);

        ColorShellMotion.Update update = motion.advance(entity, 0, 0, 0, () -> 0, null);

        assertEquals(0x16, update.entity().x());
        assertEquals(0x1E, update.entity().y());

        motion.setStateForTest(0, 3, 0x20, 0, 0x10, 0x10);
        update = motion.advance(entity(0x89, 0x72), 1, 0, 0, () -> 0, null);

        assertEquals(0x89, update.entity().x());
        assertEquals(0x72, update.entity().y());
    }

    @Test
    void walkingStateChargesAtTheTighterLinkProximityWindow() {
        ColorShellMotion motion = new ColorShellMotion();
        motion.setStateForTest(0, 1, 0, 0, 0, 0);

        ColorShellMotion.Update update = motion.advance(entity(0x40, 0x40), 0,
            0x4F, 0x4F, () -> 0, null);

        assertEquals(2, motion.state(0));
        assertEquals(0x20, motion.transitionCountdown(0));
        assertEquals(0x0E, Math.max(Math.abs(signed(motion.speedX(0))),
            Math.abs(signed(motion.speedY(0)))));
    }

    @Test
    void stateTwoStartsLandingLoopAndTogglesOnlyOnEvenFrames() {
        ColorShellMotion motion = new ColorShellMotion();
        motion.setStateForTest(0, 2, 0, 2, 0, 0);

        ColorShellMotion.Update odd = motion.advance(entity(0x40, 0x40), 1,
            0, 0, () -> 0, null);
        assertEquals(3, motion.state(0));
        assertEquals(0x18, motion.transitionCountdown(0));
        assertEquals(0, odd.entity().spriteVariant());

        ColorShellMotion.Update even = motion.advance(odd.entity(), 2,
            0, 0, () -> 0, null);
        assertEquals(1, even.entity().spriteVariant());
    }

    @Test
    void stateThreeRestartsWalkingWithZeroSpeedAtCountdownBoundary() {
        ColorShellMotion motion = new ColorShellMotion();
        motion.setStateForTest(0, 3, 0, 1, 0x10, 0xF0);

        ColorShellMotion.Update update = motion.advance(entity(0x40, 0x40), 0,
            0, 0, () -> 0, null);

        assertEquals(1, motion.state(0));
        assertEquals(0, motion.speedX(0));
        assertEquals(0, motion.speedY(0));
    }

    @Test
    void clearResetsEveryStateOneField() {
        ColorShellMotion motion = new ColorShellMotion();
        motion.setStateForTest(0, 3, 0x20, 2, 0x10, 0xF0);
        motion.clear(0);

        assertEquals(0, motion.state(0));
        assertEquals(0, motion.transitionCountdown(0));
        assertEquals(0, motion.direction(0));
        assertEquals(0, motion.speedX(0));
        assertEquals(0, motion.speedY(0));
        assertEquals(0, motion.speedZ(0));
        assertEquals(0, motion.spriteVariant(0));
    }

    private static RoomEntity entity(int x, int y) {
        return new RoomEntity(0, 0, 0xE9, x, y, EntityStatus.ACTIVE,
            EntitySpriteDefinition.unsupported(0xE9), -1);
    }

    private static int signed(int value) {
        int byteValue = value & 0xFF;
        return byteValue < 0x80 ? byteValue : byteValue - 0x100;
    }
}
