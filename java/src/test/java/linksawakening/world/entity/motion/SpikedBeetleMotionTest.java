package linksawakening.world.entity.motion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;
import linksawakening.world.entity.RoomEntityBuilder;
import org.junit.jupiter.api.Test;

final class SpikedBeetleMotionTest {
    @Test
    void flippedBeetleShakesThenRecoversAndResumesWalking() {
        SpikedBeetleMotion motion = new SpikedBeetleMotion();
        RoomEntity entity = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x2C)
                .at(64, 64)
                .spriteDefinition(EntitySpriteDefinition.unsupported(0x2C))
                .build();
        motion.flipFromShield(0, 0);
        entity = motion.advance(entity, 0, 120, 120, () -> 0, RoomEntityBackgroundCollision.OPEN, 0xFF, 0)
                .entity();
        var leftShake = motion.advance(
                entity, 0, 120, 120, () -> 0, linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN, 0x58, 0);
        assertEquals(0xF8, motion.speedX(0)); // $07:791A-$792A
        var rightShake = motion.advance(
                leftShake.entity(),
                1,
                120,
                120,
                () -> 0,
                linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN,
                0x54,
                0);
        assertEquals(0x08, motion.speedX(0));
        var recovery = motion.advance(
                rightShake.entity(),
                2,
                120,
                120,
                () -> 0,
                linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN,
                0,
                0);
        assertEquals(0, motion.state(0)); // $07:790D writes zero, not state 4
        assertEquals(0x18, motion.speedZ(0));
        assertEquals(0, motion.speedX(0));
        // The recovery frame still executes the flipped handler's flag writes.
        assertEquals(0x08, recovery.options1());
        assertEquals(0, recovery.hitboxFlags());
        var walking = motion.advance(
                recovery.entity(),
                3,
                120,
                120,
                () -> 0,
                linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN,
                0,
                0);
        assertEquals(1, motion.state(0));
        assertEquals(0x30, walking.transitionCountdown());
        assertEquals(0x48, walking.options1());
        assertEquals(0x80, walking.hitboxFlags());
    }

    @Test
    void shieldFlipRetainsTheMotionFractionsWhenTheOldDashHandlerRuns() {
        var motion = new SpikedBeetleMotion();
        RoomEntity beetle = new RoomEntityBuilder()
                .type(0x2C)
                .at(88, 75)
                .spriteDefinition(EntitySpriteDefinition.unsupported(0x2C))
                .build();
        int[] draws = {0, 6, 0};
        var draw = new AtomicInteger();
        beetle = motion.advance(
                        beetle,
                        0,
                        115,
                        40,
                        () -> draws[draw.getAndIncrement()],
                        RoomEntityBackgroundCollision.OPEN,
                        0,
                        0)
                .entity();
        for (int frame = 1; frame <= 5; frame++) {
            beetle = motion.advance(beetle, frame, 115, 40, () -> 0, RoomEntityBackgroundCollision.OPEN, 48 - frame, 0)
                    .entity();
        }
        assertEquals(89, beetle.x());

        motion.startDash(0, 130, 75, 89, 75);
        motion.flipFromShield(0, 1);
        var flipped = motion.advance(beetle, 6, 103, 80, () -> 0, RoomEntityBackgroundCollision.OPEN, 0xFE, 0);
        assertEquals(88, flipped.entity().x());
        assertEquals(0xFF, flipped.transitionCountdown());
        assertEquals(0x48, flipped.options1());
        assertEquals(0xF1, motion.speedX(0));

        flipped = motion.advance(flipped.entity(), 7, 104, 80, () -> 0, RoomEntityBackgroundCollision.OPEN, 0xFE, 0);
        assertEquals(87, flipped.entity().x());
        assertEquals(8, flipped.options1());
        flipped = motion.advance(flipped.entity(), 8, 105, 80, () -> 0, RoomEntityBackgroundCollision.OPEN, 0xFD, 0);
        assertEquals(87, flipped.entity().x());
    }
}
