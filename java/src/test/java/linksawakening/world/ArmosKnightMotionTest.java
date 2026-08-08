package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class ArmosKnightMotionTest {

    @Test
    void healthThresholdsRequestTheRomRubbleBurstsOnlyOncePerBand() {
        ArmosKnightMotion motion = new ArmosKnightMotion();
        RoomEntity entity = entity(0x50, 0x60, 0x08);

        ArmosKnightMotion.Update initial = motion.advance(entity, 0x50, 0x60, 0,
            0, 0, 0, 0xC4, 0x0C);
        assertNull(initial.rubbleRequest());

        ArmosKnightMotion.Update upperBand = motion.advance(initial.entity(),
            0x50, 0x60, 0, 0, 0, 0, 0xC4, 0x07);
        assertEquals(new ArmosKnightMotion.RubbleRequest(0x57, 0x48),
            upperBand.rubbleRequest());
        assertEquals(1, motion.inertia(0));

        ArmosKnightMotion.Update repeatedUpperBand = motion.advance(upperBand.entity(),
            0x50, 0x60, 0, 0, 0, 0, 0xC4, 0x06);
        assertNull(repeatedUpperBand.rubbleRequest());
        assertEquals(1, motion.inertia(0));

        ArmosKnightMotion.Update lowerBand = motion.advance(repeatedUpperBand.entity(),
            0x50, 0x60, 0, 0, 0, 0, 0xC4, 0x03);
        assertEquals(new ArmosKnightMotion.RubbleRequest(0x4F, 0x48),
            lowerBand.rubbleRequest());
        assertEquals(2, motion.inertia(0));

        ArmosKnightMotion.Update repeatedLowerBand = motion.advance(lowerBand.entity(),
            0x50, 0x60, 0, 0, 0, 0, 0xC4, 0x02);
        assertNull(repeatedLowerBand.rubbleRequest());
        assertEquals(2, motion.inertia(0));
    }

    private static RoomEntity entity(int x, int y, int z) {
        return new RoomEntity(0, 0, 0x88, x, y, EntityStatus.ACTIVE,
            EntitySpriteDefinition.unsupported(0x88), 0, 0, 0, z);
    }
}
