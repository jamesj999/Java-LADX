package linksawakening.world.entity.motion;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SlimeEyeHalfMotionTest {
    @Test void riseUsesUnsignedByteThresholdAndPreservesVariant() {
        var below = SlimeEyeHalfMotion.rise(0x6F, 0, 1, 0, 40, 50);
        assertEquals(2, below.state());
        assertEquals(0x60, below.speedZ());
        assertEquals(1, below.spriteVariant());
        var reached = SlimeEyeHalfMotion.rise(0x70, 0, 1, 0, 40, 50);
        assertEquals(3, reached.state());
        assertEquals(0x30, reached.countdown());
        assertEquals(2, SlimeEyeHalfMotion.rise(0x100, 0, 1, 0, 40, 50).state());
    }

    @Test void waitHidesWithoutTrackingUntilCountdownExpires() {
        var waiting = SlimeEyeHalfMotion.waitAbove(1, 0x60, 0, 40, 50, 80, 90);
        assertEquals(3, waiting.state());
        assertEquals(40, waiting.x());
        assertEquals(50, waiting.y());
        assertEquals(0xFF, waiting.spriteVariant());
        assertEquals(-1, waiting.jingle());
        var drop = SlimeEyeHalfMotion.waitAbove(0, 0x60, 0, 40, 50, 80, 90);
        assertEquals(1, drop.state());
        assertEquals(0x18, drop.countdown());
        assertEquals(0xC0, drop.speedZ());
        assertEquals(1, drop.privateState4());
        assertEquals(80, drop.x());
        assertEquals(90, drop.y());
        assertEquals(8, drop.jingle());
    }
}
