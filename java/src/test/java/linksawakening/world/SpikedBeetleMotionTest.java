package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class SpikedBeetleMotionTest {
    @Test
    void flippedBeetleShakesThenRecoversAndResumesWalking() {
        SpikedBeetleMotion motion = new SpikedBeetleMotion();
        RoomEntity entity = new RoomEntity(0, 0, 0x2C, 64, 64,
            EntityStatus.ACTIVE, EntitySpriteDefinition.unsupported(0x2C), 0);
        motion.flipFromShield(0, 0);
        var leftShake = motion.advance(entity, 0, 120, 120, () -> 0, null, 0x58, 0);
        assertEquals(0xF8, motion.speedX(0)); // $07:791A-$792A
        var rightShake = motion.advance(leftShake.entity(), 1, 120, 120,
            () -> 0, null, 0x54, 0);
        assertEquals(0x08, motion.speedX(0));
        var recovery = motion.advance(rightShake.entity(), 2, 120, 120,
            () -> 0, null, 0, 0);
        assertEquals(0, motion.state(0)); // $07:790D writes zero, not state 4
        assertEquals(0x18, motion.speedZ(0));
        assertEquals(0, motion.speedX(0));
        // The recovery frame still executes the flipped handler's flag writes.
        assertEquals(0x08, recovery.options1());
        assertEquals(0, recovery.hitboxFlags());
        var walking = motion.advance(recovery.entity(), 3, 120, 120,
            () -> 0, null, 0, 0);
        assertEquals(1, motion.state(0));
        assertEquals(0x30, walking.transitionCountdown());
        assertEquals(0x48, walking.options1());
        assertEquals(0x80, walking.hitboxFlags());
    }
}
